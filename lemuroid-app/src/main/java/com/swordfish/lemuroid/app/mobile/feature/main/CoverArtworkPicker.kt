package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun CoverArtworkPicker(
    game: Game,
    onUse: suspend (Game, Bitmap) -> Boolean,
    onDismiss: () -> Unit,
    onLocal: () -> Unit,
) {
    var query by rememberSaveable(game.cartridgeId) { mutableStateOf(game.title.take(150) + " video game") }
    var results by remember { mutableStateOf(emptyList<Pair<CoverArtworkSearch.Result, Bitmap>>()) }
    var selected by remember { mutableStateOf<Pair<CoverArtworkSearch.Result, Bitmap>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Get artwork") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(game.displayName())
                if (selected == null) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(180) },
                        label = { Text("Search artwork") },
                        singleLine = true,
                        enabled = !busy,
                    )
                    TextButton(
                        enabled = !busy && query.isNotBlank(),
                        onClick = {
                            busy = true
                            results = emptyList()
                            message = "Searching…"
                            scope.launch {
                                try {
                                    results =
                                        withContext(Dispatchers.IO) {
                                            CoverArtworkSearch.find(query.trim()).mapNotNull { result ->
                                                runCatching { result to CoverArtworkSearch.load(result) }.getOrNull()
                                            }
                                        }
                                    message = if (results.isEmpty()) "No images found. Try another title." else ""
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    message = "Artwork search failed. Try again or choose a local image."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                    ) { Text("Search") }
                    Text("Wikipedia images · choose artwork you have permission to use.")
                    results.forEach { candidate ->
                        Image(
                            candidate.second.asImageBitmap(),
                            contentDescription = candidate.first.title,
                            modifier = Modifier.fillMaxWidth().height(132.dp),
                            contentScale = ContentScale.Fit,
                        )
                        TextButton(enabled = !busy, onClick = { selected = candidate }) {
                            Text("Select · " + candidate.first.title)
                        }
                    }
                    TextButton(enabled = !busy, onClick = onLocal) { Text("Upload / replace image") }
                } else {
                    selected?.let { candidate ->
                        Image(
                            candidate.second.asImageBitmap(),
                            contentDescription = candidate.first.title,
                            modifier = Modifier.fillMaxWidth().height(210.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text(candidate.first.title + " · Wikipedia")
                        Text("Use only artwork you have permission to use.")
                    }
                    TextButton(enabled = !busy, onClick = { selected = null }) { Text("Other results") }
                }
                if (message.isNotBlank()) Text(message)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && selected != null,
                onClick = {
                    val candidate = selected ?: return@TextButton
                    busy = true
                    scope.launch {
                        try {
                            if (onUse(game, candidate.second)) {
                                onDismiss()
                            } else {
                                message = "Could not save artwork. Previous cover retained."
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            message = "Could not save artwork. Previous cover retained."
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text("Use cover") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } },
    )
}
