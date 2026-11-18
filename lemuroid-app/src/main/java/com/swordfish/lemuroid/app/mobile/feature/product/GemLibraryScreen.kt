package com.swordfish.lemuroid.app.mobile.feature.product

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.gemrating.GemCatalogueIndex
import com.swordfish.lemuroid.app.gemrating.GemCataloguePackage
import com.swordfish.lemuroid.app.gemrating.GemLocalCorrections
import com.swordfish.lemuroid.app.gemrating.GemProductionCatalogue
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** First offline GEM surface. Existing website search remains an explicitly separate action. */
@Composable
internal fun GemLibraryScreen(
    games: List<Game>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val corrections = remember(context) { GemLocalCorrections(context) }
    var catalogue by remember { mutableStateOf<GemCataloguePackage.Loaded?>(null) }
    var error by remember { mutableStateOf(false) }
    var page by rememberSaveable { mutableStateOf("For You") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
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
                    if (active == null || game == null) {
                        false
                    } else if (action.releaseId == null) {
                        corrections.clear(game)
                    } else {
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
    val selected = games.firstOrNull { it.cartridgeId == selectedId }
    val savedId = remember(selected, revision) { selected?.let { corrections.read(it) } }
    val match =
        remember(current, selected) {
            if (current != null && selected != null) current.match(selected) else null
        }
    val candidateIds =
        remember(match) { (match as? GemCatalogueIndex.Match.Review)?.candidates?.map { it.releaseId }.orEmpty() }
    val results =
        remember(current, query, selected, candidateIds) {
            current?.manifest?.releases.orEmpty().asSequence()
                .filter { selected == null || it.systemId == selected.systemId }
                .filter {
                    if (query.isBlank()) {
                        selected != null && it.releaseId in candidateIds
                    } else {
                        it.title.contains(query.trim(), ignoreCase = true)
                    }
                }
                .take(100)
                .toList()
        }
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            listOf("For You", "Browse", "My Ratings", "Search websites").forEach { destination ->
                TextButton(onClick = { page = destination }, enabled = page != destination) { Text(destination) }
            }
        }
        if (page == "Search websites") {
            GemWebsiteSearchScreen()
        } else if (page == "My Ratings") {
            GemMyRatingsScreen(games = games, modifier = Modifier.weight(1f))
        } else if (page == "For You") {
            if (error) {
                Text("Catalogue verification failed. Your ratings and saves remain unchanged.")
            } else if (current == null) {
                Text("Loading offline catalogue…")
            } else {
                GemFinderScreen(catalogue = current, games = games, modifier = Modifier.weight(1f))
            }
        } else {
            Text("Game Intelligence", style = MaterialTheme.typography.titleLarge)
            TextButton(
                onClick = { importPicker.launch(arrayOf("application/json", "application/octet-stream")) },
                enabled = restoreUri == null && !writing && request == null,
            ) { Text("Restore approved catalogue package") }
            if (message.isNotBlank()) Text(message)
            if (error) {
                Text("Catalogue verification failed. Your cartridges and saves remain unchanged.")
            } else if (current == null) {
                Text("Loading offline catalogue…")
            } else {
                Text("${current.manifest.releaseCount} historical entries · Source coverage varies")
                Text("Title matches require confirmation. Records are not compatibility or ownership evidence.")
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(160) },
                    label = { Text("Find a game") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (selected != null) {
                    Text("Link to: ${selected.displayName()}")
                    Text(
                        current.index.find(savedId.orEmpty())?.title?.let { "Linked: $it" }
                            ?: if (savedId != null) "Previous link is unavailable; retained." else "No confirmed link.",
                    )
                    Row {
                        TextButton(onClick = { selectedId = null }, enabled = !writing) { Text("Cancel link") }
                        TextButton(
                            onClick = { request = CorrectionRequest(selected.cartridgeId, null) },
                            enabled = savedId != null && !writing && request == null,
                        ) { Text("Clear link") }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (selected == null && query.isBlank()) {
                        item { Text("My cartridges", style = MaterialTheme.typography.titleMedium) }
                        items(games, key = { it.cartridgeId }) { game ->
                            TextButton(onClick = { selectedId = game.cartridgeId }) {
                                Text("${game.displayName()} · ${game.systemId}")
                            }
                        }
                    } else {
                        if (results.isEmpty()) item { Text("No matching records. Try another title.") }
                        items(results, key = { it.releaseId }) { release ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(release.title, style = MaterialTheme.typography.titleMedium)
                                Text("${release.systemId} · ${release.primaryGenre ?: "Genre unknown"}")
                                Text("Source record: ${release.releaseId}", style = MaterialTheme.typography.bodySmall)
                                if (selected != null) {
                                    TextButton(
                                        onClick = { pending = release },
                                        enabled = !writing && request == null,
                                    ) { Text("Select this entry") }
                                }
                            }
                        }
                    }
                }
            }
        }
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

private data class CorrectionRequest(val cartridgeId: String, val releaseId: String?)
