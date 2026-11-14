package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeRecoveryChoiceTest {
    @Test
    fun resultChoiceReplacesWarmLauncherPreferenceForOnlyThisCartridge() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val system = GameSystem.findById(SystemID.NDS.dbname)
        val engines = NativeEngineRegistry.engines(system)
        val game = Game(42, "fixture.nds", "content://fixtures/disc", "Fixture", system.id.dbname, null, null, 0)
        val warmed = EngineSelectionPreferences(context)
        assertTrue(warmed.setPreferred(system, engines[0].id))
        assertTrue(warmed.setPreferred(system, engines[0].id, game))
        val result = Intent().putExtra("cartdeck.reopen.engine", engines[1].id)
        assertTrue(GameLauncher.applyRecoveryChoice(context, result, game))
        assertEquals(engines[1].id, warmed.select(game, system.systemCoreConfigs.first()).engine.id)
        assertEquals(engines[0].id, warmed.preferred(system.id))
        assertFalse(
            GameLauncher.applyRecoveryChoice(context, Intent().putExtra("cartdeck.reopen.engine", "missing"), game),
        )
        assertEquals(engines[1].id, warmed.preferred(system.id, game))
    }
}
