package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.CartdeckMenuRow
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToRoute
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckCuratedDesigns
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfAppearance
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.app.utils.android.stringListResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    navController: NavController,
    onOpenRecovery: () -> Unit = {},
    onOpenAppearance: (() -> Unit)? = null,
    onOpenHelp: () -> Unit = {},
) {
    val state =
        viewModel.uiState
            .collectAsState(SettingsViewModel.State())
            .value

    val scanInProgress =
        viewModel.directoryScanInProgress
            .collectAsState(false)
            .value

    val indexingInProgress =
        viewModel.indexingInProgress
            .collectAsState(false)
            .value

    var section by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = section != null) { section = null }
    LemuroidSettingsPage(modifier = modifier) {
        val selected = section
        if (selected == null) {
            if (onOpenAppearance != null) {
                val appearance = LocalShelfAppearance.current
                val current =
                    CartdeckCuratedDesigns.byId(appearance.curatedDesignId)?.title
                        ?: appearance.edition.replaceFirstChar { it.uppercase() }
                CartdeckMenuRow(
                    title = { Text("Appearance") },
                    supporting = { Text(current) },
                    onClick = onOpenAppearance,
                )
            }
            val sections =
                listOf("Controllers", "Display & gameplay", "Shelf & library", "Help & guides", "System & advanced")
            for (name in sections) {
                CartdeckMenuRow(title = { Text(name) }, onClick = { section = name })
            }
        } else {
            TextButton(onClick = { section = null }) { Text("‹ Settings") }
            when (selected) {
                "Controllers" -> InputSettings(navController)
                "Display & gameplay" -> GeneralSettings(navController)
                "Shelf & library" ->
                    RomsSettings(
                        state = state,
                        onChangeFolder = { viewModel.changeLocalStorageFolder() },
                        indexingInProgress = indexingInProgress,
                        scanInProgress = scanInProgress,
                        onSupportedGames = { navController.navigateToRoute(MainRoute.SETTINGS_SUPPORTED_GAMES) },
                    )
                "Help & guides" -> com.swordfish.lemuroid.app.mobile.feature.cartmod.CartmodHelpSettings(onOpenHelp)
                "System & advanced" ->
                    MiscSettings(
                        indexingInProgress = indexingInProgress,
                        onOpenRecovery = onOpenRecovery,
                        isSaveSyncSupported = state.isSaveSyncSupported,
                        navController = navController,
                    )
            }
        }
    }
}

@Composable
private fun MiscSettings(
    indexingInProgress: Boolean,
    onOpenRecovery: () -> Unit,
    isSaveSyncSupported: Boolean,
    navController: NavController,
) {
    LemuroidCardSettingsGroup(
        title = { Text("System & advanced") },
    ) {
        LemuroidSettingsMenuLink(
            title = { Text("PlayStation 3 setup") },
            subtitle = { Text("Firmware, PS3 users and save management") },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
        )
        if (isSaveSyncSupported) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.settings_title_save_sync)) },
                subtitle = {
                    Text(text = stringResource(id = R.string.settings_description_save_sync))
                },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_SAVE_SYNC) },
            )
        }
        LemuroidSettingsMenuLink(
            title = { Text("Cartridge and save recovery") },
            subtitle = { Text("Explicitly reconnect unavailable cartridges or historical progress") },
            enabled = !indexingInProgress,
            onClick = onOpenRecovery,
        )
        LemuroidSettingsMenuLink(
            title = { Text("Emulation · Compatibility") },
            subtitle = { Text("Engine selection and recovery for PS1 and PSP") },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_CORES_SELECTION) },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_display_bios_info)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_display_bios_info))
            },
            enabled = !indexingInProgress,
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
        )
        if (com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Engine.engineeringBuild) {
            LemuroidSettingsMenuLink(
                title = { Text("PlayStation 2 setup") },
                subtitle = { Text("ARMSX2 BIOS, automatic engine choice and startup-message settings") },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
            )
        }
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.settings_title_advanced_settings)) },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_advanced_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_ADVANCED) },
        )
    }
}

@Composable
private fun InputSettings(navController: NavController) {
    LemuroidCardSettingsGroup(
        title = { Text("Controllers") },
    ) {
        LemuroidSettingsMenuLink(
            title = { Text("Layout") },
            subtitle = { Text("Standard or One Hand Mode · PivotArc") },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_LAYOUT) },
        )
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    R.string.pref_key_haptic_feedback_mode,
                    "press",
                    stringListResource(R.array.pref_key_haptic_feedback_mode_values),
                ),
            title = {
                Text(text = stringResource(id = R.string.settings_title_enable_touch_feedback))
            },
            items = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names),
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = "Controllers") },
            subtitle = {
                Text(text = stringResource(id = R.string.settings_description_gamepad_settings))
            },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
        )
    }
}

@Composable
private fun GeneralSettings(navController: NavController) {
    val immersiveMode = booleanPreferenceState(R.string.pref_key_enable_immersive_mode, false)
    var helpOpen by remember { mutableStateOf(false) }

    LemuroidCardSettingsGroup(
        title = { Text("Display & gameplay") },
    ) {
        LemuroidSettingsMenuLink(
            title = { Text("Display") },
            onClick = { navController.navigateToRoute(MainRoute.SETTINGS_DISPLAY) },
        )
        LemuroidSettingsSwitch(
            state = booleanPreferenceState(R.string.pref_key_autosave, true),
            title = { Text(text = stringResource(id = R.string.settings_title_enable_autosave)) },
        )
        LemuroidSettingsSwitch(
            state = immersiveMode,
            title = { Text(text = stringResource(id = R.string.settings_title_immersive_mode)) },
        )
        TextButton(onClick = { helpOpen = true }) { Text("About display & gameplay") }
    }
    if (helpOpen) {
        AlertDialog(
            onDismissRequest = { helpOpen = false },
            title = { Text("Display & gameplay") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(stringResource(R.string.settings_description_enable_autosave))
                    Text(stringResource(R.string.settings_description_immersive_mode))
                }
            },
            confirmButton = { TextButton(onClick = { helpOpen = false }) { Text("Done") } },
        )
    }
}

@Composable
private fun RomsSettings(
    state: SettingsViewModel.State,
    onChangeFolder: () -> Unit,
    indexingInProgress: Boolean,
    scanInProgress: Boolean,
    onSupportedGames: () -> Unit,
) {
    val context = LocalContext.current

    val currentDirectory = state.currentDirectory
    val emptyDirectory = stringResource(R.string.none)

    val currentDirectoryName =
        remember(state.currentDirectory) {
            runCatching {
                DocumentFile.fromTreeUri(context, Uri.parse(currentDirectory))?.name
            }.getOrNull() ?: emptyDirectory
        }

    LemuroidCardSettingsGroup(title = { Text("Shelf & library") }) {
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.directory)) },
            subtitle = { Text(text = currentDirectoryName) },
            onClick = { onChangeFolder() },
            enabled = !indexingInProgress,
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.cartdeck_formats_title)) },
            onClick = onSupportedGames,
        )
        if (scanInProgress) {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.stop)) },
                onClick = { LibraryIndexScheduler.cancelLibrarySync(context) },
            )
        } else {
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.rescan)) },
                onClick = { LibraryIndexScheduler.scheduleLibrarySync(context) },
                enabled = !indexingInProgress,
            )
        }
    }
}
