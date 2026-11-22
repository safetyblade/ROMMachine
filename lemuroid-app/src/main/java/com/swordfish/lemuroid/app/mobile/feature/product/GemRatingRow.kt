package com.swordfish.lemuroid.app.mobile.feature.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.gemrating.GemRatingLocalStore
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Uses the shared BR component system; never writes to Game or SaveBinding. */
@Composable
internal fun GemRatingRow(
    game: Game,
    deckFit: Boolean = false,
    compact: Boolean = false,
) {
    GemRatingRow(
        cartridgeId = game.cartridgeId,
        displayTitle = game.displayName(),
        platformId = game.systemId,
        deckFit = deckFit,
        compact = compact,
    )
}

@Composable
internal fun GemRatingRow(
    cartridgeId: String,
    displayTitle: String,
    platformId: String?,
    onRatingChanged: () -> Unit = {},
    catalogueReleaseId: String? = null,
    deckFit: Boolean = false,
    compact: Boolean = false,
) {
    val context = LocalContext.current.applicationContext
    val label = if (deckFit) "My DeckFit" else "My GEMRating"
    val maximum = if (deckFit) 5 else 10
    val scope = rememberCoroutineScope()
    var store by remember(cartridgeId) { mutableStateOf<GemRatingLocalStore?>(null) }
    var score by remember(cartridgeId) { mutableStateOf<Int?>(null) }
    var draft by remember(cartridgeId) { mutableStateOf<Int?>(null) }
    var editing by remember(cartridgeId) { mutableStateOf(false) }
    var busy by remember(cartridgeId) { mutableStateOf(false) }
    var error by remember(cartridgeId) { mutableStateOf<String?>(null) }

    LaunchedEffect(cartridgeId) {
        val result =
            withContext(Dispatchers.IO) {
                runCatching {
                    val local = GemRatingLocalStore.open(context)
                    val record =
                        local.repository.snapshot(local.session).records.firstOrNull { it.cartridgeId == cartridgeId }
                    local to (if (deckFit) record?.deckFit else record?.score)
                }
            }
        result.onSuccess {
            store = it.first
            score = it.second
        }.onFailure { error = "Rating unavailable. Existing data retained." }
    }

    fun commit(value: Int?) {
        val local = store ?: return
        if (busy) return
        busy = true
        scope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        local.repository.set(
                            expected = local.session,
                            cartridgeId = cartridgeId,
                            score = value,
                            displayTitle = displayTitle.take(512).ifBlank { null },
                            platformId = platformId?.take(64)?.ifBlank { null },
                            deckFitOnly = deckFit,
                            catalogueReleaseId = catalogueReleaseId,
                        )
                    }
                }
            if (result.isSuccess) {
                score = value
                error = null
                editing = false
                onRatingChanged()
            } else {
                error = "Could not save rating. Reopen to check the stored value."
            }
            busy = false
        }
    }

    val openEditor = {
        draft = score
        error = null
        editing = true
    }
    if (compact) {
        TextButton(
            onClick = openEditor,
            enabled = store != null && !busy,
            modifier = Modifier.gemFocusTarget(),
        ) {
            Icon(
                painterResource(
                    if (deckFit) R.drawable.cartdeck_feature_deckfit else R.drawable.cartdeck_feature_gemrating,
                ),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(" ${if (deckFit) "My DeckFit" else "GEMRating"} ${score?.let { "$it/$maximum" } ?: "—"}")
        }
    } else {
        CartdeckMenuRow(
            title = { Text(label) },
            leading = {
                Icon(
                    painter =
                        painterResource(
                            if (deckFit) R.drawable.cartdeck_feature_deckfit else R.drawable.cartdeck_feature_gemrating,
                        ),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            supporting = {
                Text(error ?: if (store == null) "Loading…" else score?.let { "$it/$maximum" } ?: "Unrated")
            },
            trailing = { Text(if (score == null) "Rate" else "Edit") },
            enabled = store != null && !busy,
            onClick = openEditor,
        )
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { if (!busy) editing = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(label) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(displayTitle, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text(
                        if (deckFit) {
                            "How naturally can you play using CARTDECK's on-screen controls?"
                        } else {
                            "How much do you enjoy it?"
                        },
                    )
                    if (deckFit) {
                        Text("1 · Very difficult to handle   3 · Playable with compromises   5 · Comfortable")
                        Text(
                            "No external controller or special setup. Consider launch, controls, display, audio and saves.",
                        )
                    }
                    for (range in (1..maximum).chunked(if (deckFit) 3 else 4)) {
                        Row(Modifier.fillMaxWidth()) {
                            for (value in range) {
                                FilterChip(
                                    selected = draft == value,
                                    onClick = { draft = value },
                                    enabled = !busy,
                                    modifier =
                                        Modifier.weight(1f).gemFocusTarget().semantics { selected = draft == value },
                                    label = { Text(value.toString()) },
                                )
                            }
                        }
                    }
                    Text(draft?.let { "$it/$maximum" } ?: "Unrated")
                    if (score != null) {
                        TextButton(onClick = { commit(null) }, enabled = !busy) { Text("Clear rating") }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { commit(draft) }, enabled = draft != null && !busy) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }, enabled = !busy) { Text("Cancel") }
            },
        )
    }
}
