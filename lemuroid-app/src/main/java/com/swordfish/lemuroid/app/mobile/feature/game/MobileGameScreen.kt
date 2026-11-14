package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.RectF
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.feature.shelf.readableColor
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.CartdeckPadPalette
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.LocalCartdeckNativeInputs
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.LemuroidButtonPressFeedback
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState

@Composable
fun MobileGameScreen(viewModel: BaseGameScreenViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = constraints.maxWidth > constraints.maxHeight
        val appContext = LocalContext.current
        // Read existing appearance atomically. No mutation of library, inputs,
        // ROM files, emulator state or save data.
        val appearance =
            produceState(initialValue = ShelfAppearance(), key1 = appContext) {
                value = runCatching { ShelfStore(appContext).read().appearance }.getOrDefault(ShelfAppearance())
            }.value
        val edition =
            when (appearance.edition.lowercase()) {
                "pink" -> CartdeckEditionPreset.PINK
                "green" -> CartdeckEditionPreset.GREEN
                else -> CartdeckEditionPreset.ORIGINAL
            }
        val editionTokens = remember(appContext, edition) { appContext.resolveCartdeckEdition(edition) }
        val shellTokens =
            remember(editionTokens, appearance) {
                fun role(
                    key: String,
                    fallback: Color,
                ): Color = appearance.custom[key]?.let { Color(it.toInt()) } ?: fallback

                editionTokens.copy(
                    chassis = role("chassis", editionTokens.chassis),
                    secondary = role("secondary", editionTokens.secondary),
                    bezel = role("bezel", editionTokens.bezel),
                    glass = role("glass", editionTokens.glass),
                    button = role("button", editionTokens.button),
                    accent = role("accent", editionTokens.accent),
                    onChassis = role("onChassis", editionTokens.onChassis),
                    onButton = role("onButton", editionTokens.onButton),
                    focus = role("focus", editionTokens.focus),
                )
            }
        val controllerTheme =
            remember(shellTokens, appearance.highContrast) {
                LemuroidPadTheme(
                    CartdeckPadPalette(
                        chassis = shellTokens.chassis,
                        trim = shellTokens.secondary,
                        bezel = shellTokens.bezel,
                        button = shellTokens.button,
                        accent = shellTokens.accent,
                        onButton =
                            readableColor(
                                shellTokens.onButton,
                                listOf(shellTokens.button, shellTokens.bezel),
                                4.5f,
                            ),
                        focus = shellTokens.focus,
                        highContrast = appearance.highContrast,
                    ),
                )
            }

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
        val touchControlsVisibleState = viewModel.isTouchControllerVisible().collectAsState(false)
        // The existing enabled-gamepad flow already hides the main touch pad
        // on every device. Fullscreen landscape must also hide GripArc and
        // decorative physical-shell artwork while keeping game input alive.
        // Any enabled-controller disconnect automatically restores touch.
        val fullscreenWithGamepad = isLandscape && !touchControlsVisibleState.value
        val touchControllerSettingsState =
            viewModel
                .getTouchControlsSettings(LocalDensity.current, WindowInsets.displayCutout)
                .collectAsState(null)

        val touchControllerSettings = touchControllerSettingsState.value
        val currentControllerConfig = controllerConfigState.value
        val portraitMode = CartdeckViewportPolicy.portraitMode(viewModel.systemId)

        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }

        val pivotSettings = rememberPivotArcSettings().value
        val gripManifest =
            remember(appContext) {
                runCatching { GripArcManifest.load(appContext) }.getOrNull()
            }
        val pivotBaseline = remember(appContext) { runCatching { PivotArcLayoutProfile.load(appContext) }.getOrNull() }
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
                        .onGloballyPositioned { fullScreenPosition.value = it.boundsInRoot() },
                factory = {
                    viewModel.createRetroView(localContext, lifecycle)
                },
            )

            val fullPos = fullScreenPosition.value
            val viewPos = viewportPosition.value
            val currentDensity = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val insetSafe = WindowInsets.safeDrawing
            val gestureSafe = WindowInsets.safeGestures
            // Per-cartridge mode overrides the legacy global PivotArc choice.
            val selectedPivot = effectiveArcMode.hasPivot
            val requiredPivotControls = PivotArcLayoutProfile.activeFor(viewModel.systemId)
            var compositeResolution: CompositeDeckResolution? = null
            val pivotResolution: PivotArcResolution? =
                if (selectedPivot && touchControlsVisibleState.value && fullPos != null && viewPos != null) {
                    if (pivotBaseline == null) {
                        PivotArcResolution.Unavailable("PivotArc geometry source could not be loaded.")
                    } else if (requiredPivotControls == null) {
                        PivotArcResolution.Unavailable(
                            "PivotArc controls are not supported for this system. Standard controls remain available.",
                        )
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
                        if (effectiveArcMode.hasGrip) {
                            compositeResolution =
                                if (gripManifest == null) {
                                    CompositeDeckResolution.Unavailable(
                                        CompositeDeckFailure.INVALID_GEOMETRY,
                                        "GripArc geometry could not be loaded. " +
                                            "Standard remains available; saved settings are preserved.",
                                    )
                                } else {
                                    CompositeControlDeckSolver.resolve(
                                        gripManifest, pivotBaseline, viewModel.systemId, safeBounds, viewportBounds,
                                        effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                        pivotSettings.offsetXdp, pivotSettings.offsetYdp, isLandscape,
                                    )
                                }
                            when (val joint = compositeResolution) {
                                is CompositeDeckResolution.Fits -> PivotArcResolution.Fits(joint.placement.pivot)
                                is CompositeDeckResolution.Unavailable -> PivotArcResolution.Unavailable(joint.reason)
                                null -> null
                            }
                        } else {
                            PivotArcPositionSolver.resolve(
                                baseline = pivotBaseline,
                                active = requiredPivotControls,
                                safe = safeBounds,
                                viewport = viewportBounds,
                                leftHanded = pivotSettings.hand == PivotArcHand.LEFT,
                                requestedDx = pivotSettings.offsetXdp,
                                requestedDy = pivotSettings.offsetYdp,
                                isLandscape = isLandscape,
                            )
                        }
                    }
                } else {
                    null
                }
            val jointPlacement = (compositeResolution as? CompositeDeckResolution.Fits)?.placement
            val pivotPlacement = (pivotResolution as? PivotArcResolution.Fits)?.placement
            var combinedPlacement: GripArcCombinedPlacement? = null
            var nativeProfileValidated = false
            val gripResolution: GripArcFit? =
                if (
                    effectiveArcMode.hasGrip &&
                    fullPos != null &&
                    viewPos != null
                ) {
                    if (selectedPivot && touchControlsVisibleState.value) {
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
                            if (measurementsReady && observedIds.isNotEmpty() &&
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
                                        if (measurementsReady && nativeInventory.matchesNativeInputs(observedIds)) {
                                            GripArcCombinedLayout.resolve(
                                                gripManifest, nativeInventory, observedIds,
                                                measured, safeBounds, viewportBounds,
                                                effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                            )
                                        } else {
                                            GripArcCombinedLayout.resolveDockFirst(
                                                gripManifest, nativeInventory, safeBounds, viewportBounds,
                                                effectiveArcSide, arcProfile.dx, arcProfile.dy,
                                            )
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
            DisposableEffect(viewModel, gripEnabled) {
                viewModel.setGripArcEnabled(gripEnabled)
                onDispose { viewModel.setGripArcEnabled(false) }
            }
            val pivotHoldsEnabled =
                pivotPlacement != null && touchControlsVisibleState.value && !isLoading
            DisposableEffect(viewModel, pivotHoldsEnabled) {
                viewModel.setPivotArcHoldsEnabled(pivotHoldsEnabled)
                onDispose { viewModel.setPivotArcHoldsEnabled(false) }
            }

            if (!fullscreenWithGamepad) {
                CartdeckConsoleShell(
                    tokens = shellTokens,
                    videoBounds = viewPos,
                    rootBounds = fullPos,
                    isLandscape = isLandscape,
                    patternId = if (appearance.highContrast) "none" else appearance.patternId,
                    stampId = if (appearance.highContrast) "none" else appearance.stampId,
                    decal = if (appearance.highContrast) null else appearance.decal,
                    lighting = appearance.lighting,
                    highContrast = appearance.highContrast,
                    controlBounds =
                        if (touchControlsVisibleState.value) {
                            if ((pivotPlacement != null || combinedPlacement != null) && fullPos != null) {
                                (combinedPlacement?.controls ?: pivotPlacement!!.controls).values.map { bounds ->
                                    Rect(
                                        fullPos.left + bounds.left * currentDensity.density,
                                        fullPos.top + bounds.top * currentDensity.density,
                                        fullPos.left + bounds.right * currentDensity.density,
                                        fullPos.top + bounds.bottom * currentDensity.density,
                                    )
                                }
                            } else {
                                listOfNotNull(leftPadBounds.value, rightPadBounds.value)
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
                if ((pivotPlacement != null || combinedPlacement != null) && fullPos != null) {
                    (combinedPlacement?.seats ?: pivotPlacement!!.controls).map { (key, bounds) ->
                        val seatKey =
                            if (combinedPlacement != null) {
                                key
                            } else {
                                when (key) {
                                    "D" -> "dpad"
                                    "A", "B", "X", "Y" -> "face-${key.lowercase()}"
                                    "HoldA" -> "hold-a"
                                    "HoldB" -> "hold-b"
                                    "L", "R", "L2", "R2" -> "shoulder-$key"
                                    "AnalogL" -> "analog-left"
                                    "AnalogR" -> "analog-right"
                                    else -> key.lowercase()
                                }
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
            if (jointPlacement != null && touchControlsVisibleState.value) {
                CompositeControlDeckHousing(jointPlacement, shellTokens)
            }
            if (
                touchControlsVisibleState.value &&
                touchControllerSettings != null &&
                currentControllerConfig != null
            ) {
                CartdeckHardwareSeats(
                    tokens = shellTokens,
                    edition = edition,
                    rootBounds = fullPos,
                    videoBounds = viewPos,
                    inputBounds = hardwareInputBounds,
                    isDualScreen =
                        portraitMode == CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN,
                    customColours = appearance.custom.isNotEmpty(),
                    pivotArc = pivotPlacement != null,
                    drawPivotModule = jointPlacement == null,
                )
            }

            LaunchedEffect(fullPos, viewPos) {
                val gameView = viewModel.retroGameView.retroGameViewFlow()
                if (fullPos == null || viewPos == null) return@LaunchedEffect
                val viewport =
                    RectF(
                        (viewPos.left - fullPos.left) / fullPos.width,
                        (viewPos.top - fullPos.top) / fullPos.height,
                        (viewPos.right - fullPos.left) / fullPos.width,
                        (viewPos.bottom - fullPos.top) / fullPos.height,
                    )
                gameView.viewport = viewport
            }

            // With a physical pad, take all landscape space rather than
            // reserving the old left/right touchscreen side containers.
            val useFullLandscapeViewport =
                fullscreenWithGamepad || (currentControllerConfig?.allowTouchOverlay ?: true)
            ConstraintLayout(
                modifier = Modifier.fillMaxSize(),
                constraintSet =
                    GameScreenLayout.buildConstraintSet(
                        isLandscape,
                        useFullLandscapeViewport,
                        portraitMode,
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
                        } else if (!currentControllerConfig.allowTouchOverlay) {
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_CONTAINER),
                            )
                            PadContainer(
                                modifier = Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_CONTAINER),
                            )
                        }

                        if (pivotPlacement != null || combinedPlacement != null) {
                            // Keep ConstraintLayout viewport anchors but do NOT
                            // register the Standard controls as extra input owners.
                            Box(Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD).size(1.dp))
                            Box(Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD).size(1.dp))
                        } else {
                            leftGamePad?.invoke(
                                this,
                                Modifier
                                    .layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD)
                                    .onGloballyPositioned { leftPadBounds.value = it.boundsInRoot() },
                                touchControllerSettings,
                            )
                            rightGamePad?.invoke(
                                this,
                                Modifier
                                    .layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD)
                                    .onGloballyPositioned { rightPadBounds.value = it.boundsInRoot() },
                                touchControllerSettings,
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
                        heldKeys = heldPivotKeys.value,
                        accent = shellTokens.accent,
                        onToggleHold = viewModel::togglePivotArcHold,
                        combined = jointPlacement != null,
                    )
                }
            }
            if (combinedPlacement != null && touchControlsVisibleState.value && !isLoading) {
                CompositionLocalProvider(LocalLemuroidPadTheme provides controllerTheme) {
                    GripArcCombinedControls(combinedPlacement!!)
                }
            }
            GripArcAccessory(
                placement = gripPlacement,
                profile = arcProfile,
                tokens = shellTokens,
                fullscreenWithGamepad = fullscreenWithGamepad,
                isLoading = isLoading,
                onPressed = viewModel::pressGripArcSlot,
                drawHousing = jointPlacement == null,
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
