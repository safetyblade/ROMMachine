package com.swordfish.lemuroid.app.mobile.feature.product

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Ratings are player-entered only. Persistence and identity belong to the caller. */
@Composable
internal fun PlayerRatingEditor(
    title: String,
    label: String,
    maximum: Int,
    current: Int?,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    require(maximum >= 2)
    var draft by remember(title, label, current) { mutableStateOf(current ?: 1) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                Text(title)
                Text(if (current == null) "Not rated yet" else "Current rating: $current / $maximum")
                Text("$draft / $maximum", style = MaterialTheme.typography.headlineMedium)
                Slider(
                    value = draft.toFloat(),
                    onValueChange = { draft = it.roundToInt().coerceIn(1, maximum) },
                    valueRange = 1f..maximum.toFloat(),
                    steps = maximum - 2,
                )
                TextButton(onClick = { onSave(null) }, enabled = current != null) { Text("Clear rating") }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

internal data class SaveOperationPreview(
    val operation: String,
    val scope: String,
    val verifiedItems: Int,
    val exclusions: List<String>,
    val conflicts: List<String>,
    val canProceed: Boolean,
)

/** Preview must be based on the actual service manifest, never inferred by this surface. */
@Composable
internal fun SaveOperationReview(
    preview: SaveOperationPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(preview.operation) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(preview.scope)
                Text("Verified items: ${preview.verifiedItems}")
                if (preview.exclusions.isNotEmpty()) {
                    Text("Excluded", style = MaterialTheme.typography.titleSmall)
                    preview.exclusions.forEach { Text(it) }
                }
                if (preview.conflicts.isNotEmpty()) {
                    Text("Conflicts require resolution", style = MaterialTheme.typography.titleSmall)
                    preview.conflicts.forEach { Text(it) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = preview.canProceed && preview.conflicts.isEmpty(),
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

/** No unauthenticated route may compose this surface. The manager service owns the gate. */
@Composable
internal fun GrownUpsSurface(
    actions: List<ProductAction>,
    onReturnToPlayroom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Grown-Ups", style = MaterialTheme.typography.headlineMedium)
        actions.forEach { action -> ProductActionCard(action) }
        OutlinedButton(onClick = onReturnToPlayroom, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Return to My Playroom")
        }
    }
}

internal data class SourceDraft(val name: String = "", val url: String = "", val category: String = "")

/** Draft editor: persistence, URL validation and navigation are delegated to CARTMOD services. */
@Composable
internal fun SourceEditor(
    initial: SourceDraft,
    validationError: String?,
    onRequestSave: (SourceDraft) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial.name) }
    var url by remember(initial) { mutableStateOf(initial.url) }
    var category by remember(initial) { mutableStateOf(initial.category) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("My Sources") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add a site you choose. CARTMOD does not automatically download games.")
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, label = { Text("HTTPS address") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Platform or type") }, singleLine = true)
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRequestSave(SourceDraft(name.trim(), url.trim(), category.trim())) },
                enabled = name.isNotBlank() && url.startsWith("https://", ignoreCase = true),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

internal data class ModPreparationPreview(
    val baseTitle: String,
    val editionTitle: String,
    val validationSummary: String,
    val storageSummary: String,
    val eligible: Boolean,
)

/** The actual patch transaction must independently verify input and output hashes. */
@Composable
internal fun ModPreparationReview(
    preview: ModPreparationPreview,
    onCreateSeparateEdition: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Patch Studio", style = MaterialTheme.typography.headlineMedium)
        Text("Base: ${preview.baseTitle}")
        Text("New edition: ${preview.editionTitle}")
        Text(preview.validationSummary)
        Text(preview.storageSummary)
        Text("The original cartridge and saved progress stay unchanged.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancel") }
            Button(onClick = onCreateSeparateEdition, enabled = preview.eligible) { Text("Create edition") }
        }
    }
}
