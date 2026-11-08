package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeSpine
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlin.math.floor
import kotlin.math.max

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
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
    HomeScreen(
        modifier,
        state.value,
        onGameClick,
        onGameLongClick,
        onOpenCoreSelection,
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@HomeScreen
            }

            permissionsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        { permissionsLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        { viewModel.changeLocalStorageFolder(context) },
    ) // TODO COMPOSE We need to understand what's going to happen here.
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
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painterResource(R.drawable.cartdeck_original_cslot_master), null, Modifier.size(36.dp))
            Text(
                text = stringResource(R.string.cartdeck_library),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        if (state.showNoGamesCard) {
            Button(
                onClick = onSetDirectoryClicked,
                enabled = !state.indexInProgress,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) { Text(stringResource(R.string.cartdeck_add_cartridges)) }
        } else {
            TextButton(onClick = onSetDirectoryClicked, enabled = !state.indexInProgress) {
                Text(stringResource(R.string.cartdeck_add_cartridges))
            }
        }
        if (state.indexInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Text(stringResource(R.string.cartdeck_scanning), modifier = Modifier.padding(horizontal = 16.dp))
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
        state.recentGames.firstOrNull()?.let { recent ->
            TextButton(onClick = { onGameClicked(recent) }, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.cartdeck_continue) + " · " + recent.displayName(), maxLines = 2)
            }
        }
        HomeRow(
            stringResource(R.string.cartdeck_shelf),
            (state.recentGames + state.favoritesGames + state.discoveryGames).distinctBy { it.id },
            onGameClicked,
            onGameLongClick,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRow(
    title: String,
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
) {
    if (games.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 16.dp))
        BoxWithConstraints {
            val columns = max(1, floor((maxWidth.value - 32f) / 72f).toInt())
            val cellWidth = ((maxWidth.value - 32f - (columns - 1) * 4f) / columns).dp
            Column {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    items(games.size, key = { games[it].id }) { index ->
                        val game = games[index]
                        CartridgeSpine(
                            game = game,
                            width = cellWidth,
                            onClick = { onGameClicked(game) },
                            onLongClick = { onGameLongClick(game) },
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant))
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
