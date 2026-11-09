package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.core.assetsmanager.PPSSPPAssetArchiveInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Installs the separately version-matched upstream PPSSPP assets from the signed
 * CARTDECK APK. The old PPSSPP/1.15 directory is never touched.
 */
object EmulatorModernAssetsProvisioner {
    const val PSP_UPSTREAM_REVISION = "fa50bb1976065c4f8b1b47af227d367fe9771555"
    const val PSP_ASSET_PATH = "cartdeck/ppsspp-modern-assets.zip"

    fun installedAssetDirectory(context: Context): File =
        File(context.filesDir, "system-engines/ppsspp_modern/PPSSPP")

    fun isInstalled(context: Context): Boolean {
        val directory = installedAssetDirectory(context)
        return File(directory, "compat.ini").isFile &&
            File(directory, ".cartdeck-upstream-revision").takeIf { it.isFile }?.readText() == PSP_UPSTREAM_REVISION
    }

    suspend fun installIfBundled(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            if (isInstalled(context)) return@withContext true
            val zip =
                try {
                    context.assets.open(PSP_ASSET_PATH)
                } catch (error: java.io.FileNotFoundException) {
                    return@withContext false
                }
            runCatching {
                zip.use { stream ->
                    ZipInputStream(stream).use {
                        PPSSPPAssetArchiveInstaller.install(it, installedAssetDirectory(context))
                    }
                }
                check(File(installedAssetDirectory(context), "compat.ini").isFile) {
                    "Upstream PPSSPP compatibility database missing"
                }
                File(installedAssetDirectory(context), ".cartdeck-upstream-revision")
                    .writeText(PSP_UPSTREAM_REVISION)
                true
            }.getOrElse { error ->
                Timber.e(error, "Matching upstream PPSSPP assets not installed; old engine remains usable")
                false
            }
        }
}
