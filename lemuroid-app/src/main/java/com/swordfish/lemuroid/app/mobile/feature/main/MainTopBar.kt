package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfMaterials
import com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork

@Composable
fun MainTopBar(
    currentRoute: MainRoute,
    navController: NavHostController,
    onUpdateQueryString: (String) -> Unit,
    mainUIState: MainViewModel.UiState,
) {
    Column {
        LemuroidTopAppBar(
            route = currentRoute,
            navController = navController,
            mainUIState = mainUIState,
            onUpdateQueryString = onUpdateQueryString,
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
    onUpdateQueryString: (String) -> Unit,
) {
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
                MainRoute.HOME -> BrandHeader("CARTDECK", R.drawable.cartdeck_brand_shelf)
                MainRoute.GEM -> BrandHeader("GEM", R.drawable.cartdeck_brand_gem)
                MainRoute.SAFESAVE -> BrandHeader("SafeSave", R.drawable.cartdeck_brand_safesave_mark)
                MainRoute.CARTMOD -> BrandHeader("CARTMOD", R.drawable.cartdeck_brand_cartmod)
                MainRoute.SETTINGS -> BrandHeader("Settings", R.drawable.cartdeck_brand_settings)
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
                        painterResource(R.drawable.cartdeck_action_back),
                        stringResource(id = R.string.back),
                    )
                }
            }
        },
    )
}

@Composable
private fun BrandHeader(
    title: String,
    icon: Int,
) {
    val materials = LocalShelfMaterials.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = readableColor(materials.interactionAccent, listOf(materials.roomBackdrop), 3f),
            modifier = Modifier.size(30.dp),
        )
        val ink = readableColor(materials.sectionText, listOf(materials.roomBackdrop), 4.5f)
        val wordmark =
            when (title) {
                "CARTDECK" -> R.drawable.cartdeck_wordmark_native
                "GEM" -> R.drawable.cartdeck_brand_gem_wordmark
                "SafeSave" -> R.drawable.cartdeck_brand_safesave_wordmark
                "CARTMOD" -> R.drawable.cartdeck_brand_cartmod_wordmark
                else -> null
            }
        if (wordmark != null) {
            Icon(
                painterResource(wordmark),
                contentDescription = title,
                tint = ink,
                modifier = Modifier.height(26.dp).fillMaxWidth(0.68f),
            )
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = ink,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun ShelfActions(
    saveSyncEnabled: Boolean,
    operationsInProgress: Boolean,
    arrangingShelf: Boolean,
    shelfAvailable: Boolean,
    onToggleArrangeShelf: () -> Unit,
    onShelfAppearance: () -> Unit,
    onRefreshLibrary: () -> Unit,
    onAddCartridges: () -> Unit,
) {
    val context = LocalContext.current
    var libraryOptionsExpanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { libraryOptionsExpanded = true }) {
            Icon(painterResource(R.drawable.cartdeck_action_more), contentDescription = null)
            Text("Shelf tools")
        }
        DropdownMenu(
            expanded = libraryOptionsExpanded,
            onDismissRequest = { libraryOptionsExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cartdeck_add_cartridges)) },
                enabled = !operationsInProgress,
                onClick = {
                    libraryOptionsExpanded = false
                    onAddCartridges()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.cartdeck_refresh_library)) },
                enabled = !operationsInProgress,
                onClick = {
                    libraryOptionsExpanded = false
                    onRefreshLibrary()
                },
            )
            DropdownMenuItem(
                text = {
                    val titleRes =
                        if (arrangingShelf) {
                            R.string.cartdeck_done_arranging
                        } else {
                            R.string.cartdeck_arrange_shelf
                        }
                    Text(stringResource(titleRes))
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
            leadingIcon = { Icon(painterResource(R.drawable.cartdeck_action_search), contentDescription = null) },
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
