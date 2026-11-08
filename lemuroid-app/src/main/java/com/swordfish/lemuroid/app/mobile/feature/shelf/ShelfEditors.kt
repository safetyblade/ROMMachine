package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun ShelfToolbar(
    arrange: Boolean,
    available: Boolean,
    canUndo: Boolean,
    onArrange: () -> Unit,
    onSection: () -> Unit,
    onAppearance: () -> Unit,
    onUndo: () -> Unit,
) {
    Column {
        Row {
            TextButton(
                onClick = onArrange,
                enabled = available,
            ) { Text(if (arrange) "Done arranging" else "Arrange shelf") }
            TextButton(onClick = onAppearance, enabled = available) { Text("Appearance") }
        }
        if (arrange) {
            Row {
                TextButton(onClick = onSection) { Text("Add section") }
                TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
            }
        }
    }
}

@Composable
fun SectionEditor(
    section: ShelfSection?,
    onDismiss: () -> Unit,
    onEdit: ((ShelfLayout) -> ShelfLayout) -> Unit,
) {
    var title by remember(section?.id) { mutableStateOf(section?.title ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (section == null) "Add section" else "Edit section") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = {
                    title = it.take(80)
                }, label = { Text("Section name") }, singleLine = true)
                if (section != null) {
                    TextButton(onClick = {
                        onEdit { it.moveSection(section.id, -1) }
                        onDismiss()
                    }) { Text("Move section up") }
                    TextButton(onClick = {
                        onEdit { it.moveSection(section.id, 1) }
                        onDismiss()
                    }) { Text("Move section down") }
                    TextButton(onClick = {
                        onEdit { it.insertSpace(section.id) }
                        onDismiss()
                    }) { Text("Insert empty space") }
                    TextButton(onClick = {
                        onEdit { it.insertSpace(section.id, row = true) }
                        onDismiss()
                    }) { Text("Insert empty row") }
                    if (section.id != ShelfLayout.UNSORTED) {
                        TextButton(onClick = {
                            onEdit { it.deleteSection(section.id) }
                            onDismiss()
                        }) { Text("Delete section and rehome cartridges") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onEdit { if (section == null) it.createSection(title) else it.renameSection(section.id, title) }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private val finishNames =
    linkedMapOf(
        "chassis" to "Cartridge chassis", "accent" to "Cartridge accent",
        "roomBackdrop" to "Room", "shelfBackboard" to "Back panel", "shelfDeck" to "Rail deck",
        "shelfLip" to "Rail lip", "shelfGroove" to "Rail groove", "edgeHighlight" to "Machined edge",
        "shelfShadow" to "Shadow", "sectionDivider" to "Section divider", "vacantCell" to "Reserved space",
        "sectionText" to "Section text", "focusOutline" to "Focus outline", "interactionAccent" to "Active lighting",
    )

@Composable
fun ShelfAppearanceEditor(
    appearance: ShelfAppearance,
    onDismiss: () -> Unit,
    onSave: (ShelfAppearance) -> Unit,
) {
    var edition by remember { mutableStateOf(appearance.edition) }
    var highContrast by remember { mutableStateOf(appearance.highContrast) }
    var reducedMotion by remember { mutableStateOf(appearance.reducedMotion) }
    val finishes =
        remember {
            mutableStateMapOf<String, String>().apply {
                appearance.custom.forEach { (key, value) -> put(key, "#%06X".format(value.toInt() and 0xFFFFFF)) }
            }
        }
    val valid = finishes.values.all { it.isBlank() || it.matches(Regex("#[0-9a-fA-F]{6}")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shelf appearance") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row {
                    listOf("original", "pink").forEach { choice ->
                        TextButton(onClick = {
                            edition = choice
                            finishes.clear()
                        }) {
                            Text(
                                if (edition == choice) {
                                    "✓ ${choice.replaceFirstChar { it.uppercase() }}"
                                } else {
                                    choice.replaceFirstChar {
                                        it.uppercase()
                                    }
                                },
                            )
                        }
                    }
                }
                Row {
                    Checkbox(highContrast, { highContrast = it })
                    Text("High contrast")
                }
                Row {
                    Checkbox(reducedMotion, { reducedMotion = it })
                    Text("Reduce motion")
                }
                Text("Custom finishes are optional. Text and focus lighting are adjusted to remain readable.")
                finishNames.forEach { (role, label) ->
                    OutlinedTextField(
                        value = finishes[role] ?: "",
                        onValueChange = { finishes[role] = it.take(7) },
                        label = { Text(label) },
                        placeholder = { Text("Automatic · #RRGGBB") },
                        isError =
                            finishes[role]?.let { it.isNotBlank() && !it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: false,
                        singleLine = true,
                    )
                }
                TextButton(onClick = { finishes.clear() }) { Text("Reset finishes") }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                onSave(
                    ShelfAppearance(
                        edition,
                        finishes.filterValues {
                            it.isNotBlank()
                        }.mapValues { android.graphics.Color.parseColor(it.value).toLong() },
                        highContrast,
                        reducedMotion,
                    ),
                )
                onDismiss()
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
