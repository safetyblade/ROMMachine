package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.game.xbox.XboxButtonBindingStore

/**
 * The Xbox emulator's in-process Compose menu owns this capture interface.
 * Every assignment is written only after the next physical key is released.
 * MODE/MENU/Android Back remain hard-wired escape controls.
 */
@Composable
internal fun XboxPhysicalMappingPanel(
    controllerName: String?,
    pendingTarget: Int?,
    lastInput: String,
    perCartridge: Boolean,
    mappings: Map<Int, String>,
    onScopeChange: (Boolean) -> Unit,
    onStartCapture: (Int) -> Unit,
    onCancelCapture: () -> Unit,
    onReset: () -> Unit,
) {
    Column {
        Text(
            "Physical controller",
            Modifier.padding(top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            controllerName ?: "Connect a gameplay controller to map buttons",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text("Last input: $lastInput", style = MaterialTheme.typography.bodySmall)
        GameMenuLink(
            if (perCartridge) "Scope: this cartridge" else "Scope: this physical controller",
        ) { onScopeChange(!perCartridge) }
        if (pendingTarget != null) {
            Text(
                "Press and release a physical button for Xbox ${name(pendingTarget)}. " +
                    "Menu/Back are reserved and cannot be captured.",
                style = MaterialTheme.typography.bodyMedium,
            )
            GameMenuLink("Cancel button capture", onClick = onCancelCapture)
        }
        XboxButtonBindingStore.editableKeys.forEach { target ->
            GameMenuLink(
                "Xbox ${name(target)}  ←  ${mappings[target] ?: name(target)}",
            ) { if (controllerName != null) onStartCapture(target) }
        }
        if (controllerName != null) GameMenuLink("Reset mappings for selected scope", onClick = onReset)
    }
}

private fun name(key: Int): String =
    KeyEvent.keyCodeToString(key).removePrefix("KEYCODE_BUTTON_").removePrefix("KEYCODE_DPAD_")
