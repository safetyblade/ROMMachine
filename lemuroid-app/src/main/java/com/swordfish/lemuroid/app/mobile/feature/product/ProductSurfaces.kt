package com.swordfish.lemuroid.app.mobile.feature.product

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckCuratedDesigns
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeViewport
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.entity.Game

/**
 * UI contracts only. Callers must supply authorised, real repository data and
 * actual operation handlers. Do not register navigation routes until services
 * and permission checks are implemented, integrated and tested.
 */
internal data class ProductAction(val label: String, val detail: String, val invoke: () -> Unit)

@Composable
internal fun ProductActionCard(action: ProductAction) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(action.label, style = MaterialTheme.typography.titleMedium)
            Text(action.detail, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = action.invoke, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Open")
            }
        }
    }
}

internal data class GemEntry(
    val id: String,
    val title: String,
    val system: String,
    val explanation: String,
    val owned: Boolean,
    val ratingLabel: String? = null,
)

@Composable
internal fun GemSurface(
    entries: List<GemEntry>,
    personalRecommendationsAvailable: Boolean,
    browse: ProductAction,
    myRatings: ProductAction,
    onDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("GEM", style = MaterialTheme.typography.headlineMedium)
            Text(if (personalRecommendationsAvailable) "For you" else "Discover games · not personalised")
        }
        item { ProductActionCard(browse) }
        item { ProductActionCard(myRatings) }
        if (entries.isEmpty()) {
            item { Text("No recommendations available yet") }
        }
        items(entries, key = { it.id }) { entry ->
            ProductActionCard(
                ProductAction(
                    entry.title,
                    listOfNotNull(
                        entry.system,
                        if (entry.owned) "In collection" else "Not in collection",
                        entry.ratingLabel,
                        entry.explanation,
                    ).joinToString(" · "),
                ) { onDetails(entry.id) },
            )
        }
    }
}

internal data class SaveInventoryRow(
    val id: String,
    val title: String,
    val system: String,
    val gameSaves: Int,
    val manualStates: Int,
    val resumeStates: Int,
    val cards: Int,
    val summary: String,
)

@Composable
internal fun SafeSaveSurface(
    playerName: String,
    inventory: List<SaveInventoryRow>,
    onBackUp: (() -> Unit)?,
    onRestore: (() -> Unit)?,
    availableActions: List<ProductAction>,
    onDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    unassignedCount: Int = 0,
    onUnassigned: (() -> Unit)? = null,
) {
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    var entryFocusRequested by rememberSaveable { mutableStateOf(false) }
    val restoreFocus = remember { FocusRequester() }
    val television =
        (LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val consoles = inventory.groupBy { it.system }.entries.sortedBy { cartridgeSystemLabel(it.key) }
    val signature = requireNotNull(CartdeckCuratedDesigns.byId(CartdeckCuratedDesigns.SIGNATURE_ID)).colours
    val midnight = Color(signature.getValue("chassis"))
    val electricCyan = Color(signature.getValue("focus"))
    val readableInk = Color(signature.getValue("onChassis"))
    MaterialTheme(
        colorScheme =
            MaterialTheme.colorScheme.copy(
                primary = electricCyan,
                onPrimary = midnight,
                background = midnight,
                onBackground = readableInk,
                surface = midnight,
                onSurface = readableInk,
            ),
    ) {
        LazyColumn(
            modifier.fillMaxSize().background(midnight).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Save Centre", style = MaterialTheme.typography.headlineSmall)
                    Text(playerName, style = MaterialTheme.typography.labelLarge)
                }
            }
            item {
                BoxWithConstraints {
                    if (maxWidth >= 420.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SafeSaveBackupButton(onBackUp, Modifier.weight(1f))
                            SafeSaveRestoreButton(onRestore, Modifier.weight(1f).focusRequester(restoreFocus))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SafeSaveBackupButton(onBackUp, Modifier.fillMaxWidth())
                            SafeSaveRestoreButton(onRestore, Modifier.fillMaxWidth().focusRequester(restoreFocus))
                        }
                    }
                    LaunchedEffect(television, onRestore != null) {
                        if (television && onRestore != null && !entryFocusRequested) {
                            entryFocusRequested = true
                            restoreFocus.requestFocus()
                        }
                    }
                }
            }
            item {
                val metrics =
                    listOf(
                        "Games with saves" to inventory.count { it.gameSaves + it.cards > 0 },
                        "Manual states" to inventory.sumOf { it.manualStates },
                        "Resume states" to inventory.sumOf { it.resumeStates },
                        "Full cards" to inventory.sumOf { it.cards },
                    )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    metrics.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { (label, value) ->
                                ElevatedCard(Modifier.weight(1f)) {
                                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(value.toString(), style = MaterialTheme.typography.titleLarge)
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (onUnassigned != null) {
                item {
                    TextButton(onUnassigned, Modifier.fillMaxWidth().gemFocusTarget()) {
                        Text("Unassigned Backups", modifier = Modifier.weight(1f))
                        Text(unassignedCount.toString())
                    }
                }
            }
            if (inventory.isEmpty()) item { Text("No managed saves found.") }
            consoles.forEach { (system, saves) ->
                item(key = "console-$system") {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { expanded = if (expanded == system) null else system },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).gemFocusTarget(),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(cartridgeSystemLabel(system), style = MaterialTheme.typography.titleSmall)
                                val games = saves.count { it.gameSaves + it.cards > 0 }
                                val states = saves.sumOf { it.manualStates + it.resumeStates }
                                val cards = saves.sumOf { it.cards }
                                Text("Games $games · States $states · Cards $cards")
                            }
                        }
                    }
                }
                if (expanded == system) {
                    items(saves, key = { it.id }) { save ->
                        ProductActionCard(ProductAction(save.title, save.summary) { onDetails(save.id) })
                    }
                }
            }
            items(availableActions, key = { it.label }) { action ->
                TextButton(action.invoke, Modifier.heightIn(min = 48.dp).gemFocusTarget()) { Text(action.label) }
            }
        }
    }
}

