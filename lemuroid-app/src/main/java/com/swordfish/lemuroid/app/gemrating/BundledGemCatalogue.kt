package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import java.io.InputStream

/** Pinned, deliberately restricted launch-path seed. Not a complete catalogue. */
object BundledGemCatalogue {
    const val ASSET_PATH = "gem/catalogue-v1.json"
    const val VERSION = "gem-seed-2026.11.18.1"
    const val SHA256 = "2cf0d7bab4c9ac3b2a899b9aa23a6a04a544c22574ffd9f2418a6207311f3761"
    val approval =
        GemCataloguePackage.Approval(
            VERSION,
            SHA256,
            listOf(
                GemCataloguePackage.Source(
                    id = "titlescreen",
                    name = "Title Screen",
                    url = "https://titlescreen.org/downloads",
                    licence = "CC-BY-4.0",
                    attribution =
                        "Data: Title Screen (https://titlescreen.org), CC BY 4.0 " +
                            "(https://creativecommons.org/licenses/by/4.0/). " +
                            "Adapted: selected commercial release facts; " +
                            "identifiers retained; unverified fields omitted.",
                    revision = "2026.10.10 (public release pages)",
                ),
            ),
        )

    /** Asset access only; does not require network or alter the installed library. */
    fun load(context: Context): GemCataloguePackage.Loaded = context.assets.open(ASSET_PATH).use { load(it) }

    fun load(input: InputStream): GemCataloguePackage.Loaded = GemCataloguePackage.load(input, approval)
}
