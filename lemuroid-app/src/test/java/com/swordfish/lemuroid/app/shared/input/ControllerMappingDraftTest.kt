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
