package com.swordfish.lemuroid.app.shared.settings

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LibraryFolderAccessTest {
    @Test fun providerRefusalPreservesThePreviousLibraryFolder() {
        val existing = Uri.parse("content://test/tree/existing")
        val selected = Uri.parse("content://test/tree/refused")
        var stored = existing
        assertFalse(acceptLibraryFolder(existing, selected, { throw SecurityException() }, { stored = it }))
        assertEquals(existing, stored)
    }

    @Test fun acceptedFolderOnlyNeedsReadAccessAndIsSavedAfterTheGrant() {
        val selected = Uri.parse("content://test/tree/read-only")
        val events = mutableListOf<String>()
        assertTrue(acceptLibraryFolder(null, selected, { events.add("grant:$it") }, { events.add("save:$it") }))
        assertEquals(listOf("grant:$selected", "save:$selected"), events)
    }
}
