package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.Application
import android.content.Context
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w393dp-h852dp-mdpi")
class StandardHandheldHoldTrayUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun dualScreenTrayRoutesOnlyTheSupportedSavedHoldKey() {
        val selected = KeyEvent.KEYCODE_BUTTON_X
        val mic = KeyEvent.KEYCODE_BUTTON_L2
        compose.activity.getSharedPreferences("cartdeck_standard_hold_tray_v1", Context.MODE_PRIVATE)
            .edit().putString(SystemID.NDS.dbname, "$selected,$mic").commit()
        val native = NativeControlInventory.forHoldTray(TouchControllerID.MELONDS)!!
        val measured = native.controls.associate { it.seat to Rect(20f, 480f, 110f, 570f) }
        var activeKeys = emptySet<Int>()
        val holds = mutableListOf<Int>()
        compose.setContent {
            CartridgeLibraryTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Box(Modifier.fillMaxSize()) {
                        StandardHoldTray(
                            SystemID.NDS,
                            native,
                            Rect(0f, 0f, 393f, 852f),
                            Rect(0f, 24f, 393f, 448f),
                            measured,
                            emptySet(),
                            onKeys = { activeKeys = it },
                            onToggle = { holds += it },
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Hold X").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Hold Mic").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(setOf(selected), activeKeys)
            assertEquals(listOf(selected), holds)
        }
    }
}
