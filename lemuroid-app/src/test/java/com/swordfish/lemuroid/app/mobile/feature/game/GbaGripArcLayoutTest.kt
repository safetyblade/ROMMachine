package com.swordfish.lemuroid.app.mobile.feature.game

import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GbaGripArcLayoutTest {
    private val native = NativeControlInventory.forController(TouchControllerID.GBA)!!
    private val safe = PivotArcBounds(8f, 8f, 385f, 844f)
    private val viewport = PivotArcBounds(8f, 120f, 385f, 320f)

    @Test
    fun gbaHasCompleteNativeKeysAndCompactSeparateFourSlotBank() {
        for (side in listOf(GripArcSide.RIGHT, GripArcSide.LEFT)) {
            val built = GbaGripArcLayout.resolve(native, native.inputIds, safe, viewport, side)
            assertNotNull("GBA full-body must fit on owner-size deck: $side", built)
            val placement = built!!
            assertTrue(placement.complete)
            assertEquals(native.controls.map { it.key }.toSet(), placement.controls.keys)
            assertEquals((1..4).toSet(), placement.dock.targets.keys)
            assertTrue(placement.dock.targets.values.all { it.width >= 48f && it.height >= 48f })
            val all = placement.controls.values.toList() + placement.dock.targets.values
            assertTrue(all.all { it.inside(safe) && !it.intersects(viewport) })
            assertFalse(
                all.indices.any { a ->
                    (a + 1 until all.size).any { b -> all[a].inset(1f).intersects(all[b].inset(1f)) }
                },
            )
            val a = placement.controls.getValue("A")
            val b = placement.controls.getValue("B")
            assertTrue(
                "Face buttons must be in a shared reachable cluster",
                kotlin.math.abs(a.centerX - b.centerX) < 90f,
            )
            assertTrue(
                "Face buttons must be in a shared reachable cluster",
                kotlin.math.abs(a.centerY - b.centerY) < 75f,
            )
            assertEquals(4, placement.dock.visuals.size)
        }
    }

    @Test
    fun incompleteInputAndInsufficientHeightFailClosed() {
        assertNull(
            GbaGripArcLayout.resolve(
                native,
                emptySet(),
                safe,
                viewport,
                GripArcSide.RIGHT,
            ),
        )
        assertNull(
            GbaGripArcLayout.resolve(
                native,
                native.inputIds,
                PivotArcBounds(8f, 8f, 385f, 600f),
                viewport,
                GripArcSide.RIGHT,
            ),
        )
    }
}
