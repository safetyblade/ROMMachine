package com.swordfish.lemuroid.app.mobile.feature.settings.coreselection

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.memory.rememberMemoryIntSettingState
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.EngineSelectionControls
import com.swordfish.lemuroid.app.shared.game.EmulatorModernAssetsProvisioner
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID

@Composable
fun CoresSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: CoresSelectionViewModel,
) {
    val applicationContext = LocalContext.current.applicationContext
    var assetsReady by remember { mutableStateOf(false) }
    LaunchedEffect(applicationContext) {
        assetsReady = EmulatorModernAssetsProvisioner.installIfBundled(applicationContext)
    }

    val cores = viewModel.getSelectedCores().collectAsState(emptyList()).value

    val indexingInProgress = viewModel.indexingInProgress.collectAsState(false).value

    LemuroidSettingsPage(modifier = modifier.fillMaxWidth()) {
        LemuroidCardSettingsGroup(title = { Text("Emulation · Compatibility") }) {
            GameSystem.all().filter {
                it.nativeBackend != null || it.id in setOf(SystemID.PSX, SystemID.PSP) ||
                    it.id in setOf(SystemID.DREAMCAST, SystemID.GAMECUBE, SystemID.WII, SystemID.PLAYSTATION_2)
            }.forEach { system ->
                var expanded by remember(system.id) { mutableStateOf(false) }
                TextButton(onClick = { expanded = !expanded }) { Text(stringResource(system.titleResId)) }
                if (expanded) {
                    EngineSelectionControls(system, assetsReady = assetsReady)
                    if (system.id == SystemID.XBOX360) {
                        com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360Troubleshooting()
                    }
                }
            }
        }
        ElevatedCard(
            colors =
                CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp),
        ) {
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.settings_core_selection_ds_info),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LemuroidCardSettingsGroup {
            cores.forEach { (system, core) ->
                val state = rememberMemoryIntSettingState(system.systemCoreConfigs.indexOf(core))

                LemuroidSettingsList(
                    state = state,
                    title = { Text(text = stringResource(system.titleResId)) },
                    items = system.systemCoreConfigs.map { it.coreID.coreDisplayName },
                    enabled = !indexingInProgress,
                    onItemSelected = { index, _ ->
                        viewModel.changeCore(system, system.systemCoreConfigs[index], applicationContext)
                    },
                )
            }
        }
    }
}
