package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.content.Context
import android.graphics.PointF
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.input.ControllerChordGate
import com.swordfish.lemuroid.app.shared.input.ControllerDigitalAxisState
import com.swordfish.lemuroid.app.shared.input.ControllerFrameGate
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.input.InputKey
import com.swordfish.lemuroid.app.shared.input.PhysicalControllerInput
import com.swordfish.lemuroid.app.shared.input.handle
import com.swordfish.lemuroid.app.shared.input.inputclass.getInputClass
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.app.shared.settings.GameShortcutType
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.common.coroutines.safeCollect
import com.swordfish.lemuroid.common.kotlin.NTuple2
import com.swordfish.lemuroid.common.kotlin.NTuple4
import com.swordfish.lemuroid.common.kotlin.filterNotNullValues
import com.swordfish.lemuroid.common.kotlin.toIndexedMap
import com.swordfish.lemuroid.common.kotlin.zipOnKeys
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.libretrodroid.Controller
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroView.Companion.MOTION_SOURCE_ANALOG_LEFT
import com.swordfish.libretrodroid.GLRetroView.Companion.MOTION_SOURCE_ANALOG_RIGHT
import com.swordfish.libretrodroid.GLRetroView.Companion.MOTION_SOURCE_DPAD
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.abs

class GameViewModelInput(
    private val appContext: Context,
    private val system: GameSystem,
    private val systemCoreConfig: SystemCoreConfig,
    private val inputDeviceManager: InputDeviceManager,
    private val controllerConfigsManager: ControllerConfigsManager,
    private val retroGameView: GameViewModelRetroGameView,
    private val tilt: GameViewModelTilt,
    private val sideEffects: GameViewModelSideEffects,
    private val pivotArcHoldGate: PivotArcHeldInputGate,
    private val scope: CoroutineScope,
    private val game: com.swordfish.lemuroid.lib.library.db.entity.Game? = null,
) : DefaultLifecycleObserver {
    private val menuChord = ControllerChordGate()

    private data class KeyFrame(
        val event: KeyEvent,
        val sequence: Long,
        val generation: Long,
        val descriptor: String?,
    )

    private class MotionFrame(
        val device: InputDevice,
        val source: Int,
        val sequence: Long,
        val generation: Long,
        val axes: Map<Int, Float>,
        val ranges: Map<Int, PhysicalControllerInput.Range>,
    ) {
        fun axis(code: Int): Float = axes[code] ?: 0f
    }

    private var frameSequence = 0L
    private var inputGeneration = 0L
    private var activePhysicalPorts = emptyMap<Int, Int>()

    private val hasNativeTriggers =
        systemCoreConfig.coreID in
            setOf(
                CoreID.FLYCAST_MODERN, CoreID.DOLPHIN_MODERN, CoreID.PLAY_MODERN,
            )

    private fun sendNativeTriggers(
        event: MotionFrame,
        port: Int,
    ) {
        if (!hasNativeTriggers) return
        val left =
            maxOf(
                calibratedAxis(event, MotionEvent.AXIS_LTRIGGER),
                calibratedAxis(event, MotionEvent.AXIS_BRAKE),
            ).coerceIn(0f, 1f)
        val right =
            maxOf(
                calibratedAxis(event, MotionEvent.AXIS_RTRIGGER),
                calibratedAxis(event, MotionEvent.AXIS_GAS),
            ).coerceIn(0f, 1f)
        // Source 4 is the versioned CARTDECK host's pair of analog trigger axes.
        retroGameView.retroGameView?.sendMotionEvent(4, left, right, port)
    }

    private val controllerConfigsState = MutableStateFlow<Map<Int, ControllerConfig>>(mapOf())
    private val keyEventsFlow: MutableSharedFlow<KeyFrame> = MutableSharedFlow()
    private val motionEventsFlow: MutableSharedFlow<MotionFrame> = MutableSharedFlow()

    fun getAllTiltConfigurations(): List<TiltConfiguration> {
        return controllerConfigsState.value[0]
            ?.tiltConfigurations
            ?: emptyList()
    }

    fun getControllerConfigState(): Flow<Map<Int, ControllerConfig>> {
        return controllerConfigsState
    }

    fun selectedControllerName(): String? = controllerConfigsState.value[0]?.name

    /**
     * Switch Wii input devices while the emulator stays open.
     * The existing controller-state collector updates LibretroDroid and Compose
     * from the same state, without relaunching Dolphin or changing Wii NAND.
     */
    fun selectWiiController(name: String) {
        if (system.id != SystemID.WII) return
        val controller =
            systemCoreConfig.controllerConfigs[0]?.firstOrNull { it.name == name } ?: return
        scope.launch {
            if (controllerConfigsManager.selectController(system.id, systemCoreConfig, 0, name, game)) {
                controllerConfigsState.value = controllerConfigsState.value + (0 to controller)
            }
        }
    }

    private fun sendStickMotions(
        event: MotionFrame,
        port: Int,
    ) {
        if (port < 0) return
        sendNativeTriggers(event, port)
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            if (controllerConfigsState.value[port]?.mergeDPADAndLeftStickEvents == true) {
                sendMergedMotionEvents(event, port)
            } else {
                sendSeparateMotionEvents(event, port)
            }
        }
    }

    private fun sendMergedMotionEvents(
        event: MotionFrame,
        port: Int,
    ) {
        val events =
            listOf(
                retrieveCoordinates(event, MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y),
                retrieveCoordinates(event, MotionEvent.AXIS_X, MotionEvent.AXIS_Y),
            )

        val xVal = events.maxByOrNull { abs(it.x) }?.x ?: 0f
        val yVal = events.maxByOrNull { abs(it.y) }?.y ?: 0f

        retroGameView.retroGameView?.sendMotionEvent(MOTION_SOURCE_DPAD, xVal, yVal, port)
        retroGameView.retroGameView?.sendMotionEvent(MOTION_SOURCE_ANALOG_LEFT, xVal, yVal, port)

        sendStickMotion(
            event,
            MOTION_SOURCE_ANALOG_RIGHT,
            MotionEvent.AXIS_Z,
            MotionEvent.AXIS_RZ,
            port,
        )
    }

    private fun sendStickMotion(
        event: MotionFrame,
        source: Int,
        xAxis: Int,
        yAxis: Int,
        port: Int,
    ) {
        val coords = retrieveCoordinates(event, xAxis, yAxis)
        retroGameView.retroGameView?.sendMotionEvent(source, coords.x, coords.y, port)
    }

    private fun sendDPADMotion(
        event: MotionFrame,
        source: Int,
        xAxis: Int,
        yAxis: Int,
        port: Int,
    ) {
        retroGameView.retroGameView?.sendMotionEvent(
            source,
            calibratedAxis(event, xAxis),
            calibratedAxis(event, yAxis),
            port,
        )
    }

    private fun sendSeparateMotionEvents(
        event: MotionFrame,
        port: Int,
    ) {
        sendDPADMotion(
            event,
            MOTION_SOURCE_DPAD,
            MotionEvent.AXIS_HAT_X,
            MotionEvent.AXIS_HAT_Y,
            port,
        )
        sendStickMotion(
            event,
            MOTION_SOURCE_ANALOG_LEFT,
            MotionEvent.AXIS_X,
            MotionEvent.AXIS_Y,
            port,
        )
        sendStickMotion(
            event,
            MOTION_SOURCE_ANALOG_RIGHT,
            MotionEvent.AXIS_Z,
            MotionEvent.AXIS_RZ,
            port,
        )
    }

    private fun retrieveCoordinates(
        event: MotionFrame,
        xAxis: Int,
        yAxis: Int,
    ): PointF {
        return PointF(
            calibratedAxis(event, xAxis),
            calibratedAxis(event, yAxis),
        )
    }

    private fun calibratedAxis(
        event: MotionFrame,
        code: Int,
    ): Float =
        PhysicalControllerInput.sampleAxis(
            event::axis,
            event.ranges,
            code,
            inputDeviceManager.calibration(event.device),
            coreOwnsAutomaticResponse = systemCoreConfig.coreID == CoreID.FLYCAST_MODERN,
        )

    fun sendKeyEvent(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU || InputKey(keyCode) in event.device.getInputClass().getInputKeys()) {
            val frame = KeyFrame(KeyEvent(event), ++frameSequence, inputGeneration, event.device?.descriptor)
            scope.launch {
                keyEventsFlow.emit(frame)
            }
            return true
        }
        return false
    }

    fun sendMotionEvent(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_JOYSTICK)) return false
        if (event.actionMasked == MotionEvent.ACTION_CANCEL) {
            releasePhysicalInputs()
            return true
        }
        if (event.actionMasked != MotionEvent.ACTION_MOVE) return false
        val device = event.device ?: return false
        val ranges = PhysicalControllerInput.eventRanges(event)
        if (ranges.isEmpty()) return false
        // Android owns MotionEvent lifetime. Snapshot before handing off to the asynchronous flows.
        val frame =
            MotionFrame(
                device,
                event.source,
                ++frameSequence,
                inputGeneration,
                ranges.keys.associateWith { event.getAxisValue(it) },
                ranges,
            )
        scope.launch {
            motionEventsFlow.emit(frame)
        }
        return true
    }

    fun getEnabledInputDevices(): Flow<List<InputDevice>> {
        return inputDeviceManager.getEnabledInputsObservable()
    }

    private fun updateControllers(controllers: Map<Int, ControllerConfig>) {
        retroGameView.retroGameView
            ?.getControllers()?.toIndexedMap()
            ?.zipOnKeys(controllers, this::findControllerId)
            ?.filterNotNullValues()
            ?.forEach { (port, controllerId) ->
                Timber.i("Controls setting $port to $controllerId")
                retroGameView.retroGameView?.setControllerType(port, controllerId)
            }
    }

    private fun findControllerId(
        supported: Array<Controller>,
        controllerConfig: ControllerConfig,
    ): Int? {
        return supported
            .firstOrNull { controller ->
                sequenceOf(
                    controller.id == controllerConfig.libretroId,
                    controller.description == controllerConfig.libretroDescriptor,
                ).any { it }
            }?.id
    }

    private fun releasePhysicalInputs() {
        inputGeneration++
        menuChord.clear()
        pivotArcHoldGate.clear()
        val view = retroGameView.retroGameView ?: return
        (controllerConfigsState.value.keys + activePhysicalPorts.values).forEach { port ->
            InputDeviceManager.OUTPUT_KEYS.forEach { key ->
                view.sendKeyEvent(KeyEvent.ACTION_UP, key.keyCode, port)
            }
            listOf(MOTION_SOURCE_DPAD, MOTION_SOURCE_ANALOG_LEFT, MOTION_SOURCE_ANALOG_RIGHT).forEach { source ->
                view.sendMotionEvent(source, 0f, 0f, port)
            }
            if (hasNativeTriggers) view.sendMotionEvent(4, 0f, 0f, port)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        releasePhysicalInputs()
        super.onPause(owner)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        inputDeviceManager.beginGameplaySession()

        owner.launchOnState(Lifecycle.State.RESUMED) {
            var initialized = false
            inputDeviceManager.getGamePadsPortMapperObservable()
                .map(::currentPorts)
                .distinctUntilChanged()
                .safeCollect { ports ->
                    if (initialized && activePhysicalPorts != ports) releasePhysicalInputs()
                    activePhysicalPorts = ports
                    initialized = true
                }
        }

        owner.launchOnState(Lifecycle.State.CREATED) {
            initializeControllerConfigsFlow()
        }

        owner.launchOnState(Lifecycle.State.CREATED) {
            initializeGamePadShortcutsFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeGamePadKeysFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeVirtualGamePadMotionsFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeGamePadMotionsFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeControllersConfigFlow()
        }
    }

    private suspend fun initializeControllerConfigsFlow() {
        retroGameView.waitGLEvent<GLRetroView.GLRetroEvents.FrameRendered>()
        controllerConfigsState.safeCollect {
            updateControllers(it)
        }
    }

    private suspend fun initializeGamePadShortcutsFlow() {
        inputDeviceManager.getGameShortcutsObservable()
            .distinctUntilChanged()
            .safeCollect { allShortcuts ->
                allShortcuts.values
                    .firstNotNullOfOrNull { shortcuts ->
                        shortcuts.firstOrNull { it.type == GameShortcutType.MENU }
                    }
                    ?.let {
                        val message =
                            appContext.resources.getString(
                                R.string.game_toast_settings_button_using_gamepad,
                                it.name,
                            )
                        sideEffects.showToast(message)
                    }
            }
    }

    private fun currentPorts(mapper: (InputDevice?) -> Int?): Map<Int, Int> =
        InputDevice.getDeviceIds().asSequence()
            .mapNotNull { id -> mapper(InputDevice.getDevice(id))?.let { id to it } }
            .toMap()

    private fun MotionFrame.isCurrent(): Boolean =
        generation == inputGeneration && InputDevice.getDevice(device.id)?.descriptor == device.descriptor

    private suspend fun initializeGamePadKeysFlow() {
        val pressedByDevice = mutableMapOf<Int, MutableSet<Int>>()
        val activeChords = mutableMapOf<Int, Set<Set<Int>>>()
        val menuEdge = ControllerMenuEdge()
        val forwarded = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
        var lastPorts = emptyMap<Int, Int>()
        val frameGate = ControllerFrameGate()
        var generation = inputGeneration

        fun releaseHeld() {
            forwarded.values.forEach { (port, code) ->
                retroGameView.retroGameView?.sendKeyEvent(KeyEvent.ACTION_UP, code, port)
            }
            (lastPorts.values.toSet() + forwarded.values.map { it.first }).forEach { port ->
                listOf(MOTION_SOURCE_DPAD, MOTION_SOURCE_ANALOG_LEFT, MOTION_SOURCE_ANALOG_RIGHT).forEach { source ->
                    retroGameView.retroGameView?.sendMotionEvent(source, 0f, 0f, port)
                }
                if (hasNativeTriggers) retroGameView.retroGameView?.sendMotionEvent(4, 0f, 0f, port)
            }
            forwarded.clear()
            pressedByDevice.clear()
            activeChords.clear()
            pivotArcHoldGate.clear()
        }

        val filteredKeyEvents =
            keyEventsFlow
                .filter { it.event.repeatCount == 0 && it.event.device != null }

        val combinedObservable =
            combine(
                inputDeviceManager.getGameShortcutsObservable(),
                inputDeviceManager.getGamePadsPortMapperObservable(),
                inputDeviceManager.getInputBindingsObservable(game?.cartridgeId),
                filteredKeyEvents,
                ::NTuple4,
            )

        combinedObservable
            .onStart { releaseHeld() }
            .onCompletion {
                releaseHeld()
                menuEdge.clear()
            }
            .safeCollect { (shortcuts, ports, bindings, frame) ->
                val currentPorts = currentPorts(ports)
                if (generation != inputGeneration) {
                    forwarded.clear()
                    pressedByDevice.clear()
                    activeChords.clear()
                    menuEdge.clear()
                    lastPorts = emptyMap()
                    generation = inputGeneration
                }
                if (lastPorts != currentPorts) {
                    if (lastPorts.isNotEmpty()) releaseHeld()
                    lastPorts = currentPorts
                }
                if (!frameGate.accepts(frame.sequence, frame.generation, inputGeneration)) return@safeCollect
                val event = frame.event
                if (frame.descriptor != event.device?.descriptor) return@safeCollect
                val activeIds = currentPorts.keys
                menuEdge.retainDevices(activeIds)
                menuChord.retainDevices(activeIds)
                if (pressedByDevice.keys.any { it !in activeIds }) releaseHeld()
                val device = event.device ?: return@safeCollect
                val action = event.action
                val keyCode = event.keyCode
                if (event.isCanceled) {
                    releasePhysicalInputs()
                    releaseHeld()
                    menuEdge.clear()
                    return@safeCollect
                }
                val pressedKeys = pressedByDevice.getOrPut(device.id) { mutableSetOf() }
                val port = ports(device)
                val bindKeyCode = bindings(device)[InputKey(keyCode)]?.keyCode ?: keyCode

                // Host keys are owned on every port, including unassigned devices.
                if (bindKeyCode == KeyEvent.KEYCODE_BUTTON_MODE ||
                    keyCode in setOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_MODE)
                ) {
                    if (menuEdge.handle(event)) {
                        releaseHeld()
                        sideEffects.showMenu(tilt, this)
                    }
                    return@safeCollect
                }
                val menuKeys = shortcuts[device]?.firstOrNull { it.type == GameShortcutType.MENU }?.keys.orEmpty()
                val route = menuChord.route(device.id, keyCode, action == KeyEvent.ACTION_DOWN, menuKeys)
                if (route.consumed) {
                    if (route.invoke) {
                        releaseHeld()
                        sideEffects.showMenu(tilt, this)
                    } else if (port != null) {
                        route.replay.forEach { edge ->
                            val target = bindings(device)[InputKey(edge.key)]?.keyCode ?: edge.key
                            if ((port to target) in forwarded.values) return@forEach
                            retroGameView.retroGameView?.sendKeyEvent(
                                if (edge.down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                                target,
                                port,
                            )
                        }
                    }
                    return@safeCollect
                }
                val changed =
                    when (action) {
                        KeyEvent.ACTION_DOWN -> pressedKeys.add(keyCode)
                        KeyEvent.ACTION_UP -> pressedKeys.remove(keyCode)
                        else -> false
                    }
                if (!changed) return@safeCollect
                val complete =
                    shortcuts[device].orEmpty().filter {
                        it.keys.isNotEmpty() && pressedKeys.containsAll(it.keys)
                    }
                val previous = activeChords[device.id].orEmpty()
                activeChords[device.id] = complete.map { it.keys.toSet() }.toSet()
                if (port == 0 && action == KeyEvent.ACTION_DOWN) {
                    val shortcut = complete.firstOrNull { it.keys.toSet() !in previous }
                    if (shortcut != null) {
                        when (shortcut.type) {
                            GameShortcutType.MENU -> {
                                releaseHeld()
                                sideEffects.showMenu(tilt, this)
                            }
                            GameShortcutType.QUICK_LOAD -> sideEffects.loadQuickSave()
                            GameShortcutType.QUICK_SAVE -> sideEffects.saveQuickSave()
                            GameShortcutType.TOGGLE_FAST_FORWARD -> sideEffects.toggleFastForward()
                        }
                        return@safeCollect
                    }
                }

                // Some Android drivers emit a digital trigger at half travel as well
                // as its analog axis. Do not turn that event into full pressure.
                val analogTriggerKey =
                    hasNativeTriggers &&
                        when (keyCode) {
                            KeyEvent.KEYCODE_BUTTON_L2 ->
                                device.getMotionRange(MotionEvent.AXIS_LTRIGGER) != null ||
                                    device.getMotionRange(MotionEvent.AXIS_BRAKE) != null
                            KeyEvent.KEYCODE_BUTTON_R2 ->
                                device.getMotionRange(MotionEvent.AXIS_RTRIGGER) != null ||
                                    device.getMotionRange(MotionEvent.AXIS_GAS) != null
                            else -> false
                        }
                port?.takeUnless { analogTriggerKey }?.let { targetPort ->
                    val captured =
                        targetPort == 0 &&
                            pivotArcHoldGate.setSource(
                                bindKeyCode,
                                "physical:${device.id}:$keyCode",
                                action == KeyEvent.ACTION_DOWN,
                            )
                    if (!captured) {
                        retroGameView.retroGameView?.let { gameView ->
                            val output =
                                if (action == KeyEvent.ACTION_DOWN) {
                                    (targetPort to bindKeyCode).also { forwarded[device.id to keyCode] = it }
                                } else {
                                    forwarded.remove(device.id to keyCode) ?: (targetPort to bindKeyCode)
                                }
                            gameView.sendKeyEvent(action, output.second, output.first)
                            // Post-forward observation: never intercept or retry input.
                            retroGameView.onAcceptedMappedButton(
                                source = "physical:${device.id}:${output.first}",
                                mappedKey = output.second,
                                action = action,
                            )
                        }
                    }
                }
            }
    }

    private suspend fun initializeVirtualGamePadMotionsFlow() {
        val frameGate = ControllerFrameGate()
        var generation = inputGeneration
        val digital = ControllerDigitalAxisState()
        combine(
            inputDeviceManager.getGamePadsPortMapperObservable(),
            motionEventsFlow,
            inputDeviceManager.getInputBindingsObservable(game?.cartridgeId),
        ) { ports, event, bindings -> Triple(ports, event, bindings) }
            .safeCollect { (ports, event, bindingLookup) ->
                if (generation != inputGeneration) {
                    digital.clear()
                    generation = inputGeneration
                }
                if (!event.isCurrent() || !frameGate.accepts(event.sequence, event.generation, inputGeneration)) {
                    return@safeCollect
                }
                val port = ports(event.device) ?: return@safeCollect
                val bindings = bindingLookup(event.device)
                val axes =
                    event.device.getInputClass().getAxesMap().entries.filterNot { (axis, _) ->
                        hasNativeTriggers &&
                            axis in
                            setOf(
                                MotionEvent.AXIS_LTRIGGER,
                                MotionEvent.AXIS_RTRIGGER,
                                MotionEvent.AXIS_BRAKE,
                                MotionEvent.AXIS_GAS,
                            )
                    }
                val next =
                    axes.filter { (axis, _) -> calibratedAxis(event, axis) > 0.5f }
                        .map { (_, button) ->
                            bindings[InputKey(button)]?.keyCode?.takeUnless { it == KeyEvent.KEYCODE_UNKNOWN } ?: button
                        }.toSet()
                val view = retroGameView.retroGameView ?: return@safeCollect

                fun forward(
                    code: Int,
                    action: Int,
                ) {
                    view.sendKeyEvent(action, code, port)
                    retroGameView.onAcceptedMappedButton(
                        source = "axis:$port:$code",
                        mappedKey = code,
                        action = action,
                    )
                }
                digital.update(port, next).forEach { edge ->
                    forward(edge.code, if (edge.down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP)
                }
            }
    }

    private suspend fun initializeGamePadMotionsFlow() {
        val frameGate = ControllerFrameGate()
        combine(
            inputDeviceManager.getGamePadsPortMapperObservable(),
            motionEventsFlow,
            ::NTuple2,
        ).safeCollect { (ports, event) ->
            if (!event.isCurrent() || !frameGate.accepts(event.sequence, event.generation, inputGeneration)) {
                return@safeCollect
            }
            ports(event.device)?.let { sendStickMotions(event, it) }
        }
    }

    private suspend fun initializeControllersConfigFlow() {
        try {
            retroGameView.waitRetroGameViewInitialized()
            val controllers = controllerConfigsManager.getControllerConfigs(system.id, systemCoreConfig, game)
            controllerConfigsState.value = controllers
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
