package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfMaterials
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w900dp-h1000dp-mdpi")
class SharedGameMenuTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun xboxNavigationResumesSameSessionAndDiagnosticsStayInAdvanced() {
        var visible by mutableStateOf(true)
        var resumes = 0
        var probes = 0
        val clipboard = compose.activity.getSystemService(ClipboardManager::class.java)
        compose.setContent {
            if (visible) {
                XboxGameMenu(
                    "Fixture cartridge", "Fixture engine", XboxMenuCapabilities(true, true, false),
                    onResume = {
                        resumes++
                        visible = false
                    }, onReturnToShelf = {},
                    onCopyReport = {
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("Engine report", "Fixture report"),
                        )
                    },
                    onProbe = { probes++ }, onPreviousEngine = {}, appearance = ShelfAppearance(),
                ) { Text("Touch controls fixture") }
            }
        }
        compose.onNodeWithText("Resume").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Save / Load").assertDoesNotExist()
        compose.onNodeWithText("Game Settings").assertDoesNotExist()
        compose.onNodeWithText("Copy engine/GPU report").assertDoesNotExist()
        compose.onNodeWithText("Controls").performClick()
        compose.onNodeWithText("Touch controls fixture").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Advanced").performClick()
        compose.onNodeWithText("Previous Engine").assertDoesNotExist()
        compose.onNodeWithText("Fixture engine").assertIsDisplayed()
        compose.onNodeWithText("Copy engine/GPU report").performClick()
        compose.runOnIdle { assertEquals("Fixture report", clipboard.primaryClip!!.getItemAt(0).text) }
        compose.onNodeWithText("Troubleshooting").performClick()
        compose.onNodeWithText("Probe blank viewport (debug)").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, probes)
            assertEquals(0, resumes)
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.onNodeWithText("Copy engine/GPU report").assertIsDisplayed()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Resume").performClick()
        compose.onNodeWithTag("game-menu").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, resumes) }
    }

    @Test fun releaseEngineHidesProbeAndRollbackRequiresCapability() {
        var capabilities by mutableStateOf(XboxMenuCapabilities(false, true, false))
        var rollbacks = 0
        var quits = 0
        compose.setContent {
            XboxGameMenu(
                "Fixture", "Engine", capabilities, {}, { quits++ }, {}, {}, { rollbacks++ },
                appearance = ShelfAppearance(),
            ) { Text("Controls fixture") }
        }
        compose.onNodeWithText("Advanced").performClick()
        compose.onNodeWithText("Previous Engine").assertDoesNotExist()
        compose.runOnIdle { capabilities = capabilities.copy(rollback = true) }
        compose.onNodeWithText("Previous Engine").performClick()
        compose.runOnIdle { assertEquals(1, rollbacks) }
        compose.onNodeWithText("Troubleshooting").performClick()
        compose.onNodeWithText("Probe blank viewport (debug)").assertDoesNotExist()
        compose.runOnIdle { capabilities = XboxMenuCapabilities(true, false, true) }
        compose.onNodeWithText("Probe blank viewport (debug)").assertDoesNotExist()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Return to Shelf").performClick()
        compose.runOnIdle { assertEquals(1, quits) }
    }

    @Test fun editionsCustomAppearanceAndPortraitLandscapeUseSameHome() {
        var appearance by mutableStateOf(ShelfAppearance())
        var width by mutableStateOf(393)
        var height by mutableStateOf(852)
        var primary = Color.Unspecified
        compose.setContent {
            Box(Modifier.requiredSize(width.dp, height.dp)) {
                GameMenuTheme(appearance) {
                    val color = MaterialTheme.colorScheme.primary
                    SideEffect { primary = color }
                    GameMenuPanel("Fixture", "Console", "Paused", false, {}, {}) {
                        SharedGameMenuHome(GameMenuCapabilities(true, true, true, true)) { }
                    }
                }
            }
        }
        for (edition in listOf("original", "pink", "green", "custom")) {
            for ((w, h) in listOf(320 to 640, 393 to 852, 800 to 360)) {
                val selected =
                    ShelfAppearance(
                        edition,
                        custom = if (edition == "custom") mapOf("accent" to 0xff00aaffL) else emptyMap(),
                    )
                compose.runOnIdle {
                    appearance = selected
                    width = w
                    height = h
                }
                compose.onNodeWithText("Resume").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
                compose.onNodeWithText("Return to Shelf").assertIsDisplayed()
                compose.onNodeWithText("Save / Load").performScrollTo().assertIsDisplayed()
                compose.runOnIdle { assertEquals(shelfMaterials(selected).interactionAccent, primary) }
            }
        }
    }
}
