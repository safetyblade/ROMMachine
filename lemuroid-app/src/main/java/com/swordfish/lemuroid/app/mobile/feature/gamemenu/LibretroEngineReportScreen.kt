package com.swordfish.lemuroid.app.mobile.feature.gamemenu

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
import com.swordfish.lemuroid.app.shared.game.LibretroEngineReport
import com.swordfish.lemuroid.app.shared.game.ps2next.Ps2DiscEvidence
import com.swordfish.lemuroid.app.shared.support.SupportActivity
import com.swordfish.lemuroid.app.shared.support.SupportReport
import com.swordfish.lemuroid.app.shared.support.SupportSignature
import com.swordfish.lemuroid.app.shared.support.SupportSignatureHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Explicit Advanced / Diagnostics destination. No report generation during gameplay. */
@Composable
internal fun LibretroEngineReportScreen(request: GameMenuActivity.GameMenuRequest) {
    val context = LocalContext.current
    var report by remember(request) { mutableStateOf<SupportReport?>(null) }
    LaunchedEffect(request) {
        report =
            withContext(Dispatchers.IO) {
                buildLibretroSupportReport(context, request)
            }
    }
    SupportSignature {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
            SupportSignatureHeader("Advanced diagnostics", "Review or copy the current engine report.")
            GameMenuLink("Copy diagnostic report", enabled = report != null) {
                report?.let { snapshot ->
                    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("CARTDECK diagnostic report", snapshot.text),
                    )
                    android.widget.Toast.makeText(
                        context,
                        "Diagnostic report copied.",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            GameMenuLink("Report a problem", enabled = report != null) {
                report?.let { context.startActivity(SupportActivity.intent(context, it, request.game)) }
            }
            Text(report?.text ?: "Preparing engine report…")
        }
    }
}

internal fun buildLibretroSupportReport(
    context: android.content.Context,
    request: GameMenuActivity.GameMenuRequest,
): SupportReport =
    SupportReport.create(
        source = "gameplay",
        category = "Game problem",
        diagnostic =
            runCatching {
                LibretroEngineReport.build(context, request.game, request.coreConfig, request.engineReportSession)
            }.getOrNull(),
        gameTitle = playerMenuTitle(request.game.customName ?: request.game.title),
        system = request.game.systemId,
        engine = request.engineReportSession?.let { request.coreConfig.coreID.coreName },
        serial = Ps2DiscEvidence.serial(context, request.game),
    )
