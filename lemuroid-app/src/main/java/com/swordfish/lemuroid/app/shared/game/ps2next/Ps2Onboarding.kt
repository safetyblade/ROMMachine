package com.swordfish.lemuroid.app.shared.game.ps2next

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.support.SupportDesign
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game

internal object Ps2Onboarding {
    private var active: AlertDialog? = null

    fun showIfNeeded(
        activity: Activity,
        game: Game,
        engines: EngineSelectionPreferences,
        continueLaunch: () -> Unit,
    ): Boolean {
        val prefs = activity.getSharedPreferences("ps2-onboarding", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seen", false)) return false
        if (Armsx2Storage.hasUserBios(activity.filesDir) || engines.hasLaunchHistory(game) ||
            engines.preferred(SystemID.PLAYSTATION_2, game) != null
        ) {
            prefs.edit().putBoolean("seen", true).apply()
            return false
        }
        if (activity.isFinishing || activity.isDestroyed || active?.isShowing == true) return true
        val enhancedAvailable = Armsx2Engine.engineeringBuild
        val view = activity.layoutInflater.inflate(R.layout.ps2_first_launch, null)
        val dialog = AlertDialog.Builder(activity).setView(view).create()
        view.findViewById<Button>(R.id.addPs2Bios).apply {
            isVisible = enhancedAvailable
            setOnClickListener {
                dialog.dismiss()
                openImport(activity)
            }
        }
        view.findViewById<Button>(R.id.openPs2BiosFolder).setOnClickListener { openFolder(activity) }
        view.findViewById<Button>(R.id.learnPs2Bios).setOnClickListener { showHelp(activity, enhancedAvailable) }
        view.findViewById<Button>(R.id.continuePs2WithoutBios).setOnClickListener {
            prefs.edit().putBoolean("seen", true).apply()
            dialog.dismiss()
            continueLaunch()
        }
        view.findViewById<Button>(R.id.cancelPs2Setup).setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener { if (active === dialog) active = null }
        active = dialog
        dialog.show()
        SupportDesign.style(dialog)
        listOf(
            R.id.addPs2Bios,
            R.id.openPs2BiosFolder,
            R.id.learnPs2Bios,
            R.id.continuePs2WithoutBios,
            R.id.cancelPs2Setup,
        ).forEach { id ->
            SupportDesign.style(
                view.findViewById(id),
                primary = id == if (enhancedAvailable) R.id.addPs2Bios else R.id.continuePs2WithoutBios,
            )
        }
        return true
    }

    private fun openImport(activity: Activity) {
        runCatching { activity.startActivity(Armsx2BiosRecovery.fromSettings(activity)) }
            .onFailure { Toast.makeText(activity, "PS2 setup is unavailable on this build.", Toast.LENGTH_LONG).show() }
    }

    fun openFolder(context: Context) {
        val task = java.util.concurrent.Executors.newSingleThreadExecutor()
        task.execute {
            val folder = runCatching { Ps2BiosFolder.find(context, create = true) }.getOrNull()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val opened =
                    folder != null &&
                        runCatching {
                            val target =
                                Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(folder, DocumentsContract.Document.MIME_TYPE_DIR)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(target)
                        }.isSuccess
                if (!opened) {
                    val builder =
                        AlertDialog.Builder(context)
                            .setTitle("PS2 BIOS folder")
                            .setMessage(
                                "Open the Android file picker and choose the BIOS file from your own PlayStation 2.",
                            )
                            .setNegativeButton("Close", null)
                    if (Armsx2Engine.engineeringBuild) {
                        builder.setPositiveButton("Open file picker") { _, _ ->
                            runCatching { context.startActivity(Armsx2BiosRecovery.fromSettings(context)) }
                        }
                    }
                    builder.show().also(SupportDesign::style)
                }
            }
        }
        task.shutdown()
    }

    private fun showHelp(
        activity: Activity,
        enhancedAvailable: Boolean,
    ) {
        val builder =
            AlertDialog.Builder(activity)
                .setTitle("About PS2 BIOS setup")
                .setMessage(
                    "Use a BIOS backup made from your own PlayStation 2. " +
                        "CARTDECK imports a copy and keeps your original file intact.",
                )
                .setNegativeButton("Close", null)
        if (enhancedAvailable) builder.setPositiveButton("Add PS2 BIOS") { _, _ -> openImport(activity) }
        builder.show().also(SupportDesign::style)
    }
}
