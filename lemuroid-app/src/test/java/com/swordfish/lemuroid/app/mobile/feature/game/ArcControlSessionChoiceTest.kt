package com.swordfish.lemuroid.app.mobile.feature.game

import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.game.viewmodel.PivotArcHeldInputGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcControlSessionChoiceTest {
    @Test fun standardSessionFallbackDisablesBothOwnersAndRestoresOrdinaryInputPath() {
        for (savedMode in listOf(ArcControlMode.STANDARD_GRIPARC, ArcControlMode.PIVOTARC_GRIPARC)) {
            val events = mutableListOf<Pair<Int, Int>>()
            val gate = PivotArcHeldInputGate { action, key -> events.add(action to key) }
            gate.setEnabled(savedMode.hasPivot)
            gate.setGripEnabled(savedMode.hasGrip)
            val a = KeyEvent.KEYCODE_BUTTON_A
            val b = KeyEvent.KEYCODE_BUTTON_B
            gate.toggleHold(b)
            for (slot in 1..4) gate.setGripSlot(slot, true, setOf(a, b))
            val choice = ArcControlSessionChoice().useStandard()
            val effective = choice.modeFor(savedMode)
            gate.setEnabled(effective.hasPivot)
            gate.setGripEnabled(effective.hasGrip)
            assertEquals(ArcControlMode.STANDARD, effective)
            assertTrue(gate.holds.value.isEmpty())
            for (key in setOf(a, b)) assertEquals(1, events.count { it == KeyEvent.ACTION_UP to key })
            // False delegates to the unchanged Standard/native input handler, rather than swallowing input.
            assertFalse(gate.setSource(a, "touch", true))
            assertFalse(gate.setSource(b, "physical", true))
            assertTrue(savedMode.hasGrip)
            assertEquals(ArcControlMode.STANDARD, choice.modeFor(savedMode)) // survives new layout measurements
            assertEquals(savedMode, ArcControlSessionChoice().modeFor(savedMode)) // new session restores saved choice
        }
    }

    @Test fun sideOnlyChangesAfterExplicitSessionChoiceAndDoesNotRewriteSavedChoice() {
        for (saved in GripArcSide.values()) {
            val original = ArcControlSessionChoice()
            assertEquals(saved, original.sideFor(saved))
            val changed = original.tryOppositeSide(saved)
            assertTrue(changed.sideFor(saved) != saved)
            assertEquals(saved, original.sideFor(saved))
            assertEquals(saved, ArcControlSessionChoice().sideFor(saved))
            assertEquals(saved, changed.tryOppositeSide(saved).sideFor(saved))
            assertEquals(ArcControlMode.PIVOTARC_GRIPARC, changed.modeFor(ArcControlMode.PIVOTARC_GRIPARC))
        }
    }
}
