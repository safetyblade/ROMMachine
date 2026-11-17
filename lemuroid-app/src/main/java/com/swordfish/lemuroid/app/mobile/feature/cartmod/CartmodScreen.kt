package com.swordfish.lemuroid.app.mobile.feature.cartmod

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Preparation stays separate from the normal Shelf gameplay route. */
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
    var page by rememberSaveable(cartridgeId) {
        mutableStateOf(if (cartridgeId == null) "workshop" else "manage")
    }
    BackHandler(enabled = page != "workshop" && cartridgeId == null) { page = "workshop" }
    LemuroidSettingsPage(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cartridge workshop", style = MaterialTheme.typography.headlineSmall)
            Text("Prepare your games. Play from Shelf.", style = MaterialTheme.typography.bodyMedium)
            if (page == "workshop") {
                WorkshopCard(
                    "Set up a PC game",
                    "Choose game media and an optional Windows installer.",
                    Icons.Filled.Add,
                ) { page = "import" }
                WorkshopCard(
                    "Manage installations",
                    "Open guest setup, check disks or import a prepared image.",
                    Icons.Filled.Settings,
                ) { page = "manage" }
                TextButton(onClick = onOpenShelf) { Text("Return to Shelf") }
            } else if (cartridgeId == null) {
                TextButton(onClick = { page = "workshop" }) { Text("‹ Workshop") }
            }
        }
        // Keep the operation owners composed while moving between task pages.
        if (cartridgeId == null) {
            PcImportSettings(
                enabled = enabled,
                onImported = { revision++ },
                visible = page == "import",
            )
        }
        PcWindowsSetupSettings(
            enabled = enabled,
            revision = revision,
            cartridgeId = cartridgeId,
            games = games,
            onOpenGuest = onOpenGuest,
            visible = page == "manage",
        )
        if (page != "workshop") {
            TextButton(onClick = onOpenShelf) { Text("Return to Shelf") }
        }
    }
}

@Composable
private fun WorkshopCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
