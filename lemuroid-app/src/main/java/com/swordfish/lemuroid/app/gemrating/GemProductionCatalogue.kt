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
    const val VERSION = "gem-combined-2026.11.19.1"
    const val RELEASE_COUNT = 88246
    const val LEGACY_VERSION = "gem-vdg-2026.11.18.1"
    const val LEGACY_RELEASE_COUNT = 14216
    private const val TITLE_SCREEN_REGISTRY = "gem/titlescreen-pins.json"
    private const val TITLE_SCREEN_PIN_SHA = "a04193e5fe23389d3cc8b16c56658037b5ee7deb8ecb340b3b180eb21629e4c3"
    private val legacyRegistries = listOf("gem/vdg-pins-a.json", "gem/vdg-pins-b.json")
    private val registries = legacyRegistries + TITLE_SCREEN_REGISTRY
    private val lock = Any()
    private var cachedBaseline: GemCataloguePackage.Loaded? = null

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
            cachedBaseline ?: loadContext(context, false).also { cachedBaseline = it }
        }

    // Explicit old version refs remain valid after an APK upgrade.
    internal fun loadLegacy(context: Context): GemCataloguePackage.Loaded =
        synchronized(lock) {
            loadContext(context, true)
        }

    private fun loadContext(
        context: Context,
        legacy: Boolean,
    ): GemCataloguePackage.Loaded {
        val openAsset: (String) -> InputStream = { context.assets.open(it) }
        return combine(readPins(legacy, openAsset), legacy) { pin ->
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
            cachedBaseline = null
            pin.count
        }

    /** Registry input must come from trusted APK assets, not a user document. */
    internal fun load(
        legacy: Boolean = false,
        openAsset: (String) -> InputStream,
    ): GemCataloguePackage.Loaded =
        combine(readPins(legacy, openAsset), legacy) { pin ->
            openAsset(pin.path).use { GemCataloguePackage.load(it, pin.approval()) }
        }

    internal fun readPins(
        legacy: Boolean = false,
        openAsset: (String) -> InputStream,
    ): List<Pin> {
        val paths = if (legacy) legacyRegistries else registries
        val pins =
            paths.flatMap { path ->
                val bytes = openAsset(path).use(::readBounded)
                if (path == TITLE_SCREEN_REGISTRY) {
                    require(digest(bytes) == TITLE_SCREEN_PIN_SHA) { "Unapproved Title Screen registry" }
                }
                Json.decodeFromString(ListSerializer(Pin.serializer()), bytes.decodeToString(throwOnInvalidSequence = true))
            }
        val expectedShards = if (legacy) 23 else 53
        val expectedRecords = if (legacy) LEGACY_RELEASE_COUNT else RELEASE_COUNT
        require(pins.size == expectedShards && pins.map { it.path }.distinct().size == expectedShards)
        require(pins.sumOf { it.count } == expectedRecords)
        require(pins.all { it.path.matches(Regex("gem/(vdg|titlescreen)/[a-z0-9]+\\.json")) })
        return pins
    }

    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it.toInt() and 255)
        }

    internal fun identifyApproved(
        pins: List<Pin>,
        bytes: ByteArray,
    ): Pin {
        require(bytes.size <= GemCataloguePackage.MAX_BYTES) { "Catalogue exceeds size limit" }
        val digest = digest(bytes)
        return requireNotNull(pins.singleOrNull { it.sha256 == digest }) { "Catalogue is not approved" }
    }

    private fun localFile(
        context: Context,
        pin: Pin,
    ): AtomicFile {
        // Preserve installed VDG overrides and avoid same-basename collisions across sources.
        val version = if (pin.path.startsWith("gem/vdg/")) LEGACY_VERSION else VERSION
        val root = File(context.noBackupFilesDir, "gem-catalogue/" + version)
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
        legacy: Boolean,
        load: (Pin) -> GemCataloguePackage.Loaded,
    ): GemCataloguePackage.Loaded {
        val manifests =
            pins.map { pin ->
                load(pin).manifest.also { require(it.releaseCount == pin.count) }
            }
        val releases = manifests.flatMap { it.releases }
        val sourceGroups = manifests.flatMap { it.sources }.groupBy { it.id }
        require(sourceGroups.values.all { it.distinct().size == 1 }) { "Conflicting source declarations" }
        val sources = sourceGroups.values.map { it.first() }
        val version = if (legacy) LEGACY_VERSION else VERSION
        val expectedRecords = if (legacy) LEGACY_RELEASE_COUNT else RELEASE_COUNT
        require(releases.size == expectedRecords)
        require(releases.map { it.sourceId to it.sourceRecordId }.distinct().size == releases.size)
        val index =
            GemCatalogueIndex(
                version,
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
            GemCataloguePackage.Manifest(1, version, releases.size, sources, releases),
            index,
        )
    }
}
