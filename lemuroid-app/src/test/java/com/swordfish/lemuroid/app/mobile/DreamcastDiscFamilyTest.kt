package com.swordfish.lemuroid.app.mobile

import com.swordfish.lemuroid.lib.library.DiscReferences
import com.swordfish.lemuroid.lib.storage.local.DiscFamilyStager
import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.file.Files

class DreamcastDiscFamilyTest {
    @Test fun gdiPlaylistStagesExactReferencesAndReopensWithoutChangingSources() {
        val root = Files.createTempDirectory("disc-family").toFile()
        try {
            val gdi = "2\n1 0 4 2352 \"Track One.BIN\" 0\n2 4500 0 2352 track02.raw 0"
            val bytes =
                mapOf(
                    "Set.M3U" to "#EXTM3U\ndisc.GDI\nsecond.CDI\n".toByteArray(),
                    "DISC.gdi" to gdi.toByteArray(),
                    "track one.bin" to byteArrayOf(1, 2, 3),
                    "TRACK02.RAW" to byteArrayOf(4, 5),
                    "SECOND.cdi" to byteArrayOf(6, 7),
                )
            val sources =
                bytes.mapValues { (_, value) ->
                    DiscFamilyStager.Source(value.size.toLong(), 1) {
                        ByteArrayInputStream(value)
                    }
                }
            repeat(2) {
                val files = DiscFamilyStager.stage("Set.M3U", sources, root)
                assertEquals("Set.M3U", files.first().name)
                assertEquals(
                    setOf("Set.M3U", "disc.GDI", "second.CDI", "Track One.BIN", "track02.raw"),
                    files.map { it.name }.toSet(),
                )
                assertEquals(gdi, root.resolve("disc.GDI").readText())
                assertArrayEquals(bytes.getValue("track one.bin"), root.resolve("Track One.BIN").readBytes())
                assertArrayEquals(bytes.getValue("SECOND.cdi"), root.resolve("second.CDI").readBytes())
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun cueKeepsGeometryAndTrackNamesAndRequiresDreamcastEvidence() {
        val cue = "REM HIGH-DENSITY AREA\nFILE \"Track.bin\" BINARY\n TRACK 03 MODE1/2352\n INDEX 01 00:00:00\n"
        assertEquals(listOf("Track.bin"), DiscReferences.cue(cue))
        assertEquals(NextGenDiscIdentity.Platform.DREAMCAST, NextGenDiscIdentity.probeDreamcastCue(cue).platform)
        assertNull(NextGenDiscIdentity.probeDreamcastCue(cue.substringAfter('\n')).platform)
        val root = Files.createTempDirectory("cue-family").toFile()
        try {
            val bytes = mapOf("disc.cue" to cue.toByteArray(), "track.BIN" to ByteArray(2352))
            val sources =
                bytes.mapValues { (_, value) ->
                    DiscFamilyStager.Source(value.size.toLong(), 1) {
                        ByteArrayInputStream(value)
                    }
                }
            DiscFamilyStager.stage("disc.cue", sources, root)
            assertEquals(cue, root.resolve("disc.cue").readText())
            assertTrue(root.resolve("Track.bin").isFile)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun bootSectorEvidenceDoesNotClassifyGenericPs1OrSegaCdData() {
        assertFalse(NextGenDiscIdentity.hasDreamcastBootSector("PLAYSTATION SEGA SEGASATURN".toByteArray()))
        val raw = ByteArray(150 * 2352 + 32)
        "SEGA SEGAKATANA ".toByteArray().copyInto(raw, 150 * 2352 + 16)
        assertTrue(NextGenDiscIdentity.hasDreamcastBootSector(raw))
    }

    @Test fun exactCaseWinsButAmbiguousFoldedNamesAndTraversalFail() {
        assertEquals("track.bin", DiscReferences.resolve("track.bin", listOf("TRACK.BIN", "track.bin")))
        assertNull(DiscReferences.resolve("Track.Bin", listOf("TRACK.BIN", "track.bin")))
        assertNull(DiscReferences.cue("FILE \"../track.bin\" BINARY"))
        assertNull(DiscReferences.cue("FILE \"/track.bin\" BINARY"))
        assertNull(DiscReferences.parse("disc.m3u", "https://example/disc.cdi"))
    }

    @Test fun missingTrackFailsBeforePublishingPrimaryOrReplacingExistingCache() {
        val root = Files.createTempDirectory("missing-family").toFile()
        try {
            root.resolve("disc.gdi").writeText("previous complete descriptor")
            val descriptor = "2\n1 0 4 2352 one.bin 0\n2 4500 0 2352 missing.raw 0".toByteArray()
            val sources =
                mapOf(
                    "disc.gdi" to
                        DiscFamilyStager.Source(descriptor.size.toLong(), 1) {
                            ByteArrayInputStream(descriptor)
                        },
                )
            try {
                DiscFamilyStager.stage("disc.gdi", sources, root)
                fail("Missing track was accepted")
            } catch (
                expected: IOException,
            ) {
                assertTrue(expected.message!!.contains("one.bin"))
            }
            assertEquals("previous complete descriptor", root.resolve("disc.gdi").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun playlistCyclesAndMalformedGeometryAreNotInvented() {
        assertNull(DiscReferences.parse("bad.gdi", "2\n1 0 4 2352 track.bin 0"))
        assertNull(DiscReferences.parse("bad.m3u", "bad.m3u"))
    }
}
