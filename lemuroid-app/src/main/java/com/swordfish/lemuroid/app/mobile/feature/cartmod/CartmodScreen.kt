package com.swordfish.lemuroid.app.mobile.feature.cartmod

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** HOT-03: preparation is separate from the normal Shelf gameplay route. */
@Composable
internal fun CartmodScreen(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    games: List<Game>,
    onOpenGuest: (Game) -> Unit,
    onOpenShelf: () -> Unit,
    cartridgeId: String? = null,
) {
    var revision by remember { mutableStateOf(0) }
    LemuroidSettingsPage(modifier = modifier) {
        LemuroidCardSettingsGroup(title = { Text("Cartridge workshop") }) {
            Text("Prepare locally supplied game media. Imported media is not proof of a playable installation.")
        }
        if (cartridgeId == null) {
            PcImportSettings(
                enabled = enabled,
                onImported = { revision++ },
            )
        }
        PcWindowsSetupSettings(
            enabled = enabled,
            revision = revision,
            cartridgeId = cartridgeId,
            games = games,
            onOpenGuest = onOpenGuest,
        )
        LemuroidCardSettingsGroup(title = { Text("Collection") }) {
            LemuroidSettingsMenuLink(
                title = { Text("Return to Shelf") },
                subtitle = { Text("Find imported cartridges in the shared library") },
                onClick = onOpenShelf,
            )
        }
    }
}
