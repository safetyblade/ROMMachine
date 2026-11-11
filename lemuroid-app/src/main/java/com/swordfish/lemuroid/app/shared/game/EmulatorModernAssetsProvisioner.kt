package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.core.assetsmanager.PPSSPPAssetArchiveInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.zip.ZipInputStream

/** Installs revision-matched upstream assets without touching legacy support files or PSP savedata. */
object EmulatorModernAssetsProvisioner {
    const val PSP_UPSTREAM_REVISION = "fa50bb1976065c4f8b1b47af227d367fe9771555"
    const val PSP_ASSET_PATH = "cartdeck/ppsspp-modern-assets.zip"
    const val PSP_MANIFEST_PATH = "cartdeck/ppsspp-modern.properties"
    private const val REVISION_MARKER = ".cartdeck-upstream-revision"

    fun installedAssetDirectory(context: Context): File = File(context.filesDir, "system-engines/ppsspp_modern/PPSSPP")

    private fun manifest(context: Context): PPSSPPBundleManifest =
        PPSSPPBundleManifest(context.assets.open(PSP_MANIFEST_PATH), PSP_UPSTREAM_REVISION)

    fun isInstalled(context: Context): Boolean =
        runCatching {
            val directory = installedAssetDirectory(context)
            check(File(directory, REVISION_MARKER).readText() == PSP_UPSTREAM_REVISION)
            val manifest = manifest(context)
            manifest.verify("compat.sha256", File(directory, "compat.ini"))
            manifest.verify(
                "core.sha256",
                File(context.applicationInfo.nativeLibraryDir, "libppsspp_modern_libretro_android.so"),
            )
            true
        }.getOrDefault(false)

    suspend fun installIfBundled(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            if (isInstalled(context)) return@withContext true
            runCatching {
                val manifest = manifest(context)
                manifest.verify(
                    "core.sha256",
                    File(context.applicationInfo.nativeLibraryDir, "libppsspp_modern_libretro_android.so"),
                )
                // Reopen the immutable APK asset for extraction after streaming integrity verification.
                manifest.verify("assets.sha256", context.assets.open(PSP_ASSET_PATH))
                context.assets.open(PSP_ASSET_PATH).use { stream ->
                    ZipInputStream(stream).use { archive ->
                        PPSSPPAssetArchiveInstaller.install(archive, installedAssetDirectory(context)) { staging ->
                            manifest.verify("compat.sha256", File(staging, "compat.ini"))
                            File(staging, REVISION_MARKER).writeText(PSP_UPSTREAM_REVISION)
                        }
                    }
                }
                true
            }.getOrElse { error ->
                // Missing optional binaries/assets are normal in baseline builds. Failed candidates
                // stay unavailable; selecting the old engine always uses its original assets.
                Timber.w(error, "Matching upstream PPSSPP bundle unavailable; old engine remains usable")
                false
            }
        }
}
