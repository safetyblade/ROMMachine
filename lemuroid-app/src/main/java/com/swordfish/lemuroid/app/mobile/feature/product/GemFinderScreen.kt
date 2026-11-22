package com.swordfish.lemuroid.app.mobile.feature.product

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.gemrating.GemCataloguePackage
import com.swordfish.lemuroid.app.gemrating.GemDeckFit
import com.swordfish.lemuroid.app.gemrating.GemDiscovery
import com.swordfish.lemuroid.app.gemrating.GemDiscoveryPreferences
import com.swordfish.lemuroid.app.gemrating.GemFeatureContributions
import com.swordfish.lemuroid.app.gemrating.GemLocalCorrections
import com.swordfish.lemuroid.app.gemrating.GemRatingLocalStore
import com.swordfish.lemuroid.app.gemrating.GemRatingRecommender
import com.swordfish.lemuroid.app.gemrating.GemShelfPresentation
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CatalogueCartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Offline discovery, on the current legacy Manager bridge until PROF-01 is integrated. */
@Composable
internal fun GemFinderScreen(
    catalogue: GemCataloguePackage.Loaded,
    games: List<Game>,
    modifier: Modifier = Modifier,
    onOpenCartridge: (Game) -> Unit = {},
    onInspectRelease: (String) -> Unit = {},
    onIdentifyCartridge: (Game) -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val ratingRevision = GemShelfPresentation.revision.collectAsState().value
    var input by remember(catalogue, games, ratingRevision) { mutableStateOf<GemDiscovery.Input?>(null) }
    var installedByRelease by remember(catalogue, games) { mutableStateOf(emptyMap<String, Game>()) }
    var preferences by remember { mutableStateOf<GemDiscoveryPreferences?>(null) }
    var failed by remember(catalogue, games) { mutableStateOf(false) }
    var scope by remember { mutableStateOf(GemDiscovery.Scope.FOR_YOU) }
    var share by remember { mutableStateOf(30) }
    var feed by remember { mutableStateOf<List<GemRatingRecommender.Recommendation>?>(null) }
    var surprising by remember { mutableStateOf(false) }
    var changingShare by remember { mutableStateOf(false) }
    var showPreferences by remember { mutableStateOf(false) }
    var surprise by remember { mutableStateOf<GemRatingRecommender.Recommendation?>(null) }
    var message by remember { mutableStateOf("") }

    fun changeShare(value: Int) {
        val prefs = preferences ?: return
        if (changingShare || surprising) return
        changingShare = true
        coroutineScope.launch {
            val saved = withContext(Dispatchers.IO) { prefs.saveWildcardPercent(value) }
            if (saved) share = value else message = "Wildcard preference could not be saved."
            changingShare = false
        }
    }

    LaunchedEffect(catalogue, games, ratingRevision) {
        val result =
            withContext(Dispatchers.IO) {
                runCatching {
                    val store = GemRatingLocalStore.open(context)
                    val prefs = GemDiscoveryPreferences(context, store.session)
                    val records = store.repository.snapshot(store.session).records
                    val corrections = GemLocalCorrections(context)
                    val ids = (games.map { it.cartridgeId } + records.map { it.cartridgeId }).distinct()
                    val links = ids.mapNotNull { id -> corrections.read(id)?.let { id to it } }.toMap()
                    val shelf = games.map { GemDiscovery.ShelfEntry(it.cartridgeId, it.systemId, it.title) }
                    // Small source-owned gameplay feature pack; no catalogue/Room migration.
                    val contributions =
                        context.assets.open("gem/features/v1.json").use {
                            GemFeatureContributions.load(it, catalogue.manifest.releases)
                        }
                    val prepared =
                        GemDiscovery.prepare(
                            catalogue.manifest.releases,
                            records,
                            links,
                            shelf,
                            contributions.accepted,
                        )
                    val installed =
                        games.mapNotNull { game ->
                            links[game.cartridgeId]?.takeIf {
                                it in prepared.ownedIds && catalogue.index.find(it)?.systemId == game.systemId
                            }?.let { it to game }
                        }.groupBy({ it.first }, { it.second }).mapNotNull { (id, matches) ->
                            matches.singleOrNull()?.let { id to it }
                        }.toMap()
                    Triple(prepared, prefs to prefs.wildcardPercent(), installed)
                }
            }
        result.onSuccess {
            input = it.first
            preferences = it.second.first
            installedByRelease = it.third
            share = it.second.second
        }.onFailure { failed = true }
    }

    LaunchedEffect(input, scope, share) {
        val data = input ?: return@LaunchedEffect
        feed = null
        surprise = null
        message = ""
        val result =
            withContext(Dispatchers.Default) {
                runCatching {
                    GemRatingRecommender.recommend(
                        data.catalogue,
                        data.ratings,
                        limit = 20,
                        wildcardShare = share / 100.0,
                        candidateIds = data.candidates(scope),
                    )
                }
            }
        result.onSuccess { feed = it }.onFailure { failed = true }
    }

    val data = input
    val currentFeed = feed
    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.cartdeck_feature_gemfinder),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Text("GemFinder", style = MaterialTheme.typography.titleLarge)
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val scopes =
                        listOf(
                            GemDiscovery.Scope.FOR_YOU to "For you",
                            GemDiscovery.Scope.MY_SHELF to "My shelf",
                            GemDiscovery.Scope.DISCOVER to "Discover",
                        )
                    for ((value, label) in scopes) {
                        FilterChip(
                            selected = scope == value,
                            onClick = { scope = value },
                            enabled = !surprising,
                            modifier = Modifier.gemFocusTarget(),
                            label = { Text(label) },
                        )
                    }
                }
                if (failed) {
                    Text(
                        "Discovery unavailable. Existing ratings are retained.",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else if (data == null) {
                    Text("Loading personal preferences…")
                } else {
                    if (data.ratings.isEmpty()) {
                        Text("Build your taste profile", style = MaterialTheme.typography.titleMedium)
                        Text("Pick a cartridge, rate it and identify the game to unlock personal picks.")
                    } else if (data.ratings.size < 3) {
                        Text("Early picks for you", style = MaterialTheme.typography.bodySmall)
                    }
                    if (data.ratings.isNotEmpty()) {
                        TextButton(onClick = { showPreferences = !showPreferences }) {
                            Text("Wildcards · $share%")
                        }
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (showPreferences) {
                                TextButton(
                                    onClick = { changeShare(share - 5) },
                                    enabled = share > 20 && !surprising && !changingShare,
                                    modifier = Modifier.gemFocusTarget(),
                                ) { Text("Less") }
                                TextButton(
                                    onClick = { changeShare(share + 5) },
                                    enabled = share < 50 && !surprising && !changingShare,
                                    modifier = Modifier.gemFocusTarget(),
                                ) { Text("More") }
                            }
                            TextButton(
                                onClick = {
                                    val prefs = preferences ?: return@TextButton
                                    val candidates = data.candidates(scope)
                                    surprising = true
                                    surprise = null
                                    message = ""
                                    coroutineScope.launch {
                                        val result =
                                            runCatching {
                                                val recent = withContext(Dispatchers.IO) { prefs.recentIds().toSet() }
                                                val pick =
                                                    withContext(Dispatchers.Default) {
                                                        GemRatingRecommender.surprise(
                                                            data.catalogue,
                                                            data.ratings,
                                                            recent,
                                                            candidates,
                                                        )
                                                    }
                                                if (pick != null) {
                                                    withContext(Dispatchers.IO) {
                                                        check(prefs.rememberSurprise(pick.game.releaseId))
                                                    }
                                                }
                                                pick
                                            }
                                        result.onSuccess {
                                            surprise = it
                                            message = if (it == null) "No fresh meaningful surprise." else ""
                                        }.onFailure { message = "Could not prepare a surprise." }
                                        surprising = false
                                    }
                                },
                                enabled = !surprising && !changingShare && preferences != null,
                                modifier = Modifier.gemFocusTarget(),
                            ) { Text("Surprise Me") }
                        }
                    }
                }
            }
        }
        if (!failed && data != null) {
            if (message.isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { Text(message) }
            surprise?.let { pick ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Surprise Me", style = MaterialTheme.typography.titleMedium)
                        GemRecommendationCard(
                            pick,
                            pick.game.releaseId in data.ownedIds,
                            installedByRelease[pick.game.releaseId],
                            onOpenCartridge,
                            onInspectRelease,
                        )
                    }
                }
            }
            if (data.ratings.isEmpty()) {
                items(games.take(12), key = { it.cartridgeId }) { game ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CartridgeFront(game, modifier = Modifier.width(120.dp), offline = true)
                            Column(Modifier.weight(1f)) {
                                Text(game.displayName(), style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                GemRatingRow(game, compact = true)
                                TextButton(onClick = { onIdentifyCartridge(game) }) { Text("Identify game") }
                            }
                        }
                    }
                }
            } else if (currentFeed == null) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text("Calculating recommendations…") }
            } else {
                if (currentFeed.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("No picks here yet. Rate more games or try another view.")
                    }
                }
                items(currentFeed, key = { it.game.releaseId }) { recommendation ->
                    GemRecommendationCard(
                        recommendation,
                        recommendation.game.releaseId in data.ownedIds,
                        installedByRelease[recommendation.game.releaseId],
                        onOpenCartridge,
                        onInspectRelease,
                    )
                }
            }
        }
    }
}

