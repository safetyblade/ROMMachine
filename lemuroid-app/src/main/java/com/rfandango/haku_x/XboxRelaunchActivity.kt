@file:Suppress("ktlint:standard:package-name") // Exact upstream JNI ABI namespace.

package com.rfandango.haku_x

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.app.shared.game.xbox.XboxStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** An Xbox title may launch a second XBE. Wait for its old native process, never loop after a crash. */
class XboxRelaunchActivity : androidx.activity.ComponentActivity() {
    private val preparation = com.swordfish.lemuroid.app.shared.game.LaunchPreparation()

    private fun cancelLaunch() {
        if (preparation.cancelling.value) return
        preparation.requestCancellation()
        handler.removeCallbacksAndMessages(null)
        scope.launch {
            preparation.awaitCleanup()
            finish()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var started = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val retry =
        object : Runnable {
            override fun run() {
                if (!GameProcessLock.isHeldByAnotherProcess(applicationContext)) {
                    preparation.start(scope) {
                        val failure =
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    java.io.RandomAccessFile(File(filesDir, "game_process.lock"), "rw").channel.use {
                                            channel ->
                                        val lock = channel.tryLock() ?: error("Another CARTDECK game is running")
                                        try {
                                            val selected =
                                                requireNotNull(
                                                    XboxEngineStore(File(filesDir, "native-engines/xbox")).selected(),
                                                )
                                            XboxStorage.prepare(filesDir, selected.id)
                                        } finally {
                                            lock.release()
                                        }
                                    }
                                }.exceptionOrNull()
                            }
                        if (preparation.cancelling.value || isFinishing) return@start
                        if (failure != null) {
                            AlertDialog.Builder(this@XboxRelaunchActivity).setTitle("CARTDECK · Xbox")
                                .setMessage(
                                    failure.message,
                                ).setPositiveButton("Return to shelf") { _, _ -> finish() }.show()
                        } else {
                            startActivity(
                                Intent(this@XboxRelaunchActivity, NativeCoreActivity::class.java)
                                    .putExtras(intent)
                                    .putExtra("cartdeck.relaunch", intent.getBooleanExtra("cartdeck.relaunch", true))
                                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT),
                            )
                            finish()
                        }
                    }
                } else if (SystemClock.uptimeMillis() - started < 8000) {
                    handler.postDelayed(this, 100)
                } else {
                    AlertDialog.Builder(this@XboxRelaunchActivity).setTitle("CARTDECK · Xbox")
                        .setMessage("The previous Xbox session has not stopped. Return to the shelf and try again.")
                        .setPositiveButton("Return to shelf") { _, _ -> finish() }.show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme {
                com.swordfish.lemuroid.app.shared.game.CartdeckLoadingScreen(
                    "Preparing Xbox cartridge",
                    ::cancelLaunch,
                    preparation.cancelling.collectAsState().value,
                )
            }
        }
        started = SystemClock.uptimeMillis()
        handler.postDelayed(retry, 200)
    }

    override fun onDestroy() {
        scope.cancel()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
