package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfMaterials
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import com.swordfish.lemuroid.R

@Composable
fun MainTopBar(
    currentRoute: MainRoute,
    navController: NavHostController,
    onHelpPressed: () -> Unit,
    onUpdateQueryString: (String) -> Unit,
    mainUIState: MainViewModel.UiState,
    arrangingShelf: Boolean = false,
    shelfAvailable: Boolean = false,
    onToggleArrangeShelf: () -> Unit = {},
    onShelfAppearance: () -> Unit = {},
) {
    Column {
        LemuroidTopAppBar(
            route = currentRoute,
            navController = navController,
            mainUIState = mainUIState,
            onHelpPressed = onHelpPressed,
            onUpdateQueryString = onUpdateQueryString,
            arrangingShelf = arrangingShelf,
            shelfAvailable = shelfAvailable,
            onToggleArrangeShelf = onToggleArrangeShelf,
            onShelfAppearance = onShelfAppearance,
        )

        AnimatedVisibility(mainUIState.operationInProgress) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LemuroidTopAppBar(
    route: MainRoute,
    navController: NavController,
    mainUIState: MainViewModel.UiState,
    onHelpPressed: () -> Unit,
    onUpdateQueryString: (String) -> Unit,
    arrangingShelf: Boolean = false,
    shelfAvailable: Boolean = false,
    onToggleArrangeShelf: () -> Unit = {},
    onShelfAppearance: () -> Unit = {},
) {
    val context = LocalContext.current
    val materials = LocalShelfMaterials.current
    val topBarColor = materials.roomBackdrop

    TopAppBar(
        title = {
            when (route) {
                MainRoute.SEARCH ->
                    LemuroidSearchView(
                        mainUIState = mainUIState,
                        onUpdateQueryString = onUpdateQueryString,
                    )
                MainRoute.HOME -> CartdeckHomeTitle()
                else -> Text(text = stringResource(route.titleId))
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                scrolledContainerColor = topBarColor,
                containerColor = topBarColor,
                titleContentColor = materials.sectionText,
                navigationIconContentColor = materials.sectionText,
                actionIconContentColor = materials.sectionText,
            ),
        navigationIcon = {
            AnimatedVisibility(
                visible = route.parent != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(id = R.string.back),
                    )
                }
            }
        },
        actions = {
            LemuroidTopBarActions(
                route = route,
                navController = navController,
                context = context,
                saveSyncEnabled = mainUIState.saveSyncEnabled,
                onHelpPressed = onHelpPressed,
                operationsInProgress = mainUIState.operationInProgress,
                arrangingShelf = arrangingShelf,
                shelfAvailable = shelfAvailable,
                onToggleArrangeShelf = onToggleArrangeShelf,
                onShelfAppearance = onShelfAppearance,
            )
        },
    )
}

@Composable
private fun CartdeckHomeTitle() {
    val pink = LocalShelfAppearance.current.edition == "pink"
    val materials = LocalShelfMaterials.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(
                if (pink) R.drawable.cartdeck_pink_cslot_master else R.drawable.cartdeck_original_cslot_master,
            ),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Column {
            Text(
                text = stringResource(R.string.cartdeck_header_title),
                style = MaterialTheme.typography.titleMedium,
                color = materials.sectionText,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.cartdeck_header_library),
                style = MaterialTheme.typography.labelSmall,
                color = materials.interactionAccent,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun LemuroidTopBarActions(
    route: MainRoute,
    navController: NavController,
    context: Context,
    saveSyncEnabled: Boolean,
    operationsInProgress: Boolean,
    onHelpPressed: () -> Unit,
    arrangingShelf: Boolean = false,
    shelfAvailable: Boolean = false,
    onToggleArrangeShelf: () -> Unit = {},
    onShelfAppearance: () -> Unit = {},
) {
    var libraryOptionsExpanded by remember { mutableStateOf(false) }
    Row {
        IconButton(
            onClick = { onHelpPressed() },
        ) {
            Icon(
                Icons.Outlined.Info,
                stringResource(R.string.mobile_settings_help),
            )
        }
        if (saveSyncEnabled && route != MainRoute.HOME) {
            IconButton(
                onClick = { SaveSyncWork.enqueueManualWork(context.applicationContext) },
                enabled = !operationsInProgress,
            ) {
                Icon(
                    Icons.Outlined.CloudSync,
                    stringResource(R.string.save_sync),
                )
            }
        }
        if (route.showBottomNavigation) {
            IconButton(
                onClick = { navController.navigate(MainRoute.SETTINGS.route) },
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    stringResource(R.string.settings),
                )
            }
        }
        if (route == MainRoute.HOME) {
            Box {
                IconButton(onClick = { libraryOptionsExpanded = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.cartdeck_library_options))
                }
                DropdownMenu(
                    expanded = libraryOptionsExpanded,
                    onDismissRequest = { libraryOptionsExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (arrangingShelf) R.string.cartdeck_done_arranging
                                    else R.string.cartdeck_arrange_shelf,
                                ),
                            )
                        },
                        enabled = shelfAvailable,
                        onClick = {
                            libraryOptionsExpanded = false
                            onToggleArrangeShelf()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.cartdeck_shelf_appearance)) },
                        enabled = shelfAvailable,
                        onClick = {
                            libraryOptionsExpanded = false
                            onShelfAppearance()
                        },
                    )
                    if (saveSyncEnabled) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.save_sync)) },
                            enabled = !operationsInProgress,
                            onClick = {
                                libraryOptionsExpanded = false
                                SaveSyncWork.enqueueManualWork(context.applicationContext)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LemuroidSearchView(
    mainUIState: MainViewModel.UiState,
    onUpdateQueryString: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp, bottom = 8.dp, end = 8.dp),
            shape = RoundedCornerShape(100),
            tonalElevation = 16.dp,
        ) { }

        TextField(
            value = mainUIState.searchQuery,
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            onValueChange = { onUpdateQueryString(it) },
            singleLine = true,
            keyboardActions =
                KeyboardActions(
                    onDone = { focusManager.clearFocus(true) },
                ),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }
}
