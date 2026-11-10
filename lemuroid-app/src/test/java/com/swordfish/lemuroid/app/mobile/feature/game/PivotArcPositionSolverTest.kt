package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID
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
            assertEquals(active.toSet(), result.placement.controls.keys)
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

    @Test fun leftHandMirrorsOnlyCoordinatesAndNeverIdentities() {
        val right = (resolve(SystemID.SNES) as PivotArcResolution.Fits).placement.controls
        val left = (resolve(SystemID.SNES, leftHand = true) as PivotArcResolution.Fits).placement.controls
        assertEquals(right.keys, left.keys)
        for (key in right.keys) {
            assertEquals(referenceSafe.left + referenceSafe.right - right.getValue(key).right, left.getValue(key).left, 0.001f)
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
            assertEquals(9, result.placement.controls.size)
        } else {
            assertTrue((result as PivotArcResolution.Unavailable).reason.isNotBlank())
        }
    }

    @Test fun insufficientSpaceAndLandscapeRequireStandardFallback() {
        assertTrue(resolve(SystemID.SNES, safe = PivotArcBounds(8f, 8f, 312f, 580f)) is PivotArcResolution.Unavailable)
        assertTrue(resolve(SystemID.GBA, landscape = true) is PivotArcResolution.Unavailable)
    }

    @Test fun expandedConsoleControlsNeverPretendToBeSupported() {
        for (system in listOf(SystemID.PSX, SystemID.PSP, SystemID.N64, SystemID.NDS, SystemID.NGC, SystemID.GENESIS)) {
            assertEquals(null, PivotArcLayoutProfile.activeFor(system))
        }
    }
}
