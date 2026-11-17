package com.swordfish.lemuroid.app.mobile.feature.cartmod

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** HOT-03: preparation is separate from the normal Shelf gameplay route. */
@Composable
internal fun CartmodScreen(
    modifier: Modifier,
    enabled: Boolean,
    games: List<Game>,
    onOpenGuest: (Game) -> Unit,
    cartridgeId: String? = null,
) {
    var revision by remember { mutableStateOf(0) }
    LemuroidSettingsPage(modifier = modifier) {
        Text("Prepare and manage PC cartridges.")
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
    }
}
