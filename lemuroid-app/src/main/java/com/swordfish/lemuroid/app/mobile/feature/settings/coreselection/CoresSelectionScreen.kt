package com.swordfish.lemuroid.app.mobile.feature.settings.coreselection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.swordfish.lemuroid.app.shared.game.EmulatorCompatibilityPreferences
import com.swordfish.lemuroid.app.shared.game.EmulatorModernAssetsProvisioner
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
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
            CompatibilityEngineControls(SystemID.PSX, "PlayStation", assetsReady)
            CompatibilityEngineControls(SystemID.PSP, "PlayStation Portable", assetsReady)
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

@Composable
private fun CompatibilityEngineControls(systemId: SystemID, label: String, assetRevision: Boolean) {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { EmulatorCompatibilityPreferences(context) }
    val updatedAvailable = remember(systemId, assetRevision) { preferences.available(systemId) }
    var updatedSelected by remember(systemId) {
        mutableStateOf(preferences.isSystemUpdatedSelected(systemId))
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(
            if (updatedSelected) "Updated engine selected" else "Previous working engine selected",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = {
                if (preferences.setSystemEngine(systemId, false)) updatedSelected = false
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Previous working engine") }
        OutlinedButton(
            enabled = updatedAvailable,
            onClick = {
                if (preferences.setSystemEngine(systemId, true)) updatedSelected = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Updated engine (experimental)") }
        TextButton(
            onClick = {
                if (preferences.restoreSystemDefault(systemId)) updatedSelected = false
            },
        ) { Text("Restore default") }
        if (!updatedAvailable) {
            Text(
                "The updated engine is not installed and verified yet. The working engine remains available.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
