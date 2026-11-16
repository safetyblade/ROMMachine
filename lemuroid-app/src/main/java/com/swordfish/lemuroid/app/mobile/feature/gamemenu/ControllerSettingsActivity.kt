package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Settings and cartridge entry points use the same editor, tester and store as native hosts. */
class ControllerSettingsActivity : ComponentActivity() {
    private lateinit var session: NativeControllerMappingSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val game = intent.getSerializableExtra("cartdeck.game") as? Game
        val controller =
            game?.let {
                GameSystem.findById(it.systemId).systemCoreConfigs.firstOrNull()
                    ?.controllerConfigs?.get(0)?.firstOrNull()?.touchControllerID
            }
        session =
            NativeControllerMappingSession(
                this, lifecycleScope, controller, game?.cartridgeId,
                runtime = false, portCapacity = 16,
            ) { null }
        setContent {
            GameMenuTheme {
                GameMenuPanel(
                    title = game?.displayName() ?: "CARTDECK",
                    console = "Controller configuration",
                    page = "Controls",
                    canGoBack = true,
                    onBack = ::finish,
                    onResume = ::finish,
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { session.Panel() }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        session.start()
    }

    override fun onPause() {
        session.stop()
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::session.isInitialized && session.captureKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (::session.isInitialized) session.observeMotion(event)
        return super.dispatchGenericMotionEvent(event)
    }
}
