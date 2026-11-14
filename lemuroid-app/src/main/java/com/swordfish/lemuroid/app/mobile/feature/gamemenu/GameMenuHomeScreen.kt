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
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcLayoutProfile
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.tilt.TiltConfigurationMenuEntry
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.lib.library.SystemID
import kotlin.reflect.KFunction1

@Composable
fun GameMenuHomeScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    val capabilities =
        GameMenuCapabilities(
            controls = true,
            states = gameMenuRequest.coreConfig.statesSupported,
            settings = gameMenuRequest.advancedCoreOptions.isNotEmpty() || gameMenuRequest.coreOptions.isNotEmpty(),
            advanced = true,
        )
    SharedGameMenuHome(capabilities) { action ->
        when (action) {
            GameMenuAction.RESUME -> onResult { }
            GameMenuAction.CONTROLS -> navController.navigateToRoute(GameMenuRoute.CONTROLS)
            GameMenuAction.STATES -> navController.navigateToRoute(GameMenuRoute.STATES)
            GameMenuAction.SETTINGS -> navController.navigateToRoute(GameMenuRoute.OPTIONS)
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
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (gameMenuRequest.coreConfig.statesSupported) {
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
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

/** One simple Controls page; all GripArc programming remains inside the game. */
@Composable
fun GameMenuControlsScreen(
    navController: NavController,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
    controllerOptions: @Composable () -> Unit = {},
    physicalControllerOptions: @Composable () -> Unit = {},
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        WiiControllerPicker(gameMenuRequest, onResult)
        physicalControllerOptions()
        controllerOptions()
        TiltControls(gameMenuRequest, onResult)
        val system = SystemID.values().firstOrNull { it.dbname == gameMenuRequest.game.systemId }
        if (system != null && PivotArcLayoutProfile.activeFor(system) != null) {
            GameMenuLink("PivotArc", R.drawable.ic_menu_controls) {
                navController.navigateToRoute(GameMenuRoute.PIVOTARC)
            }
        }
        LemuroidSettingsMenuLink(
            title = { Text("GripArc") },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_controls),
                    contentDescription = "GripArc",
                )
            },
            onClick = { navController.navigateToRoute(GameMenuRoute.GRIPARC) },
        )
        LemuroidSettingsMenuLink(
            title = { Text(text = stringResource(id = R.string.game_menu_edit_touch_controls)) },
            icon = {
                Icon(
                    painterResource(R.drawable.ic_menu_controls),
                    contentDescription = stringResource(id = R.string.game_menu_edit_touch_controls),
                )
            },
            onClick = { onResult { putExtra(GameMenuContract.RESULT_EDIT_TOUCH_CONTROLS, true) } },
        )
    }
}

/**
 * Available under Controls; not duplicated on the Home page.
 * Selection closes the menu and updates the active Libretro controller type.
 * No game restart, ROM modification or separate emulator installation.
 */
@Composable
private fun WiiControllerPicker(
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    onResult: KFunction1<Intent.() -> Unit, Unit>,
) {
    if (gameMenuRequest.game.systemId != SystemID.WII.dbname) return
    val controllers = gameMenuRequest.coreConfig.controllerConfigs[0].orEmpty()
    if (controllers.size < 2) return
    val selected =
        controllers.indexOfFirst {
            it.name == (gameMenuRequest.currentControllerName ?: controllers.first().name)
        }.coerceAtLeast(0)
    LemuroidSettingsList(
        title = { Text("Wii controller") },
        items = controllers.map { stringResource(id = it.displayName) },
        state = rememberMemoryIntSettingState(selected),
        onItemSelected = { index, _ ->
            onResult {
                putExtra(GameMenuContract.RESULT_CHANGE_WII_CONTROLLER, controllers[index].name)
            }
        },
    )
}
