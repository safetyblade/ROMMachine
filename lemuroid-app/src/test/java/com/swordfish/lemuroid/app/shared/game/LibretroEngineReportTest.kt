package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Properties

class LibretroEngineReportTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun reportIsAvailableOnlyUnderDiagnostics() {
        assertEquals(GameMenuRoute.DIAGNOSTICS, GameMenuRoute.ENGINE_REPORT.parent)
        assertEquals(GameMenuRoute.ADVANCED, GameMenuRoute.DIAGNOSTICS.parent)
    }

    @Test
    fun optionsAreBoundedAndPrivatePathsAreRedacted() {
        val options =
            LibretroEngineReport.safeOptions(
                listOf(
                    "reicast_vblank_pacing" to "enabled",
                    "rom_path" to "/private/game",
                    "ordinary_option" to "content://private/file",
                    "bad\nkey" to "value",
                    "reicast_internal_resolution" to "640x480",
                ),
            )
        assertEquals(
            listOf(
                "ordinary_option=[redacted]",
                "reicast_internal_resolution=640x480",
                "reicast_vblank_pacing=enabled",
            ),
            options,
        )
    }

    @Test
    fun revisionIsOnlyShownWhenSelectedBinaryMatchesManifest() {
        val binary = temporaryFolder.newFile("libfixture.so").apply { writeText("abc") }
        val digest = LibretroEngineReport.sha256(binary)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", digest)
        val revision = "a".repeat(40)
        val manifest =
            Properties().apply {
                setProperty("core.sha256", requireNotNull(digest))
                setProperty("revision", revision)
            }
        assertEquals(revision, LibretroEngineReport.verifiedRevision(digest, manifest))
        assertNull(LibretroEngineReport.verifiedRevision("b".repeat(64), manifest))
        assertNull(LibretroEngineReport.verifiedRevision(null, manifest))
    }

    @Test
    fun reportsNeverExposeBinaryPathOrInventPerformanceMetrics() {
        val session =
            LibretroReportSession(
                coreFilePath = "/private/libfixture.so",
                mediaRoute = "standard file path",
                lowLatencyAudioRequested = true,
                skipDuplicateFrames = false,
            )
        val result =
            LibretroEngineReport.render(
                "test", "gb", "Fixture", "gambatte", "test", "gb", session, null, null, 35, "test",
            )
        assertFalse(result.contains("/private/"))
        assertTrue(result.contains("selected_core_file_sha256=unavailable"))
        assertTrue(result.contains("core_video_callback_observed=false"))
        assertTrue(result.contains("performance_counters=not collected"))
        val measured =
            LibretroEngineReport.render(
                appVersion = "test",
                system = "dc",
                title = "Fixture",
                core = "flycast",
                hostVersion = "test",
                mediaFormat = "gdi",
                session = session.copy(performanceSnapshot = "runs=42;fifo_dropped=3;private=/secret"),
                digest = null,
                revision = null,
                androidSdk = 35,
                device = "test",
            )
        assertTrue(measured.contains("runs=42"))
        assertTrue(measured.contains("fifo_dropped=3"))
        assertFalse(measured.contains("/secret"))
    }
}
