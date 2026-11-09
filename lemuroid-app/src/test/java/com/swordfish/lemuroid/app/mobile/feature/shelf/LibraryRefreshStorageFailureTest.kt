package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.lemuroid.lib.storage.local.LocalStorageProvider
import com.swordfish.lemuroid.lib.storage.local.StorageAccessFrameworkProvider
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import com.swordfish.lemuroid.lib.R as LibraryR

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LibraryRefreshStorageFailureTest {
    @Test fun missingLocalRomFolderCannotMasqueradeAsAnEmptyScan() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val path = File(context.cacheDir, "missing-rom-folder-${System.nanoTime()}")
        val key = context.getString(LibraryR.string.pref_key_legacy_external_folder)

        withLibraryPreference(context, key, path.absolutePath) {
            val provider = LocalStorageProvider(context, DirectoriesManager(context))
            assertThrows(IllegalStateException::class.java) {
                runBlocking { provider.listBaseStorageFiles().toList() }
            }
        }
    }

    @Test fun accessibleEmptyLocalRomFolderIsAValidCompleteScan() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val path = File(context.cacheDir, "empty-rom-folder-${System.nanoTime()}").apply { mkdirs() }
        val key = context.getString(LibraryR.string.pref_key_legacy_external_folder)

        try {
            withLibraryPreference(context, key, path.absolutePath) {
                val provider = LocalStorageProvider(context, DirectoriesManager(context))
                val results = runBlocking { provider.listBaseStorageFiles().toList() }
                assertEquals(listOf(emptyList<Any>()), results)
            }
        } finally {
            path.deleteRecursively()
        }
    }

    @Test fun missingSafDocumentProviderCannotMasqueradeAsAnEmptyScan() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val key = context.getString(LibraryR.string.pref_key_extenral_folder)

        withLibraryPreference(context, key, "content://cartdeck.missing.documents/tree/roms") {
            val provider = StorageAccessFrameworkProvider(context)
            assertThrows(Exception::class.java) {
                runBlocking { provider.listBaseStorageFiles().toList() }
            }
        }
    }

    private fun withLibraryPreference(
        context: Context,
        key: String,
        value: String,
        block: () -> Unit,
    ) {
        val prefs = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val previous = prefs.getString(key, null)
        prefs.edit().putString(key, value).commit()
        try {
            block()
        } finally {
            val edit = prefs.edit()
            if (previous == null) edit.remove(key) else edit.putString(key, previous)
            edit.commit()
        }
    }
}
