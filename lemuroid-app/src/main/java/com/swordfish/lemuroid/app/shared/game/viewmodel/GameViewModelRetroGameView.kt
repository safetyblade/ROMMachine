package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLES20
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.app.shared.game.CoreOptionRefresh
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.LibretroEngineReport
import com.swordfish.lemuroid.app.shared.game.LibretroReportSession
import com.swordfish.lemuroid.app.shared.game.PS2LaunchJournal
import com.swordfish.lemuroid.app.shared.game.ShaderChooser
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.rumble.RumbleManager
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.app.utils.android.getGLSLVersion
import com.swordfish.lemuroid.common.coroutines.MutableStateProperty
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.common.view.disableTouchEvents
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.core.NativeCoreProvisioner
import com.swordfish.lemuroid.lib.game.GameLoader
import com.swordfish.lemuroid.lib.game.GameLoaderError
import com.swordfish.lemuroid.lib.game.GameLoaderException
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.RomFiles
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.Variable
import com.swordfish.libretrodroid.ViewportAlignment
import com.swordfish.libretrodroid.VirtualFile
import com.swordfish.touchinput.radial.settings.TouchControllerID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

private const val WII_POINTER_KEY = "dolphin_ir_mode"
private const val PCE_PAD_KEY = "pce_fast_default_joypad_type_p1"

