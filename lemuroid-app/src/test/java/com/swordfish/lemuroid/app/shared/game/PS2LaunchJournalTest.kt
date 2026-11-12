package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class PS2LaunchJournalTest {
    @Test
    fun onlyPs2LaunchUsesMainProcessSupervisorAndPreservesOriginalGameIntent() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val systems = NativeGameSystems.configurations()
        listOf(SystemID.PLAYSTATION_2, SystemID.WII, SystemID.GAMECUBE, SystemID.DREAMCAST).forEach { id ->
            val system = systems.first { it.id == id }
            val game = Game(42, "fixture.iso", "content://fixtures/disc", "Fixture", id.dbname, null, null, 0)
            BaseGameActivity.launchGame(activity, system.systemCoreConfigs.first(), game, false, false)
            val launched = Shadows.shadowOf(activity).nextStartedActivityForResult.intent
            if (id == SystemID.PLAYSTATION_2) {
                assertEquals(PS2RecoveryActivity::class.java.name, launched.component?.className)
                val target = launched.getParcelableExtra<android.content.Intent>(PS2RecoveryActivity.EXTRA_TARGET)!!
                assertEquals(game, target.getSerializableExtra(BaseGameActivity.EXTRA_GAME))
                assertFalse(target.getBooleanExtra("LOAD_SAVE", true))
            } else {
                assertFalse(launched.component?.className == PS2RecoveryActivity::class.java.name)
                assertEquals(game, launched.getSerializableExtra(BaseGameActivity.EXTRA_GAME))
            }
        }
        activity.finish()
    }

    @Test
    fun missingSupervisorTargetReturnsWithoutStartingAnEngine() {
        val activity = Robolectric.buildActivity(PS2RecoveryActivity::class.java).create().get()
        assertTrue(activity.isFinishing)
        assertEquals(null, Shadows.shadowOf(activity).nextStartedActivityForResult)
    }

    @Test
    fun independentReadersObserveCommittedStageAndFreshAttemptClearsOldFailure() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val writer = PS2LaunchJournal(context)
        writer.begin()
        writer.stage(PS2LaunchJournal.Stage.HOST_INIT)
        writer.failure("IllegalStateException /private/rom.iso")
        val reader = PS2LaunchJournal(context)
        assertEquals("HOST_INIT", reader.read().getProperty("stage"))
        assertFalse(reader.read().getProperty("error").contains('/'))
        writer.begin()
        assertEquals("REQUESTED", reader.read().getProperty("stage"))
        assertEquals(null, reader.read().getProperty("error"))
        assertEquals("0", reader.read().getProperty("pid"))
    }

    @Test
    fun legacyFrontendDrawDoesNotBecomeCoreSuccessOrTerminateGameplay() {
        assertTrue(PS2LaunchJournal.startupComplete("FRONTEND_FRAME", false))
        assertFalse(PS2LaunchJournal.startupComplete("FRONTEND_FRAME", true))
        assertTrue(PS2LaunchJournal.startupComplete("CORE_FRAME", true))
        assertFalse(PS2LaunchJournal.startupComplete("HOST_INIT", false))
    }

    @Test
    fun firstFrameIsOnlyRecordedExplicitlyAndStagingHasLargerBoundedBudget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val journal = PS2LaunchJournal(context)
        journal.begin()
        journal.stage(PS2LaunchJournal.Stage.ACTIVITY_STARTED)
        assertFalse(journal.read().getProperty("stage") == "FRONTEND_FRAME")
        assertTrue(
            PS2LaunchJournal.startupBudgetMillis("ROM_STAGE") > PS2LaunchJournal.startupBudgetMillis("HOST_INIT"),
        )
        assertEquals(180_000L, PS2LaunchJournal.startupBudgetMillis("HOST_INIT"))
        assertEquals(180_000L, PS2LaunchJournal.startupBudgetMillis("FRONTEND_FRAME"))
        journal.stage(PS2LaunchJournal.Stage.CORE_FRAME)
        journal.stage(PS2LaunchJournal.Stage.FRONTEND_FRAME)
        assertEquals("CORE_FRAME", journal.read().getProperty("stage"))
    }
}
