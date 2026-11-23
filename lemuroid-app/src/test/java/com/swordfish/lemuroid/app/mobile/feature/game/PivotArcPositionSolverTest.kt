package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PivotArcPositionSolverTest {
    private val baseline =
        PivotArcLayoutProfile.parse(
            File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
        )
    private val referenceSafe = PivotArcBounds(8f, 8f, 385f, 844f)
    private val referenceViewport = PivotArcBounds(22f, 84f, 371f, 344f)

    private fun resolve(
        system: SystemID,
        leftHand: Boolean = false,
        safe: PivotArcBounds = referenceSafe,
        viewport: PivotArcBounds = referenceViewport,
        dx: Float = 0f,
        dy: Float = 0f,
        landscape: Boolean = false,
    ): PivotArcResolution =
        PivotArcPositionSolver.resolve(
            baseline,
            PivotArcLayoutProfile.activeFor(system)!!,
            safe,
            viewport,
            leftHand,
            dx,
            dy,
            landscape,
        )

    @Test fun candidateCUsesLargerManufacturedButtonsWhenSpacePermits() {
        val placement = (resolve(SystemID.GBA) as PivotArcResolution.Fits).placement
        assertTrue(placement.visualScale > 1f)
        assertTrue(placement.visuals.getValue("D").w >= 110f)
        assertTrue(placement.visuals.getValue("A").w >= 53f)
        assertTrue(placement.visuals.getValue("B").w >= 53f)
        assertTrue(placement.visuals.getValue("L").w >= 52f)
        assertTrue(placement.visuals.getValue("R").w >= 52f)
        assertEquals(54f, placement.visuals.getValue("Start").w, 0.001f)
        assertTrue(placement.controls.getValue("D").width >= 115f)
        assertTrue(placement.controls.getValue("A").width >= 58f)
    }

    @Test fun compactPhonesUseFittingScaleWithoutBreakingTopologyOrTargets() {
        val compactSafe = PivotArcBounds(8f, 8f, 312f, 692f)
        val viewport = PivotArcBounds(22f, 84f, 298f, 344f)
        val result = resolve(SystemID.SNES, safe = compactSafe, viewport = viewport)
        if (result is PivotArcResolution.Fits) {
            assertEquals(12, result.placement.controls.size)
            assertTrue(result.placement.visualScale >= 1f)
            for ((key, bounds) in result.placement.controls) {
                if (key !in setOf("Select", "Start", "Menu")) {
                    assertTrue("$key should retain a 48dp touch target", bounds.width >= 48f)
                    assertTrue(bounds.height >= 48f)
                }
            }
        } else {
            assertTrue((result as PivotArcResolution.Unavailable).reason.isNotBlank())
        }
    }

    @Test fun versionedSourceAndTargetSizingAreSafe() {
        assertEquals("cartdeck.pivotarc.candidate-c/0.7.0", baseline.schema)
        baseline.buttonCenters.values.forEach { b ->
            assertTrue(b.touch.w >= 48f && b.touch.h >= 48f)
            assertTrue(b.visual.w <= b.touch.w && b.visual.h <= b.touch.h)
        }
    }

    @Test fun acceptedInitialProfilesHaveEveryRequiredKey() {
        for (system in listOf(SystemID.GB, SystemID.GBC, SystemID.GBA, SystemID.SNES)) {
            val active = PivotArcLayoutProfile.activeFor(system)!!
            val result = resolve(system) as PivotArcResolution.Fits
            assertEquals(active.toSet() + setOf("HoldA", "HoldB"), result.placement.controls.keys)
            val rectangles = result.placement.controls.values.toList()
            rectangles.forEach {
                assertTrue(it.inside(referenceSafe))
                assertFalse(it.intersects(referenceViewport))
            }
            for (i in rectangles.indices) {
                for (j in i + 1 until rectangles.size) {
                    assertFalse(rectangles[i].inset(3.99f).intersects(rectangles[j].inset(3.99f)))
                }
            }
        }
    }

    @Test fun candidateCShouldersAndFaceButtonsKeepApprovedAdjacency() {
        val placed = (resolve(SystemID.GBA) as PivotArcResolution.Fits).placement.controls

        fun midX(key: String) = (placed.getValue(key).left + placed.getValue(key).right) / 2f

        fun midY(key: String) = (placed.getValue(key).top + placed.getValue(key).bottom) / 2f
        assertTrue(midX("L") < midX("D"))
        assertTrue(midX("B") < midX("A"))
        assertTrue(midY("B") < midY("A"))
        assertTrue(midX("R") > midX("A"))
        assertTrue(kotlin.math.abs(midY("R") - midY("A")) < 16f)
    }

    @Test fun gbaReservesFutureFaceLocationsWhileKeepingThemNonInteractive() {
        val gba = (resolve(SystemID.GBA) as PivotArcResolution.Fits).placement
        val snes = (resolve(SystemID.SNES) as PivotArcResolution.Fits).placement
        assertFalse(gba.controls.containsKey("X"))
        assertFalse(gba.controls.containsKey("Y"))
        assertTrue(snes.controls.containsKey("X"))
        assertTrue(snes.controls.containsKey("Y"))
        assertEquals(gba.controls.getValue("D").centerY, snes.controls.getValue("D").centerY, 0.001f)
        assertEquals(gba.controls.getValue("A").centerX, snes.controls.getValue("A").centerX, 0.001f)
        assertTrue((resolve(SystemID.GBA, dy = -500f) as PivotArcResolution.Fits).placement.appliedDy > -500f)
    }

    @Test fun leftHandMirrorsOnlyCoordinatesAndNeverIdentities() {
        val right = (resolve(SystemID.SNES) as PivotArcResolution.Fits).placement.controls
        val left = (resolve(SystemID.SNES, leftHand = true) as PivotArcResolution.Fits).placement.controls
        assertEquals(right.keys, left.keys)
        for (key in right.keys) {
            assertEquals(
                referenceSafe.left + referenceSafe.right - right.getValue(key).right,
                left.getValue(key).left,
                0.001f,
            )
            assertEquals(right.getValue(key).top, left.getValue(key).top, 0.001f)
        }
    }

    @Test fun positionAdjustmentIsClampedInsideSafeArea() {
        val placed = (resolve(SystemID.GBA, dx = 500f, dy = -500f) as PivotArcResolution.Fits).placement
        placed.controls.values.forEach { assertTrue(it.inside(referenceSafe)) }
        assertTrue(placed.appliedDx < 500f)
        assertTrue(placed.appliedDy > -500f)
    }

    @Test fun smallPhoneFitsOrExplainsRatherThanDroppingControls() {
        val safe = PivotArcBounds(8f, 8f, 312f, 692f)
        val viewport = PivotArcBounds(22f, 84f, 298f, 344f)
        val result = resolve(SystemID.SNES, safe = safe, viewport = viewport)
        if (result is PivotArcResolution.Fits) {
            assertEquals(12, result.placement.controls.size)
        } else {
            assertTrue((result as PivotArcResolution.Unavailable).reason.isNotBlank())
        }
    }

    @Test fun insufficientSpaceAndLandscapeRequireStandardFallback() {
        assertTrue(resolve(SystemID.SNES, safe = PivotArcBounds(8f, 8f, 312f, 580f)) is PivotArcResolution.Unavailable)
        assertTrue(resolve(SystemID.GBA, landscape = true) is PivotArcResolution.Unavailable)
    }

    @Test fun holdTogglesUseDedicatedTargetsAndSitBesideTheirFaceButtons() {
        val placement = (resolve(SystemID.GBA) as PivotArcResolution.Fits).placement
        val buttons = placement.controls
        val a = buttons.getValue("A")
        val b = buttons.getValue("B")
        val holdA = buttons.getValue("HoldA")
        val holdB = buttons.getValue("HoldB")
        assertEquals(48f, holdA.width, 0.001f)
        assertEquals(48f, holdB.width, 0.001f)
        assertEquals(48f, holdA.height, 0.001f)
        assertEquals(48f, holdB.height, 0.001f)
        assertTrue(placement.visuals.getValue("HoldA").w < placement.visuals.getValue("A").w)
        assertTrue(placement.visuals.getValue("HoldB").w < placement.visuals.getValue("B").w)
        assertEquals(a.centerX, holdA.centerX, 0.001f)
        assertEquals(b.centerY, holdB.centerY, 0.001f)
        assertEquals(8f, holdA.top - a.bottom, 0.001f)
        assertEquals(8f, b.left - holdB.right, 0.001f)
    }

    @Test fun compactUtilityBankLivesBelowPivotArcAndNeverChangesStandardCoordinates() {
        val controls = (resolve(SystemID.GBA) as PivotArcResolution.Fits).placement.controls
        val coreBottom =
            controls.filterKeys { it !in setOf("Start", "Select", "Menu") }
                .values.maxOf { it.bottom }
        for (key in listOf("Select", "Start", "Menu")) {
            assertTrue("$key must remain below the core", controls.getValue(key).top >= coreBottom + 7.9f)
        }
        assertTrue(controls.getValue("Select").centerX < controls.getValue("Start").centerX)
        assertTrue(controls.getValue("Start").centerX < controls.getValue("Menu").centerX)
    }

    @Test fun expandedConsoleControlsNeverPretendToBeSupported() {
        for (system in listOf(
            SystemID.PSX,
            SystemID.N64,
            SystemID.GAMECUBE,
            SystemID.GENESIS,
        )) {
            assertEquals(null, PivotArcLayoutProfile.activeFor(system))
        }
    }

    @Test fun pspKeepsTheAnalogStickSeparateFromTheDpadAndFourFaceButtons() {
        for (leftHand in listOf(false, true)) {
            val placement = (resolve(SystemID.PSP, leftHand = leftHand) as PivotArcResolution.Fits).placement
            val controls = placement.controls
            val required = setOf("D", "Analog", "A", "B", "X", "Y", "L", "R", "Select", "Start", "Menu")
            assertEquals(required + setOf("HoldA", "HoldB"), controls.keys)
            val analog = controls.getValue("Analog")
            assertTrue(analog.width >= 48f && analog.height >= 48f)
            assertFalse(analog.intersects(referenceViewport))
            assertTrue(controls.filterKeys { it != "Analog" }.values.none { it.intersects(analog) })
        }
        assertEquals("Circle", PivotArcLayoutProfile.nativeLabel(SystemID.PSP, "A"))
        assertEquals("Cross", PivotArcLayoutProfile.nativeLabel(SystemID.PSP, "B"))
    }

    @Test fun pspAnalogPrimarySwapsOnlyTheTwoDirectionalSeatsForBothHands() {
        val primaryAnalog = PivotArcLayoutProfile.forSystem(baseline, SystemID.PSP, pspAnalogPrimary = true)
        val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v22-manifest.json").readText())
        for (side in GripArcSide.entries) {
            fun joint(geometry: PivotArcBaseline): CompositeControlDeckPlacement =
                (
                    CompositeControlDeckSolver.resolve(
                        manifest, geometry, SystemID.PSP, referenceSafe, referenceViewport, side,
                        0f, 0f, 0f, 0f, false,
                    ) as CompositeDeckResolution.Fits
                ).placement
            val original = joint(baseline)
            val updated = joint(primaryAnalog)
            assertEquals(original.pivot.controls.getValue("D"), updated.pivot.controls.getValue("Analog"))
            assertEquals(original.pivot.controls.getValue("Analog"), updated.pivot.controls.getValue("D"))
            assertEquals(original.dock, updated.dock)
            assertEquals(
                original.pivot.controls - setOf("D", "Analog"),
                updated.pivot.controls - setOf("D", "Analog"),
            )
        }
        assertEquals(baseline, PivotArcLayoutProfile.forSystem(baseline, SystemID.GBA, pspAnalogPrimary = true))
    }

    @Test fun dualScreenPivotArcKeepsStylusRegionAndBackendSpecificAuxiliaries() {
        val viewport = PivotArcBounds(22f, 36f, 371f, 452f)
        for (side in listOf(false, true)) {
            val ds = (resolve(SystemID.NDS, leftHand = side, viewport = viewport) as PivotArcResolution.Fits).placement
            val threeDs =
                (
                    resolve(
                        SystemID.NINTENDO_3DS,
                        leftHand = side,
                        viewport = viewport,
                    ) as PivotArcResolution.Fits
                ).placement
            assertTrue(ds.controls.keys.containsAll(listOf("Mic", "Lid", "L", "R", "Start", "Select")))
            assertTrue(threeDs.controls.keys.containsAll(listOf("D", "Analog", "L", "R", "Start", "Select")))
            assertTrue((ds.controls.values + threeDs.controls.values).all { !it.intersects(viewport) })
        }
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, pivotArcDsAuxiliaryKey(TouchControllerID.DESMUME, "Mic"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, pivotArcDsAuxiliaryKey(TouchControllerID.DESMUME, "Lid"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, pivotArcDsAuxiliaryKey(TouchControllerID.MELONDS, "Mic"))
        assertEquals(KeyEvent.KEYCODE_BUTTON_THUMBL, pivotArcDsAuxiliaryKey(TouchControllerID.MELONDS, "Lid"))
    }

    @Test fun handheldTwoButtonFamiliesKeepNativeLabelsAndOmitPhantomSelect() {
        for (system in listOf(SystemID.GG, SystemID.LYNX, SystemID.NGP, SystemID.NGC)) {
            for (leftHand in listOf(false, true)) {
                val controls = (resolve(system, leftHand = leftHand) as PivotArcResolution.Fits).placement.controls
                assertTrue(controls.keys.containsAll(listOf("D", "A", "B", "Start", "Menu", "HoldA", "HoldB")))
                assertEquals(null, controls["Select"])
                assertEquals(system == SystemID.LYNX, "L" in controls && "R" in controls)
            }
        }
        assertEquals("2", PivotArcLayoutProfile.nativeLabel(SystemID.GG, "A"))
        assertEquals("1", PivotArcLayoutProfile.nativeLabel(SystemID.GG, "B"))
        assertEquals("Pause", PivotArcLayoutProfile.nativeLabel(SystemID.LYNX, "Start"))
        assertEquals("B", PivotArcLayoutProfile.nativeLabel(SystemID.NGC, "A"))
        assertEquals("Option", PivotArcLayoutProfile.nativeLabel(SystemID.NGP, "Start"))
    }

    @Test fun standardTrayUsesOnlyMeasuredFreeSpaceAndKeepsFullTargets() {
        val safe = PivotArcBounds(8f, 8f, 320f, 600f)
        val occupied = listOf(PivotArcBounds(8f, 80f, 320f, 460f), PivotArcBounds(8f, 500f, 106f, 600f))
        val slots = findStandardHoldTray(safe, occupied, 3)
        assertEquals(3, slots.size)
        assertTrue(slots.all { it.width == 48f && it.height == 48f && it.inside(safe) })
        assertTrue(slots.none { slot -> occupied.any { slot.intersects(it) } })
        assertTrue(findStandardHoldTray(safe, emptyList(), 1).isEmpty())
        assertTrue(findStandardHoldTray(safe, listOf(safe), 1).isEmpty())
    }
}
