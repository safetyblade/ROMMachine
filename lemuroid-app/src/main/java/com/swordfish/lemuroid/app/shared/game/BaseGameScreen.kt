package com.swordfish.lemuroid.app.shared.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelRetroGameView

/**
 * The brand loader is confined to pre-game states; immediately yield the
 * viewport when a core is Loaded/Ready without adding transitions or delays.
 */
@Composable
fun BaseGameScreen(
    viewModel: BaseGameScreenViewModel,
    gameScreen: @Composable (BaseGameScreenViewModel) -> Unit,
) {
    val gameState =
        viewModel.getGameState()
            .collectAsState(GameViewModelRetroGameView.GameState.Uninitialized)
            .value

    when (gameState) {
        is GameViewModelRetroGameView.GameState.Loaded,
        GameViewModelRetroGameView.GameState.Ready,
        -> gameScreen(viewModel)
        else ->
            CartdeckLoadingScreen(
                message = (gameState as? GameViewModelRetroGameView.GameState.Loading)?.message,
            )
    }
}
