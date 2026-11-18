package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.CitraCompatibilityMode
import com.swordfish.lemuroid.app.shared.game.LibretroEngineReport
import com.swordfish.lemuroid.lib.library.CoreID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit Advanced / Diagnostics destination. No report generation during gameplay. */
@Composable
internal fun LibretroEngineReportScreen(request: GameMenuActivity.GameMenuRequest) {
    val context = LocalContext.current
    var report by remember(request) { mutableStateOf<String?>(null) }
    LaunchedEffect(request) {
        report =
            withContext(Dispatchers.IO) {
                LibretroEngineReport.build(context, request.game, request.coreConfig, request.engineReportSession)
            }
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
        GameMenuLink("Copy report", enabled = report != null) {
            report?.let { text ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("CARTDECK Engine Report", text))
            }
        }
        if (request.coreConfig.coreID == CoreID.CITRA) {
            var nextMode by remember(request.game.cartridgeId) {
                mutableStateOf(CitraCompatibilityMode.read(context, request.game))
            }
            Text("3DS compatibility test (next launch): ${nextMode.label}")
            Text("Choose one comparison, return to shelf, then reopen the game. Saves are unchanged.")
            CitraCompatibilityMode.entries.forEach { mode ->
                GameMenuLink(mode.label, enabled = nextMode != mode) {
                    CitraCompatibilityMode.write(context, request.game, mode)
                    nextMode = mode
                }
            }
        }
        Text(report ?: "Preparing engine report…")
    }
}
