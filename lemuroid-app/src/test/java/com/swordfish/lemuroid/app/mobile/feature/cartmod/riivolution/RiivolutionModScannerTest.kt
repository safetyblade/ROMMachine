package com.swordfish.lemuroid.app.mobile.feature.cartmod.riivolution

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Scaffold tests for [RiivolutionModScanner]. The fixture mod is fully
 * synthetic (authored for this test): no real game mod content is used.
 */
class RiivolutionModScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val fakeXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <wiidisc version="1">
          <id game="RFKE01"/>
          <options/>
          <patch id="fake-replacement">
            <file disc="/files/data.bin" external="FakeMod/data.bin"/>
            <file disc="/files/missing.bin" external="FakeMod/missing.bin"/>
          </patch>
        </wiidisc>
    """.trimIndent()

    private fun writeFakeMod(): File {
        val root = tempFolder.newFolder("fakemod")
        val riivolution = File(root, "riivolution").apply { mkdirs() }
        File(riivolution, "fake-mod.xml").writeText(fakeXml)
        val dataDir = File(riivolution, "FakeMod").apply { mkdirs() }
        File(dataDir, "data.bin").writeBytes(byteArrayOf(1, 2, 3, 4))
        // FakeMod/missing.bin is referenced by the XML but deliberately absent.
        return root
    }

    @Test
    fun scanRecognisesSdLayoutModAndExtractsIdsAndRefs() {
        val result = RiivolutionModScanner.scan(writeFakeMod())

        assertTrue(result is RiivolutionScanResult.Found)
        val mod = (result as RiivolutionScanResult.Found).mod
        assertEquals("RFKE01", mod.gameId)
        assertEquals(listOf("fake-replacement"), mod.patches.map { it.id })
        assertEquals(
            listOf("FakeMod/data.bin", "FakeMod/missing.bin"),
            mod.referencedExternalPaths,
        )
    }

    @Test
    fun scanRejectsFolderWithoutXml() {
        val root = tempFolder.newFolder("empty")
        val result = RiivolutionModScanner.scan(root)
        assertTrue(result is RiivolutionScanResult.Rejected)
    }

    @Test
    fun scanRejectsNonRiivolutionXml() {
        val root = tempFolder.newFolder("othermod")
        File(root, "config.xml").writeText("<someOtherFormat version=\"1\"/>")
        val result = RiivolutionModScanner.scan(root)
        assertTrue(result is RiivolutionScanResult.Rejected)
    }

    @Test
    fun stagingPlanTargetsDolphinLoadLayoutAndReportsMissingFiles() {
        val mod = (RiivolutionModScanner.scan(writeFakeMod()) as RiivolutionScanResult.Found).mod
        val plan = RiivolutionModScanner.stagingPlan(mod, File(tempFolder.root, "User"))

        val destinations = plan.files.map { it.destinationRelativePath }
        assertTrue("Load/Riivolution/fake-mod.xml" in destinations)
        assertTrue("Load/Riivolution/FakeMod/data.bin" in destinations)
        assertEquals(listOf("FakeMod/missing.bin"), plan.missingExternalPaths)
    }

    @Test
    fun presetJsonCarriesNameGameIdAndPatchIds() {
        val mod = (RiivolutionModScanner.scan(writeFakeMod()) as RiivolutionScanResult.Found).mod
        val plan = RiivolutionModScanner.stagingPlan(mod, File(tempFolder.root, "User"))
        val json = plan.preset.toJson()

        assertTrue(json.contains("\"name\": \"fake-mod\""))
        assertTrue(json.contains("\"gameId\": \"RFKE01\""))
        assertTrue(json.contains("\"id\": \"fake-replacement\""))
        assertTrue(json.contains("\"enabled\": true"))
    }
}
