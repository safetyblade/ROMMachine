package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel

class GameActivity : BaseGameActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return
        val orientation =
            GripArcProfileStore(this).orientationOverride(game) ?: PivotArcSettingsStore(this).read().orientation
        val requested =
            when (orientation) {
                ControlsOrientation.AUTOMATIC -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                ControlsOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                ControlsOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        if (requestedOrientation != requested) requestedOrientation = requested
    }

    @Composable
    override fun GameScreen(viewModel: BaseGameScreenViewModel) {
        MobileGameScreen(viewModel)
    }

    override fun getDialogClass() = GameMenuActivity::class.java
}
