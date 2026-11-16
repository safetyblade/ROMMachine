package com.swordfish.lemuroid.app.shared.input

import android.content.Context
import android.content.SharedPreferences
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent
import androidx.core.content.edit
import com.fredporciuncula.flow.preferences.FlowSharedPreferences
import com.swordfish.lemuroid.app.shared.input.lemuroiddevice.getLemuroidInputDevice
import com.swordfish.lemuroid.app.shared.settings.GameShortcut
import com.swordfish.lemuroid.app.shared.settings.GameShortcutType
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.PairSerializer
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class InputDeviceManager(
    private val context: Context,
    sharedPreferencesFactory: Lazy<SharedPreferences>,
) {
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager

    private val sharedPreferences by lazy { sharedPreferencesFactory.get() }

    private val flowSharedPreferences by lazy { FlowSharedPreferences(sharedPreferences) }

    fun getInputBindingsObservable(cartridge: String? = null): Flow<(InputDevice?) -> Map<InputKey, RetroKey>> {
        return getEnabledInputsObservable()
            .flatMapLatest { devices ->
                if (devices.isEmpty()) {
                    return@flatMapLatest flowOf<Map<InputDevice, Map<InputKey, RetroKey>>>(emptyMap())
                }
                val allDeviceBindingsFlows =
                    devices.map {
                            device ->
                        getBindingsFlow(device, cartridge).map { device to it }
                    }
                combine(allDeviceBindingsFlows) { it.toMap() }
            }
            .map { bindings -> { bindings[it] ?: mapOf() } }
    }

    fun getGameShortcutsObservable(): Flow<Map<InputDevice, List<GameShortcut>>> {
        return getEnabledInputsObservable()
            .flatMapLatest { devices ->
                if (devices.isEmpty()) {
                    return@flatMapLatest flowOf<Map<InputDevice, List<GameShortcut>>>(emptyMap())
                }
                val allShortcutFlows = devices.map { device -> getShortcutBindingsFlow(device).map { device to it } }
                combine(allShortcutFlows) { it.toMap() }
            }
    }

    val profiles = ControllerProfileStore(context)
    private val cartridgeOverrides = java.util.concurrent.ConcurrentHashMap<Pair<String, String?>, Boolean>()
    private val canonicalBindings = java.util.concurrent.ConcurrentHashMap<Pair<String, String?>, Boolean>()
    private val calibrationCache = java.util.concurrent.ConcurrentHashMap<String, ControllerProfileStore.Profile>()

    private val playerSlots = ControllerPortRegistry(16)

    fun beginGameplaySession() = playerSlots.clear()

    fun getGamePadsPortMapperObservable(): Flow<(InputDevice?) -> Int?> =
        combine(getEnabledInputsObservable(), profiles.changes()) { gamePads, _ ->
            playerSlots.update(gamePads.map { ControllerPortRegistry.Device(it.id, it.descriptor) })
            gamePads.filter { device -> gamePads.count { it.descriptor == device.descriptor } == 1 }.forEach { device ->
                runCatching { profiles.read(device.descriptor).playerPort }.getOrNull()?.let {
                    playerSlots.assign(device.id, it)
                }
            }
            val mappings = playerSlots.update(gamePads.map { ControllerPortRegistry.Device(it.id, it.descriptor) })
            return@combine { inputDevice: InputDevice? -> mappings[inputDevice?.id] }
        }.flowOn(Dispatchers.IO)

    suspend fun assignPlayerPort(
        device: InputDevice,
        port: Int,
    ) = withContext(Dispatchers.IO) {
        require(port in 0..15)
        val devices = getAllGamePads()
        require(devices.count { it.descriptor == device.descriptor } == 1) {
            "Identical descriptors require reconnect verification; existing assignments retained"
        }
        val previous = profiles.read(device.descriptor)
        devices.filter { it.descriptor != device.descriptor }.forEach { other ->
            val profile = profiles.read(other.descriptor)
            if (profile.playerPort == port) {
                profiles.save(
                    other.descriptor,
                    null,
                    profile.copy(playerPort = previous.playerPort),
                )
            }
        }
        profiles.save(device.descriptor, null, previous.copy(playerPort = port))
    }

    private fun getBindingsFlow(
        inputDevice: InputDevice,
        cartridge: String?,
    ): Flow<Map<InputKey, RetroKey>> =
        combine(
            flowSharedPreferences.getString(computeKeyBindingGamePadPreference(inputDevice)).asFlow(),
            profiles.changes(),
        ) { preference, _ ->
            profileBindings(inputDevice, cartridge) ?: parseBindingsPreference(preference, inputDevice)
        }.flowOn(Dispatchers.IO)

    private fun profileBindings(
        device: InputDevice,
        cartridge: String?,
    ): Map<InputKey, RetroKey>? {
        val profile =
            runCatching {
                profiles.read(device.descriptor, cartridge?.takeIf { it.isNotBlank() })
            }.getOrNull() ?: return null
        calibrationCache[device.descriptor] = profile
        canonicalBindings[device.descriptor to cartridge] = profile.buttons != null
        canonicalBindings[device.descriptor to null] =
            runCatching {
                profiles.read(device.descriptor).buttons != null
            }.getOrDefault(false)
        cartridgeOverrides[device.descriptor to cartridge] = cartridge?.takeIf { it.isNotBlank() }?.let {
            runCatching { profiles.hasOverride(device.descriptor, it) }.getOrDefault(false)
        } ?: false
        return profile.buttons?.takeIf { values -> values.values.all { RetroKey(it) in OUTPUT_KEYS } }
            ?.mapKeys { InputKey(it.key) }?.mapValues { RetroKey(it.value) }
    }

    fun hasCanonicalBinding(
        device: InputDevice?,
        cartridgeOnly: Boolean = false,
        cartridge: String? = null,
    ): Boolean =
        device?.descriptor?.let {
            if (cartridgeOnly) {
                cartridgeOverrides[it to cartridge] == true
            } else {
                canonicalBindings[it to cartridge] == true
            }
        } == true

    fun calibration(device: InputDevice?): ControllerProfileStore.Profile =
        device?.descriptor?.let { calibrationCache[it] } ?: ControllerProfileStore.Profile()

    fun calibratedAxis(
        event: android.view.MotionEvent,
        axis: Int,
    ): Float =
        PhysicalControllerInput.sample(
            event,
            axis,
            event.device?.descriptor?.let { calibrationCache[it] } ?: ControllerProfileStore.Profile(),
        )

    suspend fun saveCartridgeBindings(
        device: InputDevice,
        cartridge: String,
        bindings: Map<Int, Int>,
    ) = withContext(Dispatchers.IO) {
        require(bindings.values.all { RetroKey(it) in OUTPUT_KEYS })
        profiles.save(device.descriptor, cartridge, ControllerProfileStore.Profile(buttons = bindings))
    }

    private fun getShortcutBindingsFlow(device: InputDevice): Flow<List<GameShortcut>> {
        val flows =
            GameShortcutType.entries.map { type ->
                combine(
                    flowSharedPreferences.getString(computeGameShortcutPreference(device, type)).asFlow(),
                    profiles.changes(),
                ) { preference, _ -> parseShortcutPreference(preference, device, type) }
            }
        return if (flows.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(flows) { shortcuts -> shortcuts.mapNotNull { it } }
        }.flowOn(Dispatchers.IO)
    }

    private fun parseShortcutPreference(
        preference: String?,
        device: InputDevice,
        type: GameShortcutType,
    ): GameShortcut? {
        if (type == GameShortcutType.MENU) {
            val keys = runCatching { profiles.read(device.descriptor).menuKeys }.getOrNull()
            if (keys?.size == 2) return GameShortcut(type, keys)
        }
        return if (preference.isNullOrEmpty()) {
            GameShortcut.getDefault(device, type)
        } else {
            val decoded = runCatching { Json.decodeFromString(bindingsComboSerializer, preference) }
            val combo = decoded.getOrNull() ?: return GameShortcut.getDefault(device, type)
            GameShortcut(type = type, keys = setOf(combo.first.keyCode, combo.second.keyCode))
        }
    }

    suspend fun getCurrentBindings(
        inputDevice: InputDevice,
        cartridge: String? = null,
    ): Map<InputKey, RetroKey> {
        return withContext(Dispatchers.IO) {
            val preference =
                sharedPreferences.getString(
                    computeKeyBindingGamePadPreference(inputDevice),
                    "",
                )
            profileBindings(inputDevice, cartridge) ?: parseBindingsPreference(preference, inputDevice)
        }
    }

    suspend fun getCurrentShortcuts(inputDevice: InputDevice): List<GameShortcut> {
        return withContext(Dispatchers.IO) {
            GameShortcutType.entries.mapNotNull { type ->
                val preference = sharedPreferences.getString(computeGameShortcutPreference(inputDevice, type), "")
                parseShortcutPreference(preference, inputDevice, type)
            }
        }
    }

    private fun parseBindingsPreference(
        preference: String?,
        inputDevice: InputDevice,
    ): Map<InputKey, RetroKey> {
        val defaultBindings = getDefaultBinding(inputDevice)
        if (preference.isNullOrEmpty()) {
            return defaultBindings
        }

        val decoded = runCatching { Json.decodeFromString(bindingsMapSerializer, preference) }
        return decoded.getOrDefault(defaultBindings)
    }

    suspend fun updateBinding(
        inputDevice: InputDevice,
        retroKey: RetroKey,
        inputKey: InputKey,
    ) = withContext(Dispatchers.IO) {
        val prevBindings =
            getCurrentBindings(inputDevice).entries
                .map { it.key to it.value }
                .filter { (_, value) -> value != retroKey }

        val bindings = prevBindings + listOf(inputKey to retroKey)

        saveDeviceBindings(inputDevice, bindings.toMap())
    }

    fun defaultBindings(inputDevice: InputDevice): Map<InputKey, RetroKey> = getDefaultBinding(inputDevice)

    /** Commit a reviewed map atomically under the existing stable descriptor preference. */
    suspend fun saveDeviceBindings(
        inputDevice: InputDevice,
        bindings: Map<InputKey, RetroKey>,
    ) = withContext(Dispatchers.IO) {
        require(bindings.values.all { it in OUTPUT_KEYS })
        val oldProfile = profiles.read(inputDevice.descriptor)
        profiles.save(
            inputDevice.descriptor,
            null,
            oldProfile.copy(
                buttons = bindings.mapKeys { it.key.keyCode }.mapValues { it.value.keyCode },
            ),
        )
        check(
            sharedPreferences.edit()
                .putString(
                    computeKeyBindingGamePadPreference(inputDevice),
                    Json.encodeToString(bindingsMapSerializer, bindings),
                )
                .commit(),
        ) { "Controller preferences could not be saved" }
    }

    suspend fun updateShortcutBinding(
        inputDevice: InputDevice,
        shortcutType: GameShortcutType,
        inputKeys: Pair<InputKey, InputKey>,
    ) = withContext(Dispatchers.IO) {
        if (shortcutType == GameShortcutType.MENU) {
            val keys = setOf(inputKeys.first.keyCode, inputKeys.second.keyCode)
            require(keys.size == 2 && keys.all { it in SAFE_MENU_KEYS })
            val previous = profiles.read(inputDevice.descriptor)
            profiles.save(inputDevice.descriptor, null, previous.copy(menuKeys = keys))
        }
        sharedPreferences.edit(commit = true) {
            val key = computeGameShortcutPreference(inputDevice, shortcutType)
            val value = Json.encodeToString(bindingsComboSerializer, inputKeys)
            putString(key, value)
        }
    }

    /** Reset only one physical device; do not delete other pads' mappings or shortcuts. */
    suspend fun resetDeviceBindings(inputDevice: InputDevice) =
        withContext(Dispatchers.IO) {
            val profile = profiles.read(inputDevice.descriptor)
            profiles.save(inputDevice.descriptor, null, profile.copy(buttons = null))
            sharedPreferences.edit(commit = true) {
                remove(computeKeyBindingGamePadPreference(inputDevice))
            }
        }

    suspend fun resetAllBindings() =
        withContext(Dispatchers.IO) {
            profiles.resetMappings()
            sharedPreferences.edit(commit = true) {
                sharedPreferences.all.keys
                    .filter { it.startsWith(GAME_PAD_BINDING_PREFERENCE_BASE_KEY) }
                    .forEach { remove(it) }
            }
        }

    fun getGamePadsObservable(): Flow<List<InputDevice>> {
        val result = MutableStateFlow(getAllGamePads())

        val listener =
            object : InputManager.InputDeviceListener {
                override fun onInputDeviceAdded(deviceId: Int) {
                    result.value = getAllGamePads()
                }

                override fun onInputDeviceChanged(deviceId: Int) {
                    result.value = getAllGamePads()
                }

                override fun onInputDeviceRemoved(deviceId: Int) {
                    result.value = getAllGamePads()
                }
            }

        return result
            .onSubscription {
                inputManager.registerInputDeviceListener(
                    listener,
                    android.os.Handler(android.os.Looper.getMainLooper()),
                )
            }
            .onCompletion { inputManager.unregisterInputDeviceListener(listener) }
    }

    fun getDistinctGamePadsObservable(): Flow<List<InputDevice>> {
        return getGamePadsObservable()
            .map { device -> device.distinctBy { it.descriptor } }
    }

    fun getEnabledInputsObservable(): Flow<List<InputDevice>> {
        return getGamePadsObservable()
            .flatMapLatest { devices ->
                if (devices.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList())
                }

                val deviceStatuesFlows = devices.map { getDeviceStatus(it) }
                combine(deviceStatuesFlows) { deviceStatues ->
                    deviceStatues
                        .filter { it.enabled }
                        .map { it.device }
                }
            }
    }

    private fun getDeviceStatus(inputDevice: InputDevice): Flow<DeviceStatus> {
        val defaultValue =
            ControllerHardwareProfile.isGameplayPad(inputDevice) ||
                inputDevice.getLemuroidInputDevice().isEnabledByDefault(context)
        return flowSharedPreferences.getBoolean(computeEnabledGamePadPreference(inputDevice), defaultValue)
            .asFlow()
            .map { isEnabled -> DeviceStatus(inputDevice, isEnabled) }
    }

    private fun getDefaultBinding(inputDevice: InputDevice): Map<InputKey, RetroKey> {
        return inputDevice
            .getLemuroidInputDevice()
            .getDefaultBindings()
    }

    private fun getAllGamePads(): List<InputDevice> {
        return runCatching {
            InputDevice.getDeviceIds()
                .map { InputDevice.getDevice(it) }
                .filterNotNull()
                .filter { it.getLemuroidInputDevice().isSupported() || ControllerHardwareProfile.isGameplayPad(it) }
                .filter { it.name !in BLACKLISTED_DEVICES }
                .sortedBy { it.controllerNumber }
        }.getOrNull() ?: listOf()
    }

    private data class DeviceStatus(val device: InputDevice, val enabled: Boolean)

    companion object {
        private const val GAME_PAD_BINDING_PREFERENCE_BASE_KEY = "pref_key_gamepad_binding_key"
        private const val GAME_PAD_ENABLED_PREFERENCE_BASE_KEY = "pref_key_gamepad_enabled"

        private val bindingsMapSerializer = MapSerializer(InputKey.serializer(), RetroKey.serializer())
        private val bindingsComboSerializer = PairSerializer(InputKey.serializer(), InputKey.serializer())

        private fun getSharedPreferencesId(inputDevice: InputDevice) = inputDevice.descriptor

        // This is a last resort, but sadly there are some devices which present keys and the
        // SOURCE_GAMEPAD, so we basically black list them.
        private val BLACKLISTED_DEVICES =
            setOf(
                "virtual-search",
            )

        fun computeEnabledGamePadPreference(inputDevice: InputDevice) =
            "${GAME_PAD_ENABLED_PREFERENCE_BASE_KEY}_${getSharedPreferencesId(inputDevice)}"

        fun computeGameShortcutPreference(
            inputDevice: InputDevice,
            type: GameShortcutType,
        ) = "${GAME_PAD_BINDING_PREFERENCE_BASE_KEY}_${getSharedPreferencesId(inputDevice)}_shortcut_$type."

        fun computeKeyBindingGamePadPreference(inputDevice: InputDevice) =
            "${GAME_PAD_BINDING_PREFERENCE_BASE_KEY}_${getSharedPreferencesId(inputDevice)}"

        fun computeKeyBindingRetroKeyPreference(
            inputDevice: InputDevice,
            retroKey: RetroKey,
        ): String {
            val keyCode = retroKey.keyCode
            return "${GAME_PAD_BINDING_PREFERENCE_BASE_KEY}_${getSharedPreferencesId(inputDevice)}_$keyCode"
        }

        val SAFE_MENU_KEYS =
            setOf(
                KeyEvent.KEYCODE_BUTTON_THUMBL,
                KeyEvent.KEYCODE_BUTTON_THUMBR,
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_BUTTON_R1,
            )

        val OUTPUT_KEYS: List<RetroKey> =
            retroKeysOf(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_BUTTON_Y,
                KeyEvent.KEYCODE_BUTTON_START,
                KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_L1,
                KeyEvent.KEYCODE_BUTTON_L2,
                KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_BUTTON_R2,
                KeyEvent.KEYCODE_BUTTON_THUMBL,
                KeyEvent.KEYCODE_BUTTON_THUMBR,
                KeyEvent.KEYCODE_BUTTON_MODE,
                KeyEvent.KEYCODE_UNKNOWN,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT,
            )
    }
}
