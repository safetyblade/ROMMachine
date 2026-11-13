package com.swordfish.lemuroid.lib.saves

import android.util.AtomicFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Cartridge-scoped save ownership. Legacy files remain read-only migration sources.
 * Only a completed, verified copy followed by an atomic marker becomes authoritative.
 * Ambiguous legacy save ownership is never inferred from matching names alone.
 *
 * Engine-owned NAND, HDD, cards and PSP savedata are not filename-based artifacts and
 * intentionally remain under their existing engine-specific persistence contracts.
 */
class CartridgeSaveBinding(
    private val directories: DirectoriesManager,
    private val installedGames: suspend () -> List<Game>,
) {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun mayImportLegacy(
        game: Game,
        byBasename: Boolean,
    ): Boolean {
        val matches =
            installedGames().filter {
                if (byBasename) {
                    it.saveFileName().substringBeforeLast('.') == game.saveFileName().substringBeforeLast('.')
                } else {
                    it.saveFileName() == game.saveFileName()
                }
            }
        return matches.size == 1 && matches.single().cartridgeId == game.cartridgeId
    }

    suspend fun sramFile(
        game: Game,
        legacy: File,
        scope: String,
    ): File {
        val target = File(directories.getSavesDirectory(), "cartridges/${id(game)}/sram/${key(scope)}")
        val folder = bind(game, "sram", scope, legacy.parentFile!!, listOf(legacy.name), target, true)
        return File(folder, legacy.name)
    }

    suspend fun stateDirectory(
        game: Game,
        namespace: String,
    ): File {
        val root = directories.getStatesDirectory()
        val name = game.saveFileName()
        val states = listOf("$name.state") + (1..4).map { "$name.slot$it" }
        val files = states.flatMap { listOf(it, "$it.metadata") }
        return bind(
            game,
            "states",
            namespace,
            File(root, namespace),
            files,
            File(root, "cartridges/${id(game)}/${key(namespace)}"),
            false,
        )
    }

    suspend fun previewDirectory(
        game: Game,
        namespace: String,
    ): File {
        val root = directories.getStatesPreviewDirectory()
        val files = (1..4).map { "${game.saveFileName()}.slot$it.jpg" }
        return bind(
            game,
            "previews",
            namespace,
            File(root, namespace),
            files,
            File(root, "cartridges/${id(game)}/${key(namespace)}"),
            false,
        )
    }

    private suspend fun bind(
        game: Game,
        family: String,
        scope: String,
        legacyDirectory: File,
        names: List<String>,
        target: File,
        byBasename: Boolean,
    ): File =
        withContext(Dispatchers.IO) {
            val cartridge = id(game)
            val namespace = key(scope)
            locks.getOrPut("$cartridge:$family:$namespace") { Mutex() }.withLock {
                val marker = File(directories.getSaveBindingsDirectory(), "$cartridge/$family/$namespace.active")
                val atomic = AtomicFile(marker)
                if (marker.isFile || File(marker.path + ".bak").isFile) {
                    val value = atomic.openRead().bufferedReader().use { it.readText() }
                    if (!value.startsWith("savebinding-v1:$cartridge:")) {
                        throw IOException("Invalid save binding manifest: original progress retained")
                    }
                    return@withLock target
                }

                val sources = names.map { File(legacyDirectory, it) }.filter { it.isFile }
                val ownerIsUnique = mayImportLegacy(game, byBasename)
                if (sources.isNotEmpty() && !ownerIsUnique) {
                    Timber.w("Ambiguous legacy %s for cartridge %s: original retained", family, cartridge)
                }
                if (!target.isDirectory && !target.mkdirs()) {
                    throw IOException("Cannot create cartridge save directory: original progress retained")
                }
                if (ownerIsUnique) {
                    sources.forEach { copyVerifyOnce(it, File(target, it.name)) }
                }
                if (marker.parentFile?.isDirectory != true && marker.parentFile?.mkdirs() != true) {
                    throw IOException("Cannot create save binding manifest directory")
                }
                val stream = atomic.startWrite()
                try {
                    val status =
                        when {
                            sources.isEmpty() -> "fresh"
                            ownerIsUnique -> "verified-copy"
                            else -> "ambiguous-unassigned"
                        }
                    stream.write("savebinding-v1:$cartridge:$status".toByteArray(Charsets.UTF_8))
                    atomic.finishWrite(stream)
                } catch (error: Exception) {
                    atomic.failWrite(stream)
                    throw error
                }
                target
            }
        }

    private fun copyVerifyOnce(
        source: File,
        destination: File,
    ) {
        val initialDigest = digest(source)
        val initialSize = source.length()
        if (destination.exists()) {
            if (destination.length() != initialSize || digest(destination) != initialDigest) {
                throw IOException("Incomplete save copy conflicts with original; recovery is required")
            }
            return
        }
        val staging = File.createTempFile("binding-", ".pending", destination.parentFile)
        try {
            source.inputStream().use { input ->
                FileOutputStream(staging).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            if (staging.length() != initialSize || digest(staging) != initialDigest) {
                throw IOException("Unverified save copy: original progress retained")
            }
            if (digest(source) != initialDigest) {
                throw IOException("Original save changed during copy; migration not activated")
            }
            if (!staging.renameTo(destination)) {
                throw IOException("Unable to activate verified save copy")
            }
            if (digest(destination) != initialDigest) {
                throw IOException("Copied save verification failed; migration not activated")
            }
        } finally {
            staging.delete()
        }
    }

    private fun id(game: Game): String = UUID.fromString(game.cartridgeId).toString()

    private fun key(value: String): String = hash(value.toByteArray(Charsets.UTF_8))

    private fun digest(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val bytes = ByteArray(16 * 1024)
            while (true) {
                val size = input.read(bytes)
                if (size < 0) break
                md.update(bytes, 0, size)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
