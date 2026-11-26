package com.swordfish.lemuroid.app.shared.game.ps2next

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
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
        active =
            AlertDialog.Builder(activity)
                .setTitle("Welcome to PlayStation 2")
                .setMessage(
                    "Enhanced Compatibility uses a BIOS from your own PlayStation 2.\n\n" +
                        "Quick Play starts without extra files. Some games may not work.\n\n" +
                        "You can change setup in Settings.",
                )
                .setNeutralButton("BIOS setup") { _, _ -> showSetupActions(activity, enhancedAvailable) }
                .setPositiveButton("Continue without BIOS") { _, _ ->
                    prefs.edit().putBoolean("seen", true).apply()
                    continueLaunch()
                }
                .setNegativeButton("Return to shelf", null)
                .create().also { dialog ->
                    dialog.setOnDismissListener { if (active === dialog) active = null }
                    dialog.show()
                }
        return true
    }

    private fun showSetupActions(
        activity: Activity,
        enhancedAvailable: Boolean,
    ) {
        val options = linkedMapOf<String, () -> Unit>()
        if (enhancedAvailable) options["Add PS2 BIOS"] = { openImport(activity) }
        options["Open BIOS folder"] = { openFolder(activity) }
        options["Learn about BIOS setup"] = { showHelp(activity, enhancedAvailable) }
        AlertDialog.Builder(activity).setTitle("Enhanced Compatibility")
            .setItems(options.keys.toTypedArray()) { _, index -> options.values.elementAt(index).invoke() }
            .setNegativeButton("Close", null)
            .show()
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
                    builder.show()
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
        builder.show()
    }
}
