package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.ControlsLayoutPreview
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckAppearance
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
    fun standaloneDigitalPreviewRendersTheProductionControlsInBothOrientations() {
        val modes =
            linkedMapOf(
                SystemID.GBA to TouchControllerID.GBA,
                SystemID.GB to TouchControllerID.GB,
                SystemID.GBC to TouchControllerID.GB,
                SystemID.NES to TouchControllerID.NES,
                SystemID.SMS to TouchControllerID.SMS,
                SystemID.GG to TouchControllerID.GG,
                SystemID.NGP to TouchControllerID.NGP,
                SystemID.NGC to TouchControllerID.NGP,
                SystemID.PC_ENGINE to TouchControllerID.PCE,
                SystemID.SNES to TouchControllerID.SNES,
            )
        val current = mutableStateOf(modes.entries.first())
        val landscape = mutableStateOf(false)
        compose.setContent {
            Box(Modifier.requiredSize(360.dp, 216.dp).testTag("standalone-deck")) {
                ControlsLayoutPreview(
                    current.value.key,
                    current.value.value,
                    ArcControlMode.STANDARD_GRIPARC,
                    landscape.value,
                    "2",
                    standaloneGripFactory(current.value.key),
                )
            }
        }
        val folder = File("build/reports/gc01-menu-screens/griparc/standalone")
        folder.mkdirs()
        for (mode in modes.entries) {
            for (wide in listOf(false, true)) {
                compose.runOnIdle {
                    current.value = mode
                    landscape.value = wide
                }
                compose.onAllNodesWithText("GripARC cannot fit this reference handset.").assertCountEquals(0)
                val rect = compose.onNodeWithTag("standalone-deck").getUnclippedBoundsInRoot()
                compose.runOnIdle {
                    val bitmap = Bitmap.createBitmap(360, if (wide) 132 else 216, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = Canvas(bitmap)
                        canvas.translate(-rect.left.value, -rect.top.value)
                        compose.activity.window.decorView.draw(canvas)
                        val orientation = if (wide) "landscape" else "portrait"
                        File(folder, "${mode.key.name.lowercase()}-$orientation.png").outputStream().use {
                            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }

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
        for (key in listOf("A", "B")) {
            assertTrue(standard.hardwareSeats.getValue("face-${key.lowercase()}") == standard.controls.getValue(key))
        }
        assertTrue("face-ab" !in standard.hardwareSeats)
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
        val background = Color(0xFF272233)
        val root = device.screen.asRect()
        val video = device.viewport.asRect()
        compose.setContent {
            val appearance = referenceAppearance()
            val tokens = appearance.tokens
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(appearance.controllerPalette),
            ) {
                Box(Modifier.requiredSize(393.dp, 852.dp).background(background).testTag("griparc-render")) {
                    val controls = if (mode.hasPivot) joint.pivot.controls else standard.hardwareSeats
                    val dock = if (mode.hasPivot) joint.dock else standard.dock
                    CartdeckConsoleShell(tokens, video, root, false, controls.values.map { it.asRect() })
                    PadKit(
                        onInputEvents = {},
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        CartdeckHardwareSeats(
                            rootBounds = root,
                            videoBounds = video,
                            inputBounds =
                                controls.map { (key, bounds) ->
                                    (if (mode.hasPivot) pivotArcHardwareSeatKey(key) else key) to bounds.asRect()
                                }.toMap(),
                            tokens = tokens,
                            edition = CartdeckEditionPreset.ORIGINAL,
                            isDualScreen = false,
                            customColours = true,
                            pivotArc = mode.hasPivot,
                            measuredStandard = !mode.hasPivot,
                            joint = joint.takeIf { mode.hasPivot },
                        )
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
                            StandardGripArcDockJoint(standard, tokens)
                            GripArcHousing(dock, tokens)
                            GripArcCombinedControls(standard)
                        }
                        GripArcAccessory(
                            dock,
                            GripArcProfile(mode, side, 4),
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
        for (fixtureMode in listOf(ArcControlMode.STANDARD_GRIPARC, ArcControlMode.PIVOTARC_GRIPARC)) {
            compose.runOnIdle { mode = fixtureMode }
            for (slot in 1..4) {
                compose.onNodeWithTag("griparc-slot-$slot").assertIsDisplayed()
                    .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            }
            val expectedPivotWordmarks = if (fixtureMode.hasPivot) 1 else 0
            val wordmarks = compose.onAllNodesWithText("PivotArc").fetchSemanticsNodes()
            assertTrue(
                "$fixtureMode wordmarks=${wordmarks.size}, placement=${joint.wordmark}, " +
                    "upper=${joint.island.upper}, controls=${joint.pivot.controls}, future=${joint.futureFaces}",
                wordmarks.size == expectedPivotWordmarks,
            )
            val surround = compose.onNodeWithTag("griparc-housing").getUnclippedBoundsInRoot()
            val engraving = compose.onNodeWithTag("griparc-engraving").getUnclippedBoundsInRoot()
            val surroundWidth = surround.right - surround.left
            assertTrue(engraving.left > surround.left + surroundWidth * (106f / 160f))
            assertTrue(engraving.right < surround.right - surroundWidth * (10f / 160f))
            val dock = if (fixtureMode.hasPivot) joint.dock else standard.dock
            val nativeTargets = if (fixtureMode.hasPivot) joint.pivot.controls.values else standard.controls.values
            assertClearTargets(nativeTargets + dock.targets.values, device.viewport)
            captureHardware(fixtureMode.name.lowercase(), background, video) { bitmap ->
                val cap = dock.targets.getValue(1)
                bitmap.getPixel(cap.centerX.toInt(), cap.centerY.toInt()) != background.toArgb()
            }
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun soloGbaAndPspRenderFromTheirNativePivotPlacements() {
        val baseline =
            PivotArcLayoutProfile.parse(
                File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
            )
        val device = ControlDeckDeviceFixture(393f, 852f)
        val systems = listOf(SystemID.GBA, SystemID.PSP)

        fun resolve(
            system: SystemID,
            leftHand: Boolean,
            dx: Float = 0f,
        ): PivotArcPlacement =
            (
                PivotArcPositionSolver.resolve(
                    baseline,
                    PivotArcLayoutProfile.activeFor(system)!!,
                    device.safe,
                    device.viewport,
                    leftHand,
                    dx,
                    0f,
                    false,
                ) as PivotArcResolution.Fits
            ).placement

        val placements = systems.associateWith { resolve(it, leftHand = it == SystemID.GBA) }
        val adjusted = resolve(SystemID.GBA, leftHand = false, dx = 12f)
        assertTrue(placements.getValue(SystemID.GBA).controls.getValue("D") != adjusted.controls.getValue("D"))
        val background = Color(0xFF272233)
        val video = device.viewport.asRect()
        var system by mutableStateOf(SystemID.GBA)
        var useAdjustedLayout by mutableStateOf(false)
        compose.setContent {
            val appearance = referenceAppearance()
            val tokens = appearance.tokens
            val placement = if (useAdjustedLayout) adjusted else placements.getValue(system)
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(appearance.controllerPalette),
            ) {
                Box(Modifier.requiredSize(393.dp, 852.dp).background(background).testTag("griparc-render")) {
                    CartdeckConsoleShell(
                        tokens,
                        video,
                        device.screen.asRect(),
                        false,
                        placement.controls.values.map { it.asRect() },
                    )
                    CartdeckHardwareSeats(
                        rootBounds = device.screen.asRect(),
                        videoBounds = video,
                        inputBounds =
                            placement.controls.mapKeys { pivotArcHardwareSeatKey(it.key) }.mapValues {
                                it.value.asRect()
                            },
                        tokens = tokens,
                        edition = CartdeckEditionPreset.ORIGINAL,
                        isDualScreen = false,
                        customColours = true,
                        pivotArc = true,
                    )
                    PadKit(
                        onInputEvents = {},
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        PivotArcControlGroup(placement, system, emptySet(), tokens.accent, {})
                    }
                }
            }
        }
        for (fixtureSystem in systems) {
            compose.runOnIdle { system = fixtureSystem }
            val controls = placements.getValue(fixtureSystem).controls
            assertClearTargets(controls.values.toList(), device.viewport)
            if (fixtureSystem == SystemID.PSP) {
                compose.onNodeWithContentDescription("Analog stick").assertIsDisplayed()
                assertTrue(!controls.getValue("D").intersects(controls.getValue("Analog")))
            }
            assertTrue(compose.onAllNodesWithText("PivotArc").fetchSemanticsNodes().size <= 1)
            captureHardware("solo-${fixtureSystem.name.lowercase()}", background, video) { bitmap ->
                val dpad = controls.getValue("D")
                bitmap.getPixel(dpad.centerX.toInt(), dpad.centerY.toInt()) != background.toArgb()
            }
        }
        compose.runOnIdle {
            system = SystemID.GBA
            useAdjustedLayout = true
        }
        assertClearTargets(adjusted.controls.values.toList(), device.viewport)
        captureHardware("solo-gba-adjusted", background, video) { bitmap ->
            val dpad = adjusted.controls.getValue("D")
            bitmap.getPixel(dpad.centerX.toInt(), dpad.centerY.toInt()) != background.toArgb()
        }
    }

    private fun referenceAppearance() =
        resolveCartdeckAppearance(
            ShelfAppearance(
                custom =
                    mapOf(
                        "chassis" to 0xFF17171E,
                        "secondary" to 0xFF2C2538,
                        "bezel" to 0xFF111015,
                        "glass" to 0xFF100E14,
                        "button" to 0xFF402A4C,
                        "accent" to 0xFFE9599E,
                    ),
            ),
            compose.activity.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL),
        )

    private fun assertClearTargets(
        targets: List<PivotArcBounds>,
        viewport: PivotArcBounds,
    ) {
        targets.forEachIndexed { index, target ->
            assertTrue(
                "Target must have a usable 48dp area: $target",
                target.width >= 47.99f && target.height >= 47.99f,
            )
            assertTrue("Target overlaps viewport: $target", !target.intersects(viewport))
            assertTrue("Overlapping targets near $target", targets.drop(index + 1).none(target::intersects))
        }
    }

    private fun PivotArcBounds.asRect(): Rect = Rect(left, top, right, bottom)

    private fun captureHardware(
        name: String,
        background: Color,
        viewport: Rect,
        hasHardware: (Bitmap) -> Boolean,
    ) {
        val rect = compose.onNodeWithTag("griparc-render").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            val bitmap = Bitmap.createBitmap(393, 852, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-rect.left.value, -rect.top.value)
            compose.activity.window.decorView.draw(canvas)
            assertTrue("Hardware must render at the measured control", hasHardware(bitmap))
            assertTrue(
                "Controller material reached the viewport",
                bitmap.getPixel(viewport.center.x.toInt(), viewport.center.y.toInt()) == background.toArgb(),
            )
            val file = File("build/reports/gc01-menu-screens/griparc", "$name.png")
            file.parentFile!!.mkdirs()
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
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
        for (slot in 1..4) {
            compose.runOnIdle { events.clear() }
            val target = compose.onNodeWithTag("griparc-slot-$slot")
            target.performTouchInput { down(center) }
            compose.runOnIdle {
                assertTrue(events.any { it == Triple(slot, true, setOf(KeyEvent.KEYCODE_BUTTON_A)) })
            }
            target.performTouchInput { up() }
            compose.runOnIdle {
                assertTrue(events.any { it == Triple(slot, false, setOf(KeyEvent.KEYCODE_BUTTON_A)) })
            }
        }
        compose.runOnIdle { loading = true }
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
