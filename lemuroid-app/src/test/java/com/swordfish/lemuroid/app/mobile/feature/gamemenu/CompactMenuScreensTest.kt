package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.InputDeviceBuilder
import org.robolectric.shadows.ShadowInputManager
import org.robolectric.shadows.ShadowInputManagerGlobal
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = Application::class,
    qualifiers = "w900dp-h1000dp-mdpi",
    shadows = [MenuFixtureInputManager::class, MenuFixtureInputManagerGlobal::class],
)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CompactMenuScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun xboxNestedScreensUseCompactRowsAndKeepInternalsInsideDiagnostics() {
        MenuFixtureInputManager.device =
            InputDeviceBuilder.newBuilder().setId(41)
                .setName("Xbox Wireless Controller").setDescriptor("gc01-ui-fixture").setControllerNumber(1)
                .setVendorId(0x045e).setProductId(1).setExternal(true)
                .setSources(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK).build()
        val session =
            NativeControllerMappingSession(
                compose.activity,
                compose.activity.lifecycleScope,
                TouchControllerID.XBOX,
                "menu-fixture",
            ) { 0 }
        val size = mutableStateOf(393 to 852)
        val appearance = mutableStateOf(ShelfAppearance())
        val engine = "hakuX · commit 0123456789abcdef0123456789abcdef01234567"
        compose.setContent {
            DisposableEffect(session) {
                session.start()
                onDispose { session.stop() }
            }
            Box(Modifier.requiredSize(size.value.first.dp, size.value.second.dp).testTag("fixture")) {
                XboxGameMenu(
                    "Fixture game.iso", engine, XboxMenuCapabilities(false, false, false),
                    {}, {}, {}, {}, {}, appearance = appearance.value,
                    pauseFeedback = "Engine pause is not confirmed.",
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        session.Panel()
                    }
                }
            }
        }
        for ((width, height) in listOf(393 to 852, 800 to 360)) {
            compose.runOnIdle { size.value = width to height }
            capture("xbox-main-${width}x$height")
            compose.onNodeWithText("Controls").performScrollTo().performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("A ← A").fetchSemanticsNodes().isNotEmpty()
            }
            val row = compose.onNodeWithText("A ← A")
            row.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            val bounds = row.getUnclippedBoundsInRoot()
            assertTrue((bounds.bottom - bounds.top).value <= 60f)
            compose.onNodeWithText("Last input: KEYCODE_BUTTON_A").assertDoesNotExist()
            capture("xbox-controls-${width}x$height")
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Advanced").performScrollTo().performClick()
            compose.onNodeWithText(engine).assertDoesNotExist()
            compose.onNodeWithText("Engine pause is not confirmed.").assertDoesNotExist()
            capture("xbox-advanced-${width}x$height")
            compose.onNodeWithText("Diagnostics").performClick()
            compose.onNodeWithText(engine).assertIsDisplayed()
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
            compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        }
        for (edition in listOf("pink", "green", "custom")) {
            compose.runOnIdle {
                appearance.value =
                    ShelfAppearance(
                        edition,
                        custom = if (edition == "custom") mapOf("accent" to 0xff00aaffL) else emptyMap(),
                    )
            }
            capture("xbox-main-$edition")
        }
        assertEquals("Fixture game", playerMenuTitle("Fixture game.iso"))
    }

    @Test
    fun universalControlsShowsFourCardsAndRespectsOrientationAvailability() {
        compose.setContent {
            Box(Modifier.requiredSize(393.dp, 852.dp).testTag("fixture")) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    UniversalControls(game = null)
                }
            }
        }

        compose.onNodeWithText("Standard").performScrollTo().assertIsDisplayed()
        capture("universal-controls-portrait")
        compose.onNodeWithText("Use PivotARC").performScrollTo().performClick()
        compose.onNodeWithText("PivotARC").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Preview landscape").performScrollTo().performClick()
        compose.onNodeWithText("PivotARC uses Standard controls in landscape.").performScrollTo().assertIsDisplayed()
        capture("universal-controls-landscape")
        compose.onNodeWithText("GripARC").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("PivotARC + GripARC").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Use Standard").performScrollTo().performClick()
    }

    private fun capture(name: String) {
        val rect = compose.onNodeWithTag("fixture").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val bitmap =
                Bitmap.createBitmap(
                    (rect.right - rect.left).value.toInt(),
                    (rect.bottom - rect.top).value.toInt(),
                    Bitmap.Config.ARGB_8888,
                )
            val canvas = Canvas(bitmap)
            canvas.translate(-rect.left.value, -rect.top.value)
            compose.activity.window.decorView.draw(canvas)
            val output = File("build/reports/gc01-menu-screens", "$name.png")
            output.parentFile!!.mkdirs()
            output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }
}

// Android 14 routes InputDevice's static queries through InputManagerGlobal.
@Implements(className = "android.hardware.input.InputManagerGlobal", minSdk = 34, isInAndroidSdk = false)
class MenuFixtureInputManagerGlobal : ShadowInputManagerGlobal() {
    @Implementation override fun getInputDeviceIds(): IntArray =
        MenuFixtureInputManager.device?.let { intArrayOf(it.id) } ?: intArrayOf()

    @Implementation fun getInputDevice(id: Int): InputDevice? = MenuFixtureInputManager.device?.takeIf { it.id == id }

    @Implementation fun deviceHasKeys(
        deviceId: Int,
        keyCodes: IntArray,
    ): BooleanArray = BooleanArray(keyCodes.size) { true }
}

@Implements(InputManager::class)
class MenuFixtureInputManager : ShadowInputManager() {
    companion object {
        var device: InputDevice? = null
    }

    @Implementation override fun getInputDeviceIds(): IntArray = device?.let { intArrayOf(it.id) } ?: intArrayOf()

    @Implementation fun getInputDevice(id: Int): InputDevice? = device?.takeIf { it.id == id }

    @Implementation override fun deviceHasKeys(
        deviceId: Int,
        keyCodes: IntArray,
    ): BooleanArray = BooleanArray(keyCodes.size) { true }
}
