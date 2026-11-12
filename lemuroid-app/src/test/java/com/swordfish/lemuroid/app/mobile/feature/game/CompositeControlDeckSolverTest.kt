package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CompositeControlDeckSolverTest {
    private val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v1-manifest.json").readText())
    private val baseline =
        PivotArcLayoutProfile.parse(
            File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
        )
    private val viewport = PivotArcBounds(22f, 84f, 371f, 344f)

    @Test fun supportedControllerPathsHaveOneCompleteCollisionFreePlacement() {
        for (width in listOf(
            393f,
            360f,
            320f,
        )) for (system in listOf(SystemID.GB, SystemID.GBC, SystemID.GBA, SystemID.SNES)) {
            for (side in GripArcSide.values()) {
                val safe = PivotArcBounds(8f, 8f, width - 8f, 844f)
                val result = CompositeControlDeckSolver.resolve(manifest, baseline, system, safe, viewport, side)
                assertTrue("$width $system $side: $result", result is CompositeDeckResolution.Fits)
                val p = (result as CompositeDeckResolution.Fits).placement
                assertEquals(viewport, p.viewport)
                assertEquals(side, p.dock.side)
                assertEquals(
                    PivotArcLayoutProfile.activeFor(system)!!.toSet() + setOf("HoldA", "HoldB"),
                    p.pivot.controls.keys,
                )
                assertEquals(setOf("X", "Y"), p.futureFaces.keys + p.pivot.controls.keys.intersect(setOf("X", "Y")))
                val targets = (p.pivot.controls.values + p.futureFaces.values + p.dock.targets.values).toList()
                targets.forEach { r ->
                    assertTrue(r.width >= 48f && r.height >= 48f)
                    assertTrue(r.inside(safe))
                    assertFalse(r.intersects(viewport))
                }
                for (i in targets.indices) for (j in i + 1 until targets.size) assertFalse(
                    targets[i].intersects(targets[j]),
                )
                p.island.regions.forEach { r ->
                    assertTrue(r.inside(safe))
                    assertFalse(r.intersects(viewport))
                    assertTrue(p.dockFootprint.none { it.intersects(r) })
                    assertTrue(p.dock.targets.values.none { it.intersects(r) })
                }
                assertTrue(p.island.upper.width > p.island.lower.width)
                // Counts only change which of these permanent slots receive input/rendered buttons.
                for (count in 1..4) assertEquals((1..count).toList(), p.dock.targets.keys.take(count))
            }
        }
    }

    @Test fun jointMirroringRetainsEveryInputAndFixedSlotIdentity() {
        val safe = PivotArcBounds(8f, 8f, 385f, 844f)

        fun placement(side: GripArcSide) =
            (
                CompositeControlDeckSolver.resolve(
                    manifest,
                    baseline,
                    SystemID.SNES,
                    safe,
                    viewport,
                    side,
                ) as CompositeDeckResolution.Fits
            ).placement
        val right = placement(GripArcSide.RIGHT)
        val left = placement(GripArcSide.LEFT)
        val rightTargets = right.pivot.controls + right.dock.targets.mapKeys { "G${it.key}" }
        val leftTargets = left.pivot.controls + left.dock.targets.mapKeys { "G${it.key}" }
        assertEquals(rightTargets.keys, leftTargets.keys)
        for ((key, r) in rightTargets) {
            val l = leftTargets.getValue(key)
            assertEquals(safe.left + safe.right - r.centerX, l.centerX, 0.05f)
            assertEquals(r.centerY, l.centerY, 0.05f)
            assertEquals(r.width, l.width, 0.05f)
        }
    }

    @Test fun unsupportedLandscapeAndImpossibleDeckFailAtomically() {
        val safe = PivotArcBounds(8f, 8f, 385f, 844f)
        assertEquals(
            CompositeDeckFailure.UNSUPPORTED_CONTROLLER,
            (
                CompositeControlDeckSolver.resolve(
                    manifest,
                    baseline,
                    SystemID.PLAYSTATION_2,
                    safe,
                    viewport,
                    GripArcSide.RIGHT,
                ) as CompositeDeckResolution.Unavailable
            ).failure,
        )
        assertEquals(
            CompositeDeckFailure.PORTRAIT_REQUIRED,
            (
                CompositeControlDeckSolver.resolve(
                    manifest,
                    baseline,
                    SystemID.GBA,
                    safe,
                    viewport,
                    GripArcSide.RIGHT,
                    landscape = true,
                ) as CompositeDeckResolution.Unavailable
            ).failure,
        )
        val short =
            CompositeControlDeckSolver.resolve(
                manifest,
                baseline,
                SystemID.SNES,
                safe.copy(bottom = 500f),
                viewport,
                GripArcSide.RIGHT,
            )
        assertTrue(short is CompositeDeckResolution.Unavailable)
    }

    @Test fun insetAndOffsetFallbackNeverMutatesSavedGeometry() {
        val safe = PivotArcBounds(24f, 8f, 369f, 820f)
        val result =
            CompositeControlDeckSolver.resolve(
                manifest, baseline, SystemID.GBA, safe, viewport, GripArcSide.LEFT,
                Float.NaN, 999f, 999f, Float.NaN,
            )
        assertTrue(result is CompositeDeckResolution.Fits)
        val p = (result as CompositeDeckResolution.Fits).placement
        assertTrue((p.pivot.controls.values + p.dock.targets.values).all { it.inside(safe) })
    }
}
