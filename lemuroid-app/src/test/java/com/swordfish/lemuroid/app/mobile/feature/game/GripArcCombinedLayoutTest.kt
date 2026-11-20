package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GripArcCombinedLayoutTest {
    private val manifest = GripArcManifest.parse(File("src/main/assets/griparc/griparc-v22-manifest.json").readText())
    private val controllers =
        listOf(
            TouchControllerID.GB,
            TouchControllerID.GBA,
            TouchControllerID.SNES,
            TouchControllerID.PSX,
            TouchControllerID.PSX_DUALSHOCK,
            TouchControllerID.PSP,
            TouchControllerID.GAMECUBE,
            TouchControllerID.DREAMCAST,
            TouchControllerID.XBOX,
        )

    // Synthetic native-bound fixture. Runtime uses the actual native composable observations.
    private fun measured(native: NativeControlInventory): Map<String, PivotArcBounds> {
        fun box(
            x: Float,
            y: Float,
            size: Float = 48f,
        ) = PivotArcBounds(x - size / 2, y - size / 2, x + size / 2, y + size / 2)
        val slots =
            mapOf(
                "dpad" to box(82f, 615f, 128f),
                "face-ab" to box(308f, 600f, 152f),
                "face-abxy" to box(308f, 600f, 152f),
                "analog-left" to box(100f, 745f, 76f),
                "analog-right" to box(285f, 745f, 76f),
                "select" to box(144f, 500f),
                "start" to box(234f, 480f),
                "menu" to box(196f, 780f),
                "shoulder-L" to box(30f, 450f),
                "shoulder-L1" to box(30f, 450f),
                "shoulder-L2" to box(88f, 450f),
                "shoulder-R" to box(360f, 450f),
                "shoulder-R1" to box(360f, 450f),
                "shoulder-R2" to box(302f, 450f),
                "aux-Z" to box(302f, 450f),
            )
        val result = native.controls.associate { it.seat to slots.getValue(it.seat) }.toMutableMap()
        if (native.controller == TouchControllerID.GAMECUBE) {
            result["analog-left"] = slots.getValue("dpad")
            result["dpad"] = slots.getValue("analog-left")
        }
        return result
    }

    @Test fun closeFitPreservesControllerConstellationsAndMinimumTargets() {
        val anchor = PivotArcBounds(20f, 200f, 120f, 300f)
        val upper = PivotArcBounds(40f, 212f, 88f, 260f)
        val lower = PivotArcBounds(40f, 264f, 88f, 312f)
        val originalDx = lower.centerX - upper.centerX
        val originalDy = lower.centerY - upper.centerY
        val top = GripArcCombinedLayout.closeFit(upper, anchor, 0.90f, 8f, -16f)
        val bottom = GripArcCombinedLayout.closeFit(lower, anchor, 0.90f, 8f, -16f)
        assertEquals(originalDx * 0.90f, bottom.centerX - top.centerX, 0.001f)
        assertEquals(originalDy * 0.90f, bottom.centerY - top.centerY, 0.001f)
        assertTrue(top.width >= 48f && top.height >= 48f)
        assertTrue(bottom.width >= 48f && bottom.height >= 48f)
        assertTrue(kotlin.math.abs(top.centerY - upper.centerY) <= 24f)
        assertTrue(kotlin.math.abs(bottom.centerY - lower.centerY) <= 24f)
    }

    @Test fun everySelectedNativeInventorySurvivesResolutionOnBothSides() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        for (controller in controllers) for (side in GripArcSide.values()) {
            val native = NativeControlInventory.forController(controller)!!
            val placement =
                GripArcCombinedLayout.resolve(
                    manifest,
                    native, native.inputIds,
                    measured(native),
                    device.safe,
                    device.viewport,
                    side,
                    0f,
                    0f,
                )
            assertNotNull("$controller $side", placement)
            val p = placement!!
            assertTrue(p.complete)
            assertEquals(native.controls.map { it.key }.toSet(), p.controls.keys)
            assertEquals(side, p.dock.side)
            val reserved = GripArcPositionSolver.housingFootprint(p.dock.housing, 1f, side) + p.dock.targets.values
            val boxes = p.controls.values.toList()
            boxes.forEach { r ->
                assertTrue(r.width >= 48f && r.height >= 48f)
                assertTrue(r.inside(device.safe))
                assertFalse(r.intersects(device.viewport))
                assertTrue(reserved.none { it.intersects(r) })
            }
            for (i in boxes.indices) for (j in i + 1 until boxes.size) assertFalse(boxes[i].intersects(boxes[j]))
            p.seats.values.forEach { r -> assertTrue(reserved.none { it.intersects(r.inset(3f)) }) }
            native.controls.filter { it.kind == NativeControlKind.FACE }.forEach { face ->
                assertEquals(face.keyCode, p.native.controls.single { it.key == face.key }.keyCode)
            }
        }
    }

    @Test fun oversizedStandardArtworkReflowsAfterDockReservation() {
        for (device in ControlDeckDeviceFixture.representative) for (side in GripArcSide.values()) {
            for (controller in controllers) {
                val native = NativeControlInventory.forController(controller)!!
                // Complete observations can be unusably large or outside the current safe region.
                val oversized =
                    measured(native).mapValues { (_, r) ->
                        PivotArcBounds(r.left * 2f, r.top * 2f, r.right * 2f, r.bottom * 2f)
                    }
                val p =
                    GripArcCombinedLayout.resolve(
                        manifest, native, native.inputIds, oversized,
                        device.safe, device.viewport, side, 0f, 0f,
                    )
                if (p == null) {
                    // Explicit unavailable is safer than a completely rearranged controller.
                    assertTrue(device.safe.height < 650f)
                    continue
                }
                assertTrue(p.complete)
                val reserved =
                    GripArcPositionSolver.housingFootprint(p.dock.housing, p.dock.scale, side) +
                        p.dock.targets.values
                val boxes = p.controls.values.toList()
                boxes.forEach {
                    assertTrue(it.width >= 48f && it.height >= 48f)
                    assertTrue(it.inside(device.safe))
                    assertFalse(it.intersects(device.viewport))
                    assertTrue(reserved.none { r -> r.intersects(it) })
                }
                for (i in boxes.indices) for (j in i + 1 until boxes.size) {
                    assertFalse(boxes[i].intersects(boxes[j]))
                }
                assertEquals(
                    p,
                    GripArcCombinedLayout.resolve(
                        manifest, native, native.inputIds, oversized,
                        device.safe, device.viewport, side, 0f, 0f,
                    ),
                )
            }
        }
    }

    @Test fun firstFrameMatchesNativeControllerGroupsInsteadOfScatteringThem() {
        val device = ControlDeckDeviceFixture(393f, 852f)
        for (controller in controllers) {
            val native = NativeControlInventory.forController(controller)!!
            val seats = GripArcCombinedLayout.initialNativeConstellation(native, device.safe, device.viewport)
            assertNotNull("$controller", seats)
            val positions = seats!!
            assertEquals(native.controls.map { it.seat }.toSet(), positions.keys)
            val dpad = positions.getValue("dpad")
            val face = positions.values.first { it.centerX > device.safe.centerX && it.width > 80f }
            assertTrue("$controller directional controls left", dpad.centerX < face.centerX)
            for (spec in native.controls) {
                assertTrue(
                    "$controller " + spec.key + " initial geometry",
                    positions.getValue(spec.seat).isFiniteArea(),
                )
            }
            if (controller == TouchControllerID.GAMECUBE) {
                assertFalse(positions.getValue("shoulder-R").intersects(positions.getValue("aux-Z")))
            }
        }
    }

    @Test fun firstFrameNeedsNoMeasurementsFromControlsBeingReplaced() {
        for (device in ControlDeckDeviceFixture.representative) for (controller in controllers) {
            for (side in GripArcSide.values()) {
                val native = NativeControlInventory.forController(controller)!!
                val p =
                    GripArcCombinedLayout.resolveDockFirst(
                        manifest,
                        native,
                        device.safe,
                        device.viewport,
                        side,
                        0f,
                        0f,
                    )
                if (p != null) {
                    assertTrue("$controller $side $device", p.complete)
                    assertEquals(native.controls.map { it.key }.toSet(), p.controls.keys)
                    assertTrue(p.controls.values.none { it.intersects(device.viewport) })
                } else {
                    // A cramped device must not scatter or silently omit controls.
                    val baseline =
                        GripArcCombinedLayout.initialNativeConstellation(native, device.safe, device.viewport)
                    assertTrue(baseline == null || device.safe.height < 650f)
                }
            }
        }
    }

    @Test fun missingNativeGeometryIsRejectedRatherThanDroppingControls() {
        val native = NativeControlInventory.forController(TouchControllerID.PSX_DUALSHOCK)!!
        val d = ControlDeckDeviceFixture(393f, 852f)
        assertNull(
            GripArcCombinedLayout.resolve(
                manifest,
                native, native.inputIds,
                measured(native) - "analog-right",
                d.safe,
                d.viewport,
                GripArcSide.RIGHT,
                0f,
                0f,
            ),
        )
        assertFalse(native.matchesNativeInputs(native.inputIds - "key:109"))
        assertFalse(native.matchesNativeInputs(native.inputIds + "key:999"))
        assertTrue(native.matchesNativeInputs(native.inputIds))
        assertNull(
            GripArcCombinedLayout.resolve(
                manifest, native, native.inputIds + "key:999", measured(native),
                d.safe, d.viewport, GripArcSide.RIGHT, 0f, 0f,
            ),
        )
    }

    @Test fun measuredNarrowAndShortDecksCanNeverReturnClippedPartialInventory() {
        for (device in ControlDeckDeviceFixture.representative) for (controller in controllers) {
            for (side in GripArcSide.values()) {
                val native = NativeControlInventory.forController(controller)!!
                val source =
                    measured(native).mapValues { (_, r) ->
                        r.copy(
                            left = r.left * device.width / 393f,
                            right = r.right * device.width / 393f,
                            top = r.top * device.height / 852f,
                            bottom = r.bottom * device.height / 852f,
                        )
                    }
                val p =
                    GripArcCombinedLayout.resolve(
                        manifest, native, native.inputIds, source, device.safe, device.viewport, side, 0f, 0f,
                    ) ?: continue
                assertTrue(p.complete)
                assertEquals(side, p.dock.side)
                (p.controls.values + p.dock.targets.values).forEach {
                    assertTrue(it.inside(device.safe))
                    assertFalse(it.intersects(device.viewport))
                }
            }
        }
    }
}
