package com.swordfish.lemuroid.app.shared.game.xbox360

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

internal object Xbox360GameLauncher {
    suspend fun launch(
        activity: Activity,
        game: Game,
        leanback: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            check(Build.VERSION.SDK_INT >= 29 && "arm64-v8a" in Build.SUPPORTED_ABIS) {
                "Xbox 360 requires Android 10 or newer and ARM64"
            }
            check(
                activity.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x00401000),
            ) {
                "Xbox 360 requires a system Vulkan 1.1 driver; independentBlend is checked by the native engine"
            }
            val memory = android.app.ActivityManager.MemoryInfo()
            (
                activity.getSystemService(
                    android.content.Context.ACTIVITY_SERVICE,
                ) as android.app.ActivityManager
            ).getMemoryInfo(memory)
            check(!memory.lowMemory) { "Android reports low memory; close other apps before launching Xbox 360" }
            check(!GameProcessLock.isHeldByAnotherProcess(activity)) { "Another CARTDECK game is already running" }
            val props =
                runCatching { Properties().apply { activity.assets.open(Xbox360Engine.MANIFEST).use { load(it) } } }
                    .getOrElse {
                        error(
                            "This CARTDECK build does not include a verified Xbox 360 engine. " +
                                "Your cartridge and saves are retained.",
                        )
                    }
            // Lock across provisioning to prevent replacing a selected engine under a running guest.
            java.io.RandomAccessFile(File(activity.filesDir, "game_process.lock"), "rw").channel.use { channel ->
                val lock = channel.tryLock() ?: error("Another CARTDECK game is already running")
                try {
                    Xbox360Engine.store(activity.filesDir).install(
                        File(activity.applicationInfo.nativeLibraryDir),
                        props,
                        activity.assets.open("cartdeck/xenia/xenia-licenses.zip").use { it.readBytes() },
                    )
                    Xbox360Engine.data(activity.filesDir)
                } finally {
                    lock.release()
                }
            }
        }
        activity.startActivityForResult(
            Intent(activity, Xbox360Activity::class.java).putExtra("cartdeck.game", game)
                .putExtra("cartdeck.gameUri", game.fileUri).putExtra("cartdeck.gameTitle", game.displayName())
                .putExtra(
                    "cartdeck.leanback",
                    leanback,
                ).putExtra("cartdeck.startTime", android.os.SystemClock.elapsedRealtime()),
            BaseGameActivity.REQUEST_PLAY_GAME,
        )
    }
}
