package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.swordfish.lemuroid.app.shared.input.ControllerChordGate
import com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile
import com.swordfish.lemuroid.app.shared.input.ControllerHatEdges
import com.swordfish.lemuroid.app.shared.input.ControllerInputProbe
import com.swordfish.lemuroid.app.shared.input.ControllerMappingDraft
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.input.InputKey
import com.swordfish.lemuroid.app.shared.input.NativeControllerKeyMapping
import com.swordfish.lemuroid.app.shared.input.RetroKey
import com.swordfish.lemuroid.app.shared.input.handle
import com.swordfish.lemuroid.app.shared.settings.GameShortcut
import com.swordfish.lemuroid.app.shared.settings.GameShortcutType
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import com.swordfish.touchinput.radial.settings.TouchControllerID
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Menu-local configuration and observation. Hosts retain all gameplay event and JNI dispatch ownership. */
internal class NativeControllerMappingSession(
    context: Context,
    private val scope: CoroutineScope,
    private val controller: TouchControllerID?,
    private val cartridge: String? = null,
    private val runtime: Boolean = true,
    private val portCapacity: Int = 1,
    private val playerPort: (InputDevice) -> Int?,
) {
    private val manager = InputDeviceManager(context, Lazy { SharedPreferencesHelper.getSharedPreferences(context) })
    private var perCartridge by mutableStateOf(cartridge != null)
    private var bindingJob: Job? = null
    private var liveBindings: (InputDevice?) -> Map<InputKey, RetroKey> = { emptyMap() }
    private var selected: InputDevice? = null
    private var enabledIds: Set<Int>? = null
    private val heldOutputs = NativeControllerKeyMapping.HeldOutputs()
    private var draft by mutableStateOf<ControllerMappingDraft?>(null)
    private var pending by mutableStateOf<Int?>(null)
    private var revision by mutableIntStateOf(0)
    private var generation by mutableIntStateOf(0)
    private var probeRevision by mutableIntStateOf(0)
    private var message by mutableStateOf<String?>(null)
    private val probe = ControllerInputProbe()
    private val captureEdge = ControllerMenuEdge()
    private val backEdge = ControllerMenuEdge()
    private val hats = ControllerHatEdges()
    private val chord = ControllerChordGate()
    private var shortcuts = emptyMap<InputDevice, List<GameShortcut>>()
    private var replaying = false

    fun shortcut(
        event: KeyEvent,
        invoke: () -> Unit,
        replay: (KeyEvent) -> Boolean,
    ): Boolean {
        if (replaying || event.device?.let(ControllerHardwareProfile::isGameplayPad) != true) return false
        val keys =
            shortcuts[event.device]?.firstOrNull {
                it.type == GameShortcutType.MENU
            }?.keys ?: setOf(KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR)
        val decision =
            chord.route(
                event.deviceId,
                event.keyCode,
                event.action == KeyEvent.ACTION_DOWN,
                keys,
                event.isCanceled,
            )
        if (decision.invoke) invoke()
        replaying = true
        try {
            decision.replay.forEach {
                replay(
                    KeyEvent(
                        event.downTime, event.eventTime,
                        if (it.down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                        it.key, 0, event.metaState, event.deviceId, event.scanCode, event.flags, event.source,
                    ),
                )
            }
        } finally {
            replaying = false
        }
        return decision.consumed
    }

    fun start() {
        bindingJob?.cancel()
        generation++
        bindingJob =
            scope.launch {
                combine(
                    manager.getInputBindingsObservable(cartridge),
                    manager.getEnabledInputsObservable(),
                    manager.getGameShortcutsObservable(),
                ) { bindings, devices, combos ->
                    Triple(bindings, devices.map { it.id }.toSet(), combos)
                }.collect { (bindings, ids, combos) ->
                    shortcuts = combos
                    liveBindings = bindings
                    enabledIds = ids
                }
            }
    }

    fun stop() {
        bindingJob?.cancel()
        bindingJob = null
        chord.clear()
        liveBindings = { emptyMap() }
        enabledIds = null
        discard()
        selected = null
        draft = null
        hats.clear()
    }

    fun retainDevices(ids: Set<Int>) {
        chord.retainDevices(ids)
        heldOutputs.retainDevices(ids)
        captureEdge.retainDevices(ids)
        backEdge.retainDevices(ids)
        if (selected?.id !in ids) select(null)
    }

    private fun acceptsDevice(
        device: InputDevice?,
        id: Int,
    ): Boolean =
        if (device == null || !ControllerHardwareProfile.isGameplayPad(device)) {
            true
        } else {
            enabledIds?.contains(id) != false && playerPort(device) != null
        }

    fun axis(
        event: MotionEvent,
        code: Int,
    ): Float = manager.calibratedAxis(event, code)

    fun hasCanonicalBinding(
        device: InputDevice?,
        cartridgeOnly: Boolean = false,
    ): Boolean = manager.hasCanonicalBinding(device, cartridgeOnly)

    fun acceptsGameplay(event: KeyEvent): Boolean = acceptsDevice(event.device, event.deviceId)

    fun acceptsMotion(event: MotionEvent): Boolean = acceptsDevice(event.device, event.deviceId)

    fun gameplayKey(event: KeyEvent): Int =
        heldOutputs.key(
            event.deviceId,
            event.keyCode,
            event.action == KeyEvent.ACTION_DOWN,
            liveBindings(event.device)[InputKey(event.keyCode)]?.keyCode,
        )

    fun cancelInput() {
        heldOutputs.clear()
        cancelCapture()
    }

    private fun cancelCapture() {
        pending = null
        captureEdge.clear()
        backEdge.clear()
        probe.clear()
        probeRevision++
    }

    fun discard() {
        cancelInput()
        draft?.cancel()
        revision++
    }

    private fun select(device: InputDevice?) {
        discard()
        selected = device
        draft = null
        hats.clear()
        if (device != null) {
            scope.launch {
                val bindings = manager.getCurrentBindings(device, if (perCartridge) cartridge else null)
                if (selected?.id == device.id) {
                    draft = ControllerMappingDraft(bindings.mapKeys { it.key.keyCode }.mapValues { it.value.keyCode })
                }
            }
        }
    }

    /** Called only by the host while its menu is open. Menu/Guide remains intercepted by that host. */
    fun captureKey(event: KeyEvent): Boolean {
        if (event.isCanceled) {
            cancelCapture()
            return true
        }
        if (pending != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (backEdge.handle(event)) cancelCapture()
            return true
        }
        if (event.deviceId == selected?.id) {
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                probe.key(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
                probeRevision++
            }
            val target = pending
            if (target != null && managerKey(event.keyCode) && captureEdge.handle(event)) {
                draft?.assign(event.keyCode, target)
                pending = null
                revision++
                return true
            }
        }
        return pending != null
    }

    fun observeMotion(event: MotionEvent) {
        if (event.deviceId != selected?.id) return
        probe.motion(event, manager.calibration(event.device))
        event.device?.motionRanges?.forEach { probe.axis(it.axis, event.getAxisValue(it.axis)) }
        hats.update(event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
            .forEach { (code, down) ->
                probe.key(code, down)
            }
        probeRevision++
    }

    @Composable
    fun Panel() {
        PhysicalControllerMappingPanel(
            inputDeviceManager = manager,
            inventory = controller?.let(NativeControlInventory::forController),
            cartridge = cartridge,
            portCapacity = portCapacity,
            perCartridge = perCartridge,
            onScope = {
                perCartridge = it
                select(selected)
            },
            onUseDevice = {
                val device = selected
                if (device != null && cartridge != null) {
                    scope.launch {
                        manager.profiles.useDevice(device.descriptor, cartridge)
                        select(device)
                    }
                }
            },
            runtimePort = if (runtime) playerPort else null,
            captureHatBindings = false,
            captureTarget = pending,
            mappingRevision = revision,
            generation = generation,
            draft = draft,
            probe = probe,
            probeRevision = probeRevision,
            message = message,
            onSelect = ::select,
            onCapture = { _, target ->
                cancelCapture()
                pending = target
            },
            onCancel = ::cancelCapture,
            onReset = { device ->
                cancelCapture()
                draft?.reset(manager.defaultBindings(device).mapKeys { it.key.keyCode }.mapValues { it.value.keyCode })
                revision++
            },
            onUndo = {
                draft?.undo()
                revision++
            },
            onDiscard = ::discard,
            onResolveConflict = {
                draft?.resolveConflict()
                revision++
            },
            onSave = ::save,
        )
    }

    private fun save() {
        cancelCapture()
        val device = selected ?: return
        val transaction = draft ?: return
        if (transaction.conflict != null) return
        val reviewed = transaction.bindings.toMap()
        scope.launch {
            val live = InputDevice.getDevice(device.id)
            if (live == null || live.descriptor != device.descriptor || selected?.id != device.id) {
                select(null)
                return@launch
            }
            try {
                if (perCartridge && cartridge != null) {
                    manager.saveCartridgeBindings(live, cartridge, reviewed)
                } else {
                    manager.saveDeviceBindings(
                        live,
                        reviewed.mapKeys { InputKey(it.key) }.mapValues { RetroKey(it.value) },
                    )
                }
                if (transaction.bindings == reviewed) transaction.saved()
                message = "Controller mappings saved."
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                message = "Mappings were not saved. Try again."
            }
            revision++
        }
    }

    private fun managerKey(code: Int) =
        code != KeyEvent.KEYCODE_UNKNOWN &&
            code != KeyEvent.KEYCODE_BUTTON_MODE && InputDeviceManager.OUTPUT_KEYS.any { it.keyCode == code }
}
