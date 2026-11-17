package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.os.FileObserver
import android.util.AtomicFile
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/** Additive presentation preferences only. No ROM, SaveBinding, profile or database writes. */
class DisplaySettingsStore(
    private val directory: File,
    private val legacy: () -> Choices = { Choices() },
) {
    enum class Border { OFF, STANDARD }

    @Serializable
    data class Choices(
        val landscapeBorder: Border? = null,
        val portraitBorder: Border? = null,
        val alignment: String? = null,
        val filter: String? = null,
        val hd: Boolean? = null,
        val quality: Int? = null,
        val engineOptions: Map<String, String> = emptyMap(),
    ) {
        fun over(parent: Choices) =
            Choices(
                landscapeBorder ?: parent.landscapeBorder,
                portraitBorder ?: parent.portraitBorder,
                alignment ?: parent.alignment,
                filter ?: parent.filter,
                hd ?: parent.hd,
                quality ?: parent.quality,
                parent.engineOptions + engineOptions,
            )

        fun border(landscape: Boolean): Border =
            if (landscape) landscapeBorder ?: Border.OFF else portraitBorder ?: Border.STANDARD
    }

    data class Snapshot(val effective: Choices, val own: Choices, val error: String? = null)

    constructor(context: Context) : this(
        File(context.filesDir, "cartdeck/display-preferences-v1"),
        {
            val prefs = SharedPreferencesHelper.getSharedPreferences(context)
            Choices(
                alignment = prefs.getString(context.getString(R.string.pref_key_viewport_alignment), "center"),
                filter = prefs.getString(context.getString(R.string.pref_key_shader_filter), "auto"),
                hd = prefs.getBoolean(context.getString(R.string.pref_key_hd_mode), false),
                quality = prefs.getInt(context.getString(R.string.pref_key_hd_mode_quality), 1),
            )
        },
    )

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private fun file(cartridge: String?): File {
        require(cartridge == null || cartridge.isNotBlank()) { "A stable cartridge UUID is required" }
        val name =
            cartridge?.let {
                MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { b -> "%02x".format(b) }
            } ?: "global"
        return File(directory, "$name.json")
    }

    private fun <T> locked(block: () -> T): T =
        synchronized(processLock) {
            check(directory.isDirectory || directory.mkdirs())
            RandomAccessFile(File(directory, "preferences.lock"), "rw").channel.use { channel ->
                channel.lock().use { block() }
            }
        }

    private fun document(cartridge: String?): JsonObject {
        val target = file(cartridge)
        if (!target.exists() && !File(target.path + ".bak").exists()) return JsonObject(emptyMap())
        val parsed = AtomicFile(target).openRead().use { json.parseToJsonElement(it.reader().readText()).jsonObject }
        check((parsed["schema"]?.jsonPrimitive?.int ?: 1) == 1) { "Newer display preferences are preserved" }
        return parsed
    }

    private fun decode(document: JsonObject): Choices = json.decodeFromString(json.encodeToString(document))

    fun read(cartridge: String?): Snapshot =
        locked {
            val global = decode(document(null)).over(legacy())
            val own = decode(document(cartridge))
            Snapshot(if (cartridge == null) global else own.over(global), own)
        }

    fun readSafely(cartridge: String?): Snapshot =
        try {
            read(cartridge)
        } catch (error: Exception) {
            Snapshot(Choices(), Choices(), "Display preferences could not be read. Existing preferences were retained.")
        }

    fun set(
        cartridge: String?,
        field: String,
        value: String?,
    ) = locked {
        val allowed =
            when (field) {
                "landscapeBorder", "portraitBorder" -> value == null || value in Border.entries.map { it.name }
                "alignment" -> value == null || value in listOf("top", "center", "bottom")
                "hd" -> value == null || value in listOf("true", "false")
                "quality" -> value == null || value.toIntOrNull()?.let { it in 0..2 } == true
                "filter" -> value == null || value.matches(Regex("[a-zA-Z0-9_-]{1,80}"))
                else -> false
            }
        require(allowed) { "Unsupported display preference" }
        val next = document(cartridge).toMutableMap()
        next["schema"] = JsonPrimitive(1)
        next[field] =
            when {
                value == null -> JsonNull
                field == "hd" -> JsonPrimitive(value.toBooleanStrict())
                field == "quality" -> JsonPrimitive(value.toInt())
                else -> JsonPrimitive(value)
            }
        val target = AtomicFile(file(cartridge))
        val output = target.startWrite()
        try {
            output.write(json.encodeToString(JsonObject(next)).toByteArray())
            target.finishWrite(output)
        } catch (error: Throwable) {
            target.failWrite(output)
            throw error
        }
    }

    fun setEngineOption(
        cartridge: String?,
        namespace: String,
        key: String,
        value: String,
        allowed: Set<String>,
    ) = locked {
        require(namespace.isNotBlank() && key.isNotBlank() && value in allowed)
        require(namespace.length <= 160 && key.length <= 160 && value.length <= 256)
        val document = document(cartridge).toMutableMap()
        val options = document["engineOptions"]?.jsonObject?.toMutableMap() ?: mutableMapOf()
        options["$namespace:$key"] = JsonPrimitive(value)
        document["engineOptions"] = JsonObject(options)
        document["schema"] = JsonPrimitive(1)
        val atomic = AtomicFile(file(cartridge))
        val output = atomic.startWrite()
        try {
            output.write(json.encodeToString(JsonObject(document)).toByteArray())
            atomic.finishWrite(output)
        } catch (
            error: Throwable,
        ) {
            atomic.failWrite(output)
            throw error
        }
    }

    fun useGlobal(cartridge: String) =
        locked {
            document(cartridge) // Never erase an unreadable or newer preference document.
            AtomicFile(file(cartridge)).delete()
        }

    @Suppress("DEPRECATION")
    fun observe(cartridge: String?): Flow<Snapshot> =
        callbackFlow {
            check(directory.isDirectory || directory.mkdirs())
            val observer =
                object : FileObserver(directory.path, CLOSE_WRITE or MOVED_TO or DELETE) {
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
        }.conflate().map { readSafely(cartridge) }.distinctUntilChanged().flowOn(Dispatchers.IO)

    companion object {
        private val processLock = Any()
    }
}
