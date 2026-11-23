package com.swordfish.lemuroid.app.mobile.feature.product

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.gemrating.GemCatalogueIndex
import com.swordfish.lemuroid.app.gemrating.GemCataloguePackage
import com.swordfish.lemuroid.app.gemrating.GemCatalogueVersions
import com.swordfish.lemuroid.app.gemrating.GemDeckFit
import com.swordfish.lemuroid.app.gemrating.GemFeatureContributions
import com.swordfish.lemuroid.app.gemrating.GemGenreTaxonomy
import com.swordfish.lemuroid.app.gemrating.GemGameplayFeatures
import com.swordfish.lemuroid.app.gemrating.GemLocalCorrections
import com.swordfish.lemuroid.app.gemrating.GemProductionCatalogue
import com.swordfish.lemuroid.app.gemrating.GemRatingRepository
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CatalogueCartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** First offline GEM surface. Existing website search remains an explicitly separate action. */
@Composable
internal fun GemLibraryScreen(
    games: List<Game>,
    modifier: Modifier = Modifier,
    onOpenCartridge: (Game) -> Unit = {},
    initialCartridgeId: String? = null,
    onCartridgeIdConsumed: () -> Unit = {},
    onBackToShelf: () -> Unit = {},
) {
    val context = LocalContext.current
    val corrections = remember(context) { GemLocalCorrections(context) }
    val versions = remember(context) { GemCatalogueVersions(context) }
    var catalogue by remember { mutableStateOf<GemCataloguePackage.Loaded?>(null) }
    var error by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("Menu") }
    var query by rememberSaveable { mutableStateOf("") }
    var browseCatalogue by rememberSaveable { mutableStateOf(true) }
    var platformFilter by rememberSaveable { mutableStateOf("") }
    var genreFilter by rememberSaveable { mutableStateOf("") }
    var pageLimit by rememberSaveable { mutableStateOf(48) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var inspectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var linkingReleaseId by rememberSaveable { mutableStateOf<String?>(null) }
    var customEditing by remember { mutableStateOf(false) }
    var customTitle by rememberSaveable { mutableStateOf("") }
    var customGenre by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<GemCataloguePackage.Release?>(null) }
    var revision by remember { mutableStateOf(0) }
    var writing by remember { mutableStateOf(false) }
    var request by remember { mutableStateOf<CorrectionRequest?>(null) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var reloading by remember { mutableStateOf(0) }
    val importPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            restoreUri = uri
        }

    LaunchedEffect(initialCartridgeId) {
        if (initialCartridgeId != null) {
            page = "Browse"
            selectedId = initialCartridgeId
            inspectedId = null
            linkingReleaseId = null
            query = ""
            onCartridgeIdConsumed()
        }
    }

    LaunchedEffect(context, reloading) {
        val loaded = withContext(Dispatchers.IO) { runCatching { versions.load() } }
        catalogue = loaded.getOrNull()
        error = loaded.isFailure
    }

    LaunchedEffect(restoreUri) {
        val uri = restoreUri ?: return@LaunchedEffect
        val result =
            withContext(Dispatchers.IO) {
                runCatching {
                    requireNotNull(context.contentResolver.openInputStream(uri)).use {
                        GemProductionCatalogue.importApproved(context, it)
                    }
                }
            }
        message =
            result.fold(
                { count -> "Restored approved package ($count records)." },
                { "Package not approved or unreadable. Existing catalogue retained." },
            )
        restoreUri = null
        if (result.isSuccess) reloading++
    }

    LaunchedEffect(request) {
        val action = request ?: return@LaunchedEffect
        val active = catalogue
        val game = games.firstOrNull { it.cartridgeId == action.cartridgeId }
        writing = true
        val saved =
            withContext(Dispatchers.IO) {
                runCatching {
                    when {
                        game == null -> false
                        action.customTitle != null ->
                            corrections.saveCustom(game, action.customTitle, action.customGenre)
                        action.releaseId == null -> corrections.clear(game)
                        active == null -> false
                        else ->
                            active.index.find(action.releaseId)?.let { corrections.select(game, it, active) } == true
                    }
                }.getOrDefault(false)
            }
        revision++
        message = if (saved) "Catalogue selection saved." else "Could not save selection. Existing data retained."
        writing = false
        request = null
    }

    val current = catalogue
    val linkingRelease =
        remember(current, linkingReleaseId) {
            val id = linkingReleaseId ?: return@remember null
            current?.manifest?.releases?.firstOrNull { it.releaseId == id }
        }
    var contributions by remember(current) { mutableStateOf(emptyMap<String, GemFeatureContributions.Record>()) }
    LaunchedEffect(current) {
        val releases = current?.manifest?.releases ?: return@LaunchedEffect
        contributions =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open("gem/features/v1.json").use {
                        GemFeatureContributions.load(it, releases).accepted
                    }
                }.getOrDefault(emptyMap())
            }
    }
    val selected = games.firstOrNull { it.cartridgeId == selectedId }
    val savedId = remember(selected, revision) { selected?.let { corrections.read(it) } }
    val custom = remember(selected, revision) { selected?.let { corrections.custom(it) } }
    val inspected = current?.manifest?.releases?.firstOrNull { it.releaseId == inspectedId }
    var installedByRelease by remember(current, games, revision) { mutableStateOf(emptyMap<String, List<Game>>()) }
    LaunchedEffect(current, games, revision) {
        if (current != null) {
            installedByRelease =
                withContext(Dispatchers.IO) {
                    games.mapNotNull { game ->
                        val id =
                            corrections.read(game)
                                ?: (current.match(game) as? GemCatalogueIndex.Match.Confirmed)?.release?.releaseId
                        id?.takeIf { current.index.find(it)?.systemId == game.systemId }?.let { it to game }
                    }.groupBy({ it.first }, { it.second })
                }
        }
    }
    val sources = remember(current) { current?.manifest?.sources.orEmpty().associateBy { it.id } }
    val match =
        remember(current, selected) {
            if (current != null && selected != null) current.match(selected) else null
        }
    val candidateIds =
        remember(match) {
            when (match) {
                is GemCatalogueIndex.Match.Confirmed -> setOf(match.release.releaseId)
                is GemCatalogueIndex.Match.Review -> match.candidates.map { it.releaseId }.toSet()
                else -> emptySet()
            }
        }
    var results by remember(current, query, selected, candidateIds, savedId) {
        mutableStateOf<List<GemCataloguePackage.Release>>(emptyList())
    }
    var searching by remember(current, query, selected, candidateIds, savedId) { mutableStateOf(true) }
    LaunchedEffect(
        current, query, selected, candidateIds, savedId, platformFilter, genreFilter, pageLimit, contributions,
    ) {
        val term = query.trim()
        if (term.isNotEmpty()) delay(120)
        results =
            withContext(Dispatchers.Default) {
                current?.manifest?.releases.orEmpty().asSequence()
                    .filter { selected == null || it.systemId == selected.systemId }
                    .filter {
                        selected != null || platformFilter.isBlank() ||
                            cartridgeSystemLabel(it.systemId).contains(platformFilter.trim(), ignoreCase = true)
                    }
                    .filter {
                        selected != null || genreFilter.isBlank() ||
                            GemGenreTaxonomy.classify(
                                contributions[it.releaseId]?.primaryGenre ?: it.primaryGenre,
                                it.title,
                            ).primary.orEmpty().contains(genreFilter.trim(), ignoreCase = true)
                    }
                    .filter {
                        if (term.isEmpty()) {
                            selected == null || it.releaseId in candidateIds || it.releaseId == savedId
                        } else {
                            val tags =
                                contributions[it.releaseId]?.mechanics
                                    ?: GemGameplayFeatures.find(it.releaseId, it.title)?.mechanics.orEmpty()
                            it.title.contains(term, ignoreCase = true) ||
                                (
                                    selected == null &&
                                        (
                                            it.releaseId.equals(term, ignoreCase = true) ||
                                                tags.any { mechanic -> mechanic.contains(term, ignoreCase = true) }
                                        )
                                )
                        }
                    }
                    .take(pageLimit + 1)
                    .toList()
            }
        searching = false
    }
    BackHandler(enabled = page != "Menu") {
        linkingReleaseId = null
        page = "Menu"
    }
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (page != "Menu") {
            TextButton(
                onClick = {
                    linkingReleaseId = null
                    page = "Menu"
                },
                modifier = Modifier.gemFocusTarget(),
            ) { Text("‹ GEM menu") }
        }
        when (page) {
            "Menu" -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        listOf("For You", "Browse", "My Ratings", "Search", "Game Intelligence", "Manage"),
                    ) { destination ->
                        val title =
                            when (destination) {
                                "For You" -> "GemFinder"
                                "Browse" -> "Browse Games"
                                "Search" -> "Search & Homebrew"
                                "Manage" -> "GEM settings"
                                else -> destination
                            }
                        CartdeckMenuRow(
                            title = { Text(title) },
                            onClick = {
                                if (destination == "Browse" || destination == "Game Intelligence") {
                                    browseCatalogue = destination == "Browse"
                                    page = "Browse"
                                } else {
                                    page = destination
                                }
                            },
                        )
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TextButton(onClick = onBackToShelf, modifier = Modifier.gemFocusTarget()) {
                            Text("Back to shelf")
                        }
                    }
                }
            }
            "Manage" ->
                GemManagementScreen(
                    onCatalogueChanged = { reloading++ },
                    onRestoreBaseline = {
                        if (restoreUri == null && !writing) {
                            importPicker.launch(arrayOf("application/json", "application/octet-stream"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            "My Ratings" -> GemMyRatingsScreen(games = games, modifier = Modifier.weight(1f))
            "Search" -> GemWebsiteSearchScreen(modifier = Modifier.weight(1f))
            "For You" -> {
                if (error) {
                    Text("Catalogue verification failed. Your ratings and saves remain unchanged.")
                } else if (current == null) {
                    Text("Loading offline catalogue…")
                } else {
                    GemFinderScreen(
                        catalogue = current,
                        games = games,
                        modifier = Modifier.weight(1f),
                        onOpenCartridge = onOpenCartridge,
                        onIdentifyCartridge = { game ->
                            selectedId = game.cartridgeId
                            query = ""
                            inspectedId = null
                            page = "Browse"
                        },
                        onInspectRelease = { id ->
                            selectedId = null
                            query = ""
                            inspectedId = id
                            page = "Browse"
                        },
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(320.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (selected == null) "Browse Games" else "Game Intelligence",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            if (message.isNotBlank()) Text(message)
                            if (error) {
                                Text("Catalogue verification failed. Your cartridges and saves remain unchanged.")
                            } else if (current == null) {
                                Text("Loading offline catalogue…")
                            } else {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = {
                                        query = it.take(160)
                                        pageLimit = 48
                                        inspectedId = null
                                    },
                                    label = {
                                        Text(
                                            if (selected == null) "Title, release ID or mechanic" else "Find a game",
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            if (selected == null && inspected == null && current != null) {
                                TextButton(
                                    onClick = {
                                        linkingReleaseId = null
                                        browseCatalogue = !browseCatalogue
                                    },
                                ) { Text(if (browseCatalogue) "Identify my cartridges" else "Browse catalogue") }
                                if (browseCatalogue) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = platformFilter,
                                            onValueChange = {
                                                platformFilter = it.take(64)
                                                pageLimit = 48
                                            },
                                            label = { Text("Console") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                        )
                                        OutlinedTextField(
                                            value = genreFilter,
                                            onValueChange = {
                                                genreFilter = it.take(64)
                                                pageLimit = 48
                                            },
                                            label = { Text("Genre") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                        )
                                    }
                                }
                            }
                            if (selected != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CartridgeFront(selected, modifier = Modifier.width(112.dp), offline = true)
                                    Column(Modifier.weight(1f)) {
                                        Text(selected.displayName(), style = MaterialTheme.typography.titleMedium)
                                        Text(cartridgeSystemLabel(selected.systemId))
                                    }
                                }
                                val status =
                                    when {
                                        custom != null -> "Custom game information"
                                        savedId != null && current?.index?.find(savedId) != null ->
                                            "Game identified"
                                        savedId != null -> "Game information unavailable"
                                        else -> "Choose a matching game"
                                    }
                                Text(status)
                                custom?.let { Text(it.title + " · " + (it.genre ?: "Genre unknown")) }
                                savedId?.let { id -> current?.index?.find(id)?.let { Text(it.title) } }
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    TextButton(
                                        onClick = {
                                            selectedId = null
                                            inspectedId = null
                                            query = ""
                                        },
                                        enabled = !writing,
                                    ) { Text("Back to cartridges") }
                                    TextButton(
                                        onClick = {
                                            customTitle = custom?.title ?: selected.displayName()
                                            customGenre = custom?.genre.orEmpty()
                                            customEditing = true
                                        },
                                        enabled = !writing && request == null,
                                    ) { Text("Custom information") }
                                    TextButton(
                                        onClick = { request = CorrectionRequest(selected.cartridgeId, null) },
                                        enabled = (savedId != null || custom != null) && !writing && request == null,
                                    ) { Text("Reset") }
                                }
                            }
                            if (inspected != null) {
                                TextButton(onClick = { inspectedId = null }) { Text("Back to browse") }
                                val installed = installedByRelease[inspected.releaseId].orEmpty()
                                GemIntelligenceRecord(
                                    inspected,
                                    sources[inspected.sourceId],
                                    contributions[inspected.releaseId],
                                    installedGame = installed.singleOrNull(),
                                )
                                val libraryStatus = if (installed.isEmpty()) "No linked cartridge" else "Installed"
                                Text(libraryStatus)
                                if (installed.isEmpty()) {
                                    val candidates = games.count { it.systemId == inspected.systemId }
                                    if (candidates > 0) {
                                        TextButton(
                                            onClick = {
                                                linkingReleaseId = inspected.releaseId
                                                selectedId = null
                                                inspectedId = null
                                                browseCatalogue = false
                                                query = ""
                                            },
                                            modifier = Modifier.gemFocusTarget(),
                                        ) { Text("Identify an existing cartridge") }
                                    } else {
                                        val console = cartridgeSystemLabel(inspected.systemId)
                                        Text("Add a $console cartridge to your shelf to link it.")
                                    }
                                }
                                val ratingId = GemRatingRepository.catalogueRatingId(inspected.releaseId)
                                GemRatingRow(
                                    cartridgeId = ratingId,
                                    displayTitle = inspected.title,
                                    platformId = inspected.systemId,
                                    catalogueReleaseId = inspected.releaseId,
                                )
                                GemRatingRow(
                                    cartridgeId = ratingId,
                                    displayTitle = inspected.title,
                                    platformId = inspected.systemId,
                                    catalogueReleaseId = inspected.releaseId,
                                    deckFit = true,
                                )
                                installed.forEach { game ->
                                    CartdeckMenuRow(
                                        title = { Text("Open ${game.displayName()}", maxLines = 2) },
                                        leading =
                                            if (installed.size > 1) {
                                                { CartridgeFront(game, Modifier.width(72.dp), offline = true) }
                                            } else {
                                                null
                                            },
                                        onClick = { onOpenCartridge(game) },
                                    )
                                }
                            }
                        }
                    }
                    if (current != null && inspected == null) {
                        if (selected == null && !browseCatalogue) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    Text("My cartridges", style = MaterialTheme.typography.titleMedium)
                                    if (linkingRelease != null) {
                                        val console = cartridgeSystemLabel(linkingRelease.systemId)
                                        Text("Choose the $console cartridge for ${linkingRelease.title}.")
                                        TextButton(
                                            onClick = { linkingReleaseId = null },
                                        ) { Text("Cancel identification") }
                                    }
                                }
                            }
                            items(
                                games.filter {
                                    (linkingRelease == null || it.systemId == linkingRelease.systemId) &&
                                        it.displayName().contains(query.trim(), ignoreCase = true)
                                },
                                key = { it.cartridgeId },
                            ) { game ->
                                com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow(
                                    title = { Text(game.displayName(), maxLines = 2) },
                                    leading = {
                                        CartridgeFront(game, modifier = Modifier.width(88.dp), offline = true)
                                    },
                                    supporting = { Text(cartridgeSystemLabel(game.systemId)) },
                                    onClick = {
                                        selectedId = game.cartridgeId
                                        if (linkingRelease != null) {
                                            pending = linkingRelease
                                            linkingReleaseId = null
                                        }
                                        query = ""
                                    },
                                )
                            }
                        } else {
                            if (searching) {
                                item(span = { GridItemSpan(maxLineSpan) }) { Text("Searching catalogue…") }
                            } else if (results.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text("No match found. Try a shorter title or add game information.")
                                }
                            }
                            items(results.take(pageLimit), key = { it.releaseId }) { release ->
                                Column(Modifier.fillMaxWidth().padding(4.dp)) {
                                    val copies = installedByRelease[release.releaseId].orEmpty()
                                    GemIntelligenceRecord(
                                        release,
                                        sources[release.sourceId],
                                        contributions[release.releaseId],
                                        installedGame = copies.singleOrNull(),
                                    )
                                    if (selected != null) {
                                        TextButton(
                                            onClick = { pending = release },
                                            enabled = !writing && request == null,
                                            modifier = Modifier.gemFocusTarget(),
                                        ) { Text("Confirm this entry") }
                                    } else {
                                        TextButton(
                                            onClick = { inspectedId = release.releaseId },
                                            modifier = Modifier.gemFocusTarget(),
                                        ) {
                                            val label = if (copies.isEmpty()) "View release" else "In your library"
                                            Text(label)
                                        }
                                    }
                                }
                            }
                            if (results.size > pageLimit) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    TextButton(onClick = { pageLimit += 48 }, modifier = Modifier.gemFocusTarget()) {
                                        Text("Show more releases")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (customEditing && selected != null) {
        AlertDialog(
            onDismissRequest = { customEditing = false },
            title = { Text("Custom game information") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Only GEM information changes. No ROM, save or cartridge identity is modified.")
                    OutlinedTextField(
                        value = customTitle,
                        onValueChange = { customTitle = it.take(512) },
                        label = { Text("Title") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = customGenre,
                        onValueChange = { customGenre = it.take(128) },
                        label = { Text("Genre (optional)") },
                        singleLine = true,
                    )
                    Text("Custom information is not used as a verified release link for recommendations.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        request = CorrectionRequest(selected.cartridgeId, null, customTitle, customGenre)
                        customEditing = false
                    },
                    enabled = customTitle.isNotBlank() && !writing && request == null,
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { customEditing = false }) { Text("Cancel") } },
        )
    }
    val choice = pending
    if (choice != null && selected != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Confirm catalogue link") },
            text = {
                Text("Link ${selected.displayName()} to ${choice.title}? Saves and cartridge identity stay unchanged.")
            },
            confirmButton = {
                TextButton(onClick = {
                    request = CorrectionRequest(selected.cartridgeId, choice.releaseId)
                    pending = null
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun GemIntelligenceRecord(
    release: GemCataloguePackage.Release,
    source: GemCataloguePackage.Source?,
    contribution: GemFeatureContributions.Record?,
    installedGame: Game? = null,
) {
    val fallback = remember(release) { GemGameplayFeatures.find(release.releaseId, release.title) }
    val classification =
        remember(release, contribution) {
            GemGenreTaxonomy.classify(contribution?.primaryGenre ?: release.primaryGenre, release.title)
        }
    val genre = classification.primary
    val crossover = contribution?.crossoverGenre ?: fallback?.crossover ?: classification.crossover
    val playstyle = contribution?.playstyle ?: fallback?.playstyle ?: classification.playstyle
    val mechanics = (contribution?.mechanics?.toSet() ?: fallback?.mechanics.orEmpty()) + classification.mechanics
    val deckFit =
        remember(release, genre, playstyle, mechanics) {
            GemDeckFit.estimate(
                release.releaseId,
                release.systemId,
                genre,
                release.title,
                GemDeckFit.evidenceTraits(playstyle, mechanics),
            )
        }
    var details by remember(release.releaseId) { mutableStateOf(false) }
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (installedGame == null) {
                CatalogueCartridgeFront(release.title, release.systemId, Modifier.width(100.dp))
            } else {
                CartridgeFront(installedGame, Modifier.width(100.dp), offline = true)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(installedGame?.displayName() ?: release.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    cartridgeSystemLabel(release.systemId) +
                        " · " + (genre ?: "Genre unknown") +
                        " · " + (source?.name ?: release.sourceId),
                )
                val traits = listOfNotNull(crossover, playstyle)
                if (traits.isNotEmpty()) Text(traits.joinToString(" · "))
                if (mechanics.isNotEmpty()) Text(mechanics.sorted().joinToString(" · "))
                Text(deckFit.displayText, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { details = !details }) { Text(if (details) "Hide details" else "More details") }
                if (details) {
                    if (installedGame != null && installedGame.displayName() != release.title) {
                        Text("Catalogue title: ${release.title}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(source?.name.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    Text(source?.licence.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    Text("Source record: ${release.releaseId}", style = MaterialTheme.typography.bodySmall)
                    Text("Source genre: ${release.primaryGenre ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                    if (classification.reviewFlag) {
                        Text("Catalogue classification needs review", style = MaterialTheme.typography.bodySmall)
                    }
                    contribution?.sourceLicence?.let { Text("Feature rights: $it", style = MaterialTheme.typography.bodySmall) }
                    contribution?.evidence?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Text(deckFit.evidenceText, style = MaterialTheme.typography.bodySmall)
                    Text("Compatibility unverified", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private data class CorrectionRequest(
    val cartridgeId: String,
    val releaseId: String?,
    val customTitle: String? = null,
    val customGenre: String = "",
)