@Composable
private fun SafeSaveBackupButton(
    action: (() -> Unit)?,
    modifier: Modifier,
) {
    Button(onClick = {
        action?.invoke()
    }, enabled = action != null, modifier = modifier.heightIn(min = 48.dp).gemFocusTarget()) {
        Text("Back Up Everything")
    }
}

@Composable
private fun SafeSaveRestoreButton(
    action: (() -> Unit)?,
    modifier: Modifier,
) {
    OutlinedButton(onClick = {
        action?.invoke()
    }, enabled = action != null, modifier = modifier.heightIn(min = 48.dp).gemFocusTarget()) {
        Text("Restore Backup")
    }
}

internal data class PlayerChoice(val id: String, val nickname: String, val junior: Boolean)

@Composable
internal fun PlayerChooserSurface(
    players: List<PlayerChoice>,
    activePlayerId: String?,
    onRequestSwitch: (String) -> Unit,
    addPlayer: ProductAction?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Who's playing?", style = MaterialTheme.typography.headlineMedium) }
        items(players, key = { it.id }) { player ->
            ProductActionCard(
                ProductAction(
                    player.nickname,
                    listOfNotNull(
                        if (player.junior) "Junior" else null,
                        if (player.id == activePlayerId) "Current player" else null,
                    ).joinToString(" · "),
                ) { onRequestSwitch(player.id) },
            )
        }
        addPlayer?.let { item { ProductActionCard(it) } }
    }
}

/** One authorised cartridge only. Service owns selection, permission checks, launch and parent gate. */
@Composable
internal fun JuniorPlayroomSurface(
    playerName: String,
    selected: Game?,
    canContinue: Boolean,
    canNavigate: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlay: (Game) -> Unit,
    onRequestGrownUps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().padding(12.dp)) {
        val artWidth = CartridgeViewport.detailArtWidth(maxWidth.value, maxHeight.value * 0.48f).dp
        var drag by remember { mutableFloatStateOf(0f) }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text("My Playroom · $playerName", style = MaterialTheme.typography.titleLarge)
            if (selected == null) {
                Text("Ask a grown-up to add your games")
            } else {
                CartridgeFront(
                    selected,
                    modifier =
                        Modifier.width(artWidth).pointerInput(canNavigate, selected.cartridgeId) {
                            detectHorizontalDragGestures(
                                onDragStart = { drag = 0f },
                                onDragCancel = { drag = 0f },
                                onDragEnd = {
                                    if (canNavigate && drag > 48.dp.toPx()) onPrevious()
                                    if (canNavigate && drag < -48.dp.toPx()) onNext()
                                    drag = 0f
                                },
                            ) { change, delta ->
                                change.consume()
                                drag += delta
                            }
                        },
                )
                Text(selected.displayName(), maxLines = 2)
                if (canNavigate) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onPrevious, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Previous")
                        }
                        OutlinedButton(onClick = onNext, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Next")
                        }
                    }
                }
                Button(onClick = { onPlay(selected) }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(if (canContinue) "Continue" else "Play")
                }
            }
            OutlinedButton(onClick = onRequestGrownUps, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Grown-Ups")
            }
        }
    }
}

/** Only supplied, working capability actions are displayed. No placeholder tiles. */
@Composable
internal fun WorkshopSurface(
    actions: List<ProductAction>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("CARTMOD", style = MaterialTheme.typography.headlineMedium) }
        items(actions, key = { it.label }) { action -> ProductActionCard(action) }
    }
}
