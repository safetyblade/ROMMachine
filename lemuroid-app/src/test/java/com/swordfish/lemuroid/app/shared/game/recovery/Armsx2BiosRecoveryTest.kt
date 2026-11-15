package com.swordfish.lemuroid.app.shared.game.recovery

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2BiosRecovery
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Storage
import com.swordfish.lemuroid.app.shared.gamecrash.GameCrashActivity
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class Armsx2BiosRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val game =
        Game(
            42,
            "fixture.iso",
            "content://fixture/disc",
            "Fixture",
            SystemID.PLAYSTATION_2.dbname,
            null,
            null,
            0,
        )

    private fun target(): Intent =
        Armsx2BiosRecovery.fromResult(
            context,
            Intent().putExtra(Armsx2BiosRecovery.REQUIRED, true)
                .putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME, game),
        )!!

    @Test fun onlyExplicitPs2BiosFailureOffersSetupAndPreservesCartridgeIdentity() {
        assertNull(Armsx2BiosRecovery.fromResult(context, Intent()))
        val recovery = target()
        assertEquals(Armsx2BiosRecovery.ACTIVITY, recovery.component!!.className)
        assertTrue(recovery.getBooleanExtra(Armsx2BiosRecovery.SETUP, false))
        assertEquals(game.cartridgeId, (recovery.getSerializableExtra("cartdeck.game") as Game).cartridgeId)
        assertNull(Armsx2BiosRecovery.validate(context, Intent(recovery).setClassName("other.app", "Other")))
        assertNull(
            Armsx2BiosRecovery.fromResult(
                context,
                Intent().putExtra(Armsx2BiosRecovery.REQUIRED, true)
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME, game.copy(systemId = SystemID.PSP.dbname)),
            ),
        )
    }

    @Test fun returnedErrorScreenActuallyExposesTheImportActionAndCancellationKeepsItAvailable() {
        val recovery = target()
        val controller =
            Robolectric.buildActivity(
                GameCrashActivity::class.java,
                Intent(
                    context,
                    GameCrashActivity::class.java,
                ).putExtra(GameCrashActivity.EXTRA_BIOS_RECOVERY, recovery),
            )
        controller.get().setTheme(R.style.LemuroidMaterialTheme_Game)
        val activity = controller.create().start().resume().get()
        val button = activity.findViewById<Button>(R.id.importPs2Bios)
        assertEquals(View.VISIBLE, button.visibility)
        button.performClick()
        val launched = shadowOf(activity).nextStartedActivityForResult
        assertEquals(Armsx2BiosRecovery.ACTIVITY, launched.intent.component!!.className)
        assertTrue(launched.intent.getBooleanExtra(Armsx2BiosRecovery.SETUP, false))
        shadowOf(activity).receiveResult(launched.intent, Activity.RESULT_CANCELED, null)
        assertFalse(activity.isFinishing)
        assertEquals(View.VISIBLE, button.visibility)
        controller.pause().stop().destroy()
    }

    @Test fun successfulImportReturnsToShelfInsteadOfLeavingTheOldFailureScreen() {
        val controller =
            Robolectric.buildActivity(
                GameCrashActivity::class.java,
                Intent(context, GameCrashActivity::class.java)
                    .putExtra(GameCrashActivity.EXTRA_BIOS_RECOVERY, target()),
            )
        controller.get().setTheme(R.style.LemuroidMaterialTheme_Game)
        val activity = controller.create().start().resume().get()
        activity.findViewById<Button>(R.id.importPs2Bios).performClick()
        val launched = shadowOf(activity).nextStartedActivityForResult
        shadowOf(activity).receiveResult(launched.intent, Activity.RESULT_OK, Intent())
        assertTrue(activity.isFinishing)
        controller.pause().stop().destroy()
    }

    @Test fun mainSettingsSetupSharesTheExistingBiosWithoutInventingACartridge() {
        val setup = Armsx2BiosRecovery.fromSettings(context)
        assertNull(setup.getSerializableExtra("cartdeck.game"))
        assertEquals(setup, Armsx2BiosRecovery.validate(context, setup))
        assertEquals(
            Armsx2Storage.paths(context.filesDir, game.cartridgeId).bios,
            Armsx2Storage.setupPaths(context.filesDir).bios,
        )
    }

    @Test fun automaticPs2ChoiceRequiresConfiguredBiosAndExplicitChoiceSurvives() {
        val preferences = EngineSelectionPreferences(context)
        assertEquals("play_modern", preferences.ps2AutomaticEngine("play_modern", "armsx2-android", false))
        assertEquals("armsx2-android", preferences.ps2AutomaticEngine("play_modern", "armsx2-android", true))
        assertEquals("play_modern", preferences.ps2AutomaticEngine("play_modern", null, true))
        assertEquals(
            "armsx2-android",
            preferences.requestedEngine(
                SystemID.PLAYSTATION_2,
                null,
                "play_modern",
                "armsx2-android",
            ),
        )
        assertEquals(
            "play_modern",
            preferences.requestedEngine(
                SystemID.PLAYSTATION_2,
                "play_modern",
                "armsx2-android",
                "armsx2-android",
            ),
        )
        assertEquals(
            "play_modern",
            preferences.requestedEngine(
                SystemID.PLAYSTATION_2,
                null,
                "armsx2-android",
                "play_modern",
            ),
        )
        assertEquals(
            "armsx2-android",
            preferences.requestedEngine(
                SystemID.PLAYSTATION_2,
                "armsx2-android",
                "play_modern",
                "play_modern",
            ),
        )
        assertEquals("prior", preferences.requestedEngine(SystemID.PSP, null, "prior", "legacy"))
    }

    @Test fun unrelatedConsoleErrorsDoNotGetPs2Setup() {
        val controller = Robolectric.buildActivity(GameCrashActivity::class.java)
        controller.get().setTheme(R.style.LemuroidMaterialTheme_Game)
        val activity = controller.create().get()
        assertEquals(View.GONE, activity.findViewById<Button>(R.id.importPs2Bios).visibility)
        controller.destroy()
    }
}
