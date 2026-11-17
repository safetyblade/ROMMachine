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
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Settings and cartridge entry points use the same editor, tester and store as native hosts. */
class ControllerSettingsActivity : ComponentActivity() {
    private lateinit var session: NativeControllerMappingSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        val game = intent.getSerializableExtra("cartdeck.game") as? Game
        val engine = game?.let { runCatching { EngineSelectionPreferences(this).select(it, null).engine }.getOrNull() }
        val config = engine?.config ?: game?.let { GameSystem.findById(it.systemId).systemCoreConfigs.firstOrNull() }
        val controller = config?.controllerConfigs?.get(0)?.firstOrNull()?.touchControllerID
        val capacity =
            controllerPortCapacity(
                engine?.host,
                config?.controllerConfigs?.keys.orEmpty(),
                game != null,
            )
        session =
            NativeControllerMappingSession(
                this, lifecycleScope, controller, game?.cartridgeId,
                runtime = false, portCapacity = capacity,
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
        if (::session.isInitialized) {
            session.observeMotion(event)
            if (event.isFromSource(android.view.InputDevice.SOURCE_JOYSTICK)) return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}

internal fun controllerPortCapacity(
    host: NativeEngineRegistry.Host?,
    declared: Set<Int>,
    hasGame: Boolean,
): Int =
    when (host) {
        NativeEngineRegistry.Host.ARMSX3_ANDROID, NativeEngineRegistry.Host.RPCS3_LIBRETRO -> 7
        NativeEngineRegistry.Host.LIBRETRO -> ((declared.maxOrNull() ?: 0) + 1).coerceIn(1, 16)
        null -> if (hasGame) ((declared.maxOrNull() ?: 0) + 1).coerceIn(1, 16) else 16
        else -> 1 // Current native JNI adapters expose a single attached gameplay pad.
    }
