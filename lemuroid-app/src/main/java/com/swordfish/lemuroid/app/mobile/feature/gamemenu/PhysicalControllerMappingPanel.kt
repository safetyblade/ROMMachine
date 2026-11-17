package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Capture and probe use the menu Activity; the paused gameplay dispatcher stays authoritative. */
@Composable
internal fun PhysicalControllerMappingPanel(
    inputDeviceManager: InputDeviceManager,
    inventory: NativeControlInventory?,
    runtimePort: ((InputDevice) -> Int?)? = null,
    cartridge: String? = null,
    portCapacity: Int = 16,
    outputLabel: (Int) -> String? = { null },
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
    var testing by remember(selected?.id) { mutableStateOf(false) }
    var chooseDevice by remember { mutableStateOf(false) }
    var choosePort by remember { mutableStateOf(false) }
    var pendingPort by remember(selected?.id) { mutableStateOf<Int?>(null) }

    fun physicalName(code: Int): String =
        com.swordfish.lemuroid.app.shared.input.ControllerLabels.input(
            code,
            selected?.let(ControllerHardwareProfile::family) ?: ControllerHardwareProfile.Family.GENERIC,
        )
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
        outputLabel(code)?.let { return it }
        val spec = inventory?.controls?.firstOrNull { it.keyCode == code }
        return when (spec?.icon) {
            NativeControlIcon.CROSS -> "Cross"
            NativeControlIcon.CIRCLE -> "Circle"
            NativeControlIcon.SQUARE -> "Square"
            NativeControlIcon.TRIANGLE -> "Triangle"
            else ->
                spec?.label ?: com.swordfish.lemuroid.app.shared.input.ControllerLabels.input(code)
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
        val editing = draft?.dirty == true || draft?.conflict != null || captureTarget != null
        Text("Connected controller", style = MaterialTheme.typography.labelLarge)
        Text(selected.name, style = MaterialTheme.typography.titleMedium)
        Text(portLabel)
        GameMenuLink("Choose controller (${devices.size} connected)", enabled = !editing) {
            chooseDevice = true
        }
        GameMenuLink("Assign player", enabled = !editing) { choosePort = true }
        if (editing) {
            Text("Save or discard your mapping changes before switching controller, player or scope.")
        }
        if (chooseDevice) {
            AlertDialog(
                onDismissRequest = { chooseDevice = false },
                title = { Text("Choose controller") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        devices.forEach { device ->
                            val port = if (runtimePort != null) runtimePort(device) else ports(device)
                            val assignment = port?.let { "Player ${it + 1}" } ?: "Unassigned"
                            val current = if (device.id == selected.id) " · Selected" else ""
                            GameMenuLink("${device.name} · $assignment$current") {
                                selectedId = device.id
                                chooseDevice = false
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { chooseDevice = false }) { Text("Close") } },
            )
        }
        if (choosePort) {
            AlertDialog(
                onDismissRequest = { choosePort = false },
                title = { Text("Assign player") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("Choose a supported port. Assignment does not change save ownership.")
                        repeat(portCapacity.coerceIn(1, 16)) { port ->
                            val occupant = devices.firstOrNull { ports(it) == port }
                            val label = occupant?.let { " · ${it.name}" }.orEmpty()
                            GameMenuLink("Player ${port + 1}$label") {
                                pendingPort = port
                                choosePort = false
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { choosePort = false }) { Text("Cancel") } },
            )
        }
        pendingPort?.let { next ->
            AlertDialog(
                onDismissRequest = { pendingPort = null },
                title = { Text("Confirm player ${next + 1}") },
                text = {
                    Text(
                        "Assign ${selected.name} to this port? If occupied, the existing saved assignment " +
                            "may change. Actual engine attachment depends on its supported ports.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingPort = null
                        scope.launch {
                            ownershipMessage =
                                runCatching { inputDeviceManager.assignPlayerPort(selected, next) }
                                    .fold(
                                        { "Assignment saved; engine attachment remains capability-based." },
                                        { "Could not change assignment. Review current assignments." },
                                    )
                        }
                    }) { Text("Assign") }
                },
                dismissButton = { TextButton(onClick = { pendingPort = null }) { Text("Cancel") } },
            )
        }
        ownershipMessage?.let { Text(it) }
        if (devices.count { it.descriptor == selected.descriptor } > 1) {
            Text(
                "These devices report the same descriptor. Runtime IDs distinguish them in this session; " +
                    "reconnect identity needs verification before a persistent reassignment.",
            )
        }
        Text(
            ControllerHardwareProfile.family(selected).name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
        )
        val hardware =
            remember(selected.id, selected.descriptor) {
                PhysicalControllerInput.hardware(selected)
            }
        GameMenuLink(if (testing) "Close input tester" else "Test buttons, sticks and triggers") { testing = !testing }
        if (testing) {
            Text("Device identity: ${selected.descriptor}", style = MaterialTheme.typography.bodySmall)
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
            Text("Press a physical button to see its mapped output. Select a mapping to change it.")
            val face: @Composable (Int, String, Modifier) -> Unit = { source, name, modifier ->
                val target = bindings[source] ?: defaults[source]
                val used =
                    target != null && target != KeyEvent.KEYCODE_UNKNOWN &&
                        (targets == null || target in targets)
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
        if (testing) {
            ControllerCalibrationPanel(inputDeviceManager, selected, probe)
            val longHeld by produceState(emptySet<Int>(), selected.id) {
                while (true) {
                    value = probe.heldLongerThan(10_000)
                    delay(500)
                }
            }
            if (longHeld.isNotEmpty()) {
                Text(
                    "Held over 10 seconds: ${longHeld.joinToString { physicalName(it) }}. " +
                        "Release the controls; check for a stuck input if the indicator remains active.",
                )
            }
            if (probe.repeatedKeys.isNotEmpty()) {
                Text(
                    "Repeated held-button events detected; holding navigation can be normal.",
                )
            }
        }
        GameMenuLink("Restore previous saved mapping", enabled = !editing) {
            scope.launch {
                ownershipMessage =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            inputDeviceManager.profiles.restorePrevious(
                                selected.descriptor,
                                if (perCartridge) cartridge else null,
                            )
                        }
                            .fold(
                                { "Previous mapping restored." },
                                { "No readable previous mapping; current profile retained." },
                            )
                    }
                onSelect(selected)
            }
        }
        val modeName = if (bindings == defaults) "Console Authentic" else "Custom"
        val modeStatus = if (draft?.dirty == true) " (unsaved)" else ""
        Text("Mode: $modeName$modeStatus")
        Text(if (perCartridge) "Mapping source: this cartridge" else "Mapping source: device default")
        if (cartridge != null) {
            GameMenuLink(
                if (perCartridge) "Edit device default" else "Edit this cartridge override",
                enabled = !editing,
            ) {
                onScope(!perCartridge)
            }
            GameMenuLink("Use device default for this cartridge", enabled = !editing, onClick = onUseDevice)
        }
        Text("Console Authentic preserves the original console controls.")
        if (testing) {
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
        }
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
        Text("Map buttons", style = MaterialTheme.typography.titleMedium)
        Text("Console button ← Your controller button. Choose a row, then press and release the desired button.")
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
        Text("Review changes", style = MaterialTheme.typography.titleMedium)
        if (draft?.canUndo == true) GameMenuLink("Undo", onClick = onUndo)
        GameMenuLink("Reset draft to Console Authentic", onClick = { onReset(selected) })
        if (draft?.dirty == true && draft.conflict == null) GameMenuLink("Save mappings", onClick = onSave)
        GameMenuLink("Restore saved profile / discard edits", onClick = onDiscard)
    }
}

private fun physicalName(code: Int): String = com.swordfish.lemuroid.app.shared.input.ControllerLabels.input(code)
