package com.swordfish.lemuroid.app.mobile.feature.gamemenu.coreoptions

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.coreoptions.CoreOptionsPreferenceHelper
import com.swordfish.lemuroid.app.shared.coreoptions.LemuroidCoreOption
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

@Composable
fun GameMenuCoreOptionsScreen(gameMenuRequest: GameMenuActivity.GameMenuRequest) {
    val context = LocalContext.current

    val allOptions =
        remember(gameMenuRequest) {
            gameMenuRequest.coreOptions.filterNot {
                com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions.isDisplayKey(it.getKey()) ||
                    it.getKey() == CoreVariablesManager.WII_POINTER
            }
        }

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CoreOptions(gameMenuRequest.game.systemId, allOptions, context)
    }
}

@Composable
fun GameMenuTechnicalOptions(request: GameMenuActivity.GameMenuRequest) {
    CoreOptions(
        request.game.systemId,
        request.advancedCoreOptions.filterNot {
            com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions.isDisplayKey(it.getKey())
        },
        LocalContext.current,
    )
}

@Composable
private fun CoreOptions(
    systemID: String,
    coreOptions: List<LemuroidCoreOption>,
    context: Context,
) {
    if (coreOptions.isEmpty()) {
        return
    }

    for (coreOption in coreOptions) {
        if (coreOption.getEntriesValues().toSet() == CoreOptionsPreferenceHelper.BOOLEAN_SET) {
            LemuroidSettingsSwitch(
                state =
                    booleanPreferenceState(
                        CoreVariablesManager.computeSharedPreferenceKey(coreOption.getKey(), systemID),
                        coreOption.getCurrentValue() == "enabled",
                    ),
                title = { Text(text = coreOption.getDisplayName(context)) },
            )
        } else {
            LemuroidSettingsList(
                title = { Text(text = coreOption.getDisplayName(context)) },
                items = coreOption.getEntries(context),
                state =
                    indexPreferenceState(
                        CoreVariablesManager.computeSharedPreferenceKey(coreOption.getKey(), systemID),
                        coreOption.getEntriesValues().first(),
                        coreOption.getEntriesValues(),
                    ),
            )
        }
    }
}

@Composable
fun GameMenuControllerOptions(
    viewModel: GameMenuCoreOptionsViewModel,
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
) {
    val connectedGamePads by viewModel.connectedGamePads.collectAsState(0)
    ControllersOptions(gameMenuRequest, maxOf(1, connectedGamePads), LocalContext.current)
}

@Composable
private fun ControllersOptions(
    gameMenuRequest: GameMenuActivity.GameMenuRequest,
    connectedGamePads: Int,
    context: Context,
) {
    val controllers = gameMenuRequest.coreConfig.controllerConfigs

    val visibleControllers =
        (0 until connectedGamePads)
            .map { it to controllers[it] }
            .filter { (port, controllers) ->
                controllers != null && controllers.size >= 2 &&
                    !(port == 0 && gameMenuRequest.game.systemId == SystemID.WII.dbname)
            }

    if (visibleControllers.isEmpty()) {
        return
    }

    LemuroidSettingsGroup(
        title = { Text(text = stringResource(R.string.core_settings_category_controllers)) },
    ) {
        visibleControllers.forEach { (port, controllerConfigs) ->
            LemuroidSettingsList(
                title = { Text(text = context.getString(R.string.core_settings_controller, (port + 1).toString())) },
                items = controllerConfigs!!.map { stringResource(id = it.displayName) },
                state =
                    indexPreferenceState(
                        ControllerConfigsManager.getSharedPreferencesId(
                            gameMenuRequest.game.systemId,
                            gameMenuRequest.coreConfig.coreID,
                            port,
                        ),
                        controllerConfigs.map { it.name }.first(),
                        controllerConfigs.map { it.name },
                    ),
            )
        }
    }
}

@Composable
internal fun WiiPointerOptions(
    game: Game,
    config: SystemCoreConfig,
) {
    if (game.systemId != SystemID.WII.dbname) return
    val option = config.exposedSettings.firstOrNull { it.key == CoreVariablesManager.WII_POINTER } ?: return
    val context = LocalContext.current
    val manager =
        remember(context) {
            CoreVariablesManager(dagger.Lazy { SharedPreferencesHelper.getSharedPreferences(context) })
        }
    val values = option.values.map { it.key }
    var selected by remember(game.cartridgeId, game.fileUri) {
        mutableStateOf(manager.wiiPointerMode(game) ?: manager.defaultWiiPointerMode())
    }
    LemuroidSettingsList(
        title = { Text(stringResource(option.titleId)) },
        items = option.values.map { stringResource(it.titleId) },
        state = rememberMemoryIntSettingState(values.indexOf(selected).coerceAtLeast(0)),
        onItemSelected = { index, _ ->
            selected = values[index]
            manager.setWiiPointerMode(game, selected)
        },
    )
}
