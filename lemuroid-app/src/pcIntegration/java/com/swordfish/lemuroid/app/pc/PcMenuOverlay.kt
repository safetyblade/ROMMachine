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

object PcMenuOverlay {
    @JvmStatic
    fun create(
        context: Context,
        title: String,
        resume: Runnable,
        keyboard: Runnable,
        specialKeys: Runnable,
        nextDisc: Runnable,
        launch: Runnable,
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
                    engine = "DOSBox Pure",
                )

            setContent {
                GameMenuTheme {
                    var page by remember { mutableStateOf("Home") }
                    GameMenuPanel(
                        title = title,
                        console = "PC • DOSBox Pure",
                        page = page,
                        canGoBack = page != "Home",
                        onBack = { if (page == "Home") resume.run() else page = "Home" },
                        onResume = { resume.run() },
                        statusMessage = "Guest may continue running. Save in Windows before exiting.",
                    ) {
                        when (page) {
                            "Controls" -> {
                                GameMenuLink("Keyboard") { keyboard.run() }
                                GameMenuLink("Special keys") { specialKeys.run() }
                                Text("Drag on the game display to move the mouse; use click controls.")
                            }
                            "Game Settings" -> {
                                GameMenuLink("Next optical disc") { nextDisc.run() }
                                GameMenuLink("Launch configured EXE") { launch.run() }
                            }
                            "Save & Load" ->
                                Text(
                                    "Progress is written to the private guest disk. " +
                                        "Save in game and shut down Windows before returning to Shelf. " +
                                        "Save states are not supported.",
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
                                    "Experimental isolated PC host. No console host substitutions. " +
                                        "Windows launch and shutdown require guest confirmation.",
                                )
                            }
                            else ->
                                SharedGameMenuHome(
                                    GameMenuCapabilities(
                                        controls = true,
                                        states = true,
                                        settings = true,
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
