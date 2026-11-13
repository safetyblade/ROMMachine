package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CompositeControlDeckSolverTest {
    private val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v22-manifest.json").readText())
    private val baseline =
        PivotArcLayoutProfile.parse(
            File("src/main/assets/pivotarc/candidate-c-baseline.json").readText(),
        )
    private val viewport = ControlDeckDeviceFixture(393f, 852f).viewport

    @Test fun supportedControllerPathsHaveOneCompleteCollisionFreePlacement() {
        for (device in ControlDeckDeviceFixture.representative) for (system in listOf(
            SystemID.GB,
            SystemID.GBC,
            SystemID.GBA,
            SystemID.SNES,
        )) {
            val viewport = device.viewport
            for (side in GripArcSide.values()) {
                val safe = device.safe
                val result = CompositeControlDeckSolver.resolve(manifest, baseline, system, safe, viewport, side)
                // Narrow/short devices may correctly reject a joint deck. Any fit must be physically safe.
                if (result is CompositeDeckResolution.Unavailable) {
                    assertEquals(CompositeDeckFailure.INSUFFICIENT_SAFE_SPACE, result.failure)
                    continue
                }
                val p = (result as CompositeDeckResolution.Fits).placement
                assertTrue(viewport.inside(device.screen))
                assertTrue(p.dock.housing.inside(safe))
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
                    assertTrue(r.inset(PivotIslandShape.OUTLINE_WIDTH_DP / 2f).inside(safe))
                    assertFalse(r.inset(PivotIslandShape.OUTLINE_WIDTH_DP / 2f).intersects(viewport))
                    assertTrue(p.dockFootprint.none { it.intersects(r.inset(PivotIslandShape.OUTLINE_WIDTH_DP / 2f)) })
                    assertTrue(
                        p.dock.targets.values.none { it.intersects(r.inset(PivotIslandShape.OUTLINE_WIDTH_DP / 2f)) },
                    )
                }
                p.pivot.controls.forEach { (key, target) ->
                    val visual = p.pivot.visuals.getValue(key)
                    assertTrue(visual.w <= target.width && visual.h <= target.height)
                }
                manifest.slots.forEach { slot ->
                    val center = if (side == GripArcSide.RIGHT) slot.centerRight else slot.centerLeft
                    val target = p.dock.targets.getValue(slot.n)
                    assertEquals(p.dock.housing.left + center.x * p.dock.scale, target.centerX, 0.05f)
                    assertEquals(p.dock.housing.top + center.y * p.dock.scale, target.centerY, 0.05f)
                    val visual = p.dock.visuals.getValue(slot.n)
                    assertTrue(visual.w <= target.width && visual.h <= target.height)
                }
                p.wordmark?.let { brand ->
                    assertTrue(brand.inside(p.island.upper))
                    assertTrue(targets.none { brand.intersects(it) })
                }
                assertTrue(p.island.upper.width > p.island.lower.width)
                // Counts only change which of these permanent slots receive input/rendered buttons.
                for (count in 1..4) assertEquals((1..count).toList(), p.dock.targets.keys.take(count))
            }
        }
    }

    @Test fun ownerClassStillFitsAllSupportedPathsOnPreferredSide() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        for (system in listOf(SystemID.GB, SystemID.GBC, SystemID.GBA, SystemID.SNES)) {
            for (side in GripArcSide.values()) {
                val result =
                    CompositeControlDeckSolver.resolve(
                        manifest,
                        baseline,
                        system,
                        device.safe,
                        device.viewport,
                        side,
                    )
                assertTrue("$system $side", result is CompositeDeckResolution.Fits)
                assertEquals(side, (result as CompositeDeckResolution.Fits).placement.dock.side)
            }
        }
    }

    @Test fun savedOffsetFallbackKeepsPreferredSide() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        for (side in GripArcSide.values()) {
            for (offset in listOf(-1000f, 1000f)) {
                val result =
                    CompositeControlDeckSolver.resolve(
                        manifest,
                        baseline,
                        SystemID.GBA,
                        device.safe,
                        device.viewport,
                        side,
                        gripDx = offset,
                    )
                assertTrue(result is CompositeDeckResolution.Fits)
                assertEquals(side, (result as CompositeDeckResolution.Fits).placement.dock.side)
            }
        }
    }

    @Test fun invalidMeasurementsNeverReportFit() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        for (video in listOf(device.viewport.copy(right = Float.NaN), device.viewport.copy(bottom = 0f))) {
            val result =
                CompositeControlDeckSolver.resolve(
                    manifest,
                    baseline,
                    SystemID.GBA,
                    device.safe,
                    video,
                    GripArcSide.RIGHT,
                )
            assertEquals(CompositeDeckFailure.INVALID_GEOMETRY, (result as CompositeDeckResolution.Unavailable).failure)
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
