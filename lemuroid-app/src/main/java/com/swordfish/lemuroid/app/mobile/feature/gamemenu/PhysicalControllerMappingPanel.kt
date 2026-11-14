package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.input.InputKey
import com.swordfish.lemuroid.app.shared.input.RetroKey

/**
 * Libretro physical-device mapper. Capture is handled in GameMenuActivity so a
 * gameplay key cannot accidentally navigate away while it is being assigned.
 */
@Composable
internal fun PhysicalControllerMappingPanel(
    inputDeviceManager: InputDeviceManager,
    captureTarget: Int?,
    lastPhysicalKey: Int?,
    mappingRevision: Int,
    onCapture: (deviceId: Int, originalKey: Int) -> Unit,
    onCancel: () -> Unit,
    onReset: (InputDevice) -> Unit,
) {
    val devices by remember(inputDeviceManager) {
        inputDeviceManager.getGamePadsObservable()
    }.collectAsState(emptyList())
    var selectedId by remember { mutableIntStateOf(-1) }
    val selected = devices.firstOrNull { it.id == selectedId } ?: devices.firstOrNull()
    var bindings by remember { mutableStateOf<Map<InputKey, RetroKey>>(emptyMap()) }

    LaunchedEffect(selected?.id, mappingRevision) {
        bindings = selected?.let { inputDeviceManager.getCurrentBindings(it) } ?: emptyMap()
    }

    Column {
        Text(
            "Physical controller — Map buttons",
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        if (selected == null) {
            Text("Connect and enable a Bluetooth or USB gameplay controller to configure it.")
            return@Column
        }
        GameMenuLink("Controller: ${selected.name}") {
            val index = devices.indexOfFirst { it.id == selected.id }
            selectedId = devices[(index + 1) % devices.size].id
            onCancel()
        }
        Text(
            "Input test: ${lastPhysicalKey?.let(KeyEvent::keyCodeToString) ?: "Press any controller button"}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (captureTarget != null) {
            Text("Press and release the physical button to assign to ${shortName(captureTarget)}.")
            GameMenuLink("Cancel mapping", onClick = onCancel)
        }
        val targets = InputDeviceManager.OUTPUT_KEYS
            .filter {
                it.keyCode != KeyEvent.KEYCODE_UNKNOWN && it.keyCode != KeyEvent.KEYCODE_BUTTON_MODE
            }
        targets.forEach { original ->
            val source = bindings.entries.firstOrNull { it.value == original }?.key?.keyCode
            val text = source?.let(::shortName) ?: "Unassigned"
            GameMenuLink("${shortName(original.keyCode)}  ←  $text") {
                onCapture(selected.id, original.keyCode)
            }
        }
        GameMenuLink("Reset this controller's mappings", onClick = { onReset(selected) })
        Text(
            "Uses the existing console-authentic input bridge. Game-specific Modern Function " +
                "profiles are only applied when verified and explicitly selected.",
            Modifier.padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun shortName(code: Int): String =
    KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_BUTTON_").removePrefix("KEYCODE_DPAD_")
