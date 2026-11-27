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
    fun portraitSeatSizesUseClearanceWithoutChangingApprovedCenters() {
        val fitted = checkNotNull(StandaloneGripArcLayout.resolveStandalone(native, safe, viewport, false))
        val action = fitted.controls.getValue("A")
        assertTrue(fitted.visuals.getValue("A").w > 44f)
        assertTrue(fitted.dock.visuals.values.any { it.h > 40f })
        assertTrue(fitted.visuals.getValue("D").w > 76f)
        val panel = fitted.dock.housing
        assertEquals(panel.left + panel.width * 278f / 360f, action.centerX, 0.01f)
        val all = fitted.controls.values + fitted.dock.targets.values
        assertTrue(all.all { it.inside(panel) && !it.intersects(viewport) })
        assertFalse(all.indices.any { i -> all.drop(i + 1).any(all[i]::intersects) })
    }

    @Test
    fun segaModesUseSeparateInventoriesAndAtariRetainsInactiveSlots() {
        val three = checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.GENESIS_3))
        val six = checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.GENESIS_6))
        assertFalse(three.controls.any { it.label == "Mode" })
        assertTrue(six.controls.any { it.label == "Mode" })
        val profile = GripArcProfile(mappings = listOf(listOf(android.view.KeyEvent.KEYCODE_BUTTON_L1)))
        assertNull(profile.supportedActionFor(1, three.controls.mapNotNull { it.keyCode }.toSet()))
        assertNotNull(profile.supportedActionFor(1, six.controls.mapNotNull { it.keyCode }.toSet()))
        val atari = checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.ATARI2600))
        val layout = checkNotNull(StandaloneGripArcLayout.resolveStandalone(atari, safe, viewport, false))
        assertEquals(setOf(1, 2), layout.dock.targets.keys)
    }

    @Test
    fun approvedDigitalPacketsUseIndependentTargetsAcrossOrientations() {
        for (id in listOf(
            TouchControllerID.GBA,
            TouchControllerID.GB,
            TouchControllerID.NES,
            TouchControllerID.SMS,
            TouchControllerID.GG,
            TouchControllerID.NGP,
            TouchControllerID.PCE,
        )) {
            val inventory = checkNotNull(StandaloneGripArcLayout.inventoryFor(id))
            val portrait = checkNotNull(StandaloneGripArcLayout.resolveStandalone(inventory, safe, viewport, false))
            val wideSafe = PivotArcBounds(8f, 8f, 732f, 352f)
            val wideVideo = PivotArcBounds(180f, 15f, 560f, 290f)
            val landscape =
                checkNotNull(StandaloneGripArcLayout.resolveStandalone(inventory, wideSafe, wideVideo, true))
            assertEquals(portrait.controls.keys, landscape.controls.keys)
            assertTrue(landscape.complete)
            assertTrue(portrait.dock.targets.getValue(1).centerY < portrait.controls.getValue("D").centerY)
            assertTrue(landscape.dock.targets.getValue(4).right < wideVideo.left)
            assertTrue(landscape.dock.targets.getValue(2).left > wideVideo.right)
        }
    }

    @Test
    fun snesUsesAnIndependentFaceDiamondAndApprovedSlotIdentities() {
        val inventory = checkNotNull(StandaloneGripArcLayout.inventoryFor(TouchControllerID.SNES))
        val wideSafe = PivotArcBounds(8f, 8f, 732f, 352f)
        val wideVideo = PivotArcBounds(180f, 15f, 560f, 290f)
        val result = checkNotNull(StandaloneGripArcLayout.resolveStandalone(inventory, wideSafe, wideVideo, true))
        assertTrue(result.controls.getValue("X").centerY < result.controls.getValue("A").centerY)
        assertTrue(result.controls.getValue("A").centerY < result.controls.getValue("B").centerY)
        assertTrue(result.controls.getValue("Y").centerX < result.controls.getValue("X").centerX)
        assertTrue(result.controls.getValue("X").centerX < result.controls.getValue("A").centerX)
        assertTrue(result.dock.targets.getValue(2).right < wideVideo.left)
        assertTrue(result.dock.targets.getValue(3).left > wideVideo.right)
    }

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
