package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** TV settings entry uses the same model and focus/navigation contract as gameplay. */
class DisplaySettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val game = intent.getSerializableExtra("cartdeck.game") as? Game
        setContent {
            GameMenuTheme {
                GameMenuPanel(
                    title = game?.displayName() ?: "CARTDECK",
                    console = game?.systemId ?: "Application defaults",
                    page = "Display",
                    canGoBack = true,
                    onBack = ::finish,
                    onResume = ::finish,
                ) { DisplaySettingsScreen(game) }
            }
        }
    }
}
