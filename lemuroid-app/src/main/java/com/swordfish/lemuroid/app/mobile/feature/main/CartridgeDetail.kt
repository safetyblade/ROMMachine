package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.InvalidationTracker
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.product.GemRatingRow
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeViewport
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.EngineSelectionControls
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.lib.core.NativeCoreProvisioner
import com.swordfish.lemuroid.lib.core.VersionedCoreStore
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
    onUseOnlineCover: (suspend (Game, Bitmap) -> Boolean)? = null,
    onOpenIntelligence: ((Game) -> Unit)? = null,
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
    ) { front, close, playFocus ->
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
            CartridgeDetail(
                game,
                close,
                onOpen,
                onChangeCover,
                onResetCover,
                record.value.first,
                front,
                onUseOnlineCover = onUseOnlineCover,
                onOpenIntelligence = onOpenIntelligence,
                playFocus = playFocus,
            )
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
    onUseOnlineCover: (suspend (Game, Bitmap) -> Boolean)? = null,
    onOpenIntelligence: ((Game) -> Unit)? = null,
    playFocus: FocusRequester? = null,
) {
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) onChangeCover(game, uri.toString())
        }
    // Consumed before dispatch, so rapid taps cannot launch twice. Reopening permits a retry.
    var opening by remember(game.id) { mutableStateOf(false) }
    var setup by rememberSaveable(game.id) { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier.testTag("cartridge_front").fillMaxWidth().padding(12.dp),
    ) {
        val landscape = maxWidth > maxHeight
        val artWidth =
            CartridgeViewport.detailArtWidth(
                if (landscape) maxWidth.value * 0.44f else maxWidth.value,
                if (landscape) maxHeight.value else (maxHeight.value - 220f).coerceAtLeast(0f),
            ).dp
        val heading: @Composable () -> Unit = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cartdeck_back_to_shelf))
            }
            Text(
                game.displayName(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(cartridgeSystemLabel(game.systemId), style = MaterialTheme.typography.labelMedium)
        }
        val actions: @Composable () -> Unit = {
            Button(
                onClick = {
                    if (!opening) {
                        opening = true
                        onOpen(game)
                    }
                },
                enabled = !opening,
                modifier =
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .then(playFocus?.let { Modifier.focusRequester(it) } ?: Modifier),
            ) { Text(stringResource(R.string.cartdeck_open)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = { setup = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Setup") }
                if (onOpenIntelligence != null) {
                    TextButton(
                        onClick = { onOpenIntelligence(game) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Game Intelligence") }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                GemRatingRow(game, compact = true)
                GemRatingRow(game, deckFit = true, compact = true)
            }
        }
        if (landscape) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CartridgeFront(
                    game,
                    modifier = frontModifier.then(Modifier.width(artWidth)),
                    artworkRevision = artworkRevision,
                )
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    heading()
                    actions()
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                heading()
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val fit = CartridgeViewport.detailArtWidth(maxWidth.value, maxHeight.value).dp
                    CartridgeFront(
                        game,
                        modifier = frontModifier.then(Modifier.width(fit)),
                        artworkRevision = artworkRevision,
                    )
                }
                actions()
            }
        }
    }
    if (setup) {
        CartridgeSetupSheet(
            game = game,
            onDismiss = { setup = false },
            onChangeCover = { picker.launch("image/*") },
            onResetCover = { onResetCover(game) },
            emulator = { CartridgeEmulatorChoice(game) },
            onUseOnlineCover = onUseOnlineCover,
        )
    }
}

@Composable
private fun CartridgeEmulatorChoice(game: Game) {
    val systemId = SystemID.values().firstOrNull { it.dbname == game.systemId } ?: return
    val system = GameSystem.findById(game.systemId)
    EngineSelectionControls(system, game, showFallbackPolicy = false)
    if (systemId in setOf(SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.PLAYSTATION_2, SystemID.WII)) {
        NativeEngineChoice(systemId, game)
    }
}

@Composable
private fun NativeEngineChoice(
    systemId: SystemID,
    game: Game,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val core =
        when (systemId) {
            SystemID.DREAMCAST -> CoreID.FLYCAST_MODERN
            SystemID.GAMECUBE, SystemID.WII -> CoreID.DOLPHIN_MODERN
            SystemID.PLAYSTATION_2 -> CoreID.PLAY_MODERN
            else -> return
        }
    var busy by remember(systemId) { mutableStateOf(false) }
    var message by remember(systemId) { mutableStateOf<String?>(null) }
    var previousAvailable by remember(systemId) { mutableStateOf(false) }
    LaunchedEffect(systemId, busy) {
        previousAvailable =
            withContext(Dispatchers.IO) {
                VersionedCoreStore(File(context.filesDir, "native-engine-versions")).canRollback(core.coreName)
            }
    }
    if (previousAvailable) {
        OutlinedButton(
            enabled = !busy,
            onClick = {
                busy = true
                scope.launch {
                    message =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                if (NativeCoreProvisioner.choosePrevious(context, core)) {
                                    EngineSelectionPreferences(context).setPreferred(
                                        GameSystem.findById(game.systemId), core.coreName, game,
                                    )
                                    "Previous engine selected. Game saves are retained."
                                } else {
                                    "No previous engine is installed."
                                }
                            }.getOrElse { "The previous engine is unavailable." }
                        }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Use previous version") }
    }
    TextButton(
        enabled = !busy,
        onClick = {
            busy = true
            scope.launch {
                message =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            NativeCoreProvisioner.chooseCurrent(context, core)
                            EngineSelectionPreferences(context).setPreferred(
                                GameSystem.findById(game.systemId), core.coreName, game,
                            )
                            "Current engine selected. Game saves are retained."
                        }.getOrElse { "The current engine is unavailable." }
                    }
                busy = false
            }
        },
    ) { Text("Use current version / retry") }
    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}
