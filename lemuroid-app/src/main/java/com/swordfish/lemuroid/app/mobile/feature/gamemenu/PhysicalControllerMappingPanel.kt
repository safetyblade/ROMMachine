package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.input.GamePadShortcutBindingActivity
import com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile
import com.swordfish.lemuroid.app.shared.input.ControllerInputProbe
import com.swordfish.lemuroid.app.shared.input.ControllerMappingDraft
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.input.PhysicalControllerInput
import com.swordfish.lemuroid.app.shared.input.ShortcutBindingUpdater
import com.swordfish.lemuroid.app.shared.settings.GameShortcutType
import com.swordfish.touchinput.radial.settings.NativeControlIcon
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.NativeControlKind
import kotlinx.coroutines.launch

/** Capture and probe use the menu Activity; the paused gameplay dispatcher stays authoritative. */
@Composable
internal fun PhysicalControllerMappingPanel(
    inputDeviceManager: InputDeviceManager,
    inventory: NativeControlInventory?,
    runtimePort: ((InputDevice) -> Int?)? = null,
    cartridge: String? = null,
    portCapacity: Int = 16,
    perCartridge: Boolean = false,
    onScope: (Boolean) -> Unit = {},
    onUseDevice: () -> Unit = {},
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var ownershipMessage by remember { mutableStateOf<String?>(null) }
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
        GameMenuLink("Assign player port · $portLabel") {
            scope.launch {
                val next = ((ports(selected) ?: -1) + 1) % portCapacity.coerceAtLeast(1)
                ownershipMessage =
                    runCatching { inputDeviceManager.assignPlayerPort(selected, next) }
                        .fold(
                            { "Assigned player ${next + 1}; engine attachment remains capability-based." },
                            { "Could not change assignment. Existing ownership retained." },
                        )
            }
        }
        ownershipMessage?.let { Text(it) }
        if (devices.count { it.descriptor == selected.descriptor } > 1) {
            Text(
                "These devices report the same descriptor. Runtime IDs distinguish them in this session; " +
                    "reconnect identity needs verification before a persistent reassignment.",
            )
        }
        Text(
            "${ControllerHardwareProfile.family(selected)} · ${selected.descriptor}",
            style = MaterialTheme.typography.bodySmall,
        )
        val hardware =
            remember(selected.id, selected.descriptor) {
                PhysicalControllerInput.hardware(selected)
            }
        Text(
            "VID ${hardware.vendor} · PID ${hardware.product} · sources ${hardware.sources}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "Reported buttons: ${hardware.keys.joinToString { physicalName(it) }}",
            style = MaterialTheme.typography.bodySmall,
        )
        if (hardware.family in
            setOf(
                ControllerHardwareProfile.Family.GENERIC,
                ControllerHardwareProfile.Family.UNKNOWN,
            )
        ) {
            Text("Unknown layout: verify physical positions in the live tester before saving a Custom map.")
        }
        val normalized = remember(probeRevision) { probe.physical.values }
        normalized.forEach { (control, value) ->
            Text("${control.name}: ${"%.2f".format(value)}", style = MaterialTheme.typography.bodySmall)
            androidx.compose.material3.LinearProgressIndicator(
                progress = {
                    if (control.name.contains("STICK") && !control.name.endsWith("PRESS")) {
                        (value + 1f) / 2f
                    } else {
                        value.coerceIn(0f, 1f)
                    }
                },
            )
        }
        Text("Physical face positions — select a position, then press that physical button to verify its output.")
        val face: @Composable (Int, String, Modifier) -> Unit = { source, name, modifier ->
            val target = defaults[source]
            val used = target != null && target != KeyEvent.KEYCODE_UNKNOWN && (targets == null || target in targets)
            GameMenuLink(
                "$name ${if (source in held) "●" else ""} → ${target?.let(::targetName) ?: "Unavailable"}",
                modifier = modifier,
                enabled = used,
            ) { if (target != null) onCapture(selected.id, target) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Spacer(Modifier.weight(1f))
                face(KeyEvent.KEYCODE_BUTTON_Y, "NORTH", Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
            Row {
                face(KeyEvent.KEYCODE_BUTTON_X, "WEST", Modifier.weight(1f))
                face(KeyEvent.KEYCODE_BUTTON_B, "EAST", Modifier.weight(1f))
            }
            Row {
                Spacer(Modifier.weight(1f))
                face(KeyEvent.KEYCODE_BUTTON_A, "SOUTH", Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
        GameMenuLink("Configure universal menu shortcut", enabled = draft?.dirty != true) {
            context.startActivity(
                Intent(context, GamePadShortcutBindingActivity::class.java)
                    .putExtra(ShortcutBindingUpdater.REQUEST_DEVICE, selected)
                    .putExtra(ShortcutBindingUpdater.REQUEST_SHORTCUT_TYPE, GameShortcutType.MENU.name),
            )
        }
        Text(
            "Default: L3 + R3, or Select + Start on pads without stick presses. " +
                "Use two stick, shoulder, Start or Select buttons. Guide delivery depends on Android.",
        )
        ControllerCalibrationPanel(inputDeviceManager, selected, probe)
        val modeName = if (bindings == defaults) "Console Authentic" else "Custom"
        val modeStatus = if (draft?.dirty == true) " (unsaved)" else ""
        Text("Mode: $modeName$modeStatus")
        Text(if (perCartridge) "Mapping source: this cartridge" else "Mapping source: device default")
        if (cartridge != null) {
            GameMenuLink(if (perCartridge) "Edit device default" else "Edit this cartridge override") {
                onScope(!perCartridge)
            }
            GameMenuLink("Use device default for this cartridge", onClick = onUseDevice)
        }
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
            GameMenuLink(
                "${targetName(original.keyCode)} ← ${source?.let(::physicalName) ?: "Unassigned"}" +
                    if (source in held) " ● pressed" else "",
            ) {
                onCapture(selected.id, original.keyCode)
            }
        }
        if (draft?.canUndo == true) GameMenuLink("Undo", onClick = onUndo)
        GameMenuLink("Reset draft to Console Authentic", onClick = { onReset(selected) })
        if (draft?.dirty == true && draft.conflict == null) GameMenuLink("Save mappings", onClick = onSave)
        GameMenuLink("Restore saved profile / discard edits", onClick = onDiscard)
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
