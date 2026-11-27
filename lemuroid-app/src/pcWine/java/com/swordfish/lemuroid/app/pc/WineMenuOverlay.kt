package com.swordfish.lemuroid.app.pc

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuAction
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuCapabilities
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuPanel
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuTheme
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.SharedGameMenuHome
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.playerMenuTitle
import com.swordfish.lemuroid.app.shared.support.SupportActivity
import com.swordfish.lemuroid.app.shared.support.SupportReport
import com.swordfish.lemuroid.app.shared.support.SupportTransport

object WineMenuOverlay {
    @JvmStatic
    fun create(
        context: Context,
        title: String,
        resume: Runnable,
        keyboard: Runnable,
        exit: Runnable,
    ): ComposeView =
        ComposeView(context).apply {
            fun report() =
                SupportReport.create(
                    "gameplay",
                    "Game problem",
                    null,
                    gameTitle = playerMenuTitle(title),
                    system = "PC",
                    engine = "Wine",
                )

            setContent {
                GameMenuTheme {
                    var page by remember { mutableStateOf("Home") }
                    GameMenuPanel(
                        title = title,
                        console = "PC · Wine",
                        page = page,
                        canGoBack = page != "Home",
                        onBack = { if (page == "Home") resume.run() else page = "Home" },
                        onResume = { resume.run() },
                        statusMessage = "Guest may continue running. Save before exiting.",
                    ) {
                        when (page) {
                            "Controls" -> {
                                GameMenuLink("Send keyboard text") { keyboard.run() }
                                Text("Touch and physical mouse/keyboard use the Wine Android driver.")
                            }
                            "Save & Load" ->
                                Text(
                                    "Use the game's own save system. " +
                                        "Its Wine prefix and working directory are retained.",
                                )
                            "Advanced" -> {
                                GameMenuLink("Copy diagnostic report") {
                                    SupportTransport.copy(context, report())
                                    android.widget.Toast.makeText(
                                        context,
                                        "Diagnostic report copied.",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                Text(
                                    "Native ARM64 Wine and experimental PE32 translation. " +
                                        "Audio and gameplay require verification.",
                                )
                            }
                            else ->
                                SharedGameMenuHome(
                                    GameMenuCapabilities(
                                        controls = true,
                                        states = true,
                                        settings = false,
                                        advanced = true,
                                        display = false,
                                        reporting = true,
                                    ),
                                ) { action ->
                                    when (action) {
                                        GameMenuAction.RESUME -> resume.run()
                                        GameMenuAction.RETURN_TO_SHELF -> exit.run()
                                        GameMenuAction.REPORT_PROBLEM ->
                                            context.startActivity(SupportActivity.intent(context, report()))
                                        else -> page = action.label
                                    }
                                }
                        }
                    }
                }
            }
        }
}
