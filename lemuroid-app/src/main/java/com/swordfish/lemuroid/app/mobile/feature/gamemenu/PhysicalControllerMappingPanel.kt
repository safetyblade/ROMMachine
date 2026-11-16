package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile
import com.swordfish.lemuroid.app.shared.input.ControllerInputProbe
import com.swordfish.lemuroid.app.shared.input.ControllerMappingDraft
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind

/** Capture and probe use the menu Activity; the paused gameplay dispatcher stays authoritative. */
@Composable
internal fun PhysicalControllerMappingPanel(
    inputDeviceManager: InputDeviceManager,
    inventory: NativeControlInventory?,
    runtimePort: ((InputDevice) -> Int?)? = null,
    captureHatBindings: Boolean = true,
    captureTarget: Int?,
    mappingRevision: Int,
    generation: Int,
    draft: ControllerMappingDraft?,
    probe: ControllerInputProbe,
    probeRevision: Int,
    message: String?,
    onSelect: (InputDevice?) -> Unit,
    onCapture: (deviceId: Int, originalKey: Int) -> Unit,
    onCancel: () -> Unit,
    onReset: (InputDevice) -> Unit,
    onUndo: () -> Unit,
    onDiscard: () -> Unit,
    onResolveConflict: () -> Unit,
    onSave: () -> Unit,
) {
    val enabled by remember(inputDeviceManager) {
        inputDeviceManager.getEnabledInputsObservable()
    }.collectAsState(emptyList())
    val ports by remember(inputDeviceManager) {
        inputDeviceManager.getGamePadsPortMapperObservable()
    }.collectAsState({ _: InputDevice? -> null })
    val devices = enabled.filter(ControllerHardwareProfile::isGameplayPad)
    var selectedId by remember { mutableIntStateOf(-1) }
    val selected = devices.firstOrNull { it.id == selectedId } ?: devices.firstOrNull()
    LaunchedEffect(selected?.id, selected?.descriptor, generation) { onSelect(selected) }
    DisposableEffect(Unit) { onDispose { onSelect(null) } }
    // Revision reads expose the Activity-owned, non-Compose transaction and probe updates.
    val bindings = remember(draft, mappingRevision) { draft?.bindings.orEmpty() }
    val defaults =
        remember(selected?.id) {
            selected?.let { device ->
                inputDeviceManager.defaultBindings(device).mapKeys { it.key.keyCode }.mapValues { it.value.keyCode }
            }.orEmpty()
        }
    val targets = inventory?.controls?.flatMap { listOfNotNull(it.keyCode, it.pressCode) }?.toMutableSet()
    if (inventory?.controls?.any { it.kind == NativeControlKind.DPAD } == true) {
        targets?.addAll(
            listOf(
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
            ),
        )
    }

    fun targetName(code: Int): String {
        val spec = inventory?.controls?.firstOrNull { it.keyCode == code }
        return when (spec?.icon) {
            NativeControlIcon.CROSS -> "Cross"
            NativeControlIcon.CIRCLE -> "Circle"
            NativeControlIcon.SQUARE -> "Square"
            NativeControlIcon.TRIANGLE -> "Triangle"
            else ->
                spec?.label ?: KeyEvent.keyCodeToString(code)
                    .removePrefix("KEYCODE_BUTTON_").removePrefix("KEYCODE_DPAD_")
        }
    }
    val held = remember(probeRevision) { probe.held }
    val axes = remember(probeRevision) { probe.axes.toMap() }

    Column {
        Text(
            "Physical controller — Map and test",
            Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        if (selected == null) {
            Text("Connect a game controller to play. Your remote can still open CARTDECK.")
            return@Column
        }
        val actualPort = if (runtimePort != null) runtimePort(selected) else ports(selected)
        val portLabel = actualPort?.let { "Player ${it + 1}" } ?: "Unassigned player port"
        GameMenuLink("Controller: ${selected.name} · $portLabel") {
            val index = devices.indexOfFirst { it.id == selected.id }
            onDiscard()
            selectedId = devices[(index + 1) % devices.size].id
        }
        Text(
            "${ControllerHardwareProfile.family(selected)} · ${selected.descriptor}",
            style = MaterialTheme.typography.bodySmall,
        )
        val modeName = if (bindings == defaults) "Console Authentic" else "Custom"
        val modeStatus = if (draft?.dirty == true) " (unsaved)" else ""
        Text("Mode: $modeName$modeStatus")
        GameMenuLink("Console Authentic — reset draft") { onReset(selected) }
        Text("Modern Function unavailable: no verified controls for this edition.")
        Text("Input test: ${probe.lastEvent}")
        Text("Held / simultaneous chord: ${held.joinToString(" + ") { physicalName(it) }.ifEmpty { "None" }}")
        axes.forEach { (axis, range) ->
            Text(
                "${MotionEvent.axisToString(axis)}: ${range.value} · min ${range.minimum} · max ${range.maximum}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            "Axes show neutral, range and DPAD diagonals. Analog triggers and digital L2/R2 " +
                "are shown independently. " +
                (
                    if (captureHatBindings) {
                        "DPAD hats can be captured. "
                    } else {
                        "DPAD hats are test-only in this native host. "
                    }
                ) +
                "Stick and trigger axis remapping is unavailable.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (captureTarget != null) {
            Text(
                "Press and release one physical button for ${targetName(captureTarget)}. " +
                    "Menu exits; Back cancels.",
            )
            GameMenuLink("Cancel capture", onClick = onCancel)
        }
        if (draft?.conflict != null) {
            Text(
                "This physical button already has a different binding. " +
                    "Review and swap the two bindings, or Undo / Cancel.",
            )
            GameMenuLink("Swap conflicting bindings", onClick = onResolveConflict)
        }
        message?.let { Text(it) }
        InputDeviceManager.OUTPUT_KEYS.filter {
            it.keyCode != KeyEvent.KEYCODE_UNKNOWN && it.keyCode != KeyEvent.KEYCODE_BUTTON_MODE &&
                (targets == null || it.keyCode in targets)
        }.forEach { original ->
            val source = bindings.entries.firstOrNull { it.value == original.keyCode }?.key
            GameMenuLink("${targetName(original.keyCode)} ← ${source?.let(::physicalName) ?: "Unassigned"}") {
                onCapture(selected.id, original.keyCode)
            }
        }
        if (draft?.canUndo == true) GameMenuLink("Undo", onClick = onUndo)
        GameMenuLink("Reset draft to Console Authentic", onClick = { onReset(selected) })
        if (draft?.dirty == true && draft.conflict == null) GameMenuLink("Save mappings", onClick = onSave)
        GameMenuLink("Cancel unsaved mappings", onClick = onDiscard)
    }
}

private fun physicalName(code: Int): String =
    when (code) {
        KeyEvent.KEYCODE_BUTTON_A -> "SOUTH (A / Cross)"
        KeyEvent.KEYCODE_BUTTON_B -> "EAST (B / Circle)"
        KeyEvent.KEYCODE_BUTTON_X -> "WEST (X / Square)"
        KeyEvent.KEYCODE_BUTTON_Y -> "NORTH (Y / Triangle)"
        else -> KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_BUTTON_").removePrefix("KEYCODE_DPAD_")
    }
