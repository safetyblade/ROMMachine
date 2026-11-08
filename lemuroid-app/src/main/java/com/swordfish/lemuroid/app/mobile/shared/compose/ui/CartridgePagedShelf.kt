package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfRail
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlin.math.floor

@Composable
fun CartridgePagedShelf(
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    modifier: Modifier = Modifier,
    onGameFavoriteToggle: ((Game, Boolean) -> Unit)? = null,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = maxOf(1, floor((maxWidth.value - 24f) / 72f).toInt())
        val width = ((maxWidth.value - 24f - 4f * (columns - 1)) / columns).dp.coerceAtLeast(48.dp)
        LazyColumn(contentPadding = PaddingValues(12.dp)) {
            items((games.itemCount + columns - 1) / columns, key = {
                    row ->
                games.peek(row * columns)?.id ?: -row - 1
            }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(columns) { column ->
                        val index = row * columns + column
                        if (index < games.itemCount) {
                            val game = games[index]
                            if (game != null) {
                                key(game.id) {
                                    Column {
                                        CartridgeSpine(
                                            game,
                                            { onGameClick(game) },
                                            onLongClick = { onGameLongClick(game) },
                                            width = width,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ShelfRail()
                if (onGameFavoriteToggle != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(columns) { column ->
                            val index = row * columns + column
                            val game = if (index < games.itemCount) games.peek(index) else null
                            Box(Modifier.width(width).height(48.dp)) {
                                if (game != null) FavoriteToggle(game.isFavorite) { onGameFavoriteToggle(game, it) }
                            }
                        }
                    }
                }
            }
        }
    }
}
