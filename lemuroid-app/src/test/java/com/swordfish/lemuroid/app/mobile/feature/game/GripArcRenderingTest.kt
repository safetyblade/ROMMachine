package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.Application
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Exercise the production draw gate and actual accessory composables, beyond solver eligibility. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w393dp-h852dp-mdpi")
class GripArcRenderingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun combinedAccessoriesRemainDisplayedAcrossCountsThemesPressesAndRecomposition() {
        val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v22-manifest.json").readText())
        val baseline =
            PivotArcLayoutProfile.parse(
                File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
            )
        val device = ControlDeckDeviceFixture(393f, 852f)
        val native = NativeControlInventory.forController(TouchControllerID.GBA)!!
        var profile by mutableStateOf(GripArcProfile(mode = ArcControlMode.STANDARD_GRIPARC))
        var edition by mutableStateOf(CartdeckEditionPreset.ORIGINAL)
        var loading by mutableStateOf(false)
        var fullscreen by mutableStateOf(false)
        val events = mutableListOf<Triple<Int, Boolean, Set<Int>>>()
        compose.setContent {
            val joint =
                if (profile.mode.hasPivot) {
                    (
                        CompositeControlDeckSolver.resolve(
                            manifest,
                            baseline,
                            SystemID.GBA,
                            device.safe,
                            device.viewport,
                            profile.side,
                        ) as CompositeDeckResolution.Fits
                    ).placement
                } else {
                    null
                }
            val dock =
                joint?.dock ?: GripArcCombinedLayout.resolve(
                    manifest, native, native.inputIds, oversized, device.safe, device.viewport, profile.side, 0f, 0f,
                )!!.dock
            val tokens = compose.activity.resolveCartdeckEdition(edition)
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.requiredSize(393.dp, 852.dp)) {
                    if (joint != null && !loading && !fullscreen) CompositeControlDeckHousing(joint, tokens)
                    GripArcAccessory(
                        dock,
                        profile,
                        tokens,
                        fullscreen,
                        loading,
                        onPressed = { slot, pressed, mapping -> events += Triple(slot, pressed, mapping) },
                        drawHousing = joint == null,
                    )
                }
            }
        }
        for (mode in listOf(ArcControlMode.STANDARD_GRIPARC, ArcControlMode.PIVOTARC_GRIPARC)) {
            for (side in GripArcSide.values()) for (count in 1..4) for (theme in CartdeckEditionPreset.values()) {
                compose.runOnIdle {
                    edition = theme
                    profile = GripArcProfile(mode, side, count, List(4) { listOf(KeyEvent.KEYCODE_BUTTON_A) })
                }
                for (slot in 1..count) {
                    compose.onNodeWithTag("griparc-slot-$slot").assertIsDisplayed()
                        .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                }
                for (slot in count + 1..4) compose.onNodeWithTag("griparc-slot-$slot").assertDoesNotExist()
                if (mode == ArcControlMode.STANDARD_GRIPARC) {
                    compose.onNodeWithTag("griparc-housing").assertIsDisplayed()
                }
            }
        }
        compose.onNodeWithTag("griparc-slot-1").performTouchInput { down(center) }
        compose.waitForIdle()
        compose.onNodeWithTag("griparc-slot-1").assertIsDisplayed()
        compose.onNodeWithTag("griparc-slot-1").performTouchInput { up() }
        compose.runOnIdle {
            assertTrue(events.any { it == Triple(1, true, setOf(KeyEvent.KEYCODE_BUTTON_A)) })
            assertTrue(events.any { it.first == 1 && !it.second })
            loading = true
        }
        compose.onNodeWithTag("griparc-slot-1").assertDoesNotExist()
        compose.runOnIdle {
            loading = false
            fullscreen = true
        }
        compose.onNodeWithTag("griparc-slot-1").assertDoesNotExist()
        compose.runOnIdle { fullscreen = false }
        compose.onNodeWithTag("griparc-slot-1").assertIsDisplayed()
    }
}
