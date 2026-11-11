package com.swordfish.lemuroid.app.shared.library

import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale

/**
 * Safe, actionable failure details for the library refresh snackbar.
 * Never display a raw SAF URI, ROM path, game title or provider exception.
 */
internal object LibraryRefreshFailure {
    const val FOLDER_ACCESS = "folder_access"
    const val FILE_READ = "file_read"
    const val SCAN_FAILURE = "scan_failure"

    fun code(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.take(8).toList()
        if (causes.any { it is SecurityException || it is FileNotFoundException }) return FOLDER_ACCESS

        val messages = causes.map { it.message.orEmpty().lowercase(Locale.US) }
        val folderError =
            messages.any { it.contains("rom folder") } ||
                messages.any { it.contains("permission denied") } ||
                messages.any { it.contains("document tree") } ||
                messages.any { it.contains("persisted permission") } ||
                messages.any { it.contains("cannot read the selected") }
        if (folderError) return FOLDER_ACCESS
        if (causes.any { it is IOException }) return FILE_READ
        return SCAN_FAILURE
    }

    fun userMessage(error: Throwable): String =
        when (code(error)) {
            FOLDER_ACCESS ->
                "ROM folder access changed. Re-select the parent ROMS folder in Settings and refresh."
            FILE_READ ->
                "A ROM or folder could not be read. Check files in the selected ROMS folder and retry."
            else ->
                "Library scan failed. Existing cartridges and saves were protected. Check issue #115."
        }
}
