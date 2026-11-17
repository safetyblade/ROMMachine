package com.swordfish.lemuroid.app.tv.game

import android.os.Bundle
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.handle
import com.swordfish.lemuroid.app.shared.input.observeControllerMenuEdges
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.common.coroutines.safeCollect
import com.swordfish.lemuroid.common.displayToast
import kotlinx.coroutines.flow.filter

class TVGameActivity : BaseGameActivity() {
    override fun getDialogClass() = com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeControllerMenuEdges(this, menuEdge)
        initializeFlows()
    }

    @Composable
    override fun GameScreen(viewModel: BaseGameScreenViewModel) {
        TVGameScreen(viewModel)
    }

    private val menuEdge = ControllerMenuEdge()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (menuEdge.handle(event)) requestGameMenuFromHardware()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPause() {
        menuEdge.clear()
        super.onPause()
    }

    private fun initializeFlows() {
        launchOnState(Lifecycle.State.CREATED) {
            initializeShortcutToastFlow()
        }
    }

    private suspend fun initializeShortcutToastFlow() {
        inputDeviceManager
            .getEnabledInputsObservable()
            .filter { it.isEmpty() }
            .safeCollect {
                displayToast(R.string.tv_game_message_missing_gamepad)
            }
    }
}
