package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.app.shared.game.viewmodel.dispatchPadKitDirection
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.layouts.shared.ComposeTouchLayouts
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputevents.InputEvent
import gg.padkit.inputstate.InputState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Exercise the production draw gate and actual accessory composables, beyond solver eligibility. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w393dp-h852dp-mdpi")
class GripArcRenderingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun pspPivotArcRendersAndDispatchesAnIndependentAnalogStick() {
        val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v22-manifest.json").readText())
        val baseline =
            PivotArcLayoutProfile.parse(
                File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
            )
        val device = ControlDeckDeviceFixture(393f, 852f)
        val side = GripArcSide.RIGHT
        val result =
            CompositeControlDeckSolver.resolve(manifest, baseline, SystemID.PSP, device.safe, device.viewport, side)
        val joint = (result as CompositeDeckResolution.Fits).placement
        val motions = java.util.concurrent.CopyOnWriteArrayList<Offset>()
        compose.setContent {
            val tokens = compose.activity.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL)
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(),
            ) {
                Box(Modifier.requiredSize(393.dp, 852.dp).testTag("psp-pivotarc")) {
                    PadKit(
                        onInputEvents = { events ->
                            events.filterIsInstance<InputEvent.ContinuousDirection>().forEach { event ->
                                dispatchPadKitDirection(event.id, event.direction) { id, x, y ->
                                    if (id == ComposeTouchLayouts.MOTION_SOURCE_LEFT_STICK) motions.add(Offset(x, y))
                                }
                            }
                        },
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        CompositeControlDeckHousing(joint, tokens)
                        PivotArcControlGroup(joint.pivot, SystemID.PSP, emptySet(), tokens.accent, {}, combined = true)
                        GripArcAccessory(
                            joint.dock,
                            GripArcProfile(ArcControlMode.PIVOTARC_GRIPARC, side, 4),
                            tokens,
                            fullscreenWithGamepad = false,
                            isLoading = false,
                            onPressed = { _, _, _ -> },
                            drawHousing = false,
                        )
                    }
                }
            }
        }
        val analog = compose.onNodeWithContentDescription("Analog stick")
        analog.assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        for (slot in 1..4) compose.onNodeWithTag("griparc-slot-$slot").assertIsDisplayed()
        compose.onNodeWithTag("griparc-engraving").assertIsDisplayed().assertHeightIsAtLeast(16.dp)
        val seat = joint.pivot.controls.getValue("Analog")
        val stickCenter = Offset(seat.centerX, seat.centerY)
        val deck = compose.onNodeWithTag("psp-pivotarc")
        deck.performTouchInput { down(stickCenter) }
        compose.runOnIdle { assertTrue("Analog down did not reach PadKit: $motions", motions.isNotEmpty()) }
        deck.performTouchInput { moveTo(stickCenter + Offset(22f, 18f)) }
        compose.runOnIdle {
            assertTrue(
                "Expected a finite diagonal before release, received $motions",
                motions.any { it.x.isFinite() && it.y.isFinite() && it.x != 0f && it.y != 0f },
            )
        }
        deck.performTouchInput { up() }
        compose.runOnIdle {
            val last = motions.last()
            assertTrue("Expected a neutral release, received $motions", last.x == 0f && last.y == 0f)
        }
        val rect = compose.onNodeWithTag("psp-pivotarc").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val bitmap = Bitmap.createBitmap(393, 852, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-rect.left.value, -rect.top.value)
            compose.activity.window.decorView.draw(canvas)
            val image = File("build/reports/gc01-menu-screens/griparc", "psp-pivotarc-griparc.png")
            image.parentFile!!.mkdirs()
            image.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun dsPivotArcRendersAndDispatchesTheSelectedCoresNativeAuxiliaries() {
        val baseline =
            PivotArcLayoutProfile.parse(
                File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
            )
        val safe = PivotArcBounds(8f, 8f, 385f, 844f)
        val viewport = PivotArcBounds(22f, 36f, 371f, 452f)
        val result =
            PivotArcPositionSolver.resolve(
                baseline,
                PivotArcLayoutProfile.activeFor(SystemID.NDS).orEmpty(),
                safe,
                viewport,
                false,
                0f,
                0f,
                false,
            ) as PivotArcResolution.Fits
        val placement = result.placement
        val controller = mutableStateOf(TouchControllerID.MELONDS)
        val buttons = mutableListOf<InputEvent.Button>()
        compose.setContent {
            val tokens = compose.activity.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL)
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(),
            ) {
                Box(Modifier.requiredSize(393.dp, 852.dp).testTag("ds-pivotarc")) {
                    PadKit(
                        onInputEvents = { buttons.addAll(it.filterIsInstance<InputEvent.Button>()) },
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        PivotArcControlGroup(
                            placement,
                            SystemID.NDS,
                            emptySet(),
                            tokens.accent,
                            {},
                            touchController = controller.value,
                        )
                    }
                }
            }
        }
        val deck = compose.onNodeWithTag("ds-pivotarc")
        for (backend in listOf(TouchControllerID.MELONDS, TouchControllerID.DESMUME)) {
            compose.runOnIdle { controller.value = backend }
            for (key in listOf("Mic", "Lid")) {
                compose.onNodeWithContentDescription(
                    key,
                ).assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                val bounds = placement.controls.getValue(key)
                val code = pivotArcDsAuxiliaryKey(backend, key)
                deck.performTouchInput { down(Offset(bounds.centerX, bounds.centerY)) }
                compose.runOnIdle {
                    assertTrue(
                        "$backend $key should press $code: $buttons",
                        buttons.last().let { it.id == code && it.pressed },
                    )
                }
                deck.performTouchInput { up() }
                compose.runOnIdle {
                    assertTrue(
                        "$backend $key should release $code: $buttons",
                        buttons.last().let { it.id == code && !it.pressed },
                    )
                }
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun portraitFixtureRendersNativeControlsAndFourAccessoryTargetsTogether() {
        val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v22-manifest.json").readText())
        val baseline =
            PivotArcLayoutProfile.parse(
                File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
            )
        val device = ControlDeckDeviceFixture(393f, 852f)
        val native = NativeControlInventory.forController(TouchControllerID.GBA)!!
        var mode by mutableStateOf(ArcControlMode.STANDARD_GRIPARC)
        val side = GripArcSide.RIGHT
        val standard =
            GripArcCombinedLayout.resolveDockFirst(
                manifest,
                native,
                device.safe,
                device.viewport,
                side,
                0f,
                0f,
            )!!
        val joint =
            (
                CompositeControlDeckSolver.resolve(
                    manifest,
                    baseline,
                    SystemID.GBA,
                    device.safe,
                    device.viewport,
                    side,
                ) as CompositeDeckResolution.Fits
            ).placement
        compose.setContent {
            val tokens = compose.activity.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL)
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(),
            ) {
                Box(Modifier.requiredSize(393.dp, 852.dp).testTag("griparc-render")) {
                    PadKit(
                        onInputEvents = {},
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        if (mode.hasPivot) {
                            CompositeControlDeckHousing(joint, tokens)
                            PivotArcControlGroup(
                                joint.pivot,
                                SystemID.GBA,
                                emptySet(),
                                tokens.accent,
                                {},
                                combined = true,
                            )
                        } else {
                            GripArcCombinedControls(standard)
                        }
                        GripArcAccessory(
                            if (mode.hasPivot) joint.dock else standard.dock,
                            GripArcProfile(mode, side, 4),
                            tokens,
                            fullscreenWithGamepad = false,
                            isLoading = false,
                            onPressed = { _, _, _ -> },
                            drawHousing = !mode.hasPivot,
                        )
                    }
                }
            }
        }
        for (fixtureMode in listOf(ArcControlMode.STANDARD_GRIPARC, ArcControlMode.PIVOTARC_GRIPARC)) {
            compose.runOnIdle { mode = fixtureMode }
            for (slot in 1..4) compose.onNodeWithTag("griparc-slot-$slot").assertIsDisplayed()
            val rect = compose.onNodeWithTag("griparc-render").getUnclippedBoundsInRoot()
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(393, 852, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.translate(-rect.left.value, -rect.top.value)
                compose.activity.window.decorView.draw(canvas)
                val file = File("build/reports/gc01-menu-screens/griparc", "${fixtureMode.name.lowercase()}.png")
                file.parentFile!!.mkdirs()
                file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                bitmap.recycle()
            }
        }
    }

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
                joint?.dock ?: GripArcCombinedLayout.resolveDockFirst(
                    manifest, native, device.safe, device.viewport, profile.side, 0f, 0f,
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
