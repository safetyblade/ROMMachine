package com.swordfish.lemuroid.app.mobile.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.gemrating.GemRatingLocalStore
import com.swordfish.lemuroid.app.gemrating.GemRatingRepository
import com.swordfish.lemuroid.app.gemrating.GemShelfPresentation
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CatalogueCartridgeFront
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** My Ratings does not depend on the catalogue loading or a cartridge staying installed. */
@Composable
internal fun GemMyRatingsScreen(
    games: List<Game>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var records by remember { mutableStateOf<List<GemRatingRepository.Rating>?>(null) }
    val revision = GemShelfPresentation.revision.collectAsState().value
    val scope = rememberCoroutineScope()
    var overlay by remember { mutableStateOf(false) }
    var savingOverlay by remember { mutableStateOf(false) }
    var overlayError by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(context, revision) {
        overlay =
            withContext(Dispatchers.IO) {
                runCatching { GemShelfPresentation.enabled(context) }.getOrDefault(false)
            }
        val result =
            withContext(Dispatchers.IO) {
                runCatching {
                    val local = GemRatingLocalStore.open(context)
                    local.repository.snapshot(local.session).records.filter { it.score != null || it.deckFit != null }
                        .sortedByDescending { it.updatedAt }
                }
            }
        if (result.isSuccess) {
            records = result.getOrNull()
            error = false
        } else {
            error = true
        }
    }

    val installed = remember(games) { games.associateBy { it.cartridgeId } }
    val ratings = records
    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("My Ratings", style = MaterialTheme.typography.titleLarge)
                CartdeckMenuRow(
                    title = { Text("Show ratings on shelf") },
                    supporting = { Text(if (overlay) "On" else "Off") },
                    enabled = !savingOverlay,
                    onClick = {
                        savingOverlay = true
                        scope.launch {
                            val saved =
                                withContext(Dispatchers.IO) {
                                    runCatching {
                                        GemShelfPresentation.setEnabled(context, !overlay)
                                    }.getOrDefault(false)
                                }
                            overlayError = !saved
                            if (saved) overlay = !overlay
                            savingOverlay = false
                        }
                    },
                )
                if (overlayError) Text("Could not change shelf display preference.")
                if (error) {
                    Text("Ratings unavailable. Existing data retained.", color = MaterialTheme.colorScheme.error)
                } else if (ratings == null) {
                    Text("Loading ratings…")
                } else if (ratings.isEmpty()) {
                    Text("No ratings yet. Rate a game from its cartridge details.")
                }
            }
        }
        if (!error && ratings != null) {
            items(ratings, key = { it.cartridgeId }) { rating ->
                val game = installed[rating.cartridgeId]
                val title = game?.displayName() ?: rating.displayTitle ?: "Unidentified cartridge"
                val platform = game?.systemId ?: rating.platformId
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (game != null) {
                            CartridgeFront(game, modifier = Modifier.width(88.dp), offline = true)
                        } else {
                            CatalogueCartridgeFront(title, platform ?: "Unknown", Modifier.width(88.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                listOfNotNull(platform, if (game == null) "Not installed" else "On my shelf")
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            GemRatingRow(
                                cartridgeId = rating.cartridgeId,
                                displayTitle = title,
                                platformId = platform,
                            )
                            GemRatingRow(
                                cartridgeId = rating.cartridgeId,
                                displayTitle = title,
                                platformId = platform,
                                deckFit = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