@OptIn(FlowPreview::class)
class GameViewModelRetroGameView(
    private val appContext: Context,
    private val system: GameSystem,
    private val systemCoreConfig: SystemCoreConfig,
    private val settingsManager: SettingsManager,
    private val coreVariablesManager: CoreVariablesManager,
    private val sideEffects: GameViewModelSideEffects,
    private val rumbleManager: RumbleManager,
    private val controllerConfigsManager: ControllerConfigsManager,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    sealed interface GameState {
        data object Uninitialized : GameState

        data class Loading(val message: String) : GameState

        data class Loaded(
            val gameData: GameLoader.GameData,
            val retroViewData: GLRetroViewData,
        ) : GameState

        data object Ready : GameState
    }

    private var launchedGame: Game? = null
    private var reportSession: LibretroReportSession? = null

    @Volatile
    private var reportGraphics: String? = null

    @Volatile
    private var reportCoreFrameObserved = false

    private var wiiTimingCount = 0
    private var wiiTimingFirst = 0L
    private var wiiTimingLast = 0L
    private var wiiTimingLongestGap = 0L
    private var resetWiiTiming = true

    /** Snapshot only on menu invocation. The Wii timing observer keeps four bounded counters. */
    fun engineReportSession(options: List<CoreVariable>): LibretroReportSession? =
        reportSession?.copy(
            coreFrameObserved = reportCoreFrameObserved,
            graphics = reportGraphics,
            reportedOptions = LibretroEngineReport.safeOptions(options.map { it.key to it.value }),
            coreVideoTiming =
                if (wiiTimingCount > 1) {
                    "Wii core video events: $wiiTimingCount over ${wiiTimingLast - wiiTimingFirst} ms, " +
                        "longest gap $wiiTimingLongestGap ms; audio underruns unavailable"
                } else {
                    null
                },
        )

    private val wiiPointerMode = MutableStateFlow("2")
    val activeWiiPointerMode: StateFlow<String> = wiiPointerMode

    private val gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.Uninitialized)

    private val reactiveLightingSession = CartdeckReactiveLightingSession(scope)
    val reactiveLightingStrength: StateFlow<Float> = reactiveLightingSession.strength

    private val controllerLightingSession = CartdeckReactiveLightingSession(scope)
    val controllerActivityStrength: StateFlow<Float> = controllerLightingSession.strength
    private val mappedButtonActivity =
        CartdeckMappedInputActivityGate { controllerLightingSession.onControllerActivity() }

    fun setReactiveLightingSources(
        coreFeedback: Boolean,
        controllerActivity: Boolean,
    ) {
        reactiveLightingSession.configure(coreFeedback)
        controllerLightingSession.configure(controllerActivity)
        mappedButtonActivity.configure(controllerActivity)
    }

    /**
     * Cosmetic observation ONLY after a mapped game button has been
     * forwarded. Never alters the existing key-action result or port.
     */
    fun onAcceptedMappedButton(
        source: String,
        mappedKey: Int,
        action: Int,
    ) {
        mappedButtonActivity.observe(source, mappedKey, action)
    }

    private val retroGameViewFlow = MutableStateFlow<GLRetroView?>(null)
    var retroGameView: GLRetroView? by MutableStateProperty(retroGameViewFlow)

    fun getGameState(): Flow<GameState> {
        return gameState.debounce(200)
    }

    fun isPreparing(): Boolean = gameState.value is GameState.Uninitialized || gameState.value is GameState.Loading

    suspend fun initialize(
        applicationContext: Context,
        game: Game,
        systemCoreConfig: SystemCoreConfig,
        gameLoader: GameLoader,
        requestLoadSave: Boolean,
    ) {
        val currentState = gameState.value
        if (currentState != GameState.Uninitialized) return
        launchedGame = game

        val autoSaveEnabled = settingsManager.autoSave()
        val display =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DisplaySettingsStore(
                    applicationContext,
                ).readSafely(
                    game.cartridgeId.takeIf {
                        it.isNotBlank()
                    },
                ).effective
            }
        val alignment = display.alignment ?: settingsManager.viewportAlignment()
        val lowLatencyAudio = settingsManager.lowLatencyAudio()
        val enableRumble = settingsManager.enableRumble()
        val directLoad = settingsManager.allowDirectGameLoad()

        val hasMicrophonePermission =
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

        val enableMicrophone = systemCoreConfig.supportsMicrophone && hasMicrophonePermission

        val loadingStatesFlow =
            flow {
                // Fail closed if a background cleanup acquired the launch lock first.
                GameProcessLock.acquireExclusive(applicationContext)
                emitAll(
                    gameLoader.load(
                        applicationContext,
                        game,
                        requestLoadSave && autoSaveEnabled,
                        systemCoreConfig,
                        directLoad,
                    ),
                )
            }

        loadingStatesFlow
            .onEach { state ->
                if (systemCoreConfig.coreID == CoreID.PLAY_MODERN) {
                    val stage =
                        when (state) {
                            is GameLoader.LoadingState.LoadingCore -> PS2LaunchJournal.Stage.CORE_PROVISION
                            is GameLoader.LoadingState.LoadingGame -> PS2LaunchJournal.Stage.ROM_STAGE
                            is GameLoader.LoadingState.Ready -> PS2LaunchJournal.Stage.HOST_INIT
                        }
                    PS2LaunchJournal(applicationContext).stage(stage)
                }
            }
            .flowOn(Dispatchers.IO)
            .catch {
                val message =
                    if (it is GameLoaderException) {
                        getErrorMessage(it.error)
                    } else {
                        ""
                    }
                if (systemCoreConfig.coreID == CoreID.PLAY_MODERN) {
                    val code = if (it is GameLoaderException) it.error.javaClass.simpleName else it.javaClass.simpleName
                    PS2LaunchJournal(applicationContext).failure(code)
                }
                sideEffects.requestFailureFinish(message)
            }
            .debounce(200)
            .collect { loadingState ->
                gameState.value =
                    if (loadingState is GameLoader.LoadingState.Ready) {
                        Timber.i("Setting state to loaded")
                        val gameData = loadingState.gameData
                        val controllerMode =
                            if (systemCoreConfig.coreID == CoreID.MEDNAFEN_PCE_FAST) {
                                val controller =
                                    controllerConfigsManager.getControllerConfigs(system.id, systemCoreConfig, game)[0]
                                val six = controller?.touchControllerID == TouchControllerID.PCE_SIX
                                CoreVariable(PCE_PAD_KEY, if (six) "6 Buttons" else "2 Buttons")
                            } else {
                                null
                            }
                        val startupOverrides =
                            listOfNotNull(wiiPointerOverride(display, systemCoreConfig), controllerMode)
                        val launchData =
                            if (startupOverrides.isEmpty()) {
                                gameData
                            } else {
                                val keys = startupOverrides.map { it.key }.toSet()
                                gameData.copy(
                                    coreVariables =
                                        (gameData.coreVariables.filterNot { it.key in keys } + startupOverrides)
                                            .toTypedArray(),
                                )
                            }
                        val retroViewData =
                            buildRetroViewData(
                                applicationContext,
                                systemCoreConfig,
                                launchData,
                                display,
                                alignment,
                                lowLatencyAudio,
                                enableRumble,
                                enableMicrophone,
                            )
                        GameState.Loaded(
                            gameData = launchData,
                            retroViewData = retroViewData,
                        )
                    } else {
                        GameState.Loading(getLoadingMessage(loadingState))
                    }
            }
    }

    fun createRetroView(
        context: Context,
        lifecycle: LifecycleOwner,
    ): Pair<GameLoader.GameData, GLRetroView> {
        val currentState = gameState.value
        if (currentState !is GameState.Loaded) throw IllegalStateException("Game is not loaded.")

        if (system.id == SystemID.WII) {
            wiiPointerMode.value =
                currentState.gameData.coreVariables.firstOrNull {
                    it.key == WII_POINTER_KEY
                }?.value ?: "2"
        }

        reportGraphics = null
        reportCoreFrameObserved = false
        wiiTimingCount = 0
        wiiTimingFirst = 0L
        wiiTimingLast = 0L
        wiiTimingLongestGap = 0L
        reportSession =
            LibretroReportSession(
                coreFilePath = currentState.gameData.coreLibrary,
                sessionId = java.util.UUID.randomUUID().toString(),
                mediaRoute =
                    when (currentState.gameData.gameFiles) {
                        is RomFiles.Standard -> "standard file path"
                        is RomFiles.Virtual -> "virtual file descriptors"
                    },
                lowLatencyAudioRequested = currentState.retroViewData.preferLowLatencyAudio,
                skipDuplicateFrames = systemCoreConfig.skipDuplicateFrames,
            )

        if (systemCoreConfig.coreID == CoreID.PLAY_MODERN) {
            PS2LaunchJournal(appContext).selectedCore(currentState.gameData.coreLibrary)
        }
        val result =
            GLRetroView(context, currentState.retroViewData)
                .apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                }

        if (!system.hasTouchScreen) {
            result.disableTouchEvents()
        } else if (system.id == SystemID.WII) {
            // Touch mode uses GLRetroView's viewport-relative native pointer.
            // Both stick modes own the right analogue axes, including relative movement in Dolphin.
            result.setOnTouchListener { _, _ -> wiiPointerMode.value != "2" }
        }

        lifecycle.lifecycle.addObserver(result)

        if (BuildConfig.DEBUG) {
            runCatching {
                printRetroVariables(result)
            }
        }

        if (systemCoreConfig.coreID == CoreID.PLAY_MODERN) {
            // This event proves a frontend draw, not a core video-refresh or gameplay.
            scope.launch {
                result.getGLRetroEvents()
                    .filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>()
                    .first()
                PS2LaunchJournal(appContext).stage(PS2LaunchJournal.Stage.FRONTEND_FRAME)
                result.queueEvent {
                    PS2LaunchJournal(appContext).graphics(
                        GLES20.glGetString(GLES20.GL_VENDOR),
                        GLES20.glGetString(GLES20.GL_RENDERER),
                        GLES20.glGetString(GLES20.GL_VERSION),
                    )
                }
            }
        }
        // The additive host3 event requires a real core video-refresh callback.
        // It establishes frame submission, not correct gameplay, sound or saves.
        scope.launch {
            result.getGLRetroEvents()
                .filter { it.javaClass.simpleName == "CoreFrameRendered" }
                .first()
            reportCoreFrameObserved = true
            // One GL-thread snapshot after the existing core-frame event. No per-frame sampling.
            result.queueEvent {
                reportGraphics =
                    runCatching {
                        listOf(
                            GLES20.glGetString(GLES20.GL_VENDOR),
                            GLES20.glGetString(GLES20.GL_RENDERER),
                            GLES20.glGetString(GLES20.GL_VERSION),
                        ).joinToString(" | ") { it ?: "unavailable" }
                    }.getOrNull()
            }
            if (systemCoreConfig.coreID == CoreID.PLAY_MODERN) {
                PS2LaunchJournal(appContext).stage(PS2LaunchJournal.Stage.CORE_FRAME)
            }
            launchedGame?.let { runCatching { EngineSelectionPreferences(appContext).recordSuccess(it) } }
        }
        if (system.id == SystemID.WII) {
            // Use the verified host's existing event. It does not expose audio delivery or guest speed.
            scope.launch(Dispatchers.Main.immediate) {
                result.getGLRetroEvents()
                    .filter { it.javaClass.simpleName == "CoreFrameRendered" }
                    .collect {
                        if (!lifecycle.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                            return@collect
                        }
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (resetWiiTiming || now - wiiTimingFirst >= 10_000L) {
                            resetWiiTiming = false
                            wiiTimingCount = 0
                            wiiTimingFirst = now
                            wiiTimingLast = 0L
                            wiiTimingLongestGap = 0L
                        }
                        if (wiiTimingLast != 0L) {
                            wiiTimingLongestGap = maxOf(wiiTimingLongestGap, now - wiiTimingLast)
                        }
                        wiiTimingLast = now
                        wiiTimingCount++
                    }
            }
        }
        retroGameViewFlow.value = result
        gameState.value = GameState.Ready

        return currentState.gameData to result
    }

    suspend fun retroGameViewFlow() =
        retroGameViewFlow
            .filterNotNull()
            .first()

    suspend fun waitRetroGameViewInitialized() {
        retroGameViewFlow()
    }

    suspend inline fun <reified T> waitGLEvent() {
        val retroView = retroGameViewFlow()
        retroView.getGLRetroEvents()
            .filterIsInstance<T>()
            .first()
    }

    private fun buildRetroViewData(
        appContext: Context,
        systemCoreConfig: SystemCoreConfig,
        gameData: GameLoader.GameData,
        display: DisplaySettingsStore.Choices,
        alignment: String,
        lowLatencyAudio: Boolean,
        requestRumble: Boolean,
        requestMicrophone: Boolean,
    ): GLRetroViewData {
        return GLRetroViewData(appContext).apply {
            coreFilePath = gameData.coreLibrary

            when (val gameFiles = gameData.gameFiles) {
                is RomFiles.Standard -> {
                    gameFilePath = gameFiles.files.first().absolutePath
                }

                is RomFiles.Virtual -> {
                    gameVirtualFiles = gameFiles.files.map { VirtualFile(it.filePath, it.fd) }
                }
            }

            systemDirectory = gameData.systemDirectory.absolutePath
            savesDirectory = gameData.savesDirectory.absolutePath
            variables = gameData.coreVariables.map { Variable(it.key, it.value) }.toTypedArray()
            saveRAMState = gameData.saveRAMData
            shader =
                ShaderChooser.getShaderForSystem(
                    appContext.getGLSLVersion(),
                    display,
                    GameSystem.findById(gameData.game.systemId),
                )
            viewportAlignment =
                when (alignment) {
                    "top" -> ViewportAlignment.TOP
                    "bottom" -> ViewportAlignment.BOTTOM
                    else -> ViewportAlignment.CENTER
                }
            preferLowLatencyAudio = lowLatencyAudio
            rumbleEventsEnabled = requestRumble
            skipDuplicateFrames = systemCoreConfig.skipDuplicateFrames
            enableMicrophone = requestMicrophone
            immersiveMode = null
        }
    }

    private fun getLoadingMessage(loadingState: GameLoader.LoadingState): String {
        return when (loadingState) {
            is GameLoader.LoadingState.LoadingCore -> {
                appContext.getString(com.swordfish.lemuroid.ext.R.string.game_loading_download_core)
            }

            is GameLoader.LoadingState.LoadingGame -> {
                appContext.getString(R.string.game_loading_preparing_game)
            }

            else -> ""
        }
    }

    private fun printRetroVariables(retroGameView: GLRetroView) {
        scope.launch {
            // Some cores do not immediately call SET_VARIABLES so we might need to wait a little bit
            delay(1.seconds)
            retroGameView.getVariables().forEach {
                Timber.i("Libretro variable: $it")
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        owner.launchOnState(Lifecycle.State.STARTED) {
            initializeRetroGameViewErrorsFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeCoreVariablesFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeRumbleFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            applyDisplayShader()
        }
    }

    private suspend fun applyDisplayShader() {
        val view = retroGameViewFlow()
        val glEsMajor = appContext.getGLSLVersion()
        DisplaySettingsStore(appContext).observe(launchedGame?.cartridgeId)
            .map { snapshot -> ShaderChooser.getShaderForSystem(glEsMajor, snapshot.effective, system) }
            .distinctUntilChanged()
            .catch { error -> Timber.w(error, "Could not observe gameplay shader settings") }
            .collect { shader ->
                view.queueEvent {
                    if (view.shader != shader) view.shader = shader
                }
            }
    }

    private fun wiiPointerOverride(
        display: DisplaySettingsStore.Choices,
        config: SystemCoreConfig,
    ): CoreVariable? {
        if (system.id != SystemID.WII) return null
        val namespace = com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions.namespace(system.id.dbname, config)
        val mode = display.engineOptions["$namespace:$WII_POINTER_KEY"] ?: return null
        return mode.takeIf { value ->
            config.exposedSettings.any { it.key == WII_POINTER_KEY && it.values.any { it.key == value } }
        }?.let { CoreVariable(WII_POINTER_KEY, it) }
    }

    private suspend fun initializeCoreVariablesFlow() {
        try {
            waitRetroGameViewInitialized()
            val options = coreVariablesManager.getOptionsForCore(system.id, systemCoreConfig)
            val namespace =
                com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions.namespace(
                    system.id.dbname,
                    systemCoreConfig,
                )
            val variables = retroGameViewFlow().getVariables()
            val reported =
                withContext(Dispatchers.IO) {
                    runCatching {
                        com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions(appContext)
                            .record(namespace, systemCoreConfig, variables)
                    }.getOrDefault(emptyList())
                }
            val display =
                withContext(Dispatchers.IO) {
                    DisplaySettingsStore(appContext).readSafely(launchedGame?.cartridgeId).effective
                }
            val current =
                variables.mapNotNull { variable ->
                    variable.key?.let { key -> variable.value?.let { key to it } }
                }.toMap()
            val overrides =
                reported.mapNotNull { option ->
                    val existing = options.firstOrNull { it.key == option.key }?.value
                    val chosen = display.engineOptions["$namespace:${option.key}"] ?: existing ?: current[option.key]
                    chosen?.takeIf { it in option.values }?.let { CoreVariable(option.key, it) }
                }
            val keys = overrides.map { it.key }.toSet()
            val pointerOverride = wiiPointerOverride(display, systemCoreConfig)
            val desired =
                options.filterNot { it.key in keys || it.key == pointerOverride?.key } + overrides +
                    listOfNotNull(pointerOverride)
            updateCoreVariables(CoreOptionRefresh.changed(desired, current))
            if (system.id == SystemID.WII) {
                wiiPointerMode.value =
                    desired.firstOrNull {
                        it.key == WII_POINTER_KEY
                    }?.value ?: current[WII_POINTER_KEY] ?: "2"
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun initializeRumbleFlow() {
        val retroGameView = retroGameViewFlow()
        val rumbleEvents = retroGameView.getRumbleEvents()
        // Reuse the existing RESUMED-only rumble collector. Do not enable
        // host rumble when the player has disabled vibration in Settings.
        rumbleManager.collectAndProcessRumbleEvents(systemCoreConfig, rumbleEvents) { event ->
            reactiveLightingSession.onCoreRumble(event.strengthStrong, event.strengthWeak)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        resetWiiTiming = true
        setReactiveLightingSources(coreFeedback = false, controllerActivity = false)
        super.onPause(owner)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        setReactiveLightingSources(coreFeedback = false, controllerActivity = false)
        super.onDestroy(owner)
    }

    private suspend fun initializeRetroGameViewErrorsFlow() {
        retroGameViewFlow().getGLRetroErrors()
            .catch { Timber.e(it, "Exception in GLRetroErrors. Ironic.") }
            .collect { handleRetroViewError(it) }
    }

    private fun updateCoreVariables(options: List<CoreVariable>) {
        if (options.isEmpty()) return
        val updatedVariables =
            options.map { Variable(it.key, it.value) }
                .toTypedArray()

        updatedVariables.forEach {
            Timber.i("Updating core variable: ${it.key} ${it.value}")
        }

        retroGameView?.updateVariables(*updatedVariables)
    }

    private fun handleRetroViewError(errorCode: Int) {
        Timber.e("Error in GLRetroView $errorCode")
        val gameLoaderError =
            when (errorCode) {
                GLRetroView.ERROR_GL_NOT_COMPATIBLE -> GameLoaderError.GLIncompatible
                GLRetroView.ERROR_LOAD_GAME -> GameLoaderError.LoadGame
                GLRetroView.ERROR_LOAD_LIBRARY -> GameLoaderError.LoadCore
                GLRetroView.ERROR_SERIALIZATION -> GameLoaderError.Saves
                else -> GameLoaderError.Generic
            }

        // A device/host graphics mismatch is not evidence that the immutable engine is corrupt.
        // Blacklisting it hid the real error on every subsequent Open, including after a host fix.
        if (errorCode == GLRetroView.ERROR_LOAD_LIBRARY) {
            if (systemCoreConfig.coreID in setOf(CoreID.FLYCAST_MODERN, CoreID.DOLPHIN_MODERN, CoreID.PLAY_MODERN)) {
                runCatching { NativeCoreProvisioner.rollback(appContext, systemCoreConfig.coreID) }
                    .onFailure { Timber.e(it, "Unable to record native engine failure") }
            }
        }
        sideEffects.requestFailureFinish(getErrorMessage(gameLoaderError))
    }

    private fun getErrorMessage(gameError: GameLoaderError): String {
        val message =
            when (gameError) {
                is GameLoaderError.GLIncompatible -> {
                    val message =
                        if (systemCoreConfig.coreID == CoreID.PLAY_MODERN) {
                            R.string.game_loader_error_ps2_gl_incompatible
                        } else {
                            R.string.game_loader_error_gl_incompatible
                        }
                    appContext.getString(message)
                }
                is GameLoaderError.DiscFamily -> gameError.recovery
                is GameLoaderError.Generic -> {
                    appContext.getString(R.string.game_loader_error_generic)
                }
                is GameLoaderError.LoadCore -> {
                    appContext.getString(com.swordfish.lemuroid.ext.R.string.game_loader_error_load_core)
                }
                is GameLoaderError.NativeEngineDisabled -> {
                    appContext.getString(R.string.game_loader_error_native_disabled)
                }
                is GameLoaderError.LoadGame -> {
                    appContext.getString(R.string.game_loader_error_load_game)
                }
                is GameLoaderError.Saves -> {
                    appContext.getString(R.string.game_loader_error_save)
                }
                is GameLoaderError.UnsupportedArchitecture -> {
                    appContext.getString(R.string.game_loader_error_unsupported_architecture)
                }
                is GameLoaderError.MissingBiosFiles -> {
                    appContext.getString(R.string.game_loader_error_missing_bios, gameError.missingFiles)
                }
            }

        return message
    }
}
