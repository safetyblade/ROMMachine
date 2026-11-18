package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.game.ArcControlMode
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameTexts
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidSmallGameImage
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.pc.PcCartridgeStore
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainGameContextActions(
    selectedGameState: MutableState<Game?>,
    shortcutSupported: Boolean,
    onGamePlay: (Game) -> Unit,
    onGameRestart: (Game) -> Unit,
    onFavoriteToggle: (Game, Boolean) -> Unit,
    onCreateShortcut: (Game) -> Unit,
    onSetCustomName: (Game, String) -> Unit,
    onClearCustomName: (Game) -> Unit,
    onSetCustomCoverUri: (Game, String) -> Unit,
    onClearCustomCoverUri: (Game) -> Unit,
    onManagePcInstallation: ((Game) -> Unit)? = null,
    onUseOnlineCover: (suspend (Game, Bitmap) -> Boolean)? = null,
) {
    val modalSheetState = rememberModalBottomSheetState(true)
    val selectedGame = selectedGameState.value

    LaunchedEffect(selectedGame) {
        if (selectedGame != null) {
            modalSheetState.show()
        } else {
            modalSheetState.hide()
        }
    }

    if (selectedGame != null) {
        ModalBottomSheet(
            sheetState = modalSheetState,
            onDismissRequest = { selectedGameState.value = null },
        ) {
            ContextActionContent(
                selectedGame = selectedGame,
                onGamePlay = onGamePlay,
                selectedGameState = selectedGameState,
                onGameRestart = onGameRestart,
                onFavoriteToggle = onFavoriteToggle,
                shortcutSupported = shortcutSupported,
                onCreateShortcut = onCreateShortcut,
                onSetCustomName = onSetCustomName,
                onClearCustomName = onClearCustomName,
                onSetCustomCoverUri = onSetCustomCoverUri,
                onClearCustomCoverUri = onClearCustomCoverUri,
                onManagePcInstallation = onManagePcInstallation,
                onUseOnlineCover = onUseOnlineCover,
            )
        }
    }
}

