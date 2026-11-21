package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w700dp-h1000dp-mdpi")
class DisplaySettingsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun setupAndNativeMenuUseSameRomOverrideAndUnsupportedFeaturesStayHidden() {
        val game =
            Game(
                id = 4, title = "Fixture", fileName = "fixture.gb", fileUri = "content://rom/4",
                systemId = "gb", developer = null, coverFrontUrl = null,
                lastIndexedAt = 0, cartridgeId = "display-fixture",
            )
        val store = DisplaySettingsStore(compose.activity)
        store.set(null, "landscapeBorder", "STANDARD")
        store.set(game.cartridgeId, "landscapeBorder", "OFF")
        val nativeRoute = mutableStateOf(false)
        compose.setContent {
            MaterialTheme { DisplaySettingsScreen(game, libretroHost = !nativeRoute.value) }
        }
        compose.onNodeWithText("Use Global Default").performClick()
        compose.waitUntil(5000) { store.read(game.cartridgeId).own.landscapeBorder == null }
        compose.runOnIdle { nativeRoute.value = true }
        compose.onNodeWithText("Widescreen", substring = true).assertDoesNotExist()
        compose.onNodeWithText("HD scaling:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Help").assertIsDisplayed()
        compose.onAllNodesWithText("Hardware · selected")[0].assertIsDisplayed()
        compose.onNodeWithText("Use Global Default").performScrollTo().assertIsDisplayed()
        assertEquals(DisplaySettingsStore.Border.STANDARD, store.read(game.cartridgeId).effective.border(true))
    }
}
