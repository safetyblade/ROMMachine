package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.gamecrash.GameCrashActivity
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lives in the shelf process. Native signals in :game cannot kill this recovery UI. */
class PS2RecoveryActivity : ComponentActivity() {
    private val journal by lazy { PS2LaunchJournal(applicationContext) }
    private var completed = false
    private var journalStarted: String? = null
    private val coreFrameSignalAvailable by lazy {
        runCatching {
            Class.forName(
                "com.swordfish.libretrodroid.GLRetroView\$GLRetroEvents\$CoreFrameRendered",
                false,
                classLoader,
            )
        }.isSuccess
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val target = intent.getParcelableExtra<Intent>(EXTRA_TARGET)
            if (target == null) {
                finish()
                return
            }
            journalStarted = runCatching { journal.begin() }.getOrNull()
            if (runCatching { startActivityForResult(target, REQUEST_ENGINE) }.isFailure) {
                showFailure("ACTIVITY_START_FAILED")
                return
            }
        }
        journalStarted = savedInstanceState?.getString("launchStarted") ?: journalStarted
        if (journalStarted == null) return
        lifecycleScope.launch {
            var activeStage = ""
            var activeMillis = 0L
            var lastPoll = android.os.SystemClock.elapsedRealtime()
            while (!completed) {
                delay(1_000)
                val record = currentRecord()
                val stage = record.getProperty("stage", "REQUESTED")
                if (PS2LaunchJournal.startupComplete(stage, coreFrameSignalAvailable)) return@launch
                val now = android.os.SystemClock.elapsedRealtime()
                val elapsed = (now - lastPoll).coerceIn(0L, 2000L)
                lastPoll = now
                if (activeStage != stage) {
                    activeStage = stage
                    activeMillis = 0L
                }
                val pid = record.getProperty("pid", "0").toIntOrNull() ?: 0
                val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val engine =
                    manager.runningAppProcesses.orEmpty().firstOrNull {
                        it.pid == pid && it.uid == Process.myUid() && it.processName == "$packageName:game"
                    }
                // Background time and device sleep must not turn a valid slow launch into a failure.
                if (engine != null && engine.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    activeMillis += elapsed
                }
                if (activeMillis > PS2LaunchJournal.startupBudgetMillis(stage)) {
                    // Only terminate the exact, still-running private engine process.
                    if (pid != Process.myPid() && engine != null) {
                        Process.killProcess(pid)
                    }
                    showFailure("STARTUP_TIMEOUT")
                }
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_ENGINE || completed) return
        if (resultCode == Activity.RESULT_OK) {
            completed = true
            setResult(resultCode, data)
            finish()
        } else {
            showFailure(if (resultCode == Activity.RESULT_CANCELED) "ENGINE_PROCESS_ENDED" else "CONTAINED_LOAD_ERROR")
        }
    }

    private fun showFailure(code: String) {
        if (completed || isFinishing) return
        completed = true
        val record = currentRecord()
        val game =
            intent.getParcelableExtra<Intent>(EXTRA_TARGET)
                ?.getSerializableExtra(BaseGameActivity.EXTRA_GAME) as? Game
        game?.let { runCatching { EngineSelectionPreferences(applicationContext).recordFailure(it, code) } }
        GameCrashActivity.launchPlayFailure(this, record, code, game)
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(RECOVERY_DISPLAYED, true))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("launchStarted", journalStarted)
        super.onSaveInstanceState(outState)
    }

    private fun currentRecord(): java.util.Properties =
        journal.read().takeIf { journalStarted != null && it.getProperty("started") == journalStarted }
            ?: java.util.Properties()

    companion object {
        const val EXTRA_TARGET = "PS2_TARGET"
        const val RECOVERY_DISPLAYED = "support.playRecoveryDisplayed"

        suspend fun restoreInterruptedLaunch(activity: Activity): Boolean {
            val context = activity.applicationContext
            val record =
                withContext(Dispatchers.IO) {
                    val last = PS2LaunchJournal(context).read()
                    val start = last.getProperty("started") ?: return@withContext null
                    val recovered =
                        context.getSharedPreferences(GameCrashActivity.RECOVERY_PREFS, Context.MODE_PRIVATE)
                            .getString(GameCrashActivity.RECOVERED_START, null)
                    if (recovered == start || last.getProperty("stage") == PS2LaunchJournal.Stage.CLEAN_EXIT.name ||
                        PlayFailureEvidence.exitReason(context, last) == null
                    ) {
                        null
                    } else {
                        last
                    }
                } ?: return false
            if (activity.isFinishing || activity.isDestroyed) return false
            GameCrashActivity.launchPlayFailure(activity, record, "ENGINE_PROCESS_ENDED", null)
            return true
        }

        private const val REQUEST_ENGINE = 1432
    }
}
