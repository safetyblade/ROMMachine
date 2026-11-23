package com.swordfish.lemuroid.app.mobile.feature.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class SafeSaveRestorePreviewTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun replacementWithSameSizeAndTimestampRequiresAnotherPreview() {
        val save = temp.newFile("progress.sav").apply { writeText("first") }
        val timestamp = save.lastModified()
        val before = existingSaveFingerprints(listOf(save))
        save.writeText("other")
        assertTrue(save.setLastModified(timestamp))
        assertEquals(timestamp, save.lastModified())
        assertEquals(5L, save.length())
        assertNotEquals(before, existingSaveFingerprints(listOf(save)))
    }

    @Test fun absentDestinationBecomingASaveChangesThePreview() {
        val save = temp.root.resolve("progress.sav")
        assertTrue(existingSaveFingerprints(listOf(save)).isEmpty())
        save.writeText("new save")
        assertEquals(setOf(save), existingSaveFingerprints(listOf(save)).keys)
    }

    @Test fun previewRefusesToFollowASymbolicLink() {
        val save = temp.newFile("progress.sav")
        val link = temp.root.resolve("unexpected-target.sav")
        Files.createSymbolicLink(link.toPath(), save.toPath())
        assertThrows(IllegalArgumentException::class.java) { existingSaveFingerprints(listOf(link)) }
    }
}
