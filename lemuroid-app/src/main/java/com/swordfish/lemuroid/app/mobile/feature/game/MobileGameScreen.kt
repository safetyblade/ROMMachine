package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.RectF
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.rememberDisplaySettings
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberGameplayAppearance
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.LocalCartdeckNativeInputs
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.layouts.LocalWiiNunchukPointerStick
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonPressFeedback
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState

@Composable
fun MobileGameScreen(viewModel: BaseGameScreenViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = constraints.maxWidth > constraints.maxHeight
        val display = rememberDisplaySettings(viewModel.cartridgeId)
        val border = display.effective.border(isLandscape)
        val hardwareBorder = border == DisplaySettingsStore.Border.STANDARD
        val showBorder = border != DisplaySettingsStore.Border.OFF
        val appContext = LocalContext.current
        val resolved = rememberGameplayAppearance()
        val appearance = resolved.source
        val edition = resolved.edition
        val shellTokens = resolved.tokens
        val controllerTheme = remember(resolved.controllerPalette) { LemuroidPadTheme(resolved.controllerPalette) }

        LaunchedEffect(isLandscape) {
            val orientation =
                if (isLandscape) {
                    TouchControllerSettingsManager.Orientation.LANDSCAPE
                } else {
                    TouchControllerSettingsManager.Orientation.PORTRAIT
                }
            viewModel.onScreenOrientationChanged(orientation)
        }

        val isLoading = viewModel.loadingState.collectAsState(true).value
        val heldPivotKeys = viewModel.pivotArcHolds().collectAsState(emptySet())
        val arcProfile = viewModel.gripArcProfile().collectAsState().value
        val sessionChoice =
            remember(viewModel, arcProfile.mode, arcProfile.side) {
                mutableStateOf(ArcControlSessionChoice())
            }
        val effectiveArcMode = sessionChoice.value.modeFor(arcProfile.mode)
        val effectiveArcSide = sessionChoice.value.sideFor(arcProfile.side)
        val controllerConfigState = viewModel.getTouchControllerConfig().collectAsState(null)
        val touchControlsVisibleState = viewModel.isTouchControllerVisible().collectAsState(true)
        // The existing enabled-gamepad flow already hides the main touch pad
        // on every device. Fullscreen landscape must also hide GripArc and
        // decorative physical-shell artwork while keeping game input alive.
        // Any enabled-controller disconnect automatically restores touch.
        val fullscreenWithGamepad = isLandscape && !touchControlsVisibleState.value

        // The core-feedback session is RESUMED-only and is disabled while
        // loading or when the fullscreen controller layout hides the shell.
        val lifecycleOwner = LocalLifecycleOwner.current
        val isResumed =
            remember(lifecycleOwner) {
                mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
            }
        DisposableEffect(lifecycleOwner) {
            val observer =
                LifecycleEventObserver { _, _ ->
                    isResumed.value = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val lightSessionAllowed =
            !appearance.highContrast && !appearance.reducedMotion &&
                !isLoading && !fullscreenWithGamepad && isResumed.value
        val useCoreFeedback =
            lightSessionAllowed && appearance.lighting.mode == CartdeckLighting.MODE_FEEDBACK
        val useControllerActivity =
            lightSessionAllowed && appearance.lighting.mode == CartdeckLighting.MODE_CONTROLLER
        DisposableEffect(viewModel, useCoreFeedback, useControllerActivity) {
            viewModel.setReactiveLightingSources(
                coreFeedback = useCoreFeedback,
                controllerActivity = useControllerActivity,
            )
            onDispose {
                viewModel.setReactiveLightingSources(coreFeedback = false, controllerActivity = false)
            }
        }
        val coreFeedback = viewModel.reactiveLightingStrength().collectAsState().value
        val controllerActivity = viewModel.controllerActivityStrength().collectAsState().value
        val reactiveLightStrength =
            animateFloatAsState(
                targetValue =
                    when {
                        useCoreFeedback -> coreFeedback
                        useControllerActivity -> controllerActivity
                        else -> 0f
                    },
                animationSpec = tween(durationMillis = 180),
                label = "CARTDECK input and core feedback",
            ).value
        val touchControllerSettingsState =
            viewModel
                .getTouchControlsSettings(LocalDensity.current, WindowInsets.displayCutout)
                .collectAsState(null)

        val touchControllerSettings = touchControllerSettingsState.value
        val currentControllerConfig = controllerConfigState.value
        val portraitMode = CartdeckViewportPolicy.portraitMode(viewModel.systemId)
        val portraitApertureRatio =
            CartdeckViewportPolicy.portraitApertureRatio(
                viewModel.systemId,
                maxHeight.value,
                compactSoloPivot = effectiveArcMode == ArcControlMode.PIVOTARC && maxWidth < 380.dp,
            )

        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }

        val pivotSettings = rememberPivotArcSettings().value
        val assignmentMode =
            gripArcAssignmentMode(currentControllerConfig?.touchControllerID, pivotSettings.pspAnalogPrimary)
        LaunchedEffect(viewModel, assignmentMode) {
            viewModel.setGripArcAssignmentMode(assignmentMode)
        }
        val gripManifest =
            remember(appContext) {
                runCatching { GripArcManifest.load(appContext) }.getOrNull()
            }
        val basePivotGeometry =
            remember(appContext) { runCatching { PivotArcLayoutProfile.load(appContext) }.getOrNull() }
        val pivotBaseline =
            remember(basePivotGeometry, viewModel.systemId, pivotSettings.pspAnalogPrimary) {
                basePivotGeometry?.let {
                    PivotArcLayoutProfile.forSystem(
                        it,
                        viewModel.systemId,
                        pivotSettings.pspAnalogPrimary,
                    )
                }
            }
        val touchGamePads = currentControllerConfig?.getTouchControllerConfig()
        val nativeInventory = touchGamePads?.nativeInventory
        val leftGamePad = touchGamePads?.leftComposable
        val rightGamePad = touchGamePads?.rightComposable

        val hapticFeedbackMode =
            viewModel
                .getTouchHapticFeedbackMode()
                .collectAsState(HapticFeedbackMode.NONE)

        val padHapticFeedback =
            when (hapticFeedbackMode.value) {
                HapticFeedbackMode.NONE -> HapticFeedbackType.NONE
                HapticFeedbackMode.PRESS -> HapticFeedbackType.PRESS
                HapticFeedbackMode.PRESS_RELEASE -> HapticFeedbackType.PRESS_RELEASE
            }

        PadKit(
            modifier = Modifier.fillMaxSize(),
            onInputEvents = { viewModel.handleVirtualInputEvent(it) },
            hapticFeedbackType = padHapticFeedback,
            simulatedState = tiltSimulatedStates,
            simulatedControlIds = tiltSimulatedControls,
        ) {
            val localContext = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current

            val fullScreenPosition = remember { mutableStateOf<Rect?>(null) }
            var chassisWindowBottom by remember { mutableFloatStateOf(0f) }
            val windowView = LocalView.current
            val viewportPosition = remember { mutableStateOf<Rect?>(null) }
            // Read-only PadKit group measurements; nothing changes their size or input.
            val leftPadBounds = remember { mutableStateOf<Rect?>(null) }
            val rightPadBounds = remember { mutableStateOf<Rect?>(null) }
            val individualInputs =
                remember(
                    viewModel.systemId,
                    isLandscape,
                    pivotSettings.mode,
                    pivotSettings.hand,
                    effectiveArcMode,
                    effectiveArcSide,
                    currentControllerConfig?.touchControllerID,
                    touchControllerSettings,
                    constraints.maxWidth,
                    constraints.maxHeight,
                    LocalDensity.current.density,
                ) {
                    mutableStateMapOf<String, Rect>()
                }
            val nativeInputInventory =
                remember(
                    viewModel.systemId,
                    currentControllerConfig?.touchControllerID,
                    effectiveArcMode,
                    effectiveArcSide,
                    touchControllerSettings,
                    constraints.maxWidth,
                    constraints.maxHeight,
                    LocalDensity.current.density,
                ) { mutableStateMapOf<String, Set<String>>() }
            // Retain a complete native measurement contract independently of replacement controls.
            // Only geometry/profile changes invalidate it; input and appearance recompositions do not.
            val measuredNativeSnapshot =
                remember(individualInputs, nativeInputInventory) {
                    mutableStateOf<Pair<Map<String, Rect>, Set<String>>?>(null)
                }
            val observeNativeInputs: (String, Set<String>) -> Unit =
                remember(nativeInputInventory) {
                    { owner, ids -> if (nativeInputInventory[owner] != ids) nativeInputInventory[owner] = ids }
                }
            val observeBounds: (String, Rect) -> Unit =
                remember(individualInputs) {
                    { key: String, bounds: Rect ->
                        if (individualInputs[key] != bounds) individualInputs[key] = bounds
                    }
                }

            AndroidView(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .onGloballyPositioned {
                            fullScreenPosition.value = it.boundsInRoot()
                            chassisWindowBottom = it.positionInWindow().y + it.size.height
                        },
                factory = {
                    viewModel.createRetroView(localContext, lifecycle)
                },
            )

            val fullPos = fullScreenPosition.value
            val viewPos = viewportPosition.value
            val currentDensity = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val requireMeasuredStandard =
                touchControllerSettings != null &&
                    touchControllerSettings !=
                    TouchControllerSettingsManager.defaultSettings(currentDensity, WindowInsets.displayCutout)
            val insetSafe = WindowInsets.safeDrawing
            val gestureSafe = WindowInsets.safeGestures
            // Per-cartridge mode overrides the legacy global PivotArc choice.
            val selectedPivot = effectiveArcMode.hasPivot
            val wiiPointerMode = viewModel.retroGameView.activeWiiPointerMode.collectAsState().value
            val wiiPointerStick = wiiPointerMode == "1" || wiiPointerMode == "0"
            val standardWiiProfile =
                if (currentControllerConfig?.touchControllerID == TouchControllerID.WII_NUNCHUK &&
                    !effectiveArcMode.hasGrip && !isLandscape
                ) {
                    remember(wiiPointerStick) { PivotArcSoloProfiles.wiiNunchuk(wiiPointerStick, fixedHolds = false) }
                } else {
                    null
                }
            val scale = currentDensity.density
            val safeBounds =
                fullPos?.let {
                    PivotArcBounds(
                        maxOf(
                            insetSafe.getLeft(currentDensity, layoutDirection),
                            gestureSafe.getLeft(currentDensity, layoutDirection),
                        ) / scale + 8f,
                        maxOf(
                            insetSafe.getTop(currentDensity),
                            gestureSafe.getTop(currentDensity),
                        ) / scale + 8f,
                        it.width / scale -
                            maxOf(
                                insetSafe.getRight(currentDensity, layoutDirection),
                                gestureSafe.getRight(currentDensity, layoutDirection),
                            ) / scale - 8f,
                        it.height / scale -
                            maxOf(
                                insetSafe.getBottom(currentDensity),
                                gestureSafe.getBottom(currentDensity),
                            ) / scale - 8f,
                    )
                }
            val viewportBounds =
                if (fullPos != null && viewPos != null) {
                    PivotArcBounds(
                        (viewPos.left - fullPos.left) / scale,
                        (viewPos.top - fullPos.top) / scale,
                        (viewPos.right - fullPos.left) / scale,
                        (viewPos.bottom - fullPos.top) / scale,
                    )
                } else {
                    null
                }
            val soloSafe =
                safeBounds?.let {
                    val windowBottom = windowView.rootView.height
                    val reserved = maxOf(insetSafe.getBottom(currentDensity), gestureSafe.getBottom(currentDensity))
                    it.copy(bottom = pivotChassisBottom(it.bottom, chassisWindowBottom, windowBottom, reserved, scale))
                }
            val standardWiiPlacement =
                if (standardWiiProfile != null && safeBounds != null && viewportBounds != null) {
                    PivotArcSoloProfiles.standardWiiNunchuk(standardWiiProfile, safeBounds, viewportBounds)
                } else {
                    null
                }
            val soloProfile =
                if (!effectiveArcMode.hasGrip) {
                    PivotArcSoloProfiles.forMode(
                        viewModel.systemId,
                        currentControllerConfig?.touchControllerID,
                        pivotSettings.pspAnalogPrimary,
                        wiiPointerStick,
                    )
                } else {
                    null
                }
            val requiredPivotControls = PivotArcLayoutProfile.activeFor(viewModel.systemId)
            var compositeResolution: CompositeDeckResolution? = null
            val pivotResolution: PivotArcResolution? =
                if (selectedPivot && touchControlsVisibleState.value && safeBounds != null && viewportBounds != null &&
                    currentControllerConfig != null
                ) {
                    if (pivotBaseline == null && soloProfile == null) {
                        PivotArcResolution.Unavailable("PivotArc geometry source could not be loaded.")
                    } else if (soloProfile == null &&
                        (
                            requiredPivotControls == null ||
                                (!effectiveArcMode.hasGrip && PivotArcSoloProfiles.supportsSystem(viewModel.systemId))
                        )
                    ) {
                        PivotArcResolution.Unavailable(
                            "PivotArc controls are not supported for this system. Standard controls remain available.",
                        )
                    } else {
                        if (effectiveArcMode.hasGrip) {
                            compositeResolution =
                                if (gripManifest == null) {
                                    CompositeDeckResolution.Unavailable(
                                        CompositeDeckFailure.INVALID_GEOMETRY,
                                        "GripArc geometry could not be loaded. " +
                                            "Standard remains available; saved settings are preserved.",
                                    )
                                } else {
                                    remember(
                                        gripManifest, pivotBaseline, viewModel.systemId, safeBounds, viewportBounds,
                                        effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                        pivotSettings.offsetXdp, pivotSettings.offsetYdp, isLandscape,
                                    ) {
                                        CompositeControlDeckSolver.resolve(
                                            gripManifest, checkNotNull(pivotBaseline), viewModel.systemId,
                                            safeBounds, viewportBounds,
                                            effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                            pivotSettings.offsetXdp, pivotSettings.offsetYdp, isLandscape,
                                        )
                                    }
                                }
                            when (val joint = compositeResolution) {
                                is CompositeDeckResolution.Fits -> PivotArcResolution.Fits(joint.placement.pivot)
                                is CompositeDeckResolution.Unavailable -> PivotArcResolution.Unavailable(joint.reason)
                                null -> null
                            }
                        } else if (soloProfile != null) {
                            remember(soloProfile, soloSafe, viewportBounds, pivotSettings, isLandscape) {
                                PivotArcSoloSolver.resolve(
                                    profile = soloProfile,
                                    safe = checkNotNull(soloSafe),
                                    viewport = viewportBounds,
                                    leftHanded = pivotSettings.hand == PivotArcHand.LEFT,
                                    requestedDx = pivotSettings.offsetXdp,
                                    requestedDy = pivotSettings.offsetYdp,
                                    landscape = isLandscape,
                                )
                            }
                        } else {
                            PivotArcPositionSolver.resolve(
                                baseline = checkNotNull(pivotBaseline),
                                active = checkNotNull(requiredPivotControls),
                                safe = checkNotNull(soloSafe),
                                viewport = viewportBounds,
                                leftHanded = pivotSettings.hand == PivotArcHand.LEFT,
                                requestedDx = pivotSettings.offsetXdp,
                                requestedDy = pivotSettings.offsetYdp,
                                isLandscape = isLandscape,
                                requireHardware = true,
                                seatAtChassisBottom = true,
                            )
                        }
                    }
                } else {
                    null
                }
            val jointPlacement = (compositeResolution as? CompositeDeckResolution.Fits)?.placement
            val pivotPlacement = (pivotResolution as? PivotArcResolution.Fits)?.placement
            val measuredWiiStandard = standardWiiPlacement.takeIf { pivotPlacement == null }
            var combinedPlacement: GripArcCombinedPlacement? = null
            var nativeProfileValidated = false
            val standaloneInventory =
                currentControllerConfig?.touchControllerID?.takeIf(StandaloneGripArcLayout::supports)
                    ?.takeIf {
                        effectiveArcMode == ArcControlMode.STANDARD_GRIPARC &&
                            supportsStandaloneGripActions(viewModel.systemId)
                    }
                    ?.let { StandaloneGripArcLayout.inventoryFor(it) }
            val standaloneGripSelected = standaloneInventory != null
            val gripResolution: GripArcFit? =
                if (
                    effectiveArcMode.hasGrip &&
                    fullPos != null &&
                    viewPos != null
                ) {
                    if (standaloneInventory != null && !touchControlsVisibleState.value) {
                        null
                    } else if (selectedPivot && touchControlsVisibleState.value) {
                        jointPlacement?.let { GripArcFit.Fits(it.dock) }
                            ?: GripArcFit.Unavailable(
                                (compositeResolution as? CompositeDeckResolution.Unavailable)?.reason
                                    ?: (pivotResolution as? PivotArcResolution.Unavailable)?.reason
                                    ?: (
                                        "Joint controller geometry is not ready. " +
                                            "Standard remains available; saved settings are preserved."
                                    ),
                            )
                    } else if (gripManifest == null) {
                        GripArcFit.Unavailable("GripArc's approved geometry could not be loaded.")
                    } else if (GripArcInputCatalog.forSystem(viewModel.systemId).isEmpty()) {
                        GripArcFit.Unavailable("GripArc programmable inputs are not mapped for this system.")
                    } else {
                        val scale = currentDensity.density
                        val safeBounds =
                            PivotArcBounds(
                                maxOf(
                                    insetSafe.getLeft(currentDensity, layoutDirection),
                                    gestureSafe.getLeft(currentDensity, layoutDirection),
                                ) / scale + 8f,
                                maxOf(
                                    insetSafe.getTop(currentDensity),
                                    gestureSafe.getTop(currentDensity),
                                ) / scale + 8f,
                                fullPos.width / scale -
                                    maxOf(
                                        insetSafe.getRight(currentDensity, layoutDirection),
                                        gestureSafe.getRight(currentDensity, layoutDirection),
                                    ) / scale - 8f,
                                fullPos.height / scale -
                                    maxOf(
                                        insetSafe.getBottom(currentDensity),
                                        gestureSafe.getBottom(currentDensity),
                                    ) / scale - 8f,
                            )
                        val viewportBounds =
                            PivotArcBounds(
                                (viewPos.left - fullPos.left) / scale,
                                (viewPos.top - fullPos.top) / scale,
                                (viewPos.right - fullPos.left) / scale,
                                (viewPos.bottom - fullPos.top) / scale,
                            )
                        val occupied =
                            if (pivotPlacement != null) {
                                pivotPlacement.controls.values.toList()
                            } else {
                                val known =
                                    if (!touchControlsVisibleState.value) {
                                        emptyList()
                                    } else if (individualInputs.isNotEmpty()) {
                                        individualInputs.values.toList()
                                    } else {
                                        listOfNotNull(leftPadBounds.value, rightPadBounds.value)
                                    }
                                known.map { rect ->
                                    PivotArcBounds(
                                        (rect.left - fullPos.left) / scale,
                                        (rect.top - fullPos.top) / scale,
                                        (rect.right - fullPos.left) / scale,
                                        (rect.bottom - fullPos.top) / scale,
                                    )
                                }
                            }
                        if (!selectedPivot && !touchControlsVisibleState.value) {
                            GripArcPositionSolver.resolve(
                                gripManifest,
                                safeBounds,
                                viewportBounds,
                                emptyList(),
                                effectiveArcSide,
                                arcProfile.dx,
                                arcProfile.dy,
                            )
                        } else if (standaloneInventory != null) {
                            nativeProfileValidated = true
                            combinedPlacement =
                                remember(
                                    standaloneInventory,
                                    safeBounds,
                                    viewportBounds,
                                    isLandscape,
                                    pivotSettings.pspAnalogPrimary,
                                ) {
                                    StandaloneGripArcLayout.resolveStandalone(
                                        standaloneInventory,
                                        safeBounds,
                                        viewportBounds,
                                        isLandscape,
                                        pivotSettings.pspAnalogPrimary,
                                    )
                                }
                            combinedPlacement?.let { GripArcFit.Fits(it.dock) }
                                ?: GripArcFit.Unavailable("GripARC needs more room for every seat.")
                        } else if (!selectedPivot && nativeInventory == null) {
                            GripArcFit.Unavailable(
                                "Compact Standard + GripArc is not yet defined for this controller. " +
                                    "Standard remains available; GripArc settings are preserved.",
                            )
                        } else if (!selectedPivot) {
                            val liveIds = nativeInputInventory.values.flatten().toSet()
                            val liveBounds = individualInputs.toMap()
                            if (nativeInventory!!.matchesNativeInputs(liveIds) &&
                                nativeInventory.controls.all { it.seat in liveBounds }
                            ) {
                                SideEffect {
                                    if (measuredNativeSnapshot.value == null) {
                                        measuredNativeSnapshot.value = liveBounds to liveIds
                                    }
                                }
                            }
                            val snapshot = measuredNativeSnapshot.value
                            val measured =
                                (snapshot?.first ?: liveBounds).mapValues { (_, rect) ->
                                    PivotArcBounds(
                                        (rect.left - fullPos.left) / scale,
                                        (rect.top - fullPos.top) / scale,
                                        (rect.right - fullPos.left) / scale,
                                        (rect.bottom - fullPos.top) / scale,
                                    )
                                }
                            val observedIds = snapshot?.second ?: liveIds
                            val measurementsReady = GripArcCombinedLayout.hasMeasurements(nativeInventory, measured)
                            if (!isLandscape && requireMeasuredStandard && !measurementsReady) {
                                // Wait for live controls at the new size, margins or rotation.
                                null
                            } else if (measurementsReady && observedIds.isNotEmpty() &&
                                !nativeInventory.matchesNativeInputs(observedIds)
                            ) {
                                GripArcFit.Unavailable(
                                    "GripArc cannot preserve every input in this native controller profile. " +
                                        "Standard remains available; saved settings are preserved.",
                                )
                            } else {
                                nativeProfileValidated = true
                                combinedPlacement =
                                    if (!isLandscape) {
                                        remember(
                                            gripManifest, nativeInventory, observedIds, measured,
                                            safeBounds, viewportBounds, effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                            requireMeasuredStandard,
                                        ) {
                                            if (measurementsReady && nativeInventory.matchesNativeInputs(observedIds)) {
                                                GripArcCombinedLayout.resolve(
                                                    gripManifest, nativeInventory, observedIds,
                                                    measured, safeBounds, viewportBounds,
                                                    effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                                    fallbackToNativeBaseline = !requireMeasuredStandard,
                                                )
                                            } else if (requireMeasuredStandard) {
                                                null
                                            } else {
                                                GripArcCombinedLayout.resolveDockFirst(
                                                    gripManifest, nativeInventory, safeBounds, viewportBounds,
                                                    effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    }
                                combinedPlacement?.let { GripArcFit.Fits(it.dock) }
                                    ?: GripArcFit.Unavailable(
                                        "Compact Standard + GripArc cannot fit on your selected side, " +
                                            "including its default position. " +
                                            "Standard controls are available for this session; " +
                                            "GripArc settings are preserved. " +
                                            "Use portrait orientation or a larger display.",
                                    )
                            }
                        } else if (occupied.any { !it.inside(safeBounds) || it.intersects(viewportBounds.inset(4f)) }) {
                            GripArcFit.Unavailable("Controls need more room below the game screen.")
                        } else {
                            GripArcPositionSolver.resolve(
                                manifest = gripManifest,
                                safe = safeBounds,
                                viewport = viewportBounds,
                                occupied = occupied,
                                side = effectiveArcSide,
                                dx = arcProfile.dx,
                                dy = arcProfile.dy,
                            )
                        }
                    }
                } else {
                    null
                }
            val gripPlacement = (gripResolution as? GripArcFit.Fits)?.placement
            // GripArc is a visual touch accessory. In controller-driven
            // fullscreen, hide it along with Standard/PivotArc controls.
            val gripEnabled = gripPlacement != null && !fullscreenWithGamepad && !isLoading
            DisposableEffect(viewModel, gripEnabled, effectiveArcMode, standaloneInventory?.controller) {
                viewModel.setGripArcEnabled(gripEnabled)
                onDispose { viewModel.setGripArcEnabled(false) }
            }
            val pivotHoldsEnabled =
                pivotPlacement != null && touchControlsVisibleState.value && !isLoading
            val pivotController = currentControllerConfig?.touchControllerID
            val approvedHoldKeys = soloProfile?.holdKeys
            DisposableEffect(viewModel, pivotHoldsEnabled, pivotController, approvedHoldKeys) {
                viewModel.setPivotArcHoldsEnabled(pivotHoldsEnabled, pivotController, approvedHoldKeys)
                onDispose { viewModel.setPivotArcHoldsEnabled(false) }
            }

            val fittedControls =
                combinedPlacement?.controls ?: pivotPlacement?.controls ?: measuredWiiStandard?.controls
            val fittedHardwareSeats = combinedPlacement?.hardwareSeats ?: fittedControls
            val standaloneGrip = combinedPlacement.takeIf { standaloneInventory != null }
            val contouredPivot = pivotPlacement != null && jointPlacement == null && viewModel.systemId != SystemID.GBA
            if (showBorder) {
                CartdeckConsoleShell(
                    tokens = shellTokens,
                    videoBounds = viewPos,
                    rootBounds = fullPos,
                    isLandscape = isLandscape,
                    patternId = resolved.patternId,
                    stampId = resolved.stampId,
                    decal = resolved.decal,
                    lighting = resolved.lighting,
                    highContrast = appearance.highContrast,
                    feedbackStrength = reactiveLightStrength,
                    isDualScreen = viewModel.systemId == SystemID.NDS,
                    minimal = !hardwareBorder || fullscreenWithGamepad,
                    mountedPivotDeck = contouredPivot,
                    continuousLandscapeBrand = standaloneGrip != null,
                    controlBounds =
                        if (touchControlsVisibleState.value) {
                            if (fittedControls != null && fullPos != null) {
                                fittedControls.values.map {
                                        bounds ->
                                    Rect(
                                        fullPos.left + bounds.left * currentDensity.density,
                                        fullPos.top + bounds.top * currentDensity.density,
                                        fullPos.left + bounds.right * currentDensity.density,
                                        fullPos.top + bounds.bottom * currentDensity.density,
                                    )
                                }
                            } else {
                                if (isLandscape && individualInputs.isNotEmpty()) {
                                    individualInputs.values.toList()
                                } else {
                                    listOfNotNull(leftPadBounds.value, rightPadBounds.value)
                                }
                            }
                        } else {
                            emptyList()
                        },
                )
            }

            // PivotArc supplies discrete controller rectangles, unlike
            // Standard's composite AB/ABXY controls. Feed the physical-seat
            // renderer the measured solver bounds so the shared hardware
            // finish, rails, speaker and indicator art remain present from
            // the first frame. Standard retains its original bound observers.
            val hardwareInputBounds =
                if (fittedHardwareSeats != null && fullPos != null) {
                    fittedHardwareSeats.map {
                            (key, bounds) ->
                        val seatKey =
                            if (combinedPlacement != null) {
                                key
                            } else {
                                pivotArcHardwareSeatKey(key)
                            }
                        seatKey to
                            Rect(
                                fullPos.left + bounds.left * currentDensity.density,
                                fullPos.top + bounds.top * currentDensity.density,
                                fullPos.left + bounds.right * currentDensity.density,
                                fullPos.top + bounds.bottom * currentDensity.density,
                            )
                    }.toMap()
                } else {
                    individualInputs.toMap()
                }
            if (hardwareBorder && standaloneGrip != null && !fullscreenWithGamepad && !isLoading) {
                StandaloneGripArcHardware(standaloneGrip, shellTokens, isLandscape)
            }
            if (
                touchControlsVisibleState.value &&
                touchControllerSettings != null &&
                currentControllerConfig != null
            ) {
                if (standaloneGrip == null && (hardwareBorder || (pivotPlacement != null && jointPlacement == null))) {
                    CartdeckHardwareSeats(
                        tokens = shellTokens,
                        edition = edition,
                        rootBounds = fullPos,
                        videoBounds = viewPos,
                        inputBounds = hardwareInputBounds,
                        isDualScreen =
                            portraitMode == CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
                        customColours = resolved.customColours,
                        pivotArc = pivotPlacement != null,
                        contouredPivot = contouredPivot,
                        pivotUtilities = soloProfile?.hardwareUtilities ?: PivotArcModuleGeometry.legacyUtilityKeys,
                        measuredStandard =
                            measuredWiiStandard != null ||
                                combinedPlacement?.native?.controller == TouchControllerID.GBA,
                        joint = jointPlacement,
                        lighting = resolved.lighting,
                        highContrast = appearance.highContrast,
                        feedbackStrength = reactiveLightStrength,
                    )
                }
            }

            if (hardwareBorder && jointPlacement != null && touchControlsVisibleState.value) {
                CompositeControlDeckHousing(
                    jointPlacement,
                    shellTokens,
                    resolved.lighting,
                    appearance.highContrast,
                    reactiveLightStrength,
                    activeSlots = arcProfile.count,
                )
            }
            if (
                hardwareBorder && jointPlacement == null && gripPlacement != null &&
                !fullscreenWithGamepad && !isLoading
            ) {
                if (standaloneGrip == null) {
                    combinedPlacement?.let { StandardGripArcDockJoint(it, shellTokens) }
                }
                if (standaloneGrip == null) {
                    GripArcHousing(
                        gripPlacement,
                        shellTokens,
                        resolved.lighting,
                        appearance.highContrast,
                        reactiveLightStrength,
                        activeSlots = arcProfile.count,
                    )
                }
            }

            LaunchedEffect(fullPos, viewPos, portraitMode, fullscreenWithGamepad, hardwareBorder) {
                val gameView = viewModel.retroGameView.retroGameViewFlow()
                if (fullPos == null || viewPos == null) return@LaunchedEffect
                // Fit the COMPLETE rendered frame just inside its measured
                // decorative glass. One physical pixel prevents top-edge
                // shader bleed; never apply it to DS touch geometry or to
                // full-screen controller play.
                val guard =
                    if (hardwareBorder && !fullscreenWithGamepad &&
                        portraitMode == CartdeckViewportPolicy.PortraitMode.STANDARD
                    ) {
                        1f
                    } else {
                        0f
                    }
                val bounds =
                    CartdeckViewportPolicy.normalizedAperture(
                        fullPos.width,
                        fullPos.height,
                        viewPos.left - fullPos.left,
                        viewPos.top - fullPos.top,
                        viewPos.right - fullPos.left,
                        viewPos.bottom - fullPos.top,
                        guard,
                    )
                gameView.viewport = RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
            }

            // With a physical pad, take all landscape space rather than
            // reserving the old left/right touchscreen side containers.
            val gripArcLandscape =
                isLandscape && hardwareBorder && touchControlsVisibleState.value &&
                    currentControllerConfig != null &&
                    effectiveArcMode == ArcControlMode.STANDARD_GRIPARC &&
                    maxWidth >= 680.dp && maxHeight >= 320.dp
            val dedicatedHardwareLayout =
                isLandscape && hardwareBorder && touchControlsVisibleState.value &&
                    currentControllerConfig != null &&
                    (effectiveArcMode == ArcControlMode.STANDARD || gripArcLandscape)
            val useFullLandscapeViewport =
                !dedicatedHardwareLayout &&
                    (fullscreenWithGamepad || (currentControllerConfig?.allowTouchOverlay ?: true))
            ConstraintLayout(
                modifier = Modifier.fillMaxSize(),
                constraintSet =
                    GameScreenLayout.buildConstraintSet(
                        isLandscape,
                        useFullLandscapeViewport,
                        portraitMode,
                        portraitApertureRatio = portraitApertureRatio,
                        hardwareLandscape = dedicatedHardwareLayout,
                        gripArcLandscape = gripArcLandscape,
                        standaloneGripRailInset =
                            if (gripArcLandscape && standaloneGripSelected) {
                                // Leave one independent 48.5dp Menu target below the glass,
                                // plus the same safety margin used by the positioning solver.
                                val systemBottom =
                                    maxOf(insetSafe.getBottom(currentDensity), gestureSafe.getBottom(currentDensity))
                                (systemBottom / currentDensity.density + 60f).dp
                            } else {
                                0.dp
                            },
                        availableWidth = maxWidth,
                        availableHeight = maxHeight,
                        dualScreenSoloPivot =
                            touchControlsVisibleState.value &&
                                (
                                    (selectedPivot && soloProfile != null) ||
                                        (
                                            standaloneGripSelected &&
                                                standaloneInventory?.controller == TouchControllerID.NINTENDO_3DS
                                        )
                                ),
                        independentPortraitAperture =
                            selectedPivot && !effectiveArcMode.hasGrip && touchControlsVisibleState.value,
                        portraitBottomInset =
                            GameScreenLayout.portraitBottomInsetFor(
                                viewModel.systemId,
                                effectiveArcMode == ArcControlMode.STANDARD,
                                isLandscape,
                                maxHeight,
                            ),
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW)
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                            .onGloballyPositioned { viewportPosition.value = it.boundsInRoot() },
                )

                val isVisible =
                    touchControllerSettings != null &&
                        currentControllerConfig != null &&
                        touchControlsVisibleState.value

                if (isVisible) {
                    CompositionLocalProvider(
                        LocalLemuroidPadTheme provides controllerTheme,
                        LocalCartdeckControlBounds provides observeBounds,
                        LocalWiiNunchukPointerStick provides wiiPointerStick.takeUnless { effectiveArcMode.hasGrip },
                        LocalCartdeckNativeInputs provides
                            if (effectiveArcMode == ArcControlMode.STANDARD_GRIPARC) {
                                observeNativeInputs
                            } else {
                                null
                            },
                    ) {
                        if (!isLandscape) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_BOTTOM_CONTAINER),
                            )
                        } else if (dedicatedHardwareLayout || !currentControllerConfig.allowTouchOverlay) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_CONTAINER),
                            )
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_CONTAINER),
                            )
                        }

                        if (pivotPlacement != null || combinedPlacement != null || measuredWiiStandard != null) {
                            // Keep ConstraintLayout viewport anchors but do NOT
                            // register the Standard controls as extra input owners.
                            Box(Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD).size(1.dp))
                            Box(Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD).size(1.dp))
                        } else {
                            val effectiveSettings =
                                if (dedicatedHardwareLayout) {
                                    GameScreenLayout.fitLandscapeHardwareSettings(touchControllerSettings, maxHeight)
                                } else {
                                    touchControllerSettings
                                }
                            leftGamePad?.invoke(
                                this,
                                Modifier
                                    .layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD)
                                    .onGloballyPositioned { leftPadBounds.value = it.boundsInRoot() },
                                effectiveSettings,
                            )
                            rightGamePad?.invoke(
                                this,
                                Modifier
                                    .layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD)
                                    .onGloballyPositioned { rightPadBounds.value = it.boundsInRoot() },
                                effectiveSettings,
                            )
                        }
                        GameScreenRunningCentralMenu(
                            modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_GAME_CONTAINER),
                            controllerConfig = currentControllerConfig,
                            touchControllerSettings = touchControllerSettings,
                            viewModel = viewModel,
                        )
                    }
                }
            }
            if (pivotPlacement != null &&
                touchControllerSettings != null &&
                currentControllerConfig != null &&
                touchControlsVisibleState.value
            ) {
                CompositionLocalProvider(
                    LocalLemuroidPadTheme provides controllerTheme,
                    LocalCartdeckControlBounds provides observeBounds,
                ) {
                    PivotArcControlGroup(
                        placement = pivotPlacement,
                        system = viewModel.systemId,
                        heldKeys = heldPivotKeys.value,
                        accent = shellTokens.accent,
                        onToggleHold = viewModel::togglePivotArcHold,
                        combined = jointPlacement != null,
                        touchController = currentControllerConfig.touchControllerID,
                        soloProfile = soloProfile,
                    )
                }
            }
            if (measuredWiiStandard != null && touchControllerSettings != null && touchControlsVisibleState.value) {
                CompositionLocalProvider(LocalLemuroidPadTheme provides controllerTheme) {
                    PivotArcControlGroup(
                        placement = measuredWiiStandard,
                        system = viewModel.systemId,
                        heldKeys = emptySet(),
                        accent = shellTokens.accent,
                        onToggleHold = {},
                        soloProfile = standardWiiProfile,
                    )
                }
            }
            if (combinedPlacement != null && touchControlsVisibleState.value && !isLoading) {
                CompositionLocalProvider(LocalLemuroidPadTheme provides controllerTheme) {
                    GripArcCombinedControls(combinedPlacement!!, standalone = standaloneGrip != null)
                }
            }
            if (effectiveArcMode == ArcControlMode.STANDARD && touchControlsVisibleState.value &&
                !isLoading && isResumed.value
            ) {
                StandardHoldTray(
                    system = viewModel.systemId,
                    native = touchGamePads?.holdTrayInventory,
                    root = fullPos,
                    viewport = viewPos,
                    measured = individualInputs,
                    held = heldPivotKeys.value,
                    onKeys = viewModel::setStandardHoldKeys,
                    onToggle = viewModel::togglePivotArcHold,
                )
            }
            GripArcAccessory(
                placement = gripPlacement,
                profile = arcProfile,
                tokens = shellTokens,
                fullscreenWithGamepad = fullscreenWithGamepad,
                isLoading = isLoading,
                typedActions = standaloneInventory != null,
                latchedSlots = viewModel.gripArcLatches().collectAsState().value,
                inputLabels =
                    standaloneInventory?.let(StandaloneGripArcLayout::actionLabels)
                        ?: GripArcInputCatalog.forSystem(viewModel.systemId).associate { it.code to it.label },
                onPressed = { slot, pressed, mapping ->
                    viewModel.pressGripArcSlot(slot, pressed, mapping, standaloneGrip != null)
                },
                drawHousing = false,
                lighting = resolved.lighting,
                highContrast = appearance.highContrast,
                feedbackStrength = reactiveLightStrength,
            )
            val pivotError = pivotResolution as? PivotArcResolution.Unavailable
            val gripError = gripResolution as? GripArcFit.Unavailable
            val layoutError = gripError?.reason ?: pivotError?.reason
            val dismissedFallback =
                remember(effectiveArcMode, effectiveArcSide, fullPos, viewPos) { mutableStateOf(false) }
            val canTryOtherSide =
                !isLandscape && gripManifest != null &&
                    if (selectedPivot) {
                        (compositeResolution as? CompositeDeckResolution.Unavailable)?.failure ==
                            CompositeDeckFailure.INSUFFICIENT_SAFE_SPACE
                    } else {
                        nativeProfileValidated &&
                            GripArcInputCatalog.forSystem(viewModel.systemId).isNotEmpty()
                    }
            if ((!selectedPivot || effectiveArcMode.hasGrip) && gripError != null &&
                touchControlsVisibleState.value && !isLoading &&
                !dismissedFallback.value
            ) {
                AlertDialog(
                    onDismissRequest = {
                        sessionChoice.value = sessionChoice.value.useStandard()
                        dismissedFallback.value = true
                    },
                    title = { Text("GripArc unavailable on this layout") },
                    text = { Text(gripError.reason) },
                    confirmButton = {
                        TextButton(onClick = {
                            sessionChoice.value = sessionChoice.value.useStandard()
                            dismissedFallback.value = true
                        }) {
                            Text("Use Standard this session")
                        }
                    },
                    dismissButton = {
                        if (canTryOtherSide) {
                            TextButton(onClick = {
                                sessionChoice.value = sessionChoice.value.tryOppositeSide(arcProfile.side)
                            }) {
                                val otherSide = if (effectiveArcSide == GripArcSide.RIGHT) "left" else "right"
                                Text("Try $otherSide side this session")
                            }
                        }
                    },
                )
            }
            LaunchedEffect(layoutError, touchControlsVisibleState.value) {
                if (layoutError != null && touchControlsVisibleState.value &&
                    selectedPivot && !effectiveArcMode.hasGrip
                ) {
                    Toast.makeText(appContext, layoutError, Toast.LENGTH_LONG).show()
                }
            }
        }

        WiiControllerLaunchChoice(viewModel, currentControllerConfig, isLoading)

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

