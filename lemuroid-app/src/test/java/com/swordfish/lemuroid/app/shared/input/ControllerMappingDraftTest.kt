package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControllerMappingDraftTest {
    @Test fun conflictRequiresExplicitCorrectionAndCancelRestoresSavedMap() {
        val original = mapOf(96 to 96, 97 to 97)
        val draft = ControllerMappingDraft(original)
        assertFalse(draft.assign(96, 97))
        assertEquals(original, draft.bindings)
        assertEquals(96 to 97, draft.conflict)
        draft.resolveConflict()
        assertEquals(mapOf(96 to 97, 97 to 96), draft.bindings)
        assertTrue(draft.dirty)
        draft.cancel()
        assertEquals(original, draft.bindings)
        assertFalse(draft.dirty)
    }

    @Test fun twoPhysicalPressesPreserveBothActionsUntilConfirmed() {
        val original = mapOf(105 to 96, 99 to 97)
        val draft = ControllerMappingDraft(original)
        assertFalse(draft.requestSwap(105, 105))
        assertTrue(draft.requestSwap(105, 99))
        assertEquals(original, draft.bindings)
        draft.cancelConflict()
        draft.resolveConflict()
        assertEquals(original, draft.bindings)
        assertTrue(draft.requestSwap(105, 99))
        draft.resolveConflict()
        assertEquals(mapOf(105 to 97, 99 to 96), draft.bindings)
        draft.undo()
        assertEquals(original, draft.bindings)
    }

    @Test fun undoResetAndSaveFormSingleDeviceTransaction() {
        val draft = ControllerMappingDraft(mapOf(96 to 96))
        assertTrue(draft.assign(100, 99))
        draft.saved()
        assertFalse(draft.dirty)
        draft.reset(mapOf(97 to 97))
        assertTrue(draft.canUndo)
        draft.undo()
        assertEquals(mapOf(96 to 96, 100 to 99), draft.bindings)
        draft.assign(101, 99)
        assertEquals(101, draft.bindings.entries.single { it.value == 99 }.key)
        draft.cancel()
        assertEquals(100, draft.bindings.entries.single { it.value == 99 }.key)
    }
}
