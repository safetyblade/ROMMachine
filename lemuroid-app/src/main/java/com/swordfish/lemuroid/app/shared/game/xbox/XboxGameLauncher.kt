package com.swordfish.lemuroid.app.shared.game.xbox

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

object XboxGameLauncher {
    suspend fun launch(
        activity: Activity,
        game: Game,
        leanback: Boolean,
    ): Boolean {
        val failure =
            withContext(Dispatchers.IO) {
                runCatching {
                    check(Build.VERSION.SDK_INT >= 28) { "Xbox requires Android 9 or newer" }
                    check("arm64-v8a" in Build.SUPPORTED_ABIS) { "Xbox requires an ARM64 Android device" }
                    check(
                        activity.packageManager.hasSystemFeature(
                            PackageManager.FEATURE_VULKAN_HARDWARE_VERSION,
                            0x00401000,
                        ),
                    ) {
                        "Xbox requires a Vulkan 1.1 or newer graphics driver"
                    }
                    val memory = android.app.ActivityManager.MemoryInfo()
                    (activity.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                        .getMemoryInfo(memory)
                    check(
                        !memory.lowMemory,
                    ) { "Android reports low available memory; close other apps before starting Xbox" }
                    java.io.RandomAccessFile(File(activity.filesDir, "game_process.lock"), "rw").channel.use {
                            channel ->
                        val lock = channel.tryLock() ?: error("Another CARTDECK game is already running")
                        try {
                            val props =
                                Properties().apply {
                                    activity.assets.open(
                                        "cartdeck/hakux.properties",
                                    ).use { load(it) }
                                }
                            val store = XboxEngineStore(File(activity.filesDir, "native-engines/xbox"))
                            val selection =
                                store.install(
                                    File(activity.applicationInfo.nativeLibraryDir),
                                    props,
                                    activity.assets.open("cartdeck/hakux/hakux-licenses.zip").use { it.readBytes() },
                                )
                            XboxStorage.prepare(activity.filesDir, selection.id)
                        } finally {
                            lock.release()
                        }
                    }
                    val uri = android.net.Uri.parse(game.fileUri)
                    activity.contentResolver.openFileDescriptor(uri, "r")?.use {
                        val position =
                            android.system.Os.lseek(it.fileDescriptor, 0, android.system.OsConstants.SEEK_SET)
                        check(position == 0L) {
                            "Xbox disc storage must support random access"
                        }
                    } ?: error("Xbox disc is no longer accessible; refresh its ROM folder permission")
                }.exceptionOrNull()
            }
        return withContext(Dispatchers.Main) {
            if (failure != null) {
                Toast.makeText(activity, failure.message ?: "Xbox could not start", Toast.LENGTH_LONG).show()
                false
            } else {
                activity.startActivityForResult(
                    Intent(activity, com.rfandango.haku_x.NativeCoreActivity::class.java)
                        .putExtra("cartdeck.gameUri", game.fileUri)
                        .putExtra("cartdeck.gameTitle", game.displayName())
                        .putExtra("cartdeck.game", game)
                        .putExtra("cartdeck.leanback", leanback)
                        .putExtra("cartdeck.startTime", android.os.SystemClock.elapsedRealtime()),
                    com.swordfish.lemuroid.app.shared.game.BaseGameActivity.REQUEST_PLAY_GAME,
                )
                true
            }
        }
    }
}
