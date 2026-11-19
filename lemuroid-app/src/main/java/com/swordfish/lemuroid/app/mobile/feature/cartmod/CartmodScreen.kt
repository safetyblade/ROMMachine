package com.swordfish.lemuroid.app.mobile.feature.cartmod

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Preparation remains separate from normal Shelf gameplay and runtime ownership. */
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
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cartridgeId == null) {
            PcImportSettings(
                enabled = enabled,
                onImported = { revision++ },
            )
        }
        if (BuildConfig.PC_WINE_NATIVE_ENABLED && cartridgeId == null) {
            PcWineImportSettings(enabled = enabled, onImported = { revision++ })
        }
        PcWindowsSetupSettings(
            enabled = enabled,
            revision = revision,
            cartridgeId = cartridgeId,
            games = games,
            onOpenGuest = onOpenGuest,
        )
        OutlinedButton(onClick = onOpenShelf, modifier = Modifier.fillMaxWidth()) {
            Text("Return to Shelf")
        }
        CartmodFaq()
    }
}

@Composable
internal fun WorkshopPanel(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide details" else "Open")
            }
            if (expanded) content()
        }
    }
}
