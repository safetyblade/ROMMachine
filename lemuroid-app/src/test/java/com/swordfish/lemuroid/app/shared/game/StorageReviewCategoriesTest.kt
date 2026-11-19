package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.storage.cache.StorageReviewCategories
import com.swordfish.lemuroid.lib.storage.cache.StorageReviewCategories.Category
import com.swordfish.lemuroid.lib.storage.cache.StorageUsageReview
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageReviewCategoriesTest {
    @Test
    fun groupedTotalsPreserveBytesAndReclaimableAmounts() {
        val rows =
            listOf(
                StorageUsageReview.Row("Xbox 360 media", 100, 80, true),
                StorageUsageReview.Row("Internal/files/native/xbox/hdd/Partition5", 200, 0, true),
                StorageUsageReview.Row("Internal/files/native-engine-versions", 300, 0, true),
                StorageUsageReview.Row("App-specific storage/states/dolphin_modern", 400, 0, false),
                StorageUsageReview.Row("Internal/files/native/xbox360/cache/shaders", 50, 0, true),
                StorageUsageReview.Row("Internal/files/native/xbox/backups/a.zip", 20, 0, true),
                StorageUsageReview.Row("App-specific storage/custom-covers/a.png", 10, 0, false),
                StorageUsageReview.Row("Internal/perf-123.map", 5, 0, true),
            )
        val report = StorageUsageReview.Report(rows, 0)
        val summaries = StorageReviewCategories.summarize(report).associateBy { it.category }
        assertEquals(Category.values().toSet(), summaries.keys)
        assertEquals(report.internalBytes + report.externalBytes, summaries.values.sumOf { it.bytes })
        assertEquals(report.reclaimableBytes, summaries.values.sumOf { it.reclaimableBytes })
        assertEquals(80L, summaries.getValue(Category.STAGED).reclaimableBytes)
        assertEquals(200L, summaries.getValue(Category.CONSOLE).bytes)
        assertEquals(400L, summaries.getValue(Category.PROGRESS).bytes)
    }
}
