package com.swordfish.lemuroid.app.shared.game

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NativeSessionJournalTest {
    @Test fun interruptedNativeLaunchRetainsStageAndReportUntilConsumed() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val root = File(app.filesDir, "wiiu-test").apply { mkdirs() }
        NativeSessionJournal.begin(app, "Wii U", root)
        File(root, "native-stage.txt").writeText("Cemu context_reset")
        File(root, "native-core.log").writeText("Synthetic native failure")
        val report = requireNotNull(NativeSessionJournal.consumeCrash(app))
        assertTrue(report.contains("Cemu context_reset"))
        assertTrue(report.contains("Synthetic native failure"))
        assertTrue(File(app.filesDir, "last-native-crash.txt").readText().contains("Synthetic native failure"))
        assertNull(NativeSessionJournal.consumeCrash(app))
    }

    @Test fun cleanReturnDoesNotRaiseCrash() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        NativeSessionJournal.begin(app, "Wii U", app.filesDir)
        NativeSessionJournal.finish(app)
        assertNull(NativeSessionJournal.consumeCrash(app))
    }
}
