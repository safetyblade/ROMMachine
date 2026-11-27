package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.app.mobile.feature.game.ArcControlMode
import com.swordfish.lemuroid.app.mobile.feature.game.CartdeckLayoutMode
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcSettingsStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LibretroGameMenuTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var result: Intent? = null

    private fun onResult(block: Intent.() -> Unit) {
        result = Intent().apply(block)
    }

    private fun request(system: SystemID): GameMenuActivity.GameMenuRequest {
        val config =
            if (system == SystemID.WII) {
                NativeGameSystems.configurations().single { it.id == system }.systemCoreConfigs.single()
            } else {
                GameSystem.findById(system.dbname).systemCoreConfigs.first()
            }
        return GameMenuActivity.GameMenuRequest(
            emptyList(), emptyList(),
            Game(
                id = 1,
                title = "Fixture cartridge",
                fileName = "fixture.rom",
                fileUri = "content://fixture/1",
                systemId = system.dbname,
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
            ),
            config, null, true, false, false, 2, 0, TiltConfiguration.Disabled, emptyList(),
        )
    }

    @Test fun saveLoadAndControlsKeepTheirExistingRoutes() {
        val request = request(SystemID.GBA)
        val store = GripArcProfileStore(compose.activity)
        store.saveLayout(request.game, ArcControlMode.STANDARD_GRIPARC)
        compose.setContent {
            GameMenuTheme(ShelfAppearance()) {
                val nav = rememberNavController()
                NavHost(nav, GameMenuRoute.HOME.route) {
                    composable(GameMenuRoute.HOME) { GameMenuHomeScreen(nav, request, ::onResult) }
                    composable(GameMenuRoute.STATES) { GameMenuStatesHome(nav, request) }
                    composable(GameMenuRoute.SAVE) { androidx.compose.material3.Text("Save backend fixture") }
                    composable(GameMenuRoute.LOAD) { androidx.compose.material3.Text("Load backend fixture") }
                    composable(GameMenuRoute.CONTROLS) { GameMenuControlsScreen(nav, request, ::onResult) }
                }
            }
        }
        compose.onNodeWithText("Save & Load").performClick()
        compose.onNodeWithText(
            compose.activity.getString(com.swordfish.lemuroid.R.string.game_menu_save),
        ).performClick()
        compose.onNodeWithText("Save backend fixture").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText(
            compose.activity.getString(com.swordfish.lemuroid.R.string.game_menu_load),
        ).performClick()
        compose.onNodeWithText("Load backend fixture").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Controls").performClick()
        compose.onNodeWithText("PivotARC").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Programmable controls").performScrollTo().performClick()
        compose.onNodeWithText("Tap a button to assign an input or a simultaneous combination.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inheritingLayoutKeepsProgrammableAssignments() {
        val game = request(SystemID.GBA).game
        val store = GripArcProfileStore(compose.activity)
        val settings = PivotArcSettingsStore(compose.activity)
        val initial = store.defaultFor(game, ArcControlMode.STANDARD_GRIPARC)
        val custom = initial.copy(mappings = initial.mappings.reversed(), dx = 12f)
        assertTrue(store.save(game, custom))
        assertTrue(store.useGlobalLayout(game))
        settings.save(settings.read().copy(mode = CartdeckLayoutMode.ONE_HAND))
        assertEquals(ArcControlMode.PIVOTARC, store.read(game).mode)
        assertEquals(custom.mappings, store.read(game).mappings)
        assertEquals(custom.dx, store.read(game).dx)
    }

    @Test fun switchingUntouchedLayoutsDoesNotSeedLegacyJointWithStandaloneAssignments() {
        val game = request(SystemID.GBA).game
        val store = GripArcProfileStore(compose.activity)
        assertTrue(store.saveLayout(game, ArcControlMode.STANDARD_GRIPARC))
        assertEquals(store.defaultFor(game, ArcControlMode.STANDARD_GRIPARC), store.read(game))
        assertTrue(store.saveLayout(game, ArcControlMode.PIVOTARC_GRIPARC))
        assertEquals(store.defaultFor(game, ArcControlMode.PIVOTARC_GRIPARC), store.read(game))
        val custom =
            store.read(
                game,
            ).copy(
                mappings =
                    listOf(
                        listOf(android.view.KeyEvent.KEYCODE_BUTTON_A),
                        emptyList(),
                        emptyList(),
                        emptyList(),
                    ),
            )
        assertTrue(store.save(game, custom))
        assertTrue(store.saveLayout(game, ArcControlMode.STANDARD_GRIPARC))
        assertEquals(custom.mappings, store.read(game).mappings)
    }

    @Test fun nativeModeAssignmentsKeepTheirIndependentValuesAndLegacyFallback() {
        val game = request(SystemID.PSP).game
        val store = GripArcProfileStore(compose.activity)
        val layout = ArcControlMode.STANDARD_GRIPARC
        assertTrue(store.saveLayout(game, layout))
        val legacy = store.read(game)
        val dpad = legacy.copy(mappings = legacy.mappings.reversed())
        val analogue = legacy.copy(mappings = List(4) { emptyList() })
        assertTrue(store.save(game, dpad, "PSP:dpad"))
        assertTrue(store.save(game, analogue, "PSP:analogue"))
        assertEquals(dpad, store.read(game, "PSP:dpad"))
        assertEquals(analogue, store.read(game, "PSP:analogue"))
        assertEquals(legacy, store.read(game))
        assertTrue(store.saveLayout(game, ArcControlMode.PIVOTARC_GRIPARC))
        assertEquals(store.read(game), store.read(game, "PSP:dpad"))
        assertTrue(store.saveLayout(game, layout))
        assertEquals(dpad, store.read(game, "PSP:dpad"))
    }

    @Test fun wiiControlsRetainAllFourNativeChoicesAndCartridgeResult() {
        val request = request(SystemID.WII)
        val controllers = request.coreConfig.controllerConfigs.getValue(0)
        assertEquals(
            setOf("wii_sideways", "wii_classic", "wii_nunchuk", "wii_gamecube"),
            controllers.map { it.name }.toSet(),
        )
        compose.setContent {
            GameMenuTheme(ShelfAppearance()) {
                GameMenuControlsScreen(rememberNavController(), request, ::onResult)
            }
        }
        compose.onNodeWithText("Wii controller").performClick()
        for (controller in controllers) compose.onNode(
            hasText(compose.activity.getString(controller.displayName)) and hasAnyAncestor(isDialog()),
        )
            .assertIsDisplayed()
        val gc = controllers.single { it.name == "wii_gamecube" }
        compose.onNode(
            hasText(compose.activity.getString(gc.displayName)) and hasAnyAncestor(isDialog()),
        ).performClick()
        compose.runOnIdle {
            assertEquals("wii_gamecube", result!!.getStringExtra(GameMenuContract.RESULT_CHANGE_WII_CONTROLLER))
        }
    }
}
