package com.swordfish.lemuroid.lib.core.assetsmanager

import android.content.SharedPreferences
import android.net.Uri
import com.swordfish.lemuroid.lib.core.CoreUpdater
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class PPSSPPAssetsManager : CoreID.AssetsManager {
    override suspend fun clearAssets(directoriesManager: DirectoriesManager) {
        getAssetsDirectory(directoriesManager).deleteRecursively()
    }

    override suspend fun retrieveAssetsIfNeeded(
        coreUpdaterApi: CoreUpdater.CoreManagerApi,
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ) {
        val destination = getAssetsDirectory(directoriesManager)
        withContext(Dispatchers.IO) {
            PPSSPPAssetArchiveInstaller.recoverInterruptedInstallation(destination)
        }
        if (!updatedRequested(directoriesManager, sharedPreferences)) return

        try {
            val response = coreUpdaterApi.downloadZip(PPSSPP_ASSETS_URL.toString())
            check(response.isSuccessful) { "PPSSPP assets download failed: HTTP ${response.code()}" }
            val archive = response.body() ?: error("PPSSPP assets download contained no archive")
            withContext(Dispatchers.IO) {
                archive.use { PPSSPPAssetArchiveInstaller.install(it, destination) }
            }
            check(
                sharedPreferences.edit()
                    .putString(PPSSPP_ASSETS_VERSION_KEY, PPSSPP_ASSETS_VERSION)
                    .commit(),
            ) { "Unable to record installed PPSSPP assets version" }
        } catch (error: Exception) {
            // Never delete a working installation because a download or update failed.
            Timber.e(error, "Unable to update PPSSPP support assets; previous version retained")
        }
    }

    private suspend fun updatedRequested(
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val directoryExists = getAssetsDirectory(directoriesManager).exists()

            val currentVersion = sharedPreferences.getString(PPSSPP_ASSETS_VERSION_KEY, "none")
            val hasCurrentVersion = currentVersion == PPSSPP_ASSETS_VERSION

            !directoryExists || !hasCurrentVersion
        }

    private suspend fun getAssetsDirectory(directoriesManager: DirectoriesManager): File {
        return withContext(Dispatchers.IO) {
            File(directoriesManager.getSystemDirectory(), PPSSPP_ASSETS_FOLDER_NAME)
        }
    }

    companion object {
        const val PPSSPP_ASSETS_VERSION = "1.15"

        val PPSSPP_ASSETS_URL: Uri =
            Uri.parse("https://github.com/Swordfish90/LemuroidCores/")
                .buildUpon()
                .appendEncodedPath("raw/$PPSSPP_ASSETS_VERSION/assets/ppsspp.zip")
                .build()

        const val PPSSPP_ASSETS_VERSION_KEY = "ppsspp_assets_version_key"

        const val PPSSPP_ASSETS_FOLDER_NAME = "PPSSPP"
    }
}
