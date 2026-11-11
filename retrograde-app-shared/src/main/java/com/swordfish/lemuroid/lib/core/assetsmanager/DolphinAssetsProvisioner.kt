package com.swordfish.lemuroid.lib.core.assetsmanager

import android.content.Context
import java.io.File

/** Revision-matched Dolphin Sys files; installation never touches User memory cards. */
object DolphinAssetsProvisioner {
    const val SOURCE_REVISION = DolphinAssetBundleInstaller.SOURCE_REVISION

    /** Caller must run on IO dispatcher. Returns the Libretro system directory. */
    fun install(
        context: Context,
        systemDirectory: File,
        selectedCore: File,
    ): File =
        VersionedDolphinAssets.installAndSelect(
            context.assets.open("cartdeck/dolphin-modern.properties"),
            File(context.applicationInfo.nativeLibraryDir, "libdolphin_modern_libretro_android.so"),
            selectedCore,
            { context.assets.open("cartdeck/dolphin-modern-assets.zip") },
            systemDirectory,
        )
}
