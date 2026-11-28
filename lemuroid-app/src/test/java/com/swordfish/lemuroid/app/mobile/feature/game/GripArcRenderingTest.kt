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
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckAppearance
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.app.shared.game.viewmodel.dispatchPadKitDirection
import com.swordfish.lemuroid.lib.library.GameSystem
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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
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
    fun standalonePortraitFixturesUseTheProductionRenderer() = renderStandaloneFixtures(false)

    @Test
    @Config(qualifiers = "w740dp-h360dp-mdpi")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun standaloneLandscapeFixturesUseTheProductionRenderer() = renderStandaloneFixtures(true)

    /**
     * C01 regression: GameCube landscape must fit the reference handheld
     * fixture through its authored left/right hardware panels and utility
     * rail — never the legacy seat projection and never compact
     * directional targets — with the aperture allocated together with the
     * controller space.
     */
    @Test
    fun gameCubeLandscapeUsesAuthoredPanelsAndUtilityRail() {
        val inventory =
            checkNotNull(
                StandaloneGripArcLayout.inventoryFor(TouchControllerID.GAMECUBE, SystemID.GAMECUBE),
            )
        val safe = fixtureSafe(true)
        val viewport = fixtureViewport(SystemID.GAMECUBE, true)
        val placement =
            checkNotNull(
                StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, true),
            ) { "GameCube landscape did not fit its authored panels on the reference fixture" }
        assertTrue(placement.complete)
        val fit = checkNotNull(placement.standaloneGeometry).fit
        assertEquals(StandaloneGripArcLayout.GAMECUBE_LANDSCAPE_FIT, fit)
        assertTrue("Legacy projection used for GameCube landscape: $fit", !fit.contains("legacy"))
        assertTrue("Compact fallback used for GameCube landscape: $fit", !fit.contains("compact"))

        val gap = 6f
        val leftPanel = PivotArcBounds(safe.left, safe.top, viewport.left - gap, safe.bottom)
        val rightPanel = PivotArcBounds(viewport.right + gap, safe.top, safe.right, safe.bottom)
        val rail = PivotArcBounds(viewport.left, viewport.bottom + gap, viewport.right, safe.bottom)
        val leftKeys = setOf("L", "HalfL", "AnalogL", "D")
        val rightKeys = setOf("HalfR", "Z", "R", "Y", "X", "A", "B", "AnalogR")
        for (key in leftKeys) {
            assertTrue("$key is outside the left panel", placement.controls.getValue(key).inside(leftPanel))
        }
        for (key in rightKeys) {
            assertTrue("$key is outside the right panel", placement.controls.getValue(key).inside(rightPanel))
        }
        for (key in setOf("Menu", "Start")) {
            assertTrue("$key is outside the utility rail", placement.controls.getValue(key).inside(rail))
        }
        for ((slot, target) in placement.dock.targets) {
            val panel = if (slot <= 2) leftPanel else rightPanel
            assertTrue("program:$slot is outside its panel", target.inside(panel))
        }
        val display = viewport.inset(1.5f)
        val allTargets =
            placement.controls.values.toList() +
                placement.dock.targets.values
                    .toList()
        for (target in allTargets) {
            assertTrue("Target is outside the safe area: $target", target.inside(safe))
            assertTrue("Target overlaps the gameplay aperture: $target", !target.intersects(display))
        }
        for (index in allTargets.indices) {
            for (other in index + 1 until allTargets.size) {
                assertTrue(
                    "Targets overlap: ${allTargets[index]} / ${allTargets[other]}",
                    !allTargets[index].intersects(allTargets[other]),
                )
            }
        }
        // Larger directional and analogue controls; no compact shrink.
        assertTrue("D-pad was compact-shrunk", placement.controls.getValue("D").width >= 80f)
        assertTrue("Analogue stick was compact-shrunk", placement.controls.getValue("AnalogL").width >= 88f)
        // Console-authentic face cluster: A is the large central button.
        val a = placement.visuals.getValue("A")
        for (key in setOf("B", "X", "Y")) {
            assertTrue("GameCube A must stay larger than $key", a.w > placement.visuals.getValue(key).w)
        }
        // The gameplay aperture itself is untouched by the allocation.
        assertEquals(viewport, placement.standaloneGeometry?.viewport)
    }

    /**
     * C05-A: GameCube portrait uses the authored transform with explicit
     * dimensions — all 14 native and four programmable targets inside the
     * portrait panel, aperture untouched, analogue and D-pad at their
     * authored sizes (no compact shrink, no legacy route).
     */
    @Test
    fun gameCubePortraitUsesAuthoredGeometryWithExplicitSizes() {
        val inventory =
            checkNotNull(
                StandaloneGripArcLayout.inventoryFor(TouchControllerID.GAMECUBE, SystemID.GAMECUBE),
            )
        val safe = fixtureSafe(false)
        val viewport = fixtureViewport(SystemID.GAMECUBE, false)
        val placement =
            checkNotNull(
                StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, false),
            ) { "GameCube portrait did not fit its authored geometry on the reference fixture" }
        assertTrue(placement.complete)
        assertEquals("uniform chassis transform", checkNotNull(placement.standaloneGeometry).fit)
        val panel =
            PivotArcBounds(
                safe.left + 4f,
                maxOf(safe.top, viewport.bottom + 16f),
                safe.right - 4f,
                safe.bottom - 12f,
            )
        val display = viewport.inset(1.5f)
        val allTargets =
            placement.controls.values.toList() +
                placement.dock.targets.values
                    .toList()
        assertEquals(18, allTargets.size)
        for (target in allTargets) {
            assertTrue("Target is outside the portrait panel: $target", target.inside(panel))
            assertTrue("Target overlaps the gameplay aperture: $target", !target.intersects(display))
        }
        for (index in allTargets.indices) {
            for (other in index + 1 until allTargets.size) {
                assertTrue(
                    "Targets overlap: ${allTargets[index]} / ${allTargets[other]}",
                    !allTargets[index].intersects(allTargets[other]),
                )
            }
        }
        // Portrait authored sizes are 74dp (D-pad) and 78dp (analogue) —
        // smaller than the landscape authored sizes by design, and they
        // must not be compact-shrunk below them.
        assertTrue("D-pad below its authored size", placement.controls.getValue("D").width >= 74f)
        assertTrue("Analogue below its authored size", placement.controls.getValue("AnalogL").width >= 78f)
        // C05-B: thumb caps come from the explicit per-console authored
        // table — GameCube main stick 0.68, C-stick 0.60 of the target.
        assertEquals(
            placement.controls.getValue("AnalogL").width * 0.68f,
            placement.visuals.getValue("AnalogL").w,
            0.01f,
        )
        assertEquals(
            placement.controls.getValue("AnalogR").width * 0.60f,
            placement.visuals.getValue("AnalogR").w,
            0.01f,
        )
        assertEquals(viewport, placement.standaloneGeometry?.viewport)
    }

    /**
     * C05-A live-setting regression: the PSP analogue-primary setting is a
     * live layout input — flipping it must change the effective placement
     * (D-pad and analogue swap seats), proving a setting change reaches
     * the displayed controller rather than a cached calculation.
     */
    @Test
    fun pspAnaloguePrimarySettingChangesEffectivePlacement() {
        val inventory =
            checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.PSP, SystemID.PSP))
        val safe = fixtureSafe(true)
        val viewport = fixtureViewport(SystemID.PSP, true)
        val standard =
            checkNotNull(
                StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, true, pspAnalogPrimary = false),
            )
        val swapped =
            checkNotNull(
                StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, true, pspAnalogPrimary = true),
            )
        assertEquals(standard.controls.getValue("AnalogL"), swapped.controls.getValue("D"))
        assertEquals(standard.controls.getValue("D"), swapped.controls.getValue("AnalogL"))
    }

    /**
     * C06-A: the standalone GameCube deck is rendered from live state,
     * not a cached calculation. Changing the edition (theme) recolours
     * the displayed deck, and changing the measured safe area moves the
     * displayed utility-rail controls — both proven against captured
     * pixels of the actual composed deck, with the rail controls
     * (Menu/Start) rendered below the aperture.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun gameCubeStandaloneDeckAppliesLiveThemeAndLayoutChanges() {
        var edition by mutableStateOf(CartdeckEditionPreset.ORIGINAL)
        var shifted by mutableStateOf(false)
        var latestPlacement: GripArcCombinedPlacement? = null
        compose.setContent {
            val tokens = compose.activity.resolveCartdeckEdition(edition)
            val safe =
                if (shifted) PivotArcBounds(40f, 8f, 732f, 352f) else fixtureSafe(true)
            val viewport = fixtureViewport(SystemID.GAMECUBE, true)
            val glass = Rect(viewport.left, viewport.top, viewport.right, viewport.bottom)
            val inventory =
                checkNotNull(
                    StandaloneGripArcLayout.inventoryFor(TouchControllerID.GAMECUBE, SystemID.GAMECUBE),
                )
            val fitted =
                checkNotNull(StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, true)) {
                    "No standalone GameCube fit for the live-render fixture"
                }
            latestPlacement = fitted
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(),
            ) {
                Box(Modifier.requiredSize(740.dp, 360.dp).background(tokens.bezel).testTag("gc-live-deck")) {
                    CartdeckConsoleShell(
                        tokens,
                        glass,
                        Rect(0f, 0f, 740f, 360f),
                        true,
                        (fitted.controls.values + fitted.dock.targets.values)
                            .map { Rect(it.left, it.top, it.right, it.bottom) },
                        continuousLandscapeBrand = true,
                    )
                    StandaloneGripArcHardware(fitted, tokens, true)
                    PadKit(
                        onInputEvents = {},
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        GripArcCombinedControls(fitted, standalone = true)
                    }
                    GripArcAccessory(
                        fitted.dock,
                        checkNotNull(standaloneGripFactory(SystemID.GAMECUBE, TouchControllerID.GAMECUBE)),
                        tokens,
                        fullscreenWithGamepad = false,
                        isLoading = false,
                        onPressed = { _, _, _ -> },
                        drawHousing = false,
                        inputLabels = StandaloneGripArcLayout.actionLabels(inventory),
                        typedActions = true,
                    )
                }
            }
        }

        fun capture(): Bitmap {
            val rect = compose.onNodeWithTag("gc-live-deck").getUnclippedBoundsInRoot()
            lateinit var bitmap: Bitmap
            compose.runOnIdle {
                bitmap = Bitmap.createBitmap(740, 360, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.translate(-rect.left.value, -rect.top.value)
                compose.activity.window.decorView
                    .draw(canvas)
            }
            return bitmap
        }
        // The deck's own background is whatever the console shell paints
        // at the corner — sample it from the capture itself rather than
        // assuming it equals the edition bezel token (the shell's
        // chassis layer covers the root background). All pixel evidence
        // below is relative to that sampled background.
        val menuBefore = checkNotNull(latestPlacement).controls.getValue("Menu")
        val first = capture()
        val background = first.getPixel(2, 2)
        assertTrue(
            "Utility rail Menu control did not render below the aperture",
            first.getPixel(menuBefore.centerX.toInt(), menuBefore.centerY.toInt()) != background,
        )
        first.recycle()
        // A live theme change recolours the displayed deck: the sampled
        // background changes with the edition, and the rail control is
        // still drawn against it.
        val otherEdition = CartdeckEditionPreset.values().first { it != CartdeckEditionPreset.ORIGINAL }
        compose.runOnIdle { edition = otherEdition }
        val second = capture()
        val themedBackground = second.getPixel(2, 2)
        assertTrue("Theme change did not recolour the displayed deck", themedBackground != background)
        assertTrue(
            "Utility rail Menu control missing after the theme change",
            second.getPixel(menuBefore.centerX.toInt(), menuBefore.centerY.toInt()) != themedBackground,
        )
        second.recycle()
        // A live layout change (a measured safe-area inset, as from a
        // display cutout) moves the displayed controls horizontally:
        // the analogue stick's old centre is background again, its new
        // centre is drawn material.
        val analogBefore = checkNotNull(latestPlacement).controls.getValue("AnalogL")
        compose.runOnIdle { shifted = true }
        val analogAfter = checkNotNull(latestPlacement).controls.getValue("AnalogL")
        assertTrue(
            "Layout change did not move the displayed analogue stick",
            analogAfter.centerX > analogBefore.centerX + 12f,
        )
        val third = capture()
        assertEquals(
            "Old analogue position still drawn after the layout change",
            themedBackground,
            third.getPixel(analogBefore.centerX.toInt(), analogBefore.centerY.toInt()),
        )
        assertTrue(
            "Analogue stick did not render at its new position",
            third.getPixel(analogAfter.centerX.toInt(), analogAfter.centerY.toInt()) != themedBackground,
        )
        third.recycle()
    }

    /**
     * C02-A regression (priority console 1): Nintendo 64 landscape must fit
     * through the generalised authored panels + utility rail, with its own
     * authentic arrangement — D-pad and analogue on the left panel, C-button
     * cluster and A/B on the right — and no legacy or compact route.
     */
    @Test
    fun n64LandscapeUsesAuthoredPanelsWithoutLegacyProjection() {
        val inventory =
            checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.N64, SystemID.N64))
        val safe = fixtureSafe(true)
        val viewport = fixtureViewport(SystemID.N64, true)
        val placement =
            checkNotNull(
                StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, true),
            ) { "N64 landscape did not fit its authored panels on the reference fixture" }
        assertTrue(placement.complete)
        assertEquals(
            StandaloneGripArcLayout.AUTHORED_LANDSCAPE_FIT,
            placement.standaloneGeometry?.fit,
        )
        val gap = 6f
        val leftPanel = PivotArcBounds(safe.left, safe.top, viewport.left - gap, safe.bottom)
        val rightPanel = PivotArcBounds(viewport.right + gap, safe.top, safe.right, safe.bottom)
        for (key in setOf("L", "D", "AnalogL", "Z")) {
            val target = placement.controls.getValue(key)
            assertTrue("$key is outside the left panel column", target.left >= leftPanel.left - 0.05f)
            assertTrue("$key is outside the left panel column", target.right <= leftPanel.right + 0.05f)
        }
        for (key in setOf("R", "CDirection", "B", "A")) {
            val target = placement.controls.getValue(key)
            assertTrue("$key is outside the right panel column", target.left >= rightPanel.left - 0.05f)
            assertTrue("$key is outside the right panel column", target.right <= rightPanel.right + 0.05f)
        }
        val display = viewport.inset(1.5f)
        val allTargets =
            placement.controls.values.toList() +
                placement.dock.targets.values
                    .toList()
        for (target in allTargets) {
            assertTrue("Target is outside the safe area: $target", target.inside(safe))
            assertTrue("Target overlaps the gameplay aperture: $target", !target.intersects(display))
        }
        assertTrue("D-pad was compact-shrunk", placement.controls.getValue("D").width >= 58f)
        assertTrue("Analogue stick was compact-shrunk", placement.controls.getValue("AnalogL").width >= 58f)
    }

    private val standaloneModes =
        listOf(
            SystemID.GBA to TouchControllerID.GBA,
            SystemID.GB to TouchControllerID.GB,
            SystemID.GBC to TouchControllerID.GB,
            SystemID.NES to TouchControllerID.NES,
            SystemID.SMS to TouchControllerID.SMS,
            SystemID.GG to TouchControllerID.GG,
            SystemID.NGP to TouchControllerID.NGP,
            SystemID.NGC to TouchControllerID.NGP,
            SystemID.PC_ENGINE to TouchControllerID.PCE,
            SystemID.PC_ENGINE to TouchControllerID.PCE_SIX,
            SystemID.SNES to TouchControllerID.SNES,
            SystemID.GENESIS to TouchControllerID.GENESIS_3,
            SystemID.GENESIS to TouchControllerID.GENESIS_6,
            SystemID.SEGACD to TouchControllerID.GENESIS_3,
            SystemID.SEGACD to TouchControllerID.GENESIS_6,
            SystemID.ATARI2600 to TouchControllerID.ATARI2600,
            SystemID.ATARI7800 to TouchControllerID.ATARI7800,
            SystemID.LYNX to TouchControllerID.LYNX,
            SystemID.PSP to TouchControllerID.PSP,
            SystemID.DREAMCAST to TouchControllerID.DREAMCAST,
            SystemID.GAMECUBE to TouchControllerID.GAMECUBE,
            SystemID.PSX to TouchControllerID.PSX,
            SystemID.PSX to TouchControllerID.PSX_DUALSHOCK,
            SystemID.NINTENDO_3DS to TouchControllerID.NINTENDO_3DS,
            SystemID.NDS to TouchControllerID.MELONDS,
            SystemID.WS to TouchControllerID.WS_LANDSCAPE,
            SystemID.WS to TouchControllerID.WS_PORTRAIT,
            SystemID.WSC to TouchControllerID.WS_LANDSCAPE,
            SystemID.WSC to TouchControllerID.WS_PORTRAIT,
            SystemID.FBNEO to TouchControllerID.ARCADE_2,
            SystemID.FBNEO to TouchControllerID.ARCADE_3,
            SystemID.FBNEO to TouchControllerID.ARCADE_4,
            SystemID.FBNEO to TouchControllerID.ARCADE_6,
            SystemID.MAME2003PLUS to TouchControllerID.ARCADE_2,
            SystemID.MAME2003PLUS to TouchControllerID.ARCADE_3,
            SystemID.MAME2003PLUS to TouchControllerID.ARCADE_4,
            SystemID.MAME2003PLUS to TouchControllerID.ARCADE_6,
            SystemID.N64 to TouchControllerID.N64,
            SystemID.XBOX to TouchControllerID.XBOX,
            SystemID.XBOX360 to TouchControllerID.XBOX360,
            SystemID.PLAYSTATION_2 to TouchControllerID.PSX_DUALSHOCK,
            SystemID.PLAYSTATION_3 to TouchControllerID.PSX_DUALSHOCK,
            SystemID.DOS to TouchControllerID.DOS,
            SystemID.WII_U to TouchControllerID.WII_CLASSIC,
            SystemID.WII to TouchControllerID.WII_SIDEWAYS,
            SystemID.WII to TouchControllerID.WII_CLASSIC,
            SystemID.WII to TouchControllerID.GAMECUBE,
        ).map { (system, controller) -> Triple(system, controller, false) } +
            listOf(
                Triple(SystemID.PSP, TouchControllerID.PSP, true),
                Triple(SystemID.WII_U, TouchControllerID.WII_CLASSIC, true),
            )

    @Test
    fun reportStandaloneCoverageAgainstLockedDesignManifest() {
        val relative = "docs/engineering/gc01/griparc-design-packets/manifest.json"
        val manifest =
            JSONObject(
                listOf(File(relative), File("..", relative)).first(File::isFile).readText(),
            )
        val declared =
            manifest
                .getJSONArray("systems")
                .entries()
                .flatMap { system ->
                    val id = SystemID.valueOf(system.getString("id"))
                    val variants =
                        system.optJSONArray("variants")?.entries()?.map { it.getString("id") } ?: listOf("base")
                    variants.map { id to it }
                }.toSet()
        assertEquals(53, declared.size)
        assertEquals(33, declared.map { it.first }.toSet().size)

        val fixtures =
            standaloneModes.associateBy { (system, controller, alternate) ->
                val variant =
                    when (controller) {
                        TouchControllerID.PCE -> "digital2"
                        TouchControllerID.PSX -> "digital"
                        TouchControllerID.PSX_DUALSHOCK -> if (system == SystemID.PSX) "dualshock" else "base"
                        TouchControllerID.DOS -> "gamepad"
                        TouchControllerID.WII_CLASSIC ->
                            if (system == SystemID.WII) {
                                "classic"
                            } else if (alternate) {
                                "pro"
                            } else {
                                "gamepad"
                            }
                        TouchControllerID.WII_SIDEWAYS -> "sideways"
                        TouchControllerID.GAMECUBE -> if (system == SystemID.WII) "gamecube" else "base"
                        TouchControllerID.PSP -> if (alternate) "analogue_primary" else "dpad_primary"
                        TouchControllerID.WS_PORTRAIT -> "guest_vertical"
                        TouchControllerID.WS_LANDSCAPE -> "guest_horizontal"
                        TouchControllerID.GENESIS_3 -> "digital3"
                        TouchControllerID.ARCADE_2 -> "digital2"
                        TouchControllerID.ARCADE_3 -> "digital3"
                        TouchControllerID.ARCADE_4 -> "digital4"
                        TouchControllerID.GENESIS_6, TouchControllerID.ARCADE_6, TouchControllerID.PCE_SIX -> "digital6"
                        else -> "base"
                    }
                system to variant
            }
        assertEquals(standaloneModes.size, fixtures.size)
        assertTrue("Fixture has no design authority", declared.containsAll(fixtures.keys))
        val problems = mutableListOf<String>()
        val report =
            declared.map { key ->
                val (system, variant) = key
                val fixture = fixtures[key]
                val controller = fixture?.second
                val registration = supportsStandaloneGripActions(system) && StandaloneGripArcLayout.supports(controller)
                val preset = fixture?.let { standaloneGripFactory(system, controller) }
                val inventory =
                    controller?.let {
                        StandaloneGripArcLayout.inventoryFor(
                            it,
                            system,
                            wiiuPro = system == SystemID.WII_U && fixture?.third == true,
                        )
                    }
                val portrait =
                    inventory?.let {
                        StandaloneGripArcLayout.resolveStandalone(
                            it,
                            fixtureSafe(false),
                            fixtureViewport(system, false),
                            false,
                            fixture?.third == true,
                        )
                    }
                val landscape =
                    inventory?.let {
                        StandaloneGripArcLayout.resolveStandalone(
                            it,
                            fixtureSafe(true),
                            fixtureViewport(system, true),
                            true,
                            fixture?.third == true,
                        )
                    }
                val nativeRoute =
                    when (system) {
                        SystemID.XBOX -> controller == TouchControllerID.XBOX
                        SystemID.XBOX360 -> controller == TouchControllerID.XBOX360
                        SystemID.PLAYSTATION_2, SystemID.PLAYSTATION_3 -> controller == TouchControllerID.PSX_DUALSHOCK
                        SystemID.WII_U -> controller == TouchControllerID.WII_CLASSIC
                        else ->
                            controller != null &&
                                GameSystem.findById(system.dbname).systemCoreConfigs.any { config ->
                                    config.controllerConfigs[0].orEmpty().any { it.touchControllerID == controller }
                                }
                    }
                val renderer = portrait?.complete == true && landscape?.complete == true
                val dispatch =
                    nativeRoute &&
                        inventory?.controls?.all {
                            it.keyCode != null || it.motionSource != null
                        } == true
                val checks =
                    linkedMapOf(
                        "registration" to registration,
                        "preset" to (preset != null),
                        "portrait" to (portrait != null),
                        "landscape" to (landscape != null),
                        // C02-A: landscape must come from an authored route —
                        // never the legacy projection or compact fallback.
                        "landscapeAuthored" to
                            (
                                landscape == null ||
                                    landscape.standaloneGeometry?.fit in
                                    setOf(
                                        "uniform chassis transform",
                                        StandaloneGripArcLayout.AUTHORED_LANDSCAPE_FIT,
                                        StandaloneGripArcLayout.GAMECUBE_LANDSCAPE_FIT,
                                    )
                            ),
                        // C05-B: portrait must also come from the authored
                        // transform — never the legacy projection or the
                        // compact directional fallback.
                        "portraitAuthored" to
                            (
                                portrait == null ||
                                    portrait.standaloneGeometry?.fit == "uniform chassis transform"
                            ),
                        "renderer" to renderer,
                        "dispatchRegistration" to dispatch,
                    )
                if (fixture != null && checks.containsValue(false)) problems += "$system/$variant $checks"
                JSONObject(checks + mapOf("system" to system.name, "configuration" to variant))
                    .put("portraitGeometry", geometryEvidence(system, false, portrait))
                    .put("landscapeGeometry", geometryEvidence(system, true, landscape))
            }
        val folder = File("build/reports/gc01-menu-screens/griparc")
        folder.mkdirs()
        File(folder, "standalone-manifest-coverage.json").writeText(JSONArray(report).toString(2))
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    private fun geometryEvidence(
        system: SystemID,
        wide: Boolean,
        placement: GripArcCombinedPlacement?,
    ): JSONObject {
        fun bounds(rect: PivotArcBounds) = JSONArray(listOf(rect.left, rect.top, rect.right, rect.bottom))
        val geometry = placement?.standaloneGeometry
        val evidence =
            JSONObject()
                .put("units", "dp")
                .put("fixture", "reference handheld, no device measurements")
                .put("safe", bounds(fixtureSafe(wide)))
                .put("viewport", bounds(fixtureViewport(system, wide)))
        if (placement == null || geometry == null) return evidence.put("fit", "unregistered or no fit")
        val touches = placement.controls + placement.dock.targets.mapKeys { (slot, _) -> "program:$slot" }
        val caps = placement.visuals + placement.dock.visuals.mapKeys { (slot, _) -> "program:$slot" }
        val routes =
            placement.native.controls.associate { control ->
                control.key to
                    JSONObject()
                        .put("keyCode", control.keyCode ?: JSONObject.NULL)
                        .put("motionSource", control.motionSource ?: JSONObject.NULL)
            }
        return evidence
            .put("fit", geometry.fit)
            .put("panel", bounds(placement.dock.housing))
            .put("authoredPanelSize", JSONArray(listOf(geometry.reference.w, geometry.reference.h)))
            .put("authoredSeats", JSONObject(geometry.authoredSeats.mapValues { bounds(it.value) }))
            .put("touchTargets", JSONObject(touches.mapValues { bounds(it.value) }))
            .put("capSizes", JSONObject(caps.mapValues { JSONArray(listOf(it.value.w, it.value.h)) }))
            .put("configuredNativeRoutes", JSONObject(routes))
            .put(
                "analogueThumbCapDiameter",
                JSONObject(
                    placement.native.controls
                        .filter { it.kind == com.swordfish.touchinput.radial.settings.NativeControlKind.ANALOG }
                        .associate { it.key to (placement.visuals.getValue(it.key).w * 0.52f) },
                ),
            )
    }

    private fun JSONArray.entries(): List<JSONObject> = (0 until length()).map(::getJSONObject)

    private fun fixtureSafe(wide: Boolean) =
        if (wide) PivotArcBounds(8f, 8f, 732f, 352f) else PivotArcBounds(8f, 8f, 385f, 844f)

    private fun fixtureViewport(
        system: SystemID,
        wide: Boolean,
    ) = when {
        wide -> PivotArcBounds(180f, 20f, 560f, 290f)
        system in setOf(SystemID.NDS, SystemID.NINTENDO_3DS) -> PivotArcBounds(22f, 84f, 371f, 484f)
        else -> PivotArcBounds(22f, 84f, 371f, 340f)
    }

    private fun renderStandaloneFixtures(wide: Boolean) {
        val modes = standaloneModes
        val width = if (wide) 740 else 393
        val height = if (wide) 360 else 852
        val root = Rect(0f, 0f, width.toFloat(), height.toFloat())
        val safe = fixtureSafe(wide)
        val motion = mutableStateOf(modes.first())
        compose.setContent {
            val (system, id, analogPrimary) = motion.value
            val tokens = compose.activity.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL)
            val viewport = fixtureViewport(system, wide)
            val glass = Rect(viewport.left, viewport.top, viewport.right, viewport.bottom)
            val inventory =
                checkNotNull(
                    StandaloneGripArcLayout.inventoryFor(
                        id,
                        system,
                        wiiuPro = system == SystemID.WII_U && analogPrimary,
                    ),
                )
            val fitted =
                checkNotNull(
                    StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, wide, analogPrimary),
                ) {
                    "No standalone fit for $system / $id, landscape=$wide"
                }
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalLemuroidPadTheme provides LemuroidPadTheme(),
            ) {
                Box(Modifier.requiredSize(width.dp, height.dp).background(tokens.bezel).testTag("standalone-deck")) {
                    CartdeckConsoleShell(
                        tokens,
                        glass,
                        root,
                        wide,
                        (fitted.controls.values + fitted.dock.targets.values)
                            .map { Rect(it.left, it.top, it.right, it.bottom) },
                        continuousLandscapeBrand = true,
                    )
                    StandaloneGripArcHardware(fitted, tokens, wide)
                    PadKit(
                        onInputEvents = {},
                        hapticFeedbackType = HapticFeedbackType.NONE,
                        simulatedState = remember { mutableStateOf(InputState()) },
                        simulatedControlIds = remember { mutableStateOf(emptySet()) },
                    ) {
                        GripArcCombinedControls(fitted, standalone = true)
                    }
                    GripArcAccessory(
                        fitted.dock,
                        checkNotNull(standaloneGripFactory(system, id)),
                        tokens,
                        fullscreenWithGamepad = false,
                        isLoading = false,
                        onPressed = { _, _, _ -> },
                        drawHousing = false,
                        inputLabels = StandaloneGripArcLayout.actionLabels(inventory),
                        typedActions = true,
                    )
                }
            }
        }
        val folder = File("build/reports/gc01-menu-screens/griparc/standalone-full-size")
        folder.mkdirs()
        for (mode in modes) {
            val (system, controller, analogPrimary) = mode
            compose.runOnIdle { motion.value = mode }
            val rect = compose.onNodeWithTag("standalone-deck").getUnclippedBoundsInRoot()
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    val canvas = Canvas(bitmap)
                    canvas.translate(-rect.left.value, -rect.top.value)
                    compose.activity.window.decorView
                        .draw(canvas)
                    val orientation = if (wide) "landscape" else "portrait"
                    val priority =
                        when {
                            system == SystemID.WII_U && analogPrimary -> "-pro"
                            analogPrimary -> "-analogue"
                            else -> ""
                        }
                    File(folder, "${system.name.lowercase()}-${controller.name.lowercase()}$priority-$orientation.png")
                        .outputStream()
                        .use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                } finally {
                    bitmap.recycle()
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
            compose.activity.window.decorView
                .draw(canvas)
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
                compose
                    .onNodeWithContentDescription(
                        key,
                    ).assertIsDisplayed()
                    .assertWidthIsAtLeast(48.dp)
                    .assertHeightIsAtLeast(48.dp)
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
                                controls
                                    .map { (key, bounds) ->
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
                compose
                    .onNodeWithTag("griparc-slot-$slot")
                    .assertIsDisplayed()
                    .assertWidthIsAtLeast(48.dp)
                    .assertHeightIsAtLeast(48.dp)
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
            compose.activity.window.decorView
                .draw(canvas)
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
                joint?.dock ?: GripArcCombinedLayout
                    .resolveDockFirst(
                        manifest,
                        native,
                        device.safe,
                        device.viewport,
                        profile.side,
                        0f,
                        0f,
                    )!!
                    .dock
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
            for (side in GripArcSide.values()) {
                for (count in 1..4) {
                    for (theme in CartdeckEditionPreset.values()) {
                        compose.runOnIdle {
                            edition = theme
                            profile = GripArcProfile(mode, side, count, List(4) { listOf(KeyEvent.KEYCODE_BUTTON_A) })
                        }
                        for (slot in 1..count) {
                            compose
                                .onNodeWithTag("griparc-slot-$slot")
                                .assertIsDisplayed()
                                .assertWidthIsAtLeast(48.dp)
                                .assertHeightIsAtLeast(48.dp)
                        }
                        for (slot in count + 1..4) compose.onNodeWithTag("griparc-slot-$slot").assertDoesNotExist()
                        if (mode == ArcControlMode.STANDARD_GRIPARC) {
                            compose.onNodeWithTag("griparc-housing").assertIsDisplayed()
                        }
                    }
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
