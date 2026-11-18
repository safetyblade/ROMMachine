package com.swordfish.lemuroid.app.mobile.feature.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.gemrating.GemCatalogueIndex
import com.swordfish.lemuroid.app.gemrating.GemCataloguePackage
import com.swordfish.lemuroid.app.gemrating.GemGameplayFeatures
import com.swordfish.lemuroid.app.gemrating.GemLocalCorrections
import com.swordfish.lemuroid.app.gemrating.GemProductionCatalogue
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** First offline GEM surface. Existing website search remains an explicitly separate action. */
@Composable
internal fun GemLibraryScreen(
    games: List<Game>,
    modifier: Modifier = Modifier,
    onOpenCartridge: (Game) -> Unit = {},
    initialCartridgeId: String? = null,
    onCartridgeIdConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val corrections = remember(context) { GemLocalCorrections(context) }
    var catalogue by remember { mutableStateOf<GemCataloguePackage.Loaded?>(null) }
    var error by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("For You") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var inspectedId by rememberSaveable { mutableStateOf<String?>(null) }
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
            query = ""
            onCartridgeIdConsumed()
        }
    }

    LaunchedEffect(context, reloading) {
        val loaded = withContext(Dispatchers.IO) { runCatching { GemProductionCatalogue.load(context) } }
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
                        action.customTitle != null -> corrections.saveCustom(game, action.customTitle, action.customGenre)
                        action.releaseId == null -> corrections.clear(game)
                        active == null -> false
                        else -> active.index.find(action.releaseId)?.let { corrections.select(game, it, active) } == true
                    }
                }.getOrDefault(false)
            }
        revision++
        message = if (saved) "Catalogue selection saved." else "Could not save selection. Existing data retained."
        writing = false
        request = null
    }

    val current = catalogue
    val selected = games.firstOrNull { it.cartridgeId == selectedId }
    val savedId = remember(selected, revision) { selected?.let { corrections.read(it) } }
    val custom = remember(selected, revision) { selected?.let { corrections.custom(it) } }
    val inspected = current?.manifest?.releases?.firstOrNull { it.releaseId == inspectedId }
    val match =
        remember(current, selected) {
            if (current != null && selected != null) current.match(selected) else null
        }
    val candidateIds =
        remember(match) { (match as? GemCatalogueIndex.Match.Review)?.candidates?.map { it.releaseId }.orEmpty() }
    val results =
        remember(current, query, selected, candidateIds, savedId) {
            current?.manifest?.releases.orEmpty().asSequence()
                .filter { selected == null || it.systemId == selected.systemId }
                .filter {
                    if (query.isBlank()) {
                        selected != null && (it.releaseId in candidateIds || it.releaseId == savedId)
                    } else {
                        it.title.contains(query.trim(), ignoreCase = true)
                    }
                }
                .take(100)
                .toList()
        }
    Column(modifier = modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("For You", "Browse", "My Ratings", "Search websites").forEach { destination ->
                FilterChip(
                    selected = page == destination,
                    onClick = { page = destination },
                    label = { Text(destination) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        when (page) {
            "Search websites" -> GemWebsiteSearchScreen()
            "My Ratings" -> GemMyRatingsScreen(games = games, modifier = Modifier.weight(1f))
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
                            Text("Game Intelligence", style = MaterialTheme.typography.titleLarge)
                            if (message.isNotBlank()) Text(message)
                            if (error) {
                                Text("Catalogue verification failed. Your cartridges and saves remain unchanged.")
                            } else if (current == null) {
                                Text("Loading offline catalogue…")
                            } else {
                                Text("${current.manifest.releaseCount} historical entries · Source coverage varies")
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = {
                                        query = it.take(160)
                                        inspectedId = null
                                    },
                                    label = { Text("Find a game") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            if (selected != null) {
                                Text(selected.displayName(), style = MaterialTheme.typography.titleMedium)
                                val status =
                                    when {
                                        custom != null -> "Custom information · not a verified catalogue match"
                                        savedId != null && current?.index?.find(savedId) != null -> "Confirmed local link"
                                        savedId != null -> "Previous link unavailable; retained"
                                        else -> "Unmatched · suggestions require confirmation"
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
                                GemIntelligenceRecord(inspected)
                            }
                        }
                    }
                    if (current != null && inspected == null) {
                        if (selected == null && query.isBlank()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("My cartridges", style = MaterialTheme.typography.titleMedium)
                            }
                            items(games, key = { it.cartridgeId }) { game ->
                                com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow(
                                    title = { Text(game.displayName()) },
                                    supporting = { Text(cartridgeSystemLabel(game.systemId)) },
                                    onClick = {
                                        selectedId = game.cartridgeId
                                        query = ""
                                    },
                                )
                            }
                        } else {
                            if (results.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text("No matching records. Try another title or enter custom information.")
                                }
                            }
                            items(results, key = { it.releaseId }) { release ->
                                Column(Modifier.fillMaxWidth().padding(4.dp)) {
                                    GemIntelligenceRecord(release)
                                    if (selected != null) {
                                        TextButton(
                                            onClick = { pending = release },
                                            enabled = !writing && request == null,
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        ) { Text("Confirm this entry") }
                                    }
                                }
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        TextButton(
                            onClick = { importPicker.launch(arrayOf("application/json", "application/octet-stream")) },
                            enabled = restoreUri == null && !writing && request == null,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text("Restore approved catalogue package") }
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
private fun GemIntelligenceRecord(release: GemCataloguePackage.Release) {
    val feature = remember(release) { GemGameplayFeatures.find(release.releaseId, release.title) }
    androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(release.title, style = MaterialTheme.typography.titleMedium)
            Text(cartridgeSystemLabel(release.systemId) + " · " + (release.primaryGenre ?: "Genre unknown"))
            feature?.let {
                Text(listOfNotNull(it.crossover, it.playstyle).joinToString(" · "))
                if (it.mechanics.isNotEmpty()) Text(it.mechanics.sorted().joinToString(" · "))
                Text("Provisional gameplay classification", style = MaterialTheme.typography.bodySmall)
            }
            Text("Source record: ${release.releaseId}", style = MaterialTheme.typography.bodySmall)
            Text("Historical metadata · compatibility not verified", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class CorrectionRequest(
    val cartridgeId: String,
    val releaseId: String?,
    val customTitle: String? = null,
    val customGenre: String = "",
)
