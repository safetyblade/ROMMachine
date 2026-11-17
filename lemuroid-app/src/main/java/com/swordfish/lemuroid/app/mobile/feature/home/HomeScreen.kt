package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfBoard
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfSection
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfViewModel
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    shelfViewModel: ShelfViewModel,
    arrange: Boolean = false,
    onSectionEdit: (ShelfSection) -> Unit = {},
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                viewModel.updatePermissions(applicationContext)
            }
            else -> { }
        }
    }

    val permissionsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (!isGranted) {
                context.displayDetailsSettingsScreen()
            }
        }

    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState())
    val shelf = shelfViewModel.state.collectAsState().value
    LaunchedEffect(state.value.libraryGames) { shelfViewModel.reconcile(state.value.libraryGames) }
    if (shelf.layout == null) {
        Column(modifier) {
            if (shelf.error == null) {
                LinearProgressIndicator()
            } else {
                Text(shelf.error)
                TextButton(onClick = { shelfViewModel.reload() }) { Text("Retry saved shelf") }
            }
        }
        return
    }

    HomeScreen(
        modifier,
        state.value,
        onGameClick,
        onGameLongClick,
        onOpenCoreSelection,
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionsLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS,
                )
            }
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
        shelfLayout = shelf.layout,
        arrange = arrange,
        onEdit = { shelfViewModel.edit(transform = it) },
        onSectionEdit = onSectionEdit,
        tools = {
            shelf.error?.let {
                Text(it)
                TextButton(onClick = { shelfViewModel.reload() }) { Text("Retry saved shelf") }
            }
        },
    )
}

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onEnableNotificationsClicked: () -> Unit,
    onEnableMicrophoneClicked: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
    shelfLayout: ShelfLayout? = null,
    arrange: Boolean = false,
    onEdit: ((ShelfLayout) -> ShelfLayout) -> Unit = {},
    onSectionEdit: (ShelfSection) -> Unit = {},
    tools: @Composable () -> Unit = {},
) {
    val games =
        state.libraryGames.ifEmpty {
            (state.recentGames + state.favoritesGames + state.discoveryGames).distinctBy { it.id }
        }
    val layout =
        shelfLayout ?: ShelfLayout().reconcile(
            games.associate {
                shelfGameKey(it.systemId, it.fileUri) to it.displayName()
            },
        )
    val shelfStates = rememberSaveableStateHolder()
    val shortScreen = LocalConfiguration.current.screenHeightDp < 480
    UnifiedShelfChrome(modifier, games, arrange) { query, shelfModifier ->
        val projection = projectShelf(layout, games, query)
        PagedShelfSections(projection.layout, shelfModifier, enabled = !arrange && !query.active) {
                pageLayout, pageModifier, pageKey ->
            shelfStates.SaveableStateProvider(if (query.active) "filtered" else pageKey) {
                ShelfBoard(
                    pageLayout,
                    projection.games,
                    onGameClicked,
                    onGameLongClick,
                    pageModifier,
                    arrange,
                    onEdit,
                    onSectionEdit,
                    artworkRevision = state.artworkRevision,
                    showSectionHeadings = pageKey == "all" || query.active,
                    bottomContentPadding = if (arrange) 8.dp else 68.dp,
                    header = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.showNoGamesCard) {
                                Button(
                                    onClick = onSetDirectoryClicked,
                                    enabled = !state.indexInProgress,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                ) { Text(stringResource(R.string.cartdeck_add_cartridges)) }
                            } else if (pageKey == "all" && !query.active && !shortScreen) {
                                TextButton(onClick = onSetDirectoryClicked, enabled = !state.indexInProgress) {
                                    Text(stringResource(R.string.cartdeck_add_cartridges))
                                }
                            }
                            if (state.indexInProgress) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                                Text(
                                    stringResource(R.string.cartdeck_scanning),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            AnimatedVisibility(state.showNoNotificationPermissionCard) {
                                HomeNotification(
                                    titleId = R.string.home_notification_title,
                                    messageId = R.string.home_notification_message,
                                    actionId = R.string.home_notification_action,
                                    onAction = onEnableNotificationsClicked,
                                )
                            }
                            AnimatedVisibility(state.showNoMicrophonePermissionCard) {
                                HomeNotification(
                                    titleId = R.string.home_microphone_title,
                                    messageId = R.string.home_microphone_message,
                                    actionId = R.string.home_microphone_action,
                                    onAction = onEnableMicrophoneClicked,
                                )
                            }
                            AnimatedVisibility(state.showDesmumeDeprecatedCard) {
                                HomeNotification(
                                    titleId = R.string.home_notification_desmume_deprecated_title,
                                    messageId = R.string.home_notification_desmume_deprecated_message,
                                    actionId = R.string.home_notification_desmume_deprecated_action,
                                    onAction = onOpenCoreSelection,
                                )
                            }
                            val firstCollection =
                                pageLayout.sections.firstOrNull()?.id == layout.sections.firstOrNull()?.id
                            if (!query.active && !arrange && !shortScreen && (pageKey == "all" || firstCollection)) {
                                state.recentGames.firstOrNull()?.let { recent ->
                                    TextButton(
                                        onClick = { onGameClicked(recent) },
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.cartdeck_continue) + " · " + recent.displayName(),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                            if (query.active && projection.games.isEmpty()) {
                                Text("No matching cartridges", modifier = Modifier.padding(16.dp))
                            }
                            if (!query.active && !arrange && pageLayout.sections.all { it.cells.isEmpty() }) {
                                TextButton(onClick = onSetDirectoryClicked, enabled = !state.indexInProgress) {
                                    Text("Empty collection · Add cartridges")
                                }
                            }
                            tools()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeNotification(
    titleId: Int,
    messageId: Int,
    actionId: Int,
    enabled: Boolean = true,
    onAction: () -> Unit = { },
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(titleId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(messageId),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                modifier = Modifier.align(Alignment.End),
                onClick = onAction,
                enabled = enabled,
            ) {
                Text(stringResource(id = actionId))
            }
        }
    }
}
