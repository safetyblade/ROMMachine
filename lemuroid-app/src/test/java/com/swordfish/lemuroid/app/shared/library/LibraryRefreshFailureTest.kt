package com.swordfish.lemuroid.app.shared.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException
import java.io.IOException

class LibraryRefreshFailureTest {
    @Test
    fun copiedReportContainsPhaseButNeverExceptionDetails() {
        val report = LibraryRefreshFailure.report("synthetic-run", "database", IOException("content://secret/game.gba"))
        assertTrue(report.contains("Phase: database"))
        assertTrue(report.contains("Category: file_read"))
        assertFalse(report.contains("secret"))
        assertFalse(report.contains("game.gba"))
    }

    @Test
    fun staleAndroidSafPermissionIsActionableAndPathFree() {
        val exception = SecurityException("Cannot access content://com.android.providers/secret-data")
        assertEquals(LibraryRefreshFailure.FOLDER_ACCESS, LibraryRefreshFailure.code(exception))
        val message = LibraryRefreshFailure.userMessage(exception)
        assertTrue(message.contains("Re-select"))
        assertFalse(message.contains("secret-data"))
    }

    @Test
    fun renamedRomFolderAndMissingFileAreHandled() {
        assertEquals(
            LibraryRefreshFailure.FOLDER_ACCESS,
            LibraryRefreshFailure.code(IllegalStateException("ROM folder could not be enumerated")),
        )
        assertEquals(
            LibraryRefreshFailure.FOLDER_ACCESS,
            LibraryRefreshFailure.code(FileNotFoundException("content://private-file-path")),
        )
    }

    @Test
    fun nestedIOExceptionRemainsSanitized() {
        val failure = IllegalStateException("outer", IOException("private/file.iso"))
        assertEquals(LibraryRefreshFailure.FILE_READ, LibraryRefreshFailure.code(failure))
        assertFalse(LibraryRefreshFailure.userMessage(failure).contains("private/file.iso"))
    }

    @Test
    fun unknownScanErrorsNeverExposePathsOrClaimDataWasDeleted() {
        val failure = IllegalStateException("rom/private/path")
        assertEquals(LibraryRefreshFailure.SCAN_FAILURE, LibraryRefreshFailure.code(failure))
        val message = LibraryRefreshFailure.userMessage(failure)
        assertFalse(message.contains("rom/private/path"))
        assertTrue(message.contains("protected"))
    }
}
