package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.os.FileObserver
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/** Additive controller overrides; stable cartridge identity never changes saves or database rows. */
class ControllerProfileStore(private val root: File) {
    constructor(context: Context) : this(File(context.filesDir, "cartdeck/controller-profiles-v1"))

    @Serializable
    data class Profile(
        val schema: Int = 1,
        val buttons: Map<Int, Int>? = null,
        val deadzone: Float = 0f,
        val centres: Map<Int, Float> = emptyMap(),
        val rightX: Int? = null,
        val rightY: Int? = null,
        val calibrated: Boolean = false,
        val playerPort: Int? = null,
        val menuKeys: Set<Int>? = null,
    )

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private fun file(
        descriptor: String,
        cartridge: String?,
    ): File {
        require(descriptor.isNotBlank())
        require(cartridge == null || cartridge.isNotBlank())
        val identity = listOf(descriptor, cartridge ?: "device").joinToString("\u0000")
        val hash =
            MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
                .joinToString("") { "%02x".format(it) }
        return File(root, "$hash.json")
    }

    private fun <T> locked(block: () -> T): T =
        synchronized(lock) {
            check(root.isDirectory || root.mkdirs())
            RandomAccessFile(File(root, "profiles.lock"), "rw").channel.use { channel ->
                channel.lock().use { block() }
            }
        }

    private fun readFile(
        descriptor: String,
        cartridge: String?,
    ): Profile? {
        val target = file(descriptor, cartridge)
        if (!target.exists() && !File(target.path + ".bak").exists()) return null
        val profile = AtomicFile(target).openRead().use { json.decodeFromString<Profile>(it.reader().readText()) }
        check(profile.schema == 1) { "Newer controller profile retained" }
        require(profile.deadzone.isFinite() && profile.deadzone in 0f..0.5f)
        require(profile.centres.values.all { it.isFinite() })
        return profile
    }

    fun read(
        descriptor: String,
        cartridge: String? = null,
    ): Profile =
        locked {
            val device = readFile(descriptor, null) ?: Profile()
            val override = cartridge?.let { readFile(descriptor, it) }
            if (override == null) device else device.copy(buttons = override.buttons ?: device.buttons)
        }

    fun hasOverride(
        descriptor: String,
        cartridge: String,
    ): Boolean =
        locked {
            readFile(descriptor, cartridge)?.buttons != null
        }

    fun save(
        descriptor: String,
        cartridge: String?,
        profile: Profile,
    ) = locked {
        require(profile.schema == 1 && profile.deadzone.isFinite() && profile.deadzone in 0f..0.5f)
        require(profile.centres.values.all { it.isFinite() })
        readFile(descriptor, cartridge) // Preserve unreadable/newer documents.
        val target = file(descriptor, cartridge)
        val atomic = AtomicFile(target)
        val old = if (target.exists()) atomic.openRead().use { it.readBytes() } else null
        val fields = old?.let { json.parseToJsonElement(it.decodeToString()).jsonObject }.orEmpty()
        val next = json.parseToJsonElement(json.encodeToString(profile)).jsonObject
        val encoded = json.encodeToString(JsonObject(fields + next)).toByteArray()
        if (old != null) {
            val backup = AtomicFile(File(target.path + ".previous"))
            val previous = backup.startWrite()
            try {
                previous.write(old)
                backup.finishWrite(previous)
            } catch (
                error: Throwable,
            ) {
                backup.failWrite(previous)
                throw error
            }
        }
        val output = atomic.startWrite()
        try {
            output.write(encoded)
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
    }

    fun useDevice(
        descriptor: String,
        cartridge: String,
    ) = locked {
        readFile(descriptor, cartridge)
        AtomicFile(file(descriptor, cartridge)).delete()
    }

    fun rememberPrevious(
        descriptor: String,
        cartridge: String?,
        buttons: Map<Int, Int>,
    ) = locked {
        readFile(descriptor, cartridge)
        val target = AtomicFile(File(file(descriptor, cartridge).path + ".previous"))
        val output = target.startWrite()
        try {
            output.write(json.encodeToString(Profile(buttons = buttons)).toByteArray())
            target.finishWrite(output)
        } catch (error: Throwable) {
            target.failWrite(output)
            throw error
        }
    }

    fun restorePrevious(
        descriptor: String,
        cartridge: String?,
    ) {
        val previous =
            locked {
                val target = File(file(descriptor, cartridge).path + ".previous")
                AtomicFile(target).openRead().use { json.decodeFromString<Profile>(it.reader().readText()) }
            }
        val current = read(descriptor, cartridge)
        save(descriptor, cartridge, current.copy(buttons = previous.buttons))
    }

    fun resetMappings() =
        locked {
            val documents =
                root.listFiles().orEmpty().filter { it.extension == "json" }.map { target ->
                    val parsed =
                        AtomicFile(target).openRead().use {
                            json.parseToJsonElement(it.reader().readText()).jsonObject
                        }
                    check(parsed["schema"]?.jsonPrimitive?.int == 1) { "Newer controller profile retained" }
                    target to parsed
                }
            documents.forEach { (target, parsed) ->
                val fields = parsed.toMutableMap()
                fields["buttons"] = JsonNull
                fields["menuKeys"] = JsonNull
                val atomic = AtomicFile(target)
                val output = atomic.startWrite()
                try {
                    output.write(json.encodeToString(JsonObject(fields)).toByteArray())
                    atomic.finishWrite(output)
                } catch (
                    error: Throwable,
                ) {
                    atomic.failWrite(output)
                    throw error
                }
            }
        }

    @Suppress("DEPRECATION")
    fun changes() =
        callbackFlow {
            check(root.isDirectory || root.mkdirs())
            val observer =
                object : FileObserver(root.path, CLOSE_WRITE or MOVED_TO or DELETE) {
                    override fun onEvent(
                        event: Int,
                        path: String?,
                    ) {
                        if (path?.contains(".json") == true) trySend(Unit)
                    }
                }
            observer.startWatching()
            trySend(Unit)
            awaitClose { observer.stopWatching() }
        }.conflate().flowOn(Dispatchers.IO)

    companion object {
        private val lock = Any()
    }
}
