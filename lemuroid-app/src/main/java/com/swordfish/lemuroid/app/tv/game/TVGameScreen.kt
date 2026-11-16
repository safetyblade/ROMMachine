package com.swordfish.lemuroid.app.tv.game

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.rememberDisplaySettings
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore

@Composable
fun TVGameScreen(viewModel: BaseGameScreenViewModel) {
    val localContext = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current

    val display = rememberDisplaySettings(viewModel.cartridgeId)
    val frame =
        if (display.effective.border(true) == DisplaySettingsStore.Border.STANDARD) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.outline)
        } else {
            Modifier
        }

    AndroidView(
        modifier = Modifier.fillMaxSize().then(frame),
        factory = {
            viewModel.createRetroView(localContext, lifecycle)
        },
    )

    val isLoading =
        viewModel.loadingState
            .collectAsState(true)
            .value

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}
