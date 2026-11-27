package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.unit.Density
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.game.GameService
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcAction
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfile
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.feature.game.StandaloneGripArcLayout
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelInput
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelRetroGameView
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelSaves
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelSideEffects
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTilt
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls
import com.swordfish.lemuroid.app.shared.game.viewmodel.PivotArcHeldInputGate
import com.swordfish.lemuroid.app.shared.game.viewmodel.WonderSwanPivotInput
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.shared.rumble.RumbleManager
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.common.longAnimationDuration
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.game.GameLoader
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.inputevents.InputEvent
import gg.padkit.inputstate.InputState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class BaseGameScreenViewModel(
    private val appContext: Context,
    game: Game,
    settingsManager: SettingsManager,
    inputDeviceManager: InputDeviceManager,
    controllerConfigsManager: ControllerConfigsManager,
    system: GameSystem,
    systemCoreConfig: SystemCoreConfig,
    sharedPreferences: SharedPreferences,
    savesManager: SavesManager,
    statesManager: StatesManager,
    statesPreviewManager: StatesPreviewManager,
    coreVariablesManager: CoreVariablesManager,
    rumbleManager: RumbleManager,
) : ViewModel(), DefaultLifecycleObserver {
    private val preparation = LaunchPreparation()

    fun launchCancelling(): StateFlow<Boolean> = preparation.cancelling

    fun cancelLoading() {
        if (preparation.cancelling.value) return
        preparation.requestCancellation()
        pivotArcHeldInputGate.clear()
        viewModelScope.launch {
            preparation.awaitCleanup()
            sideEffects.requestSuccessfulFinish()
        }
    }

    class Factory(
        private val appContext: Context,
        private val game: Game,
        private val settingsManager: SettingsManager,
        private val inputDeviceManager: InputDeviceManager,
        private val controllerConfigsManager: ControllerConfigsManager,
        private val system: GameSystem,
        private val systemCoreConfig: SystemCoreConfig,
        private val sharedPreferences: SharedPreferences,
        private val savesManager: SavesManager,
        private val statesManager: StatesManager,
        private val statesPreviewManager: StatesPreviewManager,
        private val coreVariablesManager: CoreVariablesManager,
        private val rumbleManager: RumbleManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BaseGameScreenViewModel(
                appContext,
                game,
                settingsManager,
                inputDeviceManager,
                controllerConfigsManager,
                system,
                systemCoreConfig,
                sharedPreferences,
                savesManager,
                statesManager,
                statesPreviewManager,
                coreVariablesManager,
                rumbleManager,
            ) as T
        }
    }

    val systemId = system.id
    private val activeGame = game
    val cartridgeId = game.cartridgeId
    val launchTitle = "${game.displayName()} · ${system.id.dbname}"

    fun isPreparingLaunch(): Boolean = retroGameView.isPreparing()

    private val gripArcStore = GripArcProfileStore(appContext)
    private var gripArcAssignmentMode: String? = null
    private val gripArcProfileState = MutableStateFlow(gripArcStore.read(game))

    private val sideEffects = GameViewModelSideEffects(viewModelScope)
    val retroGameView =
        GameViewModelRetroGameView(
            appContext,
            system,
            systemCoreConfig,
            settingsManager,
            coreVariablesManager,
            sideEffects,
            rumbleManager,
            viewModelScope,
        )
    private val pivotArcHeldInputGate =
        PivotArcHeldInputGate { action, key ->
            retroGameView.retroGameView?.sendKeyEvent(action, key)
        }
    private val tilt = GameViewModelTilt(appContext, settingsManager)
    private val inputs =
        GameViewModelInput(
            appContext,
            system,
            systemCoreConfig,
            inputDeviceManager,
            controllerConfigsManager,
            retroGameView,
            tilt,
            sideEffects,
            pivotArcHeldInputGate,
            viewModelScope,
            game,
        )
    private val touchControls =
        GameViewModelTouchControls(
            settingsManager,
            TouchControllerSettingsManager(sharedPreferences),
            retroGameView,
            inputs,
            tilt,
            sideEffects,
            pivotArcHeldInputGate,
            viewModelScope,
        )
    private val saves =
        GameViewModelSaves(
            appContext,
            system,
            game,
            systemCoreConfig,
            retroGameView,
            settingsManager,
            savesManager,
            statesManager,
            statesPreviewManager,
            sideEffects,
        )

    val loadingState = MutableStateFlow(false)

    private inline fun withLoading(block: () -> Unit) {
        loadingState.value = true
        try {
            block()
        } finally {
            loadingState.value = false
        }
    }

    fun gripArcProfile(): StateFlow<GripArcProfile> = gripArcProfileState

    fun setGripArcAssignmentMode(mode: String?) {
        if (gripArcAssignmentMode == mode) return
        pivotArcHeldInputGate.releaseGripSlots()
        gripArcAssignmentMode = mode
        reloadGripArcProfile()
    }

    fun reloadGripArcProfile() {
        val latest = gripArcStore.read(activeGame, gripArcAssignmentMode)
        if (latest != gripArcProfileState.value) {
            pivotArcHeldInputGate.releaseGripSlots()
            inputs.releasePhysicalInputs()
            gripArcProfileState.value = latest
        }
    }

    fun setGripArcEnabled(enabled: Boolean) {
        pivotArcHeldInputGate.setGripEnabled(enabled)
    }

    fun gripArcLatches(): StateFlow<Set<Int>> = pivotArcHeldInputGate.gripLatches

    fun pressGripArcSlot(
        slot: Int,
        pressed: Boolean,
        mapping: Set<Int>,
        standalone: Boolean,
    ) {
        val profile = gripArcProfileState.value
        if (standalone) {
            val inventory = StandaloneGripArcLayout.inventoryFor(touchControls.activeTouchController()) ?: return
            val action = StandaloneGripArcLayout.guestAction(profile, slot, inventory) ?: return
            if (action == GripArcAction.OpenMenu) {
                if (pressed) requestGameMenuFromHardware()
            } else {
                pivotArcHeldInputGate.applyGripAction(slot, pressed, action, viewModelScope)
            }
        } else {
            pivotArcHeldInputGate.setGripSlot(slot, pressed, mapping)
        }
    }

    /** Read-only, game-session-only strength. Zero means ordinary static lighting. */
    fun reactiveLightingStrength(): StateFlow<Float> = retroGameView.reactiveLightingStrength

    fun controllerActivityStrength(): StateFlow<Float> = retroGameView.controllerActivityStrength

    fun setReactiveLightingSources(
        coreFeedback: Boolean,
        controllerActivity: Boolean,
    ) {
        retroGameView.setReactiveLightingSources(coreFeedback, controllerActivity)
    }

    fun getGameState(): Flow<GameViewModelRetroGameView.GameState> {
        return retroGameView.getGameState()
    }

    fun getSideEffects(): Flow<GameViewModelSideEffects.UiEffect> {
        return sideEffects.getUiEffects().onEach { effect ->
            when (effect) {
                is GameViewModelSideEffects.UiEffect.ShowMenu,
                is GameViewModelSideEffects.UiEffect.SuccessfulFinish,
                is GameViewModelSideEffects.UiEffect.FailureFinish,
                -> pivotArcHeldInputGate.clear()
                else -> Unit
            }
        }
    }

    fun getTiltConfiguration(): Flow<TiltConfiguration> {
        return tilt.getTiltConfiguration()
    }

    fun getSimulatedTiltEvents(): Flow<InputState> {
        return tilt.getSimulatedTiltEvents()
    }

    fun getTouchControlsSettings(
        density: Density,
        insets: WindowInsets,
    ): Flow<TouchControllerSettingsManager.Settings?> {
        return touchControls.getTouchControlsSettings(density, insets)
    }

    fun getTouchHapticFeedbackMode(): Flow<HapticFeedbackMode> {
        return touchControls.getTouchHapticFeedbackMode()
    }

    fun createRetroView(
        context: Context,
        lifecycle: LifecycleOwner,
    ): GLRetroView {
        val (gameData, result) = retroGameView.createRetroView(context, lifecycle)
        viewModelScope.launch {
            gameData.quickSaveData?.let {
                saves.restoreAutoSaveAsync(it)
            }
        }
        return result
    }

    suspend fun loadGame(
        applicationContext: Context,
        game: Game,
        systemCoreConfig: SystemCoreConfig,
        gameLoader: GameLoader,
        requestLoadSave: Boolean,
    ) {
        pivotArcHeldInputGate.clear()
        Timber.i("Calling load game: $game")
        preparation.start(viewModelScope) {
            retroGameView.initialize(applicationContext, game, systemCoreConfig, gameLoader, requestLoadSave)
        }?.join()
    }

    fun showEditControls(show: Boolean) {
        touchControls.showEditControls(show)
    }

    fun isEditControlShown(): Flow<Boolean> {
        return touchControls.isEditControlsShown()
    }

    fun updateTouchControllerSettings(touchControllerSettings: TouchControllerSettingsManager.Settings) {
        touchControls.updateTouchControllerSettings(touchControllerSettings)
    }

    fun resetTouchControls() {
        touchControls.resetTouchControls()
    }

    fun onScreenOrientationChanged(orientation: TouchControllerSettingsManager.Orientation) {
        inputs.releasePhysicalInputs()
        touchControls.updateScreenOrientation(orientation)
    }

    fun isTouchControllerVisible(): Flow<Boolean> {
        return touchControls.isTouchControllerVisible()
    }

    fun getTouchControllerConfig(): Flow<ControllerConfig> {
        return touchControls.getTouchControllerConfig()
    }

    /** Wii controller preference is independent of game progress and engine version. */
    val cartridgeSessionKey: String = "${game.systemId}_${game.id}"

    fun selectedWiiControllerName(): String? = inputs.selectedControllerName()

    fun selectWiiController(name: String) {
        inputs.selectWiiController(name)
    }

    fun changeTiltConfiguration(tiltConfig: TiltConfiguration) {
        tilt.changeTiltConfiguration(tiltConfig)
    }

    fun isMenuPressed(): Flow<Boolean> {
        return touchControls.isMenuPressed()
    }

    suspend fun saveSlot(index: Int) {
        if (loadingState.value) return
        withLoading {
            try {
                saves.saveSlot(index)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Save slot write failed")
                sideEffects.showToast(appContext.getString(R.string.game_save_write_failed))
            }
        }
    }

    suspend fun loadSlot(index: Int) {
        if (loadingState.value) return
        withLoading {
            saves.loadSlot(index)
        }
    }

    fun saveQuickSave() {
        Timber.d("Saving quick save")
        if (loadingState.value) return
        withLoading {
            saves.saveQuickSave()
        }
    }

    fun loadQuickSave() {
        Timber.d("Loading quick save")
        if (loadingState.value) return
        withLoading {
            saves.loadQuickSave()
        }
    }

    fun toggleFastForward() {
        Timber.d("Loading quick save")
        retroGameView.retroGameView?.apply {
            frameSpeed = if (frameSpeed == 1) 2 else 1
        }
    }

    suspend fun reset() =
        withLoading {
            pivotArcHeldInputGate.clear()
            try {
                delay(appContext.longAnimationDuration().toLong())
                retroGameView.retroGameViewFlow().reset()
            } catch (e: Throwable) {
                Timber.e(e, "Error in reset")
            }
        }

    /** Remote/Menu action uses the same existing side-effect path as a controller shortcut. */
    fun requestGameMenuFromHardware(): Boolean {
        if (retroGameView.isPreparing()) {
            cancelLoading()
            return true
        }
        if (loadingState.value) return false
        sideEffects.showMenu(tilt, inputs)
        return true
    }

    fun requestFinish() {
        if (retroGameView.isPreparing()) {
            cancelLoading()
            return
        }
        pivotArcHeldInputGate.clear()
        if (loadingState.value) return
        viewModelScope.launch {
            withLoading {
                val snapshot = saves.captureSaveSnapshot(true) ?: return@launch
                try {
                    saves.writeSaveSnapshot(snapshot)
                    sideEffects.requestSuccessfulFinish()
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.e(error, "Save before finish failed")
                    sideEffects.showToast(appContext.getString(R.string.game_save_write_failed))
                }
            }
        }
    }

    fun requestBackgroundSave() {
        pivotArcHeldInputGate.clear()
        if (loadingState.value) return
        GameService.schedule {
            val snapshot = saves.captureSaveSnapshot(false)
            try {
                saves.writeSaveSnapshot(snapshot)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.e(error, "Background save failed")
                sideEffects.showToast(appContext.getString(R.string.game_save_write_failed))
            }
        }
    }

    fun handleVirtualInputEvent(events: List<InputEvent>) {
        touchControls.handleVirtualInputEvent(events)
    }

    fun pivotArcHolds(): Flow<Set<Int>> = pivotArcHeldInputGate.holds

    fun setPivotArcHoldsEnabled(
        enabled: Boolean,
        controller: TouchControllerID? = null,
        approvedHoldKeys: Set<Int>? = null,
    ) {
        val wonderSwan = WonderSwanPivotInput.forController(controller)
        if (approvedHoldKeys != null) {
            pivotArcHeldInputGate.setEnabled(enabled, approvedHoldKeys, wonderSwan?.cursorKeys.orEmpty())
        } else if (wonderSwan == null) {
            pivotArcHeldInputGate.setEnabled(enabled)
        } else {
            pivotArcHeldInputGate.setEnabled(enabled, wonderSwan.faceKeys, wonderSwan.cursorKeys)
        }
    }

    fun togglePivotArcHold(keyCode: Int) {
        pivotArcHeldInputGate.toggleHold(keyCode)
    }

    fun setStandardHoldKeys(keys: Set<Int>) {
        pivotArcHeldInputGate.setStandardHoldKeys(keys)
    }

    override fun onResume(owner: LifecycleOwner) {
        reloadGripArcProfile()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        owner.lifecycle.addObserver(tilt)
        owner.lifecycle.addObserver(inputs)
        owner.lifecycle.addObserver(retroGameView)
        owner.lifecycle.addObserver(touchControls)
    }

    override fun onCleared() {
        retroGameView.setReactiveLightingSources(coreFeedback = false, controllerActivity = false)
        pivotArcHeldInputGate.setEnabled(false)
        pivotArcHeldInputGate.setGripEnabled(false)
        super.onCleared()
    }

    fun sendKeyEvent(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        return inputs.sendKeyEvent(keyCode, event)
    }

    fun sendMotionEvent(event: MotionEvent): Boolean {
        return inputs.sendMotionEvent(event)
    }
}
