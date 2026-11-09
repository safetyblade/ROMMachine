package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.room.InvalidationTracker
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.app.shared.game.EmulatorCompatibilityPreferences
import com.swordfish.lemuroid.app.shared.game.EmulatorModernAssetsProvisioner
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold

/** Observe the existing record by ID, including asynchronous cover changes. No new storage. */
@Composable
fun CartridgeDetailHost(
    gameId: Int,
    database: RetrogradeDatabase,
    onDismiss: () -> Unit,
    onOpen: (Game) -> Unit,
    onChangeCover: (Game, String) -> Unit,
    onResetCover: (Game) -> Unit,
    sourceBounds: androidx.compose.ui.geometry.Rect? = null,
) {
    val record =
        remember(gameId, database) {
            callbackFlow {
                val observer =
                    object : InvalidationTracker.Observer("games") {
                        override fun onInvalidated(tables: Set<String>) {
                            trySend(Unit)
                        }
                    }
                database.invalidationTracker.addObserver(observer)
                trySend(Unit)
                awaitClose { database.invalidationTracker.removeObserver(observer) }
            }.conflate().runningFold(0) { revision, _ -> revision + 1 }
                .map { revision -> revision to database.gameDao().selectById(gameId) }.flowOn(Dispatchers.IO)
        }.collectAsState(initial = 0 to null)
    val game = record.value.second
    CartridgePickup(
        gameId,
        game,
        record.value.first > 0,
        sourceBounds,
        onDismiss,
        artworkRevision = record.value.first,
    ) { front, close ->
        if (game == null) {
            Column(Modifier.padding(24.dp)) {
                if (record.value.first == 0) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.cartdeck_cartridge_unavailable))
                }
                TextButton(onClick = close) { Text(stringResource(R.string.cartdeck_back_to_shelf)) }
            }
        } else {
            CartridgeDetail(game, close, onOpen, onChangeCover, onResetCover, record.value.first, front)
        }
    }
}

@Composable
fun CartridgeDetail(
    game: Game,
    onDismiss: () -> Unit,
    onOpen: (Game) -> Unit,
    onChangeCover: (Game, String) -> Unit,
    onResetCover: (Game) -> Unit,
    artworkRevision: Int = 0,
    frontModifier: Modifier = Modifier,
) {
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) onChangeCover(game, uri.toString())
        }
    // Consumed before dispatch, so rapid taps cannot launch twice. Reopening permits a retry.
    var opening by remember(game.id) { mutableStateOf(false) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.Start)) {
            Text(stringResource(R.string.cartdeck_back_to_shelf))
        }
        Text(game.displayName(), style = MaterialTheme.typography.headlineSmall)
        Text(cartridgeSystemLabel(game.systemId), style = MaterialTheme.typography.titleMedium)
        CartridgeFront(game, modifier = frontModifier, artworkRevision = artworkRevision)
        CartridgeEmulatorChoice(game)
        Button(
            onClick = {
                if (!opening) {
                    opening = true
                    onOpen(game)
                }
            },
            enabled = !opening,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.cartdeck_open)) }
        OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.cartdeck_change_cover))
        }
        TextButton(
            onClick = { onResetCover(game) },
            enabled = game.customCoverUri != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.cartdeck_reset_cover)) }
    }
}

@Composable
private fun CartridgeEmulatorChoice(game: Game) {
    val systemId = game.systemId
    if (systemId != SystemID.PSX && systemId != SystemID.PSP) return

    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { EmulatorCompatibilityPreferences(context) }
    var assetsReady by remember(game.id) { mutableStateOf(false) }
    LaunchedEffect(context, game.id) {
        assetsReady = EmulatorModernAssetsProvisioner.installIfBundled(context)
    }
    // Only expose a switch when the candidate is actually packaged and the
    // matching PSP assets have already been installed.
    if (!remember(game.id, assetsReady) { preferences.available(systemId) }) return

    var updated by remember(game.id) {
        mutableStateOf(preferences.isUpdatedSelectedForGame(systemId, game.id))
    }
    Text(
        if (updated) "Using updated engine for this game" else "Using previous working engine",
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButton(
        onClick = {
            if (preferences.setGameEngine(systemId, game.id, !updated)) {
                updated = !updated
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (updated) "Use previous working engine" else "Try updated engine") }
    TextButton(
        onClick = {
            if (preferences.restoreGameDefault(systemId, game.id)) {
                updated = preferences.isUpdatedSelectedForGame(systemId, game.id)
            }
        },
    ) { Text("Use system default") }
    if (preferences.lastFailedCandidate(systemId, game.id) != null && !updated) {
        Text(
            "The updated engine could not start. The previous working engine is selected.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
