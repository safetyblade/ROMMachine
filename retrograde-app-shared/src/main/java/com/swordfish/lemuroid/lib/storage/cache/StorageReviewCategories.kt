package com.swordfish.lemuroid.lib.storage.cache

/** Presentation-only categories. Classification never grants permission to delete a file. */
object StorageReviewCategories {
    enum class Category(val label: String, val explanation: String) {
        STAGED("Staged game copies", "Temporary copies used for game loading."),
        CONSOLE("Console storage", "Virtual hard drives, installed content and console data."),
        ENGINE("Emulator installations", "Engine binaries, versions and system resources."),
        PROGRESS("Saves and save states", "Game progress, memory cards and state previews."),
        PERFORMANCE("Performance caches", "Compiled modules, shaders and other runtime caches."),
        BACKUPS("Recovery backups", "Previous game data and engine migration backups."),
        LIBRARY("Library and artwork", "Game catalogue and cartridge artwork."),
        OTHER("Other app data", "Settings, diagnostics and remaining application files."),
    }

    data class Summary(val category: Category, val bytes: Long, val reclaimableBytes: Long)

    private val mediaLabels =
        setOf(
            "SAF ROM copies",
            "Local archive copies",
            "Wii U media",
            "ARMSX2 media",
            "Xbox 360 media",
            "PS3 ARMSX3 media",
            "PS3 RPCS3 media",
        )

    fun category(row: StorageUsageReview.Row): Category {
        val label = row.label
        val parts = label.split('/')
        return when {
            label in mediaLabels -> Category.STAGED
            parts.any { it in setOf("backups", "emulator-baseline-backups") } -> Category.BACKUPS
            parts.any { it in setOf("saves", "states", "state-previews", "memcards") } -> Category.PROGRESS
            parts.any {
                it in setOf(
                    "native-engines",
                    "native-engine-versions",
                    "native-engine-assets",
                    "system-engines",
                    "system",
                    "bios",
                    "cores",
                )
            } -> Category.ENGINE
            label.startsWith("Internal/files/native/") && parts.any {
                it in setOf("cache", "hle")
            } -> Category.PERFORMANCE
            label.startsWith("Internal/files/native/") -> Category.CONSOLE
            parts.any { it in setOf("databases", "custom-covers", "image_cache") } -> Category.LIBRARY
            else -> Category.OTHER
        }
    }

    fun summarize(report: StorageUsageReview.Report): List<Summary> =
        report.rows.groupBy(::category).map { (category, rows) ->
            Summary(category, rows.sumOf { it.bytes }, rows.sumOf { it.reclaimableBytes })
        }.sortedByDescending { it.bytes }
}