@Composable
private fun GemRecommendationCard(
    recommendation: GemRatingRecommender.Recommendation,
    owned: Boolean,
    installedGame: Game?,
    onOpenCartridge: (Game) -> Unit,
    onInspectRelease: (String) -> Unit,
) {
    val game = recommendation.game
    val deckFit =
        remember(game.releaseId, game.platform, game.primaryGenre) {
            GemDeckFit.estimate(game.releaseId, game.platform, game.primaryGenre, game.title)
        }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (installedGame != null) {
                CartridgeFront(installedGame, modifier = Modifier.width(120.dp), offline = true)
            } else {
                CatalogueCartridgeFront(game.title, game.platform, Modifier.width(120.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    cartridgeSystemLabel(game.platform) + if (owned) " · On my shelf" else " · Not linked to shelf",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(recommendation.reason, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                if (recommendation.wildcard) Text("Wildcard", style = MaterialTheme.typography.bodySmall)
                Text(deckFit.displayText, style = MaterialTheme.typography.bodySmall)
                if (installedGame != null) {
                    GemRatingRow(installedGame, deckFit = true, compact = true)
                }
                if (installedGame != null) {
                    TextButton(
                        onClick = { onOpenCartridge(installedGame) },
                        modifier = Modifier.gemFocusTarget(),
                    ) { Text("Open cartridge") }
                }
                TextButton(
                    onClick = { onInspectRelease(game.releaseId) },
                    modifier = Modifier.gemFocusTarget(),
                ) { Text("Game Intelligence") }
            }
        }
    }
}