/**
 * One-time choice when each Wii cartridge first reaches an interactive game
 * screen. This is dismissible, never changes the disc or Wii system memory,
 * and the in-game Controls menu remains available to switch at any time.
 */
@Composable
private fun WiiControllerLaunchChoice(
    viewModel: BaseGameScreenViewModel,
    currentControllerConfig: ControllerConfig?,
    isLoading: Boolean,
) {
    if (viewModel.systemId != SystemID.WII) return

    val context = LocalContext.current
    val preferences =
        remember(context) {
            context.getSharedPreferences("cartdeck_wii_controller_choice_v1", android.content.Context.MODE_PRIVATE)
        }
    val promptKey = remember(viewModel) { "shown_${viewModel.cartridgeSessionKey}" }
    val show =
        remember(viewModel, promptKey) {
            mutableStateOf(!preferences.getBoolean(promptKey, false))
        }
    if (!show.value || isLoading || currentControllerConfig == null) return

    val dismiss = {
        preferences.edit().putBoolean(promptKey, true).apply()
        show.value = false
    }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Wii controller") },
        text = {
            Column {
                Text("Sideways Wii Remote is the default. Your controller choice is saved for this cartridge.")
                Text("You can switch again at any time from the game's Wii controller menu.")
                listOf(
                    "wii_sideways" to "Sideways Wii Remote",
                    "wii_classic" to "Classic Controller",
                    "wii_nunchuk" to "Wii Remote + Nunchuk",
                    "wii_gamecube" to "GameCube Controller (supported Wii games)",
                ).forEach { (name, label) ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.selectWiiController(name)
                            dismiss()
                        },
                    ) {
                        Text(if (currentControllerConfig.name == name) "✓ $label" else label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) {
                Text("Continue")
            }
        },
    )
}

