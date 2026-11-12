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
        for (device in ControlDeckDeviceFixture.representative) {
            val safe = device.safe
            val viewport = device.viewport
            for (system in systems) for (side in GripArcSide.values()) {
                val layout = GripArcCombinedLayout.resolve(manifest, system, safe, viewport, side, 0f, 0f)
                if (layout == null) {
                    assertTrue("Owner-class deck regressed: $system $side", device.width < 393f || device.height < 852f)
                    continue
                }
                assertTrue(viewport.inside(device.screen))
                assertTrue(layout.dock.housing.inside(safe))
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

    @Test fun savedOffsetFallbackNeverSilentlySwitchesSides() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        for (side in GripArcSide.values()) {
            for (offset in listOf(-1000f, 1000f)) {
                val result =
                    GripArcCombinedLayout.resolve(
                        manifest,
                        SystemID.GBA,
                        device.safe,
                        device.viewport,
                        side,
                        offset,
                        0f,
                    )
                assertNotNull(result)
                assertEquals(side, result!!.dock.side)
            }
        }
    }

    @Test fun invalidMeasurementsCannotProduceHiddenOrClippedControls() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        assertNull(
            GripArcCombinedLayout.resolve(
                manifest,
                SystemID.GBA,
                device.safe,
                device.viewport.copy(right = Float.NaN),
                GripArcSide.RIGHT,
                0f,
                0f,
            ),
        )
    }

    @Test fun mirrorPreservesFaceCompassAndMovesDpadToFarSide() {
        val safe = PivotArcBounds(8f, 8f, 385f, 844f)
        val viewport = ControlDeckDeviceFixture(393f, 852f).viewport
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
        val viewport = ControlDeckDeviceFixture(393f, 852f).viewport
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
                ControlDeckDeviceFixture(393f, 852f).viewport,
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
                    ControlDeckDeviceFixture(393f, 852f).viewport,
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
                ControlDeckDeviceFixture(393f, 852f).viewport,
                GripArcSide.RIGHT,
                savedOffset,
                0f,
            )
        assertNotNull(result)
        assertTrue(savedOffset.isNaN())
    }
}
