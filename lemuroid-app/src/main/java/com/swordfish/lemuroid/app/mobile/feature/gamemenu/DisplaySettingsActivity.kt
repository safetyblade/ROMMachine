package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** TV settings entry uses the same model and focus/navigation contract as gameplay. */
class DisplaySettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameMenuTheme {
                GameMenuPanel(
                    title = "CARTDECK",
                    console = "Application defaults",
                    page = "Display",
                    canGoBack = true,
                    onBack = ::finish,
                    onResume = ::finish,
                ) { DisplaySettingsScreen() }
            }
        }
    }
}
