package com.swordfish.lemuroid.app.shared.support

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.shared.game.SessionExitEvidence
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SupportReportTest {
    @Test
    fun previewAndArchiveShareTheSameRedactedSnapshot() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report =
            SupportReport.create(
                "failure recovery",
                "Crash or freeze",
                "core=Play!\npath=/storage/emulated/0/Private/game.iso\n" +
                    "token=unstructured\nAuthorization: Bearer private-token\nuri=content://provider/private\n" +
                    "GL_RENDERER=Adreno (TM) 840",
                gameTitle = "Conflict: Desert Storm",
                system = "PS2",
                engine = "Play!",
            )
        for (value in listOf(report.text, report.json)) {
            assertFalse(value.contains("/storage/"))
            assertFalse(value.contains("content://provider"))
            assertFalse(value.contains("private-token"))
            assertFalse(value.contains("unstructured"))
            assertTrue(value.contains("Adreno (TM) 840"))
        }
        val store = SupportReportStore(context)
        store.save(report)
        assertEquals(report.id, store.list().single().id)
        ZipFile(store.packageReport(report)).use { zip ->
            assertEquals(2, zip.size())
            assertEquals(
                report.text,
                zip.getInputStream(zip.getEntry("report.txt")).bufferedReader().use { it.readText() },
            )
            assertEquals(
                report.json,
                zip.getInputStream(zip.getEntry("report.json")).bufferedReader().use { it.readText() },
            )
        }
        assertEquals(1, JSONObject(report.json).getInt("schemaVersion"))
        store.delete(report.id)
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun retainedHistoryIsBoundedAndExpiredReportsAreRemoved() {
        val store = SupportReportStore(ApplicationProvider.getApplicationContext<Context>())
        val latest = System.currentTimeMillis()
        repeat(12) { index ->
            val json =
                JSONObject(SupportReport.create("settings", "Other", null).json)
                    .put("createdAtMillis", latest - TimeUnit.MINUTES.toMillis(index.toLong()))
            store.save(SupportReport.parse(json.toString()))
        }
        val expired =
            JSONObject(SupportReport.create("settings", "Other", null).json)
                .put("createdAtMillis", latest - TimeUnit.DAYS.toMillis(15))
        store.save(SupportReport.parse(expired.toString()))
        assertEquals(10, store.list().size)
        assertEquals(latest, store.list().first().createdAtMillis)
        assertTrue(store.list().none { it.id == expired.getString("id") })
    }

    @Test
    fun ticketRequiresAcknowledgementForTheSameReport() {
        val id = SupportReport.create("settings", "Other", null).id
        val receipt = JSONObject().put("accepted", true).put("reportId", id).put("reference", "CD-58")
        assertEquals("CD-58", SupportTicketTransport.validateReceipt(id, receipt.toString()))
        assertTrue(
            runCatching { SupportTicketTransport.validateReceipt("another-report", receipt.toString()) }.isFailure,
        )
        assertFalse(SupportTicketTransport.enabled)
    }

    @Test
    fun androidExitIsScopedToProcessPidAndLaunchTime() {
        assertTrue(SessionExitEvidence.matches("cartdeck:game", 42, 14L, "cartdeck:game", 42, 13L))
        assertFalse(SessionExitEvidence.matches("cartdeck:ps2next", 42, 14L, "cartdeck:game", 42, 13L))
        assertFalse(SessionExitEvidence.matches("cartdeck:game", 41, 14L, "cartdeck:game", 42, 13L))
        assertFalse(SessionExitEvidence.matches("cartdeck:game", 42, 12L, "cartdeck:game", 42, 13L))
        assertFalse(SessionExitEvidence.matches("cartdeck:game", 0, 14L, "cartdeck:game", 0, 13L))
    }

    @Test
    fun anrTraceRetainsBoundedFramesForTheSelectedProcess() {
        val trace =
            """
            ----- pid 42 at 2026-11-26 -----
              at com.cartdeck.Engine.waitForFrame(Engine.kt:32)
            arbitrary personal data
            ----- end 42 -----
            ----- pid 55 at 2026-11-26 -----
              at com.other.Example.unrelated(Example.java:5)
            """.trimIndent()
        assertEquals(
            "  at com.cartdeck.Engine.waitForFrame(Engine.kt:32)",
            SessionExitEvidence.selectAnrFrames(trace, 42),
        )
        assertNull(SessionExitEvidence.selectAnrFrames(trace, 80))
    }
}