@Composable
private fun ContextActionContent(
    selectedGame: Game,
    onGamePlay: (Game) -> Unit,
    selectedGameState: MutableState<Game?>,
    onGameRestart: (Game) -> Unit,
    onFavoriteToggle: (Game, Boolean) -> Unit,
    shortcutSupported: Boolean,
    onCreateShortcut: (Game) -> Unit,
    onSetCustomName: (Game, String) -> Unit,
    onClearCustomName: (Game) -> Unit,
    onSetCustomCoverUri: (Game, String) -> Unit,
    onClearCustomCoverUri: (Game) -> Unit,
    onManagePcInstallation: ((Game) -> Unit)?,
    onUseOnlineCover: (suspend (Game, Bitmap) -> Boolean)?,
) {
    val context = LocalContext.current
    val managedPc =
        remember(selectedGame.fileUri, selectedGame.cartridgeId) {
            runCatching {
                val uri = Uri.parse(selectedGame.fileUri)
                if (uri.scheme != "file") {
                    false
                } else {
                    val file = uri.path?.let(::File)
                    val managed = file?.let { PcCartridgeStore(context.filesDir).resolve(it) }
                    managed?.id?.toString() == selectedGame.cartridgeId
                }
            }.getOrDefault(false)
        }
    var showCustomNameDialog by remember { mutableStateOf(false) }
    var searchingCovers by rememberSaveable(selectedGame.id) { mutableStateOf(false) }
    var showHoldControlsDialog by remember(selectedGame.fileUri) { mutableStateOf(false) }
    val arcStore = remember(context) { GripArcProfileStore(context) }
    var selectedArcMode by remember(selectedGame.fileUri) { mutableStateOf(arcStore.read(selectedGame).mode) }
    var arcSaveFailed by remember(selectedGame.fileUri) { mutableStateOf(false) }

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to persist read URI permission for custom cover")
                }
                onSetCustomCoverUri(selectedGame, uri.toString())
                selectedGameState.value = null
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Bottom)),
    ) {
        ContextActionHeader(game = selectedGame)
        Divider()
        ContextActionEntry(
            label = stringResource(id = R.string.game_context_menu_resume),
            icon = R.drawable.cartdeck_action_play,
            onClick = {
                onGamePlay(selectedGame)
                selectedGameState.value = null
            },
        )
        ContextActionEntry(
            label = stringResource(id = R.string.game_context_menu_restart),
            icon = R.drawable.cartdeck_action_refresh,
            onClick = {
                onGameRestart(selectedGame)
                selectedGameState.value = null
            },
        )
        ContextActionEntry(
            label = "Hold Controls · " + selectedArcMode.label,
            icon = R.drawable.cartdeck_action_controller,
            onClick = { showHoldControlsDialog = true },
        )

        if (managedPc && onManagePcInstallation != null) {
            ContextActionEntry(
                label = stringResource(R.string.cartdeck_manage_pc_installation),
                icon = R.drawable.cartdeck_action_controller,
                onClick = {
                    selectedGameState.value = null
                    onManagePcInstallation(selectedGame)
                },
            )
        }

        if (selectedGame.isFavorite) {
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_remove_from_favorites),
                icon = R.drawable.cartdeck_action_favourite,
                onClick = {
                    onFavoriteToggle(selectedGame, false)
                    selectedGameState.value = null
                },
            )
        } else {
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_add_to_favorites),
                icon = R.drawable.cartdeck_action_favourite_selected,
                onClick = {
                    onFavoriteToggle(selectedGame, true)
                    selectedGameState.value = null
                },
            )
        }

        if (shortcutSupported) {
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_create_shortcut),
                icon = R.drawable.cartdeck_action_controller,
                onClick = {
                    onCreateShortcut(selectedGame)
                    selectedGameState.value = null
                },
            )
        }

        if (selectedGame.customName != null) {
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_remove_custom_name),
                icon = R.drawable.cartdeck_action_close,
                onClick = {
                    onClearCustomName(selectedGame)
                    selectedGameState.value = null
                },
            )
        } else {
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_add_custom_name),
                icon = R.drawable.cartdeck_action_edit,
                onClick = {
                    showCustomNameDialog = true
                },
            )
        }

        ContextActionEntry(
            label = "Search for covers",
            icon = R.drawable.cartdeck_action_cover,
            onClick = {
                if (onUseOnlineCover != null) searchingCovers = true else imagePickerLauncher.launch("image/*")
            },
        )
        if (selectedGame.customCoverUri != null) {
            ContextActionEntry(
                label = "Upload / replace image",
                icon = R.drawable.cartdeck_action_cover,
                onClick = { imagePickerLauncher.launch("image/*") },
            )
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_remove_custom_artwork),
                icon = R.drawable.cartdeck_action_close,
                onClick = {
                    onClearCustomCoverUri(selectedGame)
                    selectedGameState.value = null
                },
            )
        } else {
            ContextActionEntry(
                label = stringResource(id = R.string.game_context_menu_add_custom_artwork),
                icon = R.drawable.cartdeck_action_cover,
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
            )
        }
    }

    if (searchingCovers && onUseOnlineCover != null) {
        CoverArtworkPicker(
            game = selectedGame,
            onUse = { game, image ->
                onUseOnlineCover(game, image).also { saved ->
                    if (saved) selectedGameState.value = null
                }
            },
            onDismiss = { searchingCovers = false },
            onLocal = {
                searchingCovers = false
                imagePickerLauncher.launch("image/*")
            },
        )
    }

    if (showHoldControlsDialog) {
        AlertDialog(
            onDismissRequest = { showHoldControlsDialog = false },
            title = { Text("Hold Controls") },
            text = {
                Column {
                    Text("Choose one control layout for this cartridge.")
                    ArcControlMode.values().forEach { mode ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val current = arcStore.read(selectedGame)
                                        if (arcStore.save(selectedGame, current.copy(mode = mode))) {
                                            selectedArcMode = mode
                                            arcSaveFailed = false
                                            showHoldControlsDialog = false
                                        } else {
                                            arcSaveFailed = true
                                        }
                                    },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selectedArcMode == mode, onClick = null)
                            Text(mode.label)
                        }
                    }
                    if (arcSaveFailed) Text("The selection could not be saved.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHoldControlsDialog = false }) { Text("Close") }
            },
        )
    }

    if (showCustomNameDialog) {
        CustomNameDialog(
            initialName = selectedGame.fileName,
            onDismiss = { showCustomNameDialog = false },
            onConfirm = { newName ->
                showCustomNameDialog = false
                onSetCustomName(selectedGame, newName)
                selectedGameState.value = null
            },
        )
    }
}

@Composable
private fun ContextActionHeader(game: Game) {
    Row(
        modifier =
            Modifier.padding(
                start = 16.dp,
                top = 8.dp,
                bottom = 8.dp,
                end = 16.dp,
            ),
    ) {
        LemuroidSmallGameImage(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(40.dp)
                    .align(Alignment.CenterVertically),
            game = game,
        )
        LemuroidGameTexts(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            game = game,
        )
    }
}

@Composable
private fun ContextActionEntry(
    modifier: Modifier = Modifier,
    label: String,
    icon: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(start = 16.dp),
            painter = painterResource(icon),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = label,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FakeScrim(modalSheetState: SheetState) {
    AnimatedVisibility(
        visible = modalSheetState.targetValue != SheetValue.Hidden,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BottomSheetDefaults.ScrimColor),
        )
    }
}
