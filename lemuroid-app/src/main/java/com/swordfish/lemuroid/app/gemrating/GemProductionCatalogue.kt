package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Reads the shipped approval registry, then validates each immutable source shard. */
object GemProductionCatalogue {
    const val VERSION = "gem-vdg-2026.11.18.1"
    const val RELEASE_COUNT = 14216
    private val registries = listOf("gem/vdg-pins-a.json", "gem/vdg-pins-b.json")
    private val lock = Any()

    @Serializable
    data class Pin(
        val path: String,
        val version: String,
        val sha256: String,
        val count: Int,
        val sources: List<GemCataloguePackage.Source>,
    ) {
        fun approval() = GemCataloguePackage.Approval(version, sha256, sources)
    }

    fun load(context: Context): GemCataloguePackage.Loaded =
        synchronized(lock) {
            val openAsset: (String) -> InputStream = { context.assets.open(it) }
            combine(readPins(openAsset)) { pin ->
                val restored =
                    runCatching {
                        localFile(context, pin).openRead().use { GemCataloguePackage.load(it, pin.approval()) }
                    }.getOrNull()
                restored ?: openAsset(pin.path).use { GemCataloguePackage.load(it, pin.approval()) }
            }
        }

    /**
     * Restore only a package already approved by the shipped registry. New independently
     * issued versions require the later signed update mechanism. Never trust imported pins.
     */
    fun importApproved(
        context: Context,
        input: InputStream,
    ): Int =
        synchronized(lock) {
            val bytes = readBounded(input)
            val pin = identifyApproved(readPins { context.assets.open(it) }, bytes)
            val loaded = GemCataloguePackage.load(bytes.inputStream(), pin.approval())
            require(loaded.manifest.releaseCount == pin.count)
            val destination = localFile(context, pin)
            val stream = destination.startWrite()
            try {
                stream.write(bytes)
                destination.finishWrite(stream)
            } catch (failure: Throwable) {
                destination.failWrite(stream)
                throw failure
            }
            pin.count
        }

    /** Registry input must come from trusted APK assets, not a user document. */
    internal fun load(openAsset: (String) -> InputStream): GemCataloguePackage.Loaded =
        combine(readPins(openAsset)) { pin ->
            openAsset(pin.path).use { GemCataloguePackage.load(it, pin.approval()) }
        }

    internal fun readPins(openAsset: (String) -> InputStream): List<Pin> {
        val pins =
            registries.flatMap { path ->
                openAsset(path).bufferedReader(Charsets.UTF_8).use {
                    Json.decodeFromString(ListSerializer(Pin.serializer()), it.readText())
                }
            }
        require(pins.size == 23 && pins.map { it.path }.distinct().size == 23)
        require(pins.sumOf { it.count } == RELEASE_COUNT)
        require(pins.all { it.path.matches(Regex("gem/vdg/[a-z0-9]+\\.json")) })
        return pins
    }

    internal fun identifyApproved(
        pins: List<Pin>,
        bytes: ByteArray,
    ): Pin {
        require(bytes.size <= GemCataloguePackage.MAX_BYTES) { "Catalogue exceeds size limit" }
        val digest =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
        return requireNotNull(pins.singleOrNull { it.sha256 == digest }) { "Catalogue is not approved" }
    }

    private fun localFile(
        context: Context,
        pin: Pin,
    ): AtomicFile {
        val root = File(context.noBackupFilesDir, "gem-catalogue/" + VERSION)
        return AtomicFile(File(root, pin.path.substringAfterLast('/')))
    }

    private fun readBounded(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) {
                val byte = input.read()
                if (byte < 0) break
                output.write(byte)
            } else {
                output.write(buffer, 0, count)
            }
            require(output.size() <= GemCataloguePackage.MAX_BYTES) { "Catalogue exceeds size limit" }
        }
        return output.toByteArray()
    }

    private fun combine(
        pins: List<Pin>,
        load: (Pin) -> GemCataloguePackage.Loaded,
    ): GemCataloguePackage.Loaded {
        val manifests =
            pins.map { pin ->
                load(pin).manifest.also { require(it.releaseCount == pin.count) }
            }
        val releases = manifests.flatMap { it.releases }
        val sources = manifests.flatMap { it.sources }
        require(releases.size == RELEASE_COUNT && sources.map { it.id }.distinct().size == sources.size)
        val index =
            GemCatalogueIndex(
                VERSION,
                releases.map {
                    GemCatalogueIndex.Release(
                        it.releaseId,
                        it.systemId,
                        it.title,
                        it.verifiedSha256,
                        it.verifiedSerials,
                        it.primaryGenre,
                    )
                },
            )
        return GemCataloguePackage.Loaded(
            GemCataloguePackage.Manifest(1, VERSION, releases.size, sources, releases),
            index,
        )
    }
}
