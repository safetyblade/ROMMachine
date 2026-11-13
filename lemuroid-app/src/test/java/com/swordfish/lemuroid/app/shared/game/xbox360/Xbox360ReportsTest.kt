package com.swordfish.lemuroid.app.shared.game.xbox360

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class Xbox360ReportsTest {
    @Test fun reportsAreBoundedLocalAndDoNotEraseGuestData() {
        val root = Files.createTempDirectory("cartdeck-report").toFile()
        try {
            val saves = java.io.File(Xbox360Engine.data(root), "content/homebrew-save").apply { writeText("retained") }
            Xbox360Reports.write(root, "guest_image=UNVERIFIED\n" + "x".repeat(20000))
            assertTrue(Xbox360Reports.read(root).length <= 16384)
            assertTrue(saves.readText() == "retained")
            val error =
                Xbox360Reports.safeError(
                    IllegalStateException(
                        "Cannot open content://private/provider/title /storage/emulated/0/private/title.xex",
                    ),
                )
            assertFalse(error.contains("private"))
            assertFalse(error.contains("title.xex"))
        } finally {
            root.deleteRecursively()
        }
    }
}