/**
 * Retain the existing ConstraintLayout anchor without painting an independent
 * glass rectangle over the P1-C2B connected chassis and decorative seats.
 */
@Composable
private fun PadContainer(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

@Composable
private fun GameScreenRunningCentralMenu(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
    controllerConfig: ControllerConfig,
) {
    val menuPressed = viewModel.isMenuPressed().collectAsState(false)
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        LemuroidButtonPressFeedback(
            pressed = menuPressed.value,
            animationDurationMillis = MENU_LOADING_ANIMATION_MILLIS,
            icon = R.drawable.button_menu,
        )
        MenuEditTouchControls(viewModel, controllerConfig, touchControllerSettings)
    }
}

@Composable
private fun MenuEditTouchControls(
    viewModel: BaseGameScreenViewModel,
    controllerConfig: ControllerConfig,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
) {
    val showEditControls = viewModel.isEditControlShown().collectAsState(false)
    if (!showEditControls.value) return

    Dialog(onDismissRequest = { viewModel.showEditControls(false) }) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuEditTouchControlRow(Icons.Default.OpenInFull, "Scale", 0f) {
                    Slider(
                        value = touchControllerSettings.scale,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(scale = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Horizontal Margin", 90f) {
                    Slider(
                        value = touchControllerSettings.marginX,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginX = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Vertical Margin", 0f) {
                    Slider(
                        value = touchControllerSettings.marginY,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginY = it),
                            )
                        },
                    )
                }
                if (controllerConfig.allowTouchRotation) {
                    MenuEditTouchControlRow(Icons.Default.RotateLeft, "Rotate", 0f) {
                        Slider(
                            value = touchControllerSettings.rotation,
                            onValueChange = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(rotation = it),
                                )
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { viewModel.resetTouchControls() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_reset))
                    }
                    TextButton(
                        onClick = { viewModel.showEditControls(false) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEditTouchControlRow(
    icon: ImageVector,
    label: String,
    rotation: Float,
    slider: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.rotate(rotation),
            imageVector = icon,
            contentDescription = label,
        )
        slider()
    }
}
