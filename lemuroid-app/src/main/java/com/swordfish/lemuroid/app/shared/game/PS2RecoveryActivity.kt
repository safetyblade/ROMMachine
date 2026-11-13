package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties

/** Lives in the shelf process. Native signals in :game cannot kill this recovery UI. */
class PS2RecoveryActivity : ComponentActivity() {
    private val journal by lazy { PS2LaunchJournal(applicationContext) }
    private var completed = false
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
            if (runCatching { journal.begin() }.isFailure) {
                showFailure("JOURNAL_UNAVAILABLE")
                return
            }
            if (runCatching { startActivityForResult(target, REQUEST_ENGINE) }.isFailure) {
                showFailure("ACTIVITY_START_FAILED")
                return
            }
        }
        lifecycleScope.launch {
            var activeStage = ""
            var activeMillis = 0L
            var lastPoll = android.os.SystemClock.elapsedRealtime()
            while (!completed) {
                delay(1_000)
                val record = journal.read()
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
        val record = journal.read()
        val stage = record.getProperty("stage", "REQUESTED")
        (intent.getParcelableExtra<Intent>(EXTRA_TARGET)?.getSerializableExtra(BaseGameActivity.EXTRA_GAME) as? Game)
            ?.let { runCatching { EngineSelectionPreferences(applicationContext).recordFailure(it, code) } }
        // Bring the main-process recovery screen above a stalled engine activity.
        runCatching {
            startActivity(Intent(this, PS2RecoveryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        AlertDialog.Builder(this)
            .setTitle(if (stage == "CORE_FRAME") "PS2 engine stopped" else "PS2 did not launch")
            .setMessage(
                "Stopped after $stage. Your cartridge and saves are retained. Copy the diagnostic report for support.",
            )
            .setNeutralButton("Copy report") { _, _ ->
                lifecycleScope.launch {
                    val report = withContext(Dispatchers.IO) { buildReport(code) }
                    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("CARTDECK PS2 diagnostic", report))
                    returnToShelf(stage)
                }
            }
            .setPositiveButton("Return to shelf") { _, _ -> returnToShelf(stage) }
            .setOnCancelListener { returnToShelf(stage) }
            .show()
    }

    private fun buildReport(code: String): String {
        val record = journal.read()
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val started = record.getProperty("started", "0").toLongOrNull() ?: 0L
        val pid = record.getProperty("pid", "0").toIntOrNull() ?: 0
        val exitRecord =
            if (Build.VERSION.SDK_INT >= 30) {
                runCatching {
                    manager.getHistoricalProcessExitReasons(packageName, pid.coerceAtLeast(0), 16)
                        .filter {
                            it.processName == "$packageName:game" && it.timestamp >= started &&
                                (pid <= 0 || it.pid == pid)
                        }
                        .maxByOrNull { it.timestamp }
                }.getOrNull()
            } else {
                null
            }
        val exit =
            if (Build.VERSION.SDK_INT >= 30) {
                exitRecord?.let {
                    "reason=${it.reason}; status=${it.status}; timestamp=${it.timestamp}"
                }
            } else {
                null
            }
        val nativeTrace =
            if (Build.VERSION.SDK_INT >= 31 &&
                exitRecord?.reason == ApplicationExitInfo.REASON_CRASH_NATIVE
            ) {
                runCatching { NativeCrashTrace.read(exitRecord.traceInputStream) }
                    .getOrDefault("Android native trace unavailable. Use the documented ADB capture.")
            } else {
                "No matching native tombstone (native traces require Android 12+)."
            }
        val provenance =
            runCatching {
                assets.open("cartdeck/play-modern.properties").use { input -> Properties().apply { load(input) } }
            }.getOrDefault(Properties())
        val revision = provenance.getProperty("revision", provenance.getProperty("upstream.revision", "unavailable"))
        val checksum = provenance.getProperty("core.sha256", "unavailable")
        val stage = record.getProperty("stage", "REQUESTED")
        val error = record.getProperty("error", "none")
        val abis = Build.SUPPORTED_ABIS.joinToString()
        val gles = manager.deviceConfigurationInfo.reqGlEsVersion.toString(16)
        val host = com.swordfish.lemuroid.lib.BuildConfig.NATIVE_HOST_VERSION
        val selectedVersion = record.getProperty("selectedVersion", "unavailable")
        val selectedSHA = record.getProperty("selectedSHA256", "unavailable")
        val glVendor = record.getProperty("glVendor", "unavailable before frontend draw")
        val glRenderer = record.getProperty("glRenderer", "unavailable before frontend draw")
        val glVersion = record.getProperty("glVersion", "unavailable before frontend draw")
        val report =
            listOf(
                "CARTDECK PS2 launch diagnostic",
                "app=${BuildConfig.VERSION_NAME}; versionCode=${BuildConfig.VERSION_CODE}",
                "engine=Play!; host=$host; bundledEngineRevision=$revision",
                "bundledEngineSHA256=$checksum",
                "selectedEngineVersion=$selectedVersion; selectedEngineSHA256=$selectedSHA",
                "device=${Build.MANUFACTURER} ${Build.MODEL}; sdk=${Build.VERSION.SDK_INT}; abi=$abis",
                "advertisedGLES=$gles; lastVerifiedStage=$stage",
                "GL_VENDOR=$glVendor; GL_RENDERER=$glRenderer; GL_VERSION=$glVersion",
                "failure=$code; containedError=$error",
                "androidProcessExit=${exit ?: "not available"}",
                nativeTrace,
                "Core frame submission alone does not prove gameplay, sound, input or saves.",
            ).joinToString("\n")
        runCatching {
            val target = java.io.File(filesDir, "native/ps2/last-launch-report.txt")
            target.parentFile?.mkdirs()
            val pending = java.io.File(target.parentFile, "last-launch-report.pending")
            pending.writeText(report)
            check(pending.renameTo(target))
        }
        return report
    }

    private fun returnToShelf(stage: String) {
        setResult(
            BaseGameActivity.RESULT_ERROR,
            Intent().putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR, "PS2 stopped after $stage. Saves retained."),
        )
        finish()
    }

    companion object {
        const val EXTRA_TARGET = "PS2_TARGET"
        private const val REQUEST_ENGINE = 1432
    }
}
