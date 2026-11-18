package com.swordfish.lemuroid.app.mobile.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.gemrating.GemRatingLocalStore
import com.swordfish.lemuroid.app.gemrating.GemRatingRepository
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** My Ratings does not depend on the catalogue loading or a cartridge staying installed. */
@Composable
internal fun GemMyRatingsScreen(
    games: List<Game>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var records by remember { mutableStateOf<List<GemRatingRepository.Rating>?>(null) }
    var revision by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(context, revision) {
        val result =
            withContext(Dispatchers.IO) {
                runCatching {
                    val local = GemRatingLocalStore.open(context)
                    local.repository.snapshot(local.session).records.filter { it.score != null }
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
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("My Ratings", style = MaterialTheme.typography.titleLarge)
        if (error) {
            Text("Ratings unavailable. Existing data retained.", color = MaterialTheme.colorScheme.error)
        } else if (ratings == null) {
            Text("Loading ratings…")
        } else if (ratings.isEmpty()) {
            Text("No ratings yet. Rate a game from its cartridge details.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ratings, key = { it.cartridgeId }) { rating ->
                    val game = installed[rating.cartridgeId]
                    val title = game?.displayName() ?: rating.displayTitle ?: "Unidentified cartridge"
                    val platform = game?.systemId ?: rating.platformId
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                onRatingChanged = { revision++ },
                            )
                        }
                    }
                }
            }
        }
    }
}
