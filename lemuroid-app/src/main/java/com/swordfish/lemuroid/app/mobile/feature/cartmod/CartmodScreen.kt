package com.swordfish.lemuroid.app.mobile.feature.cartmod

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.swordfish.lemuroid.app.mobile.feature.settings.general.PcImportSettings
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage

/** Only exposes existing PC media import; installation/runtime readiness remains owned by PERF. */
@Composable
internal fun CartmodScreen(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onOpenShelf: () -> Unit,
) {
    LemuroidSettingsPage(modifier = modifier) {
        LemuroidCardSettingsGroup(title = { Text("Cartridge workshop") }) {
            Text("Prepare locally supplied game media. Imported media is not proof of a playable installation.")
        }
        PcImportSettings(enabled = enabled)
        LemuroidCardSettingsGroup(title = { Text("Collection") }) {
            LemuroidSettingsMenuLink(
                title = { Text("Return to Shelf") },
                subtitle = { Text("Find imported cartridges in the shared library") },
                onClick = onOpenShelf,
            )
        }
    }
}
