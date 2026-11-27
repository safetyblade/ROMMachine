package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.alorma.compose.settings.storage.memory.rememberMemoryBooleanSettingState
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.tilt.TiltConfigurationMenuEntry
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.shared.support.SupportActivity
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KFunction1

@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val capabilities =
        GameMenuCapabilities(
            controls = true,
            // Always provide save guidance; actual save-state actions remain capability gated.
            states = true,
            settings =
                gameMenuRequest.coreOptions.any {
                    !com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions.isDisplayKey(it.getKey())
                },
            advanced = true,
            reporting = true,
        )
    SharedGameMenuHome(capabilities) { action ->
        when (action) {
            GameMenuAction.RESUME -> onResult { }
            GameMenuAction.CONTROLS -> navController.navigateToRoute(GameMenuRoute.CONTROLS)
            GameMenuAction.DISPLAY -> navController.navigateToRoute(GameMenuRoute.DISPLAY)
            GameMenuAction.STATES -> navController.navigateToRoute(GameMenuRoute.STATES)
            GameMenuAction.SETTINGS -> navController.navigateToRoute(GameMenuRoute.OPTIONS)
            GameMenuAction.REPORT_PROBLEM ->
                scope.launch {
                    val report = withContext(Dispatchers.IO) { buildLibretroSupportReport(context, gameMenuRequest) }
                    context.startActivity(
                        SupportActivity.intent(context, report),
                    )
                }
            GameMenuAction.ADVANCED -> navController.navigateToRoute(GameMenuRoute.ADVANCED)
            GameMenuAction.RETURN_TO_SHELF -> onResult { putExtra(GameMenuContract.RESULT_QUIT, true) }
        }
    }
}

@Composable
fun GameMenuStatesHome(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
) {
    val hostCapabilities =
        GameplayMenuHostProfiles.capabilities(
            GameplayMenuHostKind.LIBRETRO,
            gameMenuRequest.coreConfig.statesSupported,
        )
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (!hostCapabilities.canSaveState) {
            Text(
                "Save progress through the game's own save system. " +
                    "Emulator save states are not available for this engine.",
            )
        } else {
            Text(
                "Save states are emulator snapshots, separate from the game's own save files.",
            )
            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_save)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_save),
                        contentDescription = stringResource(id = R.string.game_menu_save),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.SAVE) },
            )

            LemuroidSettingsMenuLink(
                title = { Text(text = stringResource(id = R.string.game_menu_load)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_load),
                        contentDescription = stringResource(id = R.string.game_menu_load),
                    )
                },
                onClick = { navController.navigateToRoute(GameMenuRoute.LOAD) },
            )
        }
    }
}

@Composable
fun GameMenuAdvancedScreen(
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
    onDiagnostics: () -> Unit = {},
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        GameMenuLink("Diagnostics", onClick = onDiagnostics)
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_restart)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_restart),
                    contentDescription = stringResource(id = R.string.game_menu_restart),
                )
            },
            onClick = {
                onResult { putExtra(GameMenuContract.RESULT_RESET, true) }
            },
        )

        LemuroidSettingsSwitch(
            title = { Text(text = stringResource(id = R.string.game_menu_mute_audio)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_mute),
                    contentDescription = stringResource(id = R.string.game_menu_mute_audio),
                )
            },
            state = rememberMemoryBooleanSettingState(!gameMenuRequest.audioEnabled),
            onCheckedChange = {
                onResult { putExtra(GameMenuContract.RESULT_ENABLE_AUDIO, !it) }
            },
        )

        if (gameMenuRequest.fastForwardSupported) {
            LemuroidSettingsSwitch(
                title = { Text(text = stringResource(id = R.string.game_menu_fast_forward)) },
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_fast_forward),
                        contentDescription = stringResource(id = R.string.game_menu_fast_forward),
                    )
                },
                state = rememberMemoryBooleanSettingState(gameMenuRequest.fastForwardEnabled),
                onCheckedChange = {
                    onResult { putExtra(GameMenuContract.RESULT_ENABLE_FAST_FORWARD, it) }
                },
            )
        }

        if (gameMenuRequest.numDisks > 1) {
            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_change_disk_button)) },
                items = (1..gameMenuRequest.numDisks).map { stringResource(R.string.game_menu_change_disk_disk, it) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_menu_disk),
                        contentDescription = stringResource(id = R.string.game_menu_change_disk_button),
                    )
                },
                state = rememberMemoryIntSettingState(gameMenuRequest.currentDisk),
                onItemSelected = { index, _ ->
                    onResult { putExtra(GameMenuContract.RESULT_CHANGE_DISK, index) }
                },
            )
        }
    }
}

@Composable
private fun TiltControls(
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    Column {
        if (gameMenuRequest.allTiltConfigurations.isNotEmpty()) {
            val tiltConfigurationEntries =
                gameMenuRequest.allTiltConfigurations
                    .map { TiltConfigurationMenuEntry.fromTiltConfiguration(it) }

            val selectedIndex =
                gameMenuRequest.allTiltConfigurations
                    .indexOf(gameMenuRequest.currentTiltConfiguration)

            LemuroidSettingsList(
                title = { Text(text = stringResource(id = R.string.game_menu_tilt_sensor)) },
                items = tiltConfigurationEntries.map { stringResource(it.descriptionId) },
                useSelectedValueAsSubtitle = false,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = stringResource(id = R.string.game_menu_tilt_sensor),
                    )
                },
                state = rememberMemoryIntSettingState(selectedIndex),
                onItemSelected = { index, _ ->
                    onResult {
                        putExtra(
                            GameMenuContract.RESULT_CHANGE_TILT_CONFIG,
                            tiltConfigurationEntries[index].configuration,
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun GameMenuControlsScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
    physicalControllerOptions: @Composable () -> Unit = {},
    controllerOptions: @Composable () -> Unit = {},
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        UniversalControls(
            gameMenuRequest.game,
            core = gameMenuRequest.coreConfig,
            activeController = gameMenuRequest.currentControllerName,
            onLiveController = { name ->
                onResult { putExtra(GameMenuContract.RESULT_CHANGE_WII_CONTROLLER, name) }
            },
            physical = physicalControllerOptions,
            advanced = {
                controllerOptions()
                TiltControls(gameMenuRequest, onResult)
                GameMenuLink("PivotARC positioning and handedness") {
                    navController.navigateToRoute(GameMenuRoute.PIVOTARC)
                }
                GameMenuLink(stringResource(R.string.game_menu_edit_touch_controls)) {
                    onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) }
                }
            },
        )
    }
}
