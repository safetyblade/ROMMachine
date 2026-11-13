package com.swordfish.lemuroid.app.engine

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineSelectionPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val system = GameSystem.findById(SystemID.NDS.dbname)
    private val engines = NativeEngineRegistry.engines(system)
    private val game = Game(42, "fixture.nds", "content://fixtures/disc", "Fixture", system.id.dbname, null, null, 0)

    @Test
    fun cartridgeOverrideWinsConsolePreferenceAndSurvivesNewInstancesAndShelfIds() {
        val preferences = EngineSelectionPreferences(context)
        assertTrue(preferences.setPreferred(system, engines[0].id))
        assertTrue(preferences.setPreferred(system, engines[1].id, game))
        val reopened = EngineSelectionPreferences(context)
        assertEquals(engines[1].id, reopened.select(game.copy(id = 99), system.systemCoreConfigs.first()).engine.id)
        assertTrue(reopened.inheritConsole(game))
        assertEquals(engines[0].id, reopened.select(game, system.systemCoreConfigs.first()).engine.id)
        assertFalse(reopened.setPreferred(system, "unbuilt_engine", game))
    }

    @Test
    fun automaticKeepsObservedSuccessfulEngineAndResetAllowsExplicitRetry() {
        val preferences = EngineSelectionPreferences(context)
        preferences.setPreferred(system, null, game)
        preferences.beginLaunch(game, engines[0])
        preferences.recordSuccess(game)
        assertEquals(engines[0].id, preferences.select(game, system.systemCoreConfigs[1]).engine.id)
        preferences.recordFailure(game, "LOAD_GAME")
        assertBlocked(preferences)
        preferences.setPreferred(system, null, game)
        assertEquals(engines[0].id, preferences.select(game, system.systemCoreConfigs.first()).engine.id)
    }

    @Test
    fun failureNeverCyclesAndFallbackRequiresUserPolicyAndPreviouslyProvenEngine() {
        val preferences = EngineSelectionPreferences(context)
        preferences.setPreferred(system, engines[0].id, game)
        preferences.beginLaunch(game, engines[0])
        preferences.recordSuccess(game)
        // Explicit user choice does not discard the prior success: begin directly models the new attempt.
        preferences.beginLaunch(game, engines[1])
        preferences.recordFailure(game, "RETRO_INIT")
        preferences.setPreferred(system, engines[1].id)
        preferences.inheritConsole(game)
        assertBlocked(preferences)
        preferences.setFallbackPolicy(system.id, EngineSelectionPreferences.FallbackPolicy.NEXT_MANUAL_LAUNCH)
        val fallback = preferences.select(game, system.systemCoreConfigs[1])
        assertEquals(engines[0].id, fallback.engine.id)
        assertTrue(fallback.changedEngine)
        preferences.beginLaunch(game, fallback.engine)
        preferences.recordFailure(game, "LOAD_GAME")
        assertBlocked(preferences)
    }

    @Test
    fun failureAfterCoreVideoCanReturnToPriorSuccessfulEngineWithoutCycling() {
        val preferences = EngineSelectionPreferences(context)
        preferences.setPreferred(system, engines[0].id, game)
        preferences.beginLaunch(game, engines[0])
        preferences.recordSuccess(game)
        preferences.setPreferred(system, engines[1].id, game)
        preferences.beginLaunch(game, engines[1])
        preferences.recordSuccess(game)
        preferences.recordFailure(game, "CORE_STOPPED")
        preferences.setFallbackPolicy(system.id, EngineSelectionPreferences.FallbackPolicy.NEXT_MANUAL_LAUNCH)
        val fallback = preferences.select(game, system.systemCoreConfigs[1])
        assertEquals(engines[0].id, fallback.engine.id)
        assertTrue(fallback.changedEngine)
        preferences.beginLaunch(game, fallback.engine)
        preferences.recordFailure(game, "CORE_STOPPED")
        assertBlocked(preferences)
    }

    @Test
    fun acceptedPresentationDoesNotReplaceProvenEngineOrClearFailure() {
        val preferences = EngineSelectionPreferences(context)
        preferences.beginLaunch(game, engines[0])
        preferences.recordSuccess(game)
        preferences.beginLaunch(game, engines[1])
        preferences.recordPresentation(game)
        assertEquals(engines[0].id, preferences.select(game, system.systemCoreConfigs[1]).engine.id)
        preferences.recordFailure(game, "NATIVE_STOPPED")
        preferences.recordPresentation(game)
        assertEquals("NATIVE_STOPPED", preferences.failureStage(game))
        assertNull(preferences.previous(game))
    }

    @Test
    fun xboxPresentCallbackCannotCreateGraphicalSuccessEvenThroughLegacyMethod() {
        val preferences = EngineSelectionPreferences(context)
        val xbox = GameSystem.findById(SystemID.XBOX.dbname)
        val cartridge = game.copy(systemId = xbox.id.dbname, fileUri = "content://fixtures/xbox-presentation")
        preferences.beginLaunch(cartridge, NativeEngineRegistry.engines(xbox).single())
        preferences.recordSuccess(cartridge)
        val records =
            java.io.File(context.filesDir, "native-engine-outcomes").listFiles().orEmpty()
                .filter { it.name.startsWith("game_xbox_") && it.extension == "properties" }
                .map { file -> java.util.Properties().apply { file.inputStream().use { load(it) } } }
        val evidence = records.single { it.getProperty("lastSelected") == "hakux" }
        assertEquals("PRESENT_ACCEPTED", evidence.getProperty("stage"))
        assertTrue(evidence.getProperty("presentationAccepted").startsWith("hakux:"))
        assertNull(evidence.getProperty("successful"))
        assertNull(evidence.getProperty("previous"))
    }

    private fun assertBlocked(preferences: EngineSelectionPreferences) {
        var blocked = false
        try {
            preferences.select(game, system.systemCoreConfigs.first())
        } catch (_: EngineSelectionPreferences.SelectionException) {
            blocked = true
        }
        assertTrue("A confirmed failed engine must require explicit recovery", blocked)
    }
}
