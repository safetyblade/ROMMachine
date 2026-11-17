package com.swordfish.lemuroid.app.mobile.feature.product

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** GEM's actual website search; recommendations remain separately capability-gated. */
@Composable
internal fun GemWebsiteSearchScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { GemWebsiteStore(context) }
    val initial = remember(store) { runCatching { store.read() } }
    var sources by remember(store) { mutableStateOf(initial.getOrDefault(emptyList())) }
    var query by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }

    fun save(next: List<GemWebsiteSource>): Boolean {
        if (initial.isFailure) return false
        return runCatching { store.write(next) }.getOrDefault(false).also { saved ->
            if (saved) {
                sources = next
                message = ""
            } else {
                message = "Could not save websites. Existing sources are retained."
            }
        }
    }

    fun open(source: GemWebsiteSource, term: String) {
        val url = source.searchUri(term)
        if (url == null) {
            message = "Enter a search term or correct the website address."
            return
        }
        message =
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.toASCIIString()))) }
                .fold({ "" }, { "No browser available to open this website." })
    }

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Search websites", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(500) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search for a game, mod or guide") },
            singleLine = true,
        )
        Text("Search opens the selected site in your browser. Do not enter private information.")
        if (initial.isFailure) {
            Text(
                "Saved websites could not be read. Existing data is retained.",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            sources.filter { it.enabled }.groupBy { it.category.ifBlank { "Websites" } }.forEach { (category, list) ->
                Text(category, style = MaterialTheme.typography.titleMedium)
                list.forEach { source ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(source.name, style = MaterialTheme.typography.titleMedium)
                            Text(source.hostname(), style = MaterialTheme.typography.bodySmall)
                            Button(
                                onClick = { open(source, query) },
                                enabled = source.template.isBlank() || query.isNotBlank(),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(if (source.template.isBlank()) "Open website" else "Search website")
                            }
                        }
                    }
                }
            }
            Text("My Sources", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = {
                    editId = null
                    editorOpen = true
                },
                enabled = sources.size < 100,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(if (sources.isEmpty()) "Add your first website" else "Add a website")
            }
            sources.forEachIndexed { index, source ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(source.name, style = MaterialTheme.typography.titleMedium)
                        Text(source.hostname() + if (source.enabled) " · Enabled" else " · Disabled")
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(
                                onClick = {
                                    editId = source.id
                                    editorOpen = true
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Edit") }
                            TextButton(
                                onClick = {
                                    save(sources.map { if (it.id == source.id) it.copy(enabled = !it.enabled) else it })
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text(if (source.enabled) "Disable" else "Enable") }
                        }
                        Row(Modifier.fillMaxWidth()) {
                            TextButton(
                                enabled = index > 0,
                                onClick = {
                                    save(sources.toMutableList().apply { add(index - 1, removeAt(index)) })
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Up") }
                            TextButton(
                                enabled = index < sources.lastIndex,
                                onClick = {
                                    save(sources.toMutableList().apply { add(index + 1, removeAt(index)) })
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Down") }
                            TextButton(
                                onClick = { deleteId = source.id },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            ) { Text("Remove") }
                        }
                    }
                }
            }
        }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
    }
    if (editorOpen && initial.isSuccess) {
        val existing = sources.firstOrNull { it.id == editId }
        GemWebsiteEditor(
            existing = existing,
            onCancel = { editorOpen = false },
            onTest = { open(it, "example game") },
            onSave = { source ->
                val duplicate = sources.any {
                    it.id != source.id &&
                        (it.name.equals(source.name, ignoreCase = true) ||
                            (it.home.equals(source.home, ignoreCase = true) && it.template == source.template))
                }
                if (duplicate) {
                    "A website with this name or address already exists."
                } else {
                    val next =
                        if (existing == null) {
                            sources + source
                        } else {
                            sources.map { if (it.id == source.id) source else it }
                        }
                    if (save(next)) {
                        editorOpen = false
                        null
                    } else {
                        "Could not save. Existing websites are retained."
                    }
                }
            },
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Remove website?") },
            text = { Text(sources.firstOrNull { it.id == id }?.name.orEmpty()) },
            confirmButton = {
                TextButton(onClick = {
                    if (save(sources.filterNot { it.id == id })) deleteId = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GemWebsiteEditor(
    existing: GemWebsiteSource?,
    onCancel: () -> Unit,
    onTest: (GemWebsiteSource) -> Unit,
    onSave: (GemWebsiteSource) -> String?,
) {
    val draftId = rememberSaveable(existing?.id) { existing?.id ?: java.util.UUID.randomUUID().toString() }
    var name by rememberSaveable(draftId) { mutableStateOf(existing?.name.orEmpty()) }
    var home by rememberSaveable(draftId) { mutableStateOf(existing?.home.orEmpty()) }
    var category by rememberSaveable(draftId) { mutableStateOf(existing?.category.orEmpty()) }
    var template by rememberSaveable(draftId) { mutableStateOf(existing?.template.orEmpty()) }
    var error by rememberSaveable(draftId) { mutableStateOf("") }
    val draft =
        GemWebsiteSource(
            draftId, name.trim(), home.trim(), category.trim(), template.trim(), existing?.enabled ?: true,
        )
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (existing == null) "Add website" else "Edit website") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it.take(80) }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(home, { home = it.take(2000) }, label = { Text("HTTPS website") }, singleLine = true)
                OutlinedTextField(category, { category = it.take(60) }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(
                    template,
                    { template = it.take(2000) },
                    label = { Text("Search address (optional)") },
                    supportingText = { Text("Example: https://example.com/search?q={query}") },
                    singleLine = true,
                )
                draft.searchUri("example game")?.let { Text("Preview: " + it.toASCIIString()) }
                TextButton(
                    enabled = draft.validationError() == null,
                    onClick = { onTest(draft) },
                ) { Text("Test website") }
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = draft.validationError() ?: onSave(draft).orEmpty()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
