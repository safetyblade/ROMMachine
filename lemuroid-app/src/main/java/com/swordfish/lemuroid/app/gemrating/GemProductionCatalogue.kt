package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream

/** Reads the shipped approval registry, then validates each immutable source shard. */
object GemProductionCatalogue {
    const val VERSION = "gem-vdg-2026.11.18.1"
    const val RELEASE_COUNT = 14216
    private val registries = listOf("gem/vdg-pins-a.json", "gem/vdg-pins-b.json")

    @Serializable
    data class Pin(
        val path: String,
        val version: String,
        val sha256: String,
        val count: Int,
        val sources: List<GemCataloguePackage.Source>,
    )

    fun load(context: Context): GemCataloguePackage.Loaded = load { context.assets.open(it) }

    /**
     * The opener must address trusted APK assets, not a document picker or arbitrary user storage.
     * Every shard is checked before the combined catalogue is returned.
     */
    internal fun load(openAsset: (String) -> InputStream): GemCataloguePackage.Loaded {
        val pins =
            registries.flatMap { path ->
                openAsset(path).bufferedReader(Charsets.UTF_8).use {
                    Json.decodeFromString(ListSerializer(Pin.serializer()), it.readText())
                }
            }
        require(pins.size == 23 && pins.map { it.path }.distinct().size == 23)
        require(pins.sumOf { it.count } == RELEASE_COUNT)
        val manifests =
            pins.map { pin ->
                require(pin.path.matches(Regex("gem/vdg/[a-z0-9]+\\.json")))
                val approval = GemCataloguePackage.Approval(pin.version, pin.sha256, pin.sources)
                openAsset(pin.path).use { GemCataloguePackage.load(it, approval) }.manifest.also {
                    require(it.releaseCount == pin.count)
                }
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
