package com.swordfish.lemuroid.app.mobile.feature.shelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
        }
        Row {
            TextButton(onClick = onAppearance, enabled = available) {
                Column {
                    Text("CARTDECK Appearance")
                    Text("Console and cartridge shelf", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                }
            }
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
