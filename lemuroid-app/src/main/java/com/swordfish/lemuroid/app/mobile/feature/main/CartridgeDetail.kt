package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.room.InvalidationTracker
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
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
    // Dialog also owns Back while the database record is loading.
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            if (game == null) {
                Column(Modifier.padding(24.dp)) {
                    if (record.value.first == 0) {
                        CircularProgressIndicator()
                    } else {
                        Text(stringResource(R.string.cartdeck_cartridge_unavailable))
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cartdeck_back_to_shelf)) }
                }
            } else {
                CartridgeDetail(game, onDismiss, onOpen, onChangeCover, onResetCover, record.value.first)
            }
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
        CartridgeFront(game, artworkRevision = artworkRevision)
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
