package com.swordfish.lemuroid.app.shared.gamecrash

import android.app.Activity
import android.app.AlertDialog
import android.app.ApplicationExitInfo
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import com.swordfish.lemuroid.app.shared.game.ExternalGameLauncherActivity
import com.swordfish.lemuroid.app.shared.game.PlayFailureEvidence
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2BiosRecovery
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Storage
import com.swordfish.lemuroid.app.shared.support.SupportActivity
import com.swordfish.lemuroid.app.shared.support.SupportReport
import com.swordfish.lemuroid.app.shared.support.SupportReportStore
import com.swordfish.lemuroid.app.shared.support.SupportTransport
import com.swordfish.lemuroid.app.tv.main.MainTVActivity
import com.swordfish.lemuroid.app.tv.shared.TVHelper
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Properties

class GameCrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)
        findViewById<TextView>(R.id.text1).text = "That didn't go to plan."
        val detail = findViewById<TextView>(R.id.text2)
        val record = intent.getSerializableExtra(EXTRA_PLAY_RECORD) as? Properties
        val code = intent.getStringExtra(EXTRA_PLAY_CODE)
        val cartridge = intent.getSerializableExtra(EXTRA_GAME) as? Game
        val game = cartridge?.displayName()
        val context = applicationContext
        detail.text = intent.getStringExtra(EXTRA_MESSAGE) ?: playerMessage(game, code, null)

        val snapshot =
            (savedInstanceState?.getString(EXTRA_SNAPSHOT) ?: intent.getStringExtra(EXTRA_SNAPSHOT))
                ?.let { SupportReport.parse(it) }
        val details = intent.getStringExtra(EXTRA_MESSAGE_DETAIL)
        val report =
            evidenceScope.async {
                val exitReason = record?.let { PlayFailureEvidence.exitReason(context, it) }
                val value =
                    snapshot ?: SupportReport.create(
                        source = "failure recovery",
                        category =
                            if (exitReason == ApplicationExitInfo.REASON_ANR || code == "STARTUP_TIMEOUT") {
                                "Crash or freeze"
                            } else {
                                "Game stopped unexpectedly"
                            },
                        diagnostic =
                            if (record != null && code != null) {
                                runCatching { PlayFailureEvidence.build(context, record, code) }.getOrNull() ?: code
                            } else {
                                details
                            },
                        gameTitle = game,
                        system = if (record != null) "PS2" else null,
                        engine = if (record != null) "Play!" else null,
                        session = record?.getProperty("session"),
                    )
                runCatching { SupportReportStore(context).save(value) }
                if (record != null) {
                    record.getProperty("started")?.let { started ->
                        context.getSharedPreferences(
                            RECOVERY_PREFS,
                            MODE_PRIVATE,
                        ).edit().putString(RECOVERED_START, started).apply()
                    }
                }
                value to exitReason
            }
        lifecycleScope.launch {
            val (value, exitReason) = report.await()
            completedReport = value
            if (record != null) detail.text = playerMessage(game, code, exitReason)
        }

        val ps2 =
            cartridge?.takeIf {
                it.systemId == com.swordfish.lemuroid.lib.library.SystemID.PLAYSTATION_2.dbname
            }
        val failedPlay = record != null
        val alternative =
            ps2?.let { current ->
                NativeEngineRegistry.engines(GameSystem.findById(current.systemId)).firstOrNull {
                    val isEnhanced = it.host == NativeEngineRegistry.Host.ARMSX2_ANDROID
                    isEnhanced == failedPlay && NativeEngineRegistry.eligibility(this, it).eligible
                }
            }
        val biosMissing = failedPlay && alternative != null && !Armsx2Storage.hasUserBios(filesDir)
        val setup = if (biosMissing) Armsx2BiosRecovery.fromSettings(this) else null
        val recovery = Armsx2BiosRecovery.validate(this, intent.getParcelableExtra(EXTRA_BIOS_RECOVERY) ?: setup)
        findViewById<Button>(R.id.retryGame).apply {
            isVisible = ps2 != null && (failedPlay || recovery == null)
            setOnClickListener { ps2?.let { reopen(it, null) } }
        }
        findViewById<Button>(R.id.tryOtherEngine).apply {
            val label = if (failedPlay) "Try Enhanced Compatibility" else "Try Quick Play"
            text = label
            isVisible = alternative != null && !biosMissing
            setOnClickListener {
                if (ps2 != null && alternative != null) {
                    AlertDialog.Builder(this@GameCrashActivity)
                        .setTitle("$label?")
                        .setMessage("Saved progress may remain associated with the original engine.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton(label) { _, _ -> reopen(ps2, alternative.id) }
                        .show()
                }
            }
        }
        findViewById<Button>(R.id.importPs2Bios).apply {
            isVisible = recovery != null
            setOnClickListener {
                recovery?.let { target ->
                    runCatching { startActivityForResult(target, REQUEST_BIOS) }
                        .onFailure { message("PS2 setup is unavailable on this build.") }
                }
            }
        }
        findViewById<Button>(R.id.copyReport).setOnClickListener {
            lifecycleScope.launch {
                runCatching { SupportTransport.copy(this@GameCrashActivity, report.await().first) }
                    .onSuccess { message("Diagnostic report copied.") }
                    .onFailure { message("Could not copy the diagnostic report.") }
            }
        }
        findViewById<Button>(R.id.reportProblem).setOnClickListener {
            lifecycleScope.launch {
                startActivity(
                    SupportActivity.intent(this@GameCrashActivity, report.await().first),
                )
            }
        }
        findViewById<Button>(R.id.advancedDiagnostics).setOnClickListener {
            lifecycleScope.launch {
                AlertDialog.Builder(this@GameCrashActivity)
                    .setTitle("Advanced diagnostics")
                    .setMessage(report.await().first.text)
                    .setPositiveButton("Close", null)
                    .show()
            }
        }
        findViewById<Button>(R.id.returnToShelf).setOnClickListener { returnToShelf() }
        onBackPressedDispatcher.addCallback(this) { returnToShelf() }
    }

    private fun returnToShelf() {
        // A pinned shortcut can be the bottom activity of the task, with no existing shelf underneath it.
        val shelf = if (TVHelper.isTV(this)) MainTVActivity::class.java else MainActivity::class.java
        startActivity(Intent(this, shelf).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }

    private var completedReport: SupportReport? = null

    override fun onSaveInstanceState(outState: Bundle) {
        completedReport?.let { outState.putString(EXTRA_SNAPSHOT, it.json) }
        super.onSaveInstanceState(outState)
    }

    private fun reopen(
        game: Game,
        engine: String?,
    ) {
        val preferences = EngineSelectionPreferences(applicationContext)
        val selected =
            runCatching {
                if (engine == null) {
                    preferences.retrySelectedEngine(game)
                    true
                } else {
                    preferences.setPreferred(GameSystem.findById(game.systemId), engine, game)
                }
            }.getOrDefault(false)
        if (!selected) {
            message("This engine is unavailable. You can return to the shelf.")
            return
        }
        startActivity(
            Intent(this, ExternalGameLauncherActivity::class.java).apply {
                data = Uri.parse("lemuroid://$packageName/play-game/id/${game.id}")
                putExtra(ExternalGameLauncherActivity.EXTRA_FRESH_LAUNCH, true)
            },
        )
        finish()
    }

    private fun playerMessage(
        game: String?,
        code: String?,
        exitReason: Int?,
    ): String =
        buildString {
            game?.let { appendLine(it) }
            appendLine("PlayStation 2 · Quick Play")
            appendLine()
            append(
                when {
                    exitReason == ApplicationExitInfo.REASON_ANR || code == "STARTUP_TIMEOUT" ->
                        "The game stopped responding."
                    code == "ACTIVITY_START_FAILED" || code == "CONTAINED_LOAD_ERROR" ->
                        "The game could not start."
                    else -> "The game stopped unexpectedly."
                },
            )
        }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_BIOS && resultCode == RESULT_OK) {
            message("PS2 BIOS imported. Reopen your cartridge from the shelf.")
            finish()
        } else if (requestCode == REQUEST_BIOS && resultCode != RESULT_CANCELED) {
            message("The BIOS could not be imported. Check the file and try again.")
        }
    }

    companion object {
        private const val REQUEST_BIOS = 617
        const val EXTRA_BIOS_RECOVERY = "EXTRA_PS2_BIOS_RECOVERY"
        private const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        private const val EXTRA_MESSAGE_DETAIL = "EXTRA_MESSAGE_DETAIL"
        private const val EXTRA_PLAY_RECORD = "support.playRecord"
        private const val EXTRA_PLAY_CODE = "support.playCode"
        private const val EXTRA_GAME = "support.game"
        private const val EXTRA_SNAPSHOT = "support.snapshot"
        internal const val RECOVERY_PREFS = "support-recovery"
        internal const val RECOVERED_START = "recovered-play-start"
        private val evidenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun launch(
            activity: Activity,
            message: String,
            messageDetail: String?,
            biosRecovery: Intent? = null,
        ) {
            val intent =
                Intent(activity, GameCrashActivity::class.java).apply {
                    putExtra(EXTRA_MESSAGE, message)
                    putExtra(EXTRA_MESSAGE_DETAIL, messageDetail?.take(96_000))
                    putExtra(EXTRA_BIOS_RECOVERY, biosRecovery)
                }
            activity.startActivity(intent)
        }

        internal fun launchReport(
            activity: Activity,
            report: SupportReport,
            biosRecovery: Intent? = null,
            cartridge: Game? = null,
        ) {
            val game = report["game"].takeUnless { it == SupportReport.NOT_CAPTURED }
            val system = report["console"].takeUnless { it == SupportReport.NOT_CAPTURED }
            val message =
                buildString {
                    if (game != null) appendLine(game)
                    if (system != null) appendLine(system)
                    append(
                        if (biosRecovery == null) {
                            report["category"] + "."
                        } else {
                            "Add a PS2 BIOS to use Enhanced Compatibility."
                        },
                    )
                }
            activity.startActivity(
                Intent(activity, GameCrashActivity::class.java).apply {
                    putExtra(EXTRA_MESSAGE, message)
                    putExtra(EXTRA_SNAPSHOT, report.json)
                    putExtra(EXTRA_BIOS_RECOVERY, biosRecovery)
                    putExtra(EXTRA_GAME, cartridge)
                },
            )
        }

        internal fun launchPlayFailure(
            activity: Activity,
            record: Properties,
            code: String,
            game: Game?,
        ) {
            activity.startActivity(
                Intent(activity, GameCrashActivity::class.java).apply {
                    putExtra(EXTRA_PLAY_RECORD, record)
                    putExtra(EXTRA_PLAY_CODE, code)
                    putExtra(EXTRA_GAME, game)
                },
            )
        }
    }
}
