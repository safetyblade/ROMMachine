package com.swordfish.lemuroid.app.mobile.feature.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgePagedShelf
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidEmptyView
import com.swordfish.lemuroid.lib.library.db.dao.TitleSearch
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel,
    searchQuery: String,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
    onResetSearchQuery: () -> Unit,
) {
    val searchGames = viewModel.searchResults.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        onResetSearchQuery()
    }

    LaunchedEffect(key1 = searchQuery) {
        viewModel.queryString.value = searchQuery
    }

    val refresh = searchGames.loadState.refresh
    when {
        TitleSearch.tokens(searchQuery).isEmpty() ->
            SearchEmptyView(modifier, stringResource(R.string.game_page_search_suggestion))
        refresh is LoadState.Loading && searchGames.itemCount == 0 -> SearchLoadingView(modifier)
        refresh is LoadState.Error ->
            Column(modifier) {
                Text("Search could not be loaded.")
                TextButton(onClick = { searchGames.retry() }) { Text("Retry") }
            }
        searchGames.itemCount == 0 -> SearchEmptyView(modifier, stringResource(R.string.empty_view_default))
        else ->
            Box(modifier) {
                SearchResultsView(
                    Modifier.fillMaxSize(),
                    searchGames,
                    onGameClick,
                    onGameLongClick,
                    onGameFavoriteToggle,
                )
                if (searchGames.loadState.append is LoadState.Loading) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.BottomCenter),
                    )
                }
                if (searchGames.loadState.append is LoadState.Error) {
                    TextButton(
                        onClick = { searchGames.retry() },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) { Text("Retry more results") }
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultsView(
    modifier: Modifier,
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    CartridgePagedShelf(games, onGameClick, onGameLongClick, modifier, onGameFavoriteToggle)
}

@Composable
private fun SearchEmptyView(
    modifier: Modifier,
    text: String,
) {
    LemuroidEmptyView(
        modifier = modifier,
        text = text,
    )
}

@Composable
private fun SearchLoadingView(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
