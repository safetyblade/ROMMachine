@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.coreoptions.GameMenuControllerOptions
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.coreoptions.GameMenuCoreOptionsScreen
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.coreoptions.GameMenuCoreOptionsViewModel
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.states.GameMenuStatesScreen
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.states.GameMenuStatesViewModel
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.shared.coreoptions.LemuroidCoreOption
import com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile
import com.swordfish.lemuroid.app.shared.input.ControllerHatEdges
import com.swordfish.lemuroid.app.shared.input.ControllerInputProbe
import com.swordfish.lemuroid.app.shared.input.ControllerMappingDraft
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.input.InputKey
import com.swordfish.lemuroid.app.shared.input.RetroKey
import com.swordfish.lemuroid.app.shared.input.handle
import com.swordfish.lemuroid.app.shared.input.observeControllerMenuEdges
import com.swordfish.lemuroid.common.kotlin.serializable
import com.swordfish.lemuroid.lib.android.RetrogradeComponentActivity
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.NativeControlInventory
import kotlinx.coroutines.launch
import java.security.InvalidParameterException
import javax.inject.Inject

class GameMenuActivity : RetrogradeComponentActivity() {
    @Inject
    lateinit var inputDeviceManager: InputDeviceManager

    /** Capture only while the user explicitly chooses Map; never steal normal gamepad menus. */
    private var pendingBinding by mutableStateOf<Pair<Int, Int>?>(null)
    private var lastPhysicalKey by mutableStateOf<Int?>(null)
    private var mappingRevision by mutableStateOf(0)
    private var mappingDraft by mutableStateOf<ControllerMappingDraft?>(null)
    private var mappingDevice: InputDevice? = null
    private val captureEdge = ControllerMenuEdge()
    private val probe = ControllerInputProbe()
    private val hatEdges = ControllerHatEdges()
    private var probeRevision by mutableStateOf(0)
    private var mappingMessage by mutableStateOf<String?>(null)
    private var mappingGeneration by mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        mappingGeneration++
    }

    private fun cancelCapture() {
        pendingBinding = null
        captureEdge.clear()
        probe.clear()
        probeRevision++
    }

    private fun selectMappingDevice(device: InputDevice?) {
        cancelCapture()
        mappingDraft?.cancel()
        mappingDraft = null
        mappingDevice = device
        hatEdges.clear()
        mappingMessage = null
        if (device != null) {
            lifecycleScope.launch {
                val initial = inputDeviceManager.getCurrentBindings(device)
                if (mappingDevice?.id == device.id) {
                    mappingDraft =
                        ControllerMappingDraft(
                            initial.mapKeys { it.key.keyCode }.mapValues { it.value.keyCode },
                        )
                }
            }
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.deviceId == mappingDevice?.id) {
            event.device?.motionRanges?.forEach { probe.axis(it.axis, event.getAxisValue(it.axis)) }
            hatEdges.update(event.getAxisValue(MotionEvent.AXIS_HAT_X), event.getAxisValue(MotionEvent.AXIS_HAT_Y))
                .forEach { (code, down) ->
                    probe.key(code, down)
                    val capture = pendingBinding
                    if (capture?.first == event.deviceId && captureEdge.handle(event.deviceId, code, down)) {
                        mappingDraft?.assign(code, capture.second)
                        pendingBinding = null
                        mappingRevision++
                    }
                }
            probeRevision++
        }
        return pendingBinding != null || super.dispatchGenericMotionEvent(event)
    }

    private val menuEdge = ControllerMenuEdge()

    override fun onPause() {
        menuEdge.clear()
        cancelCapture()
        mappingDraft?.cancel()
        mappingDraft = null
        mappingDevice = null
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (menuEdge.handle(event)) {
                pendingBinding = null
                finish()
            }
            return true
        }
        if (event.deviceId == mappingDevice?.id &&
            event.device?.let(ControllerHardwareProfile::isGameplayPad) == true
        ) {
            if (event.isCanceled) {
                cancelCapture()
                return true
            }
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                lastPhysicalKey = event.keyCode
                probe.key(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
                probeRevision++
            }
        }
        val capture = pendingBinding
        if (capture != null) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (menuEdge.handle(event)) cancelCapture()
                return true
            }
            if (event.deviceId == capture.first && captureEdge.handle(event) &&
                InputDeviceManager.OUTPUT_KEYS.any { it.keyCode == event.keyCode } &&
                event.keyCode != KeyEvent.KEYCODE_UNKNOWN
            ) {
                pendingBinding = null
                mappingDraft?.assign(event.keyCode, capture.second)
                mappingRevision++
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Inject
    lateinit var statesManager: StatesManager

    @Inject
    lateinit var statesPreviewManager: StatesPreviewManager

    data class GameMenuRequest(
        val coreOptions: List<LemuroidCoreOption>,
        val advancedCoreOptions: List<LemuroidCoreOption>,
        val game: Game,
        val coreConfig: SystemCoreConfig,
        val currentControllerName: String?,
        val audioEnabled: Boolean,
        val fastForwardSupported: Boolean,
        val fastForwardEnabled: Boolean,
        val numDisks: Int,
        val currentDisk: Int,
        val currentTiltConfiguration: TiltConfiguration,
        val allTiltConfigurations: List<TiltConfiguration>,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeControllerMenuEdges(this, menuEdge, captureEdge)

        enableEdgeToEdge(
            SystemBarStyle.dark(Color.TRANSPARENT),
            SystemBarStyle.dark(Color.TRANSPARENT),
        )

        val extras = intent.extras

        val gameMenuRequest =
            GameMenuRequest(
                coreOptions =
                    intent.serializable<Array<LemuroidCoreOption>>(GameMenuContract.EXTRA_CORE_OPTIONS)
                        ?.toList()
                        ?: throw InvalidParameterException("Missing EXTRA_CORE_OPTIONS"),
                advancedCoreOptions =
                    intent.serializable<Array<LemuroidCoreOption>>(GameMenuContract.EXTRA_ADVANCED_CORE_OPTIONS)
                        ?.toList()
                        ?: throw InvalidParameterException("Missing EXTRA_ADVANCED_CORE_OPTIONS"),
                game =
                    intent.serializable<Game>(GameMenuContract.EXTRA_GAME)
                        ?: throw InvalidParameterException("Missing EXTRA_GAME"),
                coreConfig =
                    intent.serializable<SystemCoreConfig>(GameMenuContract.EXTRA_SYSTEM_CORE_CONFIG)
                        ?: throw InvalidParameterException("Missing EXTRA_SYSTEM_CORE_CONFIG"),
                currentControllerName =
                    extras?.getString(GameMenuContract.EXTRA_CURRENT_CONTROLLER_NAME),
                audioEnabled =
                    extras?.getBoolean(GameMenuContract.EXTRA_AUDIO_ENABLED, false) ?: false,
                fastForwardSupported =
                    extras?.getBoolean(GameMenuContract.EXTRA_FAST_FORWARD_SUPPORTED, false) ?: false,
                fastForwardEnabled =
                    extras?.getBoolean(GameMenuContract.EXTRA_FAST_FORWARD, false) ?: false,
                numDisks =
                    extras?.getInt(GameMenuContract.EXTRA_DISKS, 0) ?: 0,
                currentDisk =
                    extras?.getInt(GameMenuContract.EXTRA_CURRENT_DISK, 0) ?: 0,
                currentTiltConfiguration =
                    intent.serializable<TiltConfiguration>(GameMenuContract.EXTRA_CURRENT_TILT_CONFIG)
                        ?: TiltConfiguration.Disabled,
                allTiltConfigurations =
                    intent.serializable<Array<TiltConfiguration>>(GameMenuContract.EXTRA_TILT_ALL_CONFIGS)
                        ?.toList()
                        ?: emptyList(),
            )

        setContent {
            GameMenuScreen(gameMenuRequest)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GameMenuScreen(gameMenuRequest: GameMenuRequest) {
        GameMenuTheme {
            val hostCapabilities =
                GameplayMenuHostProfiles.capabilities(
                    GameplayMenuHostKind.LIBRETRO,
                    gameMenuRequest.coreConfig.statesSupported,
                )
            val navController = rememberNavController()
            val entry = navController.currentBackStackEntryAsState().value
            val route = entry?.destination?.route?.let { GameMenuRoute.findByRoute(it) } ?: GameMenuRoute.HOME
            val page =
                when (route) {
                    // The active engine has not acknowledged a pause here. Do not claim one.
                    GameMenuRoute.HOME -> "Game menu"
                    GameMenuRoute.CONTROLS -> "Controls"
                    GameMenuRoute.DISPLAY -> "Display"
                    GameMenuRoute.STATES -> "Save & Load"
                    GameMenuRoute.OPTIONS -> "Game Settings"
                    GameMenuRoute.ADVANCED -> "Advanced"
                    GameMenuRoute.GRIPARC -> "GripArc"
                    GameMenuRoute.PIVOTARC -> "PivotArc"
                    else -> stringResource(route.titleId)
                }
            GameMenuPanel(
                gameMenuRequest.game.displayName(),
                com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel(gameMenuRequest.game.systemId),
                page,
                route.canGoBack(),
                onBack = { if (!navController.popBackStack()) onResult { } },
                onResume = { onResult { } },
                statusMessage =
                    if (route != GameMenuRoute.ADVANCED || hostCapabilities.canPause) {
                        null
                    } else {
                        "Menu open. Engine pause is not confirmed."
                    },
            ) {
                NavHost(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    navController = navController,
                    startDestination = GameMenuRoute.HOME.route,
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                ) {
                    composable(GameMenuRoute.HOME) {
                        GameMenuHomeScreen(navController, gameMenuRequest, ::onResult)
                    }
                    composable(
                        GameMenuRoute.DISPLAY,
                    ) { DisplaySettingsScreen(gameMenuRequest.game, libretroHost = true) }
                    composable(GameMenuRoute.STATES) { GameMenuStatesHome(navController, gameMenuRequest) }
                    composable(GameMenuRoute.ADVANCED) { GameMenuAdvancedScreen(gameMenuRequest, ::onResult) }
                    composable(GameMenuRoute.PIVOTARC) {
                        com.swordfish.lemuroid.app.mobile.feature.settings.layout.PivotArcSettingsScreen()
                    }
                    composable(GameMenuRoute.SAVE) {
                        GameMenuStatesScreen(
                            viewModel(
                                factory =
                                    GameMenuStatesViewModel.Factory(
                                        application,
                                        gameMenuRequest,
                                        statesManager,
                                        false,
                                        statesPreviewManager,
                                    ),
                            ),
                            onStateClicked = {
                                onResult { putExtra(GameMenuContract.RESULT_SAVE, it) }
                            },
                        )
                    }
                    composable(GameMenuRoute.LOAD) {
                        GameMenuStatesScreen(
                            viewModel(
                                factory =
                                    GameMenuStatesViewModel.Factory(
                                        application,
                                        gameMenuRequest,
                                        statesManager,
                                        true,
                                        statesPreviewManager,
                                    ),
                            ),
                            onStateClicked = {
                                onResult { putExtra(GameMenuContract.RESULT_LOAD, it) }
                            },
                        )
                    }
                    composable(GameMenuRoute.CONTROLS) {
                        GameMenuControlsScreen(
                            navController = navController,
                            gameMenuRequest = gameMenuRequest,
                            onResult = ::onResult,
                            controllerOptions = {
                                GameMenuControllerOptions(
                                    viewModel(factory = GameMenuCoreOptionsViewModel.Factory(inputDeviceManager)),
                                    gameMenuRequest,
                                )
                            },
                            physicalControllerOptions = {
                                PhysicalControllerMappingPanel(
                                    inputDeviceManager = inputDeviceManager,
                                    inventory =
                                        gameMenuRequest.coreConfig.controllerConfigs[0]?.let { controllers ->
                                            val selected =
                                                controllers.firstOrNull { controller ->
                                                    controller.name == gameMenuRequest.currentControllerName
                                                }
                                                    ?: controllers.firstOrNull()
                                            selected?.touchControllerID?.let {
                                                NativeControlInventory.forController(
                                                    it,
                                                )
                                            }
                                        },
                                    captureTarget = pendingBinding?.second,
                                    mappingRevision = mappingRevision,
                                    generation = mappingGeneration,
                                    draft = mappingDraft,
                                    probe = probe,
                                    probeRevision = probeRevision,
                                    message = mappingMessage,
                                    onSelect = ::selectMappingDevice,
                                    onCapture = { id, native ->
                                        cancelCapture()
                                        pendingBinding = id to native
                                    },
                                    onCancel = ::cancelCapture,
                                    onReset = { device ->
                                        cancelCapture()
                                        mappingDraft?.reset(
                                            inputDeviceManager.defaultBindings(device)
                                                .mapKeys { it.key.keyCode }.mapValues { it.value.keyCode },
                                        )
                                        mappingRevision++
                                    },
                                    onUndo = {
                                        mappingDraft?.undo()
                                        mappingRevision++
                                    },
                                    onDiscard = {
                                        cancelCapture()
                                        mappingDraft?.cancel()
                                        mappingRevision++
                                    },
                                    onResolveConflict = {
                                        mappingDraft?.resolveConflict()
                                        mappingRevision++
                                    },
                                    onSave = {
                                        cancelCapture()
                                        val device = mappingDevice
                                        val draft = mappingDraft
                                        if (device != null && draft != null && draft.conflict == null) {
                                            val reviewed = draft.bindings.toMap()
                                            lifecycleScope.launch {
                                                val live = InputDevice.getDevice(device.id)
                                                if (live != null && live.descriptor == device.descriptor &&
                                                    mappingDevice?.id == device.id
                                                ) {
                                                    try {
                                                        inputDeviceManager.saveDeviceBindings(
                                                            live,
                                                            reviewed.mapKeys {
                                                                InputKey(
                                                                    it.key,
                                                                )
                                                            }.mapValues { RetroKey(it.value) },
                                                        )
                                                        if (draft.bindings == reviewed) draft.saved()
                                                        mappingMessage = "Controller mappings saved."
                                                    } catch (error: Exception) {
                                                        if (error is kotlinx.coroutines.CancellationException) {
                                                            throw error
                                                        }
                                                        mappingMessage = "Mappings were not saved. Try again."
                                                    }
                                                } else {
                                                    selectMappingDevice(null)
                                                }
                                                mappingRevision++
                                            }
                                        }
                                    },
                                )
                            },
                        )
                    }
                    composable(GameMenuRoute.GRIPARC) {
                        GripArcEditorScreen(gameMenuRequest.game)
                    }
                    composable(GameMenuRoute.OPTIONS) {
                        GameMenuCoreOptionsScreen(gameMenuRequest)
                    }
                }
            }
        }
    }

    private fun onResult(block: Intent.() -> Unit) {
        val resultIntent = Intent()
        resultIntent.block()
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    @dagger.Module
    abstract class Module
}
