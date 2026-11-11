package com.swordfish.lemuroid.app.psp

import com.swordfish.lemuroid.lib.saves.PSPSavedataSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PSPSavedataSeedTest {
    @Test
    fun copiesSavedataWithoutStatesAndNeverOverwritesEitherEngine() {
        val parent = Files.createTempDirectory("psp-savedata-").toFile()
        try {
            val legacy = File(parent, "legacy").apply { mkdirs() }
            val modern = File(parent, "modern").apply { mkdirs() }
            val original =
                File(legacy, "PSP/SAVEDATA/ULUS12345/DATA.BIN").apply {
                    parentFile.mkdirs()
                    writeText("baseline")
                }
            File(legacy, "PSP/PPSSPP_STATE").apply { mkdirs() }
            PSPSavedataSeed.seedIfAbsent(legacy, modern)
            val candidate = File(modern, "PSP/SAVEDATA/ULUS12345/DATA.BIN")
            assertEquals("baseline", candidate.readText())
            assertFalse(File(modern, "PSP/PPSSPP_STATE").exists())
            candidate.writeText("candidate progress")
            PSPSavedataSeed.seedIfAbsent(legacy, modern)
            assertEquals("candidate progress", candidate.readText())
            assertEquals("baseline", original.readText())
        } finally {
            parent.deleteRecursively()
        }
    }
}
