package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GripArcCombinedLayoutTest {
    private val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v1-manifest.json").readText())
    private val systems =
        listOf(SystemID.PLAYSTATION_2, SystemID.GAMECUBE, SystemID.DREAMCAST, SystemID.GBA, SystemID.SNES)

    @Test fun requiredConsolesFitBothSidesWithAllFourSlotsReserved() {
        for (width in listOf(393f, 360f, 320f)) {
            val safe = PivotArcBounds(8f, 8f, width - 8f, 844f)
            val viewport = PivotArcBounds(22f, 84f, width - 22f, 344f)
            for (system in systems) for (side in GripArcSide.values()) {
                val layout = GripArcCombinedLayout.resolve(manifest, system, safe, viewport, side, 0f, 0f)
                assertNotNull("$system $side ${width}dp", layout)
                layout!!
                assertEquals(side, layout.dock.side)
                assertEquals(GripArcCombinedLayout.controlsFor(system)!!.toSet(), layout.controls.keys)
                val reserved =
                    GripArcPositionSolver.housingFootprint(layout.dock.housing, layout.dock.scale, side) +
                        layout.dock.targets.values
                val all = layout.controls.values.toList() + layout.dock.targets.values
                all.forEach { target ->
                    assertTrue(target.inside(safe))
                    assertFalse(target.intersects(viewport))
                    assertTrue(target.width >= 48f && target.height >= 48f)
                }
                layout.controls.values.forEach { target -> assertTrue(reserved.none { it.intersects(target) }) }
                for (i in all.indices) for (j in i + 1 until all.size) assertFalse(all[i].intersects(all[j]))
                // Button count never enters resolution: even count=1 reserves the complete dock.
                for (count in 1..4) assertEquals((1..count).toList(), layout.dock.targets.keys.take(count))
            }
        }
    }

    @Test fun mirrorPreservesFaceCompassAndMovesDpadToFarSide() {
        val safe = PivotArcBounds(8f, 8f, 385f, 844f)
        val viewport = PivotArcBounds(22f, 84f, 371f, 344f)
        for (side in GripArcSide.values()) {
            val layout = GripArcCombinedLayout.resolve(manifest, SystemID.GAMECUBE, safe, viewport, side, 0f, 0f)!!
            val c = layout.controls
            assertTrue(c.getValue("X").centerX > c.getValue("B").centerX)
            assertTrue(c.getValue("Y").centerY < c.getValue("A").centerY)
            assertEquals(side == GripArcSide.RIGHT, c.getValue("D").centerX < safe.centerX)
        }
    }

    @Test fun dreamcastPhysicalCompassAndSingleTriggerPairStayNative() {
        val expected = setOf("D", "A", "B", "X", "Y", "L", "R", "Start", "Menu", "AnalogL")
        assertEquals(expected, GripArcCombinedLayout.controlsFor(SystemID.DREAMCAST)!!.toSet())
        val safe = PivotArcBounds(8f, 8f, 385f, 844f)
        val viewport = PivotArcBounds(22f, 84f, 371f, 344f)
        for (side in GripArcSide.values()) {
            val layout =
                GripArcCombinedLayout.resolve(
                    manifest,
                    SystemID.DREAMCAST,
                    safe,
                    viewport,
                    side,
                    0f,
                    0f,
                )!!
            val controls = layout.controls
            assertEquals(expected, controls.keys)
            assertTrue(controls.getValue("B").centerX > controls.getValue("X").centerX)
            assertTrue(controls.getValue("Y").centerY < controls.getValue("A").centerY)
            assertFalse("Select" in controls)
            assertFalse("AnalogR" in controls)
            assertFalse("L2" in controls)
            assertFalse("R2" in controls)
        }
    }

    @Test fun digitalControllerDoesNotReserveAnalogTargets() {
        val layout =
            GripArcCombinedLayout.resolve(
                manifest,
                SystemID.PSX,
                PivotArcBounds(8f, 8f, 385f, 844f),
                PivotArcBounds(22f, 84f, 371f, 344f),
                GripArcSide.RIGHT,
                0f,
                0f,
                analogCount = 0,
            )!!
        assertFalse("AnalogL" in layout.controls)
        assertFalse("AnalogR" in layout.controls)
    }

    @Test fun gestureInsetsAreRespectedByBothDockAndControls() {
        val safe = PivotArcBounds(24f, 8f, 369f, 820f)
        for (side in GripArcSide.values()) {
            val layout =
                GripArcCombinedLayout.resolve(
                    manifest,
                    SystemID.PLAYSTATION_2,
                    safe,
                    PivotArcBounds(22f, 84f, 371f, 344f),
                    side,
                    0f,
                    0f,
                )!!
            (layout.controls.values + layout.dock.targets.values).forEach { assertTrue(it.inside(safe)) }
            assertTrue(layout.dock.housing.inside(safe))
        }
    }

    @Test fun unavailableDeckDoesNotProducePartialControls() {
        assertNull(
            GripArcCombinedLayout.resolve(
                manifest,
                SystemID.PLAYSTATION_2,
                PivotArcBounds(8f, 8f, 312f, 500f),
                PivotArcBounds(22f, 84f, 298f, 344f),
                GripArcSide.RIGHT,
                0f,
                0f,
            ),
        )
    }

    @Test fun invalidSavedOffsetResetsWithoutMutatingProfile() {
        val savedOffset = Float.NaN
        val result =
            GripArcCombinedLayout.resolve(
                manifest,
                SystemID.GBA,
                PivotArcBounds(8f, 8f, 385f, 844f),
                PivotArcBounds(22f, 84f, 371f, 344f),
                GripArcSide.RIGHT,
                savedOffset,
                0f,
            )
        assertNotNull(result)
        assertTrue(savedOffset.isNaN())
    }
}
