package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun CartridgePagedShelf(
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    onGameFavoriteToggle: ((Game, Boolean) -> Unit)? = null,
) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LazyRow(
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(games.itemCount, key = games.itemKey { it.id }) { index ->
                val game = games[index] ?: return@items
                Column {
                    CartridgeSpine(
                        game,
                        onClick = { onGameClick(game) },
                        onLongClick = { onGameLongClick(game) },
                    )
                    if (onGameFavoriteToggle != null) {
                        Box(Modifier.size(48.dp)) {
                            FavoriteToggle(
                                isToggled = game.isFavorite,
                                onFavoriteToggle = { onGameFavoriteToggle(game, it) },
                            )
                        }
                    }
                }
            }
        }
    }
}
