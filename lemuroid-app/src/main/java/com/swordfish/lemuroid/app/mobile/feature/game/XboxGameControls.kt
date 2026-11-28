package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.rememberDisplaySettings
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberGameplayAppearance
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.app.shared.game.viewmodel.PivotArcHeldInputGate
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalCartdeckControlBounds
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.PadKit
import gg.padkit.inputevents.InputEvent
import android.graphics.Rect as AndroidRect

/** The standard CARTDECK shell and PadKit own input; SDL only supplies the game surface beneath it. */
@Composable
fun XboxGameControls(
    onInput: (List<InputEvent>) -> Unit,
    settingsManager: TouchControllerSettingsManager? = null,
    controllerId: TouchControllerID = TouchControllerID.XBOX,
    physicalControllerConnected: Boolean = false,
    game: com.swordfish.lemuroid.lib.library.db.entity.Game? = null,
    onGripPressed: (Int, Boolean, Set<Int>) -> Unit = { _, _, _ -> },
    canUseGrip: () -> Boolean = { false },
    wiiuPro: Boolean = false,
    onGripMenu: () -> Unit = {},
    presentationAspect: Float = 4f / 3f,
    pivotSystem: SystemID? = null,
    pivotGate: PivotArcHeldInputGate? = null,
    onTogglePivotHold: (Int) -> Unit = {},
    onViewport: (AndroidRect) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val display = rememberDisplaySettings(game?.cartridgeId)
    val resolved = rememberGameplayAppearance()
    val appearance = resolved.source
    val edition = resolved.edition
    val tokens = resolved.tokens
    val theme = remember(resolved.controllerPalette) { LemuroidPadTheme(resolved.controllerPalette) }
    val manager =
        settingsManager ?: remember {
            TouchControllerSettingsManager(SharedPreferencesHelper.getSharedPreferences(context))
        }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = constraints.maxWidth > constraints.maxHeight
        val border = display.effective.border(landscape)
        // SDL owns the guest surface beneath this transparent Compose overlay.
        // The maximised path must not instantiate PadKit's virtual controller.
        val isTv =
            remember(context) {
                context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
            }
        if (landscape && (physicalControllerConnected || isTv)) {
            CartdeckMaximisedFrame(
                tokens,
                onViewport,
                presentationAspect,
                isTv,
                border != DisplaySettingsStore.Border.OFF,
                lighting = resolved.lighting,
                highContrast = appearance.highContrast,
            )
            return@BoxWithConstraints
        }
        val orientation =
            if (landscape) {
                TouchControllerSettingsManager.Orientation.LANDSCAPE
            } else {
                TouchControllerSettingsManager.Orientation.PORTRAIT
            }
        val density = LocalDensity.current
        val insets = WindowInsets.displayCutout
        val settings =
            remember(orientation, density, insets) {
                manager.observeSettings(controllerId, orientation, density, insets)
            }.collectAsState(TouchControllerSettingsManager.Settings()).value
        val root = remember { mutableStateOf<Rect?>(null) }
        val video = remember { mutableStateOf<Rect?>(null) }
        val bounds = remember(landscape) { mutableStateMapOf<String, Rect>() }
        val observe: (String, Rect) -> Unit =
            remember(bounds) {
                { name, rect -> if (bounds[name] != rect) bounds[name] = rect }
            }
        val assignmentMode = gripArcAssignmentMode(controllerId, wiiuPro = wiiuPro)
        var useStandardForSession by remember(game) { mutableStateOf(false) }
        val savedArc = game?.let { rememberGripArcProfile(it, assignmentMode).value }
        val arc = savedArc.takeUnless { useStandardForSession }
        val hostSystem =
            pivotSystem ?: if (controllerId == TouchControllerID.XBOX360) SystemID.XBOX360 else SystemID.XBOX
        val standalone = supportsStandaloneGripActions(hostSystem) && StandaloneGripArcLayout.supports(controllerId)
        val gripChassis = standalone && arc?.mode == ArcControlMode.STANDARD_GRIPARC && !physicalControllerConnected
        val hardwareBorder = gripChassis || border == DisplaySettingsStore.Border.STANDARD
        val inventory =
            remember(controllerId, hostSystem, wiiuPro) {
                if (standalone) {
                    StandaloneGripArcLayout.inventoryFor(controllerId, hostSystem, wiiuPro)
                } else {
                    com.swordfish.touchinput.radial.settings.NativeControlInventory.forController(controllerId)
                }
            }
        val manifest =
            remember(game, standalone) {
                if (game != null && !standalone) runCatching { GripArcManifest.load(context) }.getOrNull() else null
            }
        val viewport = video.value
        var chassisWindowBottom by remember { mutableFloatStateOf(0f) }
        val windowView = LocalView.current
        val origin = root.value
        val safeDrawing = WindowInsets.safeDrawing
        val safeGestures = WindowInsets.safeGestures
        val placement =
            if (arc?.mode == ArcControlMode.STANDARD_GRIPARC && inventory != null &&
                viewport != null && origin != null && !physicalControllerConnected && !isTv
            ) {
                val scale = density.density
                val direction = androidx.compose.ui.unit.LayoutDirection.Ltr
                val safe =
                    PivotArcBounds(
                        maxOf(safeDrawing.getLeft(density, direction), safeGestures.getLeft(density, direction)) /
                            scale + 8f,
                        maxOf(safeDrawing.getTop(density), safeGestures.getTop(density)) / scale + 8f,
                        origin.width / scale -
                            maxOf(safeDrawing.getRight(density, direction), safeGestures.getRight(density, direction)) /
                            scale - 8f,
                        origin.height / scale -
                            maxOf(safeDrawing.getBottom(density), safeGestures.getBottom(density)) / scale - 8f,
                    )
                val videoBounds =
                    PivotArcBounds(
                        (viewport.left - origin.left) / scale,
                        (viewport.top - origin.top) / scale,
                        (viewport.right - origin.left) / scale,
                        (viewport.bottom - origin.top) / scale,
                    )
                if (standalone) {
                    remember(inventory, safe, videoBounds, landscape) {
                        StandaloneGripArcLayout.resolveStandalone(inventory, safe, videoBounds, landscape)
                    }
                } else {
                    manifest?.let {
                        val legacySafe =
                            PivotArcBounds(
                                insets.getLeft(density, direction) / scale,
                                insets.getTop(density) / scale,
                                (constraints.maxWidth - insets.getRight(density, direction)) / scale,
                                (constraints.maxHeight - insets.getBottom(density)) / scale,
                            )
                        GripArcCombinedLayout.resolveDockFirst(
                            it,
                            inventory,
                            legacySafe,
                            videoBounds,
                            arc.side,
                            arc.dx,
                            arc.dy,
                        )?.takeIf { fitted -> fitted.complete }
                    }
                }
            } else {
                null
            }
        val gripEnabled = standalone && placement != null
        DisposableEffect(pivotGate, gripEnabled, arc, inventory, landscape) {
            pivotGate?.setGripEnabled(gripEnabled)
            onDispose { pivotGate?.setGripEnabled(false) }
        }
        val latchedSlots = pivotGate?.gripLatches?.collectAsState()?.value.orEmpty()
        val pivotSettings = rememberPivotArcSettings().value
        val pivotProfile =
            if (arc?.mode == ArcControlMode.PIVOTARC && pivotGate != null && !physicalControllerConnected && !isTv) {
                PivotArcSoloProfiles.forNativeHost(hostSystem, controllerId)
            } else {
                null
            }
        val pivotPlacement =
            if (pivotProfile != null && viewport != null && origin != null) {
                val scale = density.density
                val direction = androidx.compose.ui.unit.LayoutDirection.Ltr
                val safe =
                    PivotArcBounds(
                        maxOf(safeDrawing.getLeft(density, direction), safeGestures.getLeft(density, direction)) /
                            scale + 8f,
                        maxOf(safeDrawing.getTop(density), safeGestures.getTop(density)) / scale + 8f,
                        origin.width / scale -
                            maxOf(safeDrawing.getRight(density, direction), safeGestures.getRight(density, direction)) /
                            scale - 8f,
                        pivotChassisBottom(
                            origin.height / scale -
                                maxOf(safeDrawing.getBottom(density), safeGestures.getBottom(density)) / scale - 8f,
                            chassisWindowBottom,
                            windowView.rootView.height,
                            maxOf(safeDrawing.getBottom(density), safeGestures.getBottom(density)),
                            scale,
                        ),
                    )
                val videoBounds =
                    PivotArcBounds(
                        (viewport.left - origin.left) / scale,
                        (viewport.top - origin.top) / scale,
                        (viewport.right - origin.left) / scale,
                        (viewport.bottom - origin.top) / scale,
                    )
                (
                    PivotArcSoloSolver.resolve(
                        pivotProfile,
                        safe,
                        videoBounds,
                        pivotSettings.hand == PivotArcHand.LEFT,
                        pivotSettings.offsetXdp,
                        pivotSettings.offsetYdp,
                        landscape,
                    ) as? PivotArcResolution.Fits
                )?.placement
            } else {
                null
            }
        val activePivot = pivotPlacement != null
        val holdKeys = pivotProfile?.holdKeys.orEmpty()
        DisposableEffect(pivotGate, activePivot, holdKeys) {
            pivotGate?.setEnabled(activePivot, holdKeys)
            onDispose { pivotGate?.setEnabled(false) }
        }
        val pivotHeld = pivotGate?.holds?.collectAsState()?.value.orEmpty()
        val measuredBounds =
            if (pivotPlacement != null && origin != null) {
                pivotPlacement.controls.map { (key, rect) ->
                    val scale = density.density
                    pivotArcHardwareSeatKey(key) to
                        Rect(
                            origin.left + rect.left * scale,
                            origin.top + rect.top * scale,
                            origin.left + rect.right * scale,
                            origin.top + rect.bottom * scale,
                        )
                }.toMap()
            } else {
                bounds
            }
        val unavailable =
            arc != null && arc.mode != ArcControlMode.STANDARD &&
                (arc.mode != ArcControlMode.STANDARD_GRIPARC || placement == null) &&
                (arc.mode != ArcControlMode.PIVOTARC || pivotPlacement == null) && viewport != null &&
                !physicalControllerConnected && !isTv
        var settledFailure by remember(origin, viewport, unavailable) { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(origin, viewport, unavailable) {
            if (unavailable) {
                kotlinx.coroutines.delay(250)
                settledFailure = true
            }
        }
        if (settledFailure && !useStandardForSession) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { useStandardForSession = true },
                title = { androidx.compose.material3.Text("CARTDECK controller layout") },
                text = {
                    androidx.compose.material3.Text(
                        "There is not enough room for the selected controller layout on this screen. " +
                            "CARTDECK will use Standard controls for this session and keep your cartridge setting.",
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        useStandardForSession = true
                    }) { androidx.compose.material3.Text("Continue") }
                },
            )
        }
        PadKit(
            Modifier.fillMaxSize().onGloballyPositioned {
                root.value = it.boundsInRoot()
                chassisWindowBottom = it.positionInWindow().y + it.size.height
            },
            onInputEvents = onInput,
        ) {
            if (gripChassis || border != DisplaySettingsStore.Border.OFF) {
                CartdeckConsoleShell(
                    tokens = tokens,
                    videoBounds = video.value,
                    rootBounds = root.value,
                    isLandscape = landscape,
                    controlBounds = measuredBounds.values.toList(),
                    patternId = resolved.patternId,
                    stampId = resolved.stampId,
                    decal = resolved.decal,
                    // Native hosts currently provide baseline lighting, not core-feedback events.
                    lighting = resolved.lighting,
                    highContrast = appearance.highContrast,
                    minimal = !hardwareBorder,
                    mountedPivotDeck = activePivot,
                    continuousLandscapeBrand = standalone && placement != null,
                )
            }
            if (standalone && hardwareBorder && placement != null) {
                StandaloneGripArcHardware(placement, tokens, landscape)
            }
            if ((!standalone || placement == null) && (hardwareBorder || activePivot)) {
                CartdeckHardwareSeats(
                    rootBounds = root.value,
                    videoBounds = video.value,
                    inputBounds = measuredBounds,
                    tokens = tokens,
                    edition = edition,
                    isDualScreen = false,
                    customColours = resolved.customColours,
                    pivotArc = activePivot,
                    contouredPivot = activePivot,
                    pivotUtilities = pivotProfile?.hardwareUtilities ?: PivotArcModuleGeometry.legacyUtilityKeys,
                    lighting = resolved.lighting,
                    highContrast = appearance.highContrast,
                )
            }
            CompositionLocalProvider(
                LocalLemuroidPadTheme provides theme,
                LocalCartdeckControlBounds provides observe,
            ) {
                val roomyLandscape =
                    this@BoxWithConstraints.maxWidth >= 680.dp && this@BoxWithConstraints.maxHeight >= 320.dp
                val gripArcLandscape =
                    landscape && hardwareBorder && !physicalControllerConnected && !isTv &&
                        arc?.mode == ArcControlMode.STANDARD_GRIPARC && (standalone || roomyLandscape)
                val dedicatedHardwareLayout =
                    landscape && hardwareBorder &&
                        (arc == null || arc.mode == ArcControlMode.STANDARD || gripArcLandscape)
                ConstraintLayout(
                    modifier = Modifier.fillMaxSize(),
                    constraintSet =
                        GameScreenLayout.buildConstraintSet(
                            landscape,
                            !dedicatedHardwareLayout,
                            CartdeckViewportPolicy.PortraitMode.STANDARD,
                            hardwareLandscape = dedicatedHardwareLayout,
                            gripArcLandscape = gripArcLandscape,
                            standaloneGripRailInset =
                                if (standalone && gripArcLandscape) {
                                    (
                                        maxOf(safeDrawing.getBottom(density), safeGestures.getBottom(density)) /
                                            density.density + 60f
                                    ).dp
                                } else {
                                    0.dp
                                },
                            independentPortraitAperture = pivotProfile != null || gripChassis,
                            standalonePortraitBottomReserve =
                                if (gripChassis && StandaloneGripArcLayout.densePortrait(controllerId)) {
                                    val bottom = maxOf(safeDrawing.getBottom(density), safeGestures.getBottom(density))
                                    (bottom / density.density + 452f).dp
                                } else {
                                    0.dp
                                },
                            availableWidth = this@BoxWithConstraints.maxWidth,
                            availableHeight = this@BoxWithConstraints.maxHeight,
                            portraitApertureRatio =
                                if (presentationAspect >= 1.6f) "16:9" else CartdeckViewportPolicy.STANDARD_RATIO,
                        ),
                ) {
                    Box(
                        Modifier.layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW).onGloballyPositioned {
                            val measured = it.boundsInRoot()
                            val fit =
                                NativeViewportPolicy.fit(
                                    measured.width,
                                    measured.height,
                                    presentationAspect,
                                    edgeGuardPx = 1f,
                                )
                            if (fit.width > 0f && fit.height > 0f) {
                                val fitted =
                                    Rect(
                                        measured.left + fit.left,
                                        measured.top + fit.top,
                                        measured.left + fit.right,
                                        measured.top + fit.bottom,
                                    )
                                if (video.value != fitted) video.value = fitted
                                NativeViewportPolicy.inwardPixels(
                                    fitted.left,
                                    fitted.top,
                                    fitted.right,
                                    fitted.bottom,
                                )?.let { pixels ->
                                    onViewport(
                                        AndroidRect(pixels.left, pixels.top, pixels.right, pixels.bottom),
                                    )
                                }
                            }
                        },
                    )
                    if (placement == null && pivotPlacement == null && !physicalControllerConnected && !isTv) {
                        val config = TouchControllerID.getConfig(controllerId)
                        val effectiveSettings =
                            if (dedicatedHardwareLayout) {
                                GameScreenLayout.fitLandscapeHardwareSettings(
                                    settings,
                                    this@BoxWithConstraints.maxHeight,
                                )
                            } else {
                                settings
                            }
                        config.leftComposable(
                            this@PadKit,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD),
                            effectiveSettings,
                        )
                        config.rightComposable(
                            this@PadKit,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD),
                            effectiveSettings,
                        )
                    }
                }
                if (pivotPlacement != null && pivotProfile != null) {
                    PivotArcControlGroup(
                        placement = pivotPlacement,
                        system = hostSystem,
                        heldKeys = pivotHeld,
                        accent = tokens.accent,
                        onToggleHold = onTogglePivotHold,
                        touchController = controllerId,
                        soloProfile = pivotProfile,
                    )
                }
                if (placement != null && arc != null && !physicalControllerConnected && !isTv) {
                    GripArcCombinedControls(placement, standalone = standalone)
                    GripArcAccessory(
                        placement.dock,
                        arc,
                        tokens,
                        false,
                        false,
                        onPressed = { slot, pressed, keys ->
                            if (!standalone) {
                                onGripPressed(slot, pressed, keys)
                            } else if (gripEnabled && canUseGrip()) {
                                when (val action = StandaloneGripArcLayout.guestAction(arc, slot, placement.native)) {
                                    GripArcAction.OpenMenu -> if (pressed) onGripMenu()
                                    null -> Unit
                                    else -> pivotGate?.applyGripAction(slot, pressed, action, scope)
                                }
                            }
                        },
                        inputLabels =
                            if (standalone) {
                                StandaloneGripArcLayout.actionLabels(placement.native)
                            } else {
                                GripArcInputCatalog.forSystem(hostSystem).associate { it.code to it.label }
                            },
                        typedActions = standalone,
                        latchedSlots = latchedSlots,
                        drawHousing = hardwareBorder && !standalone,
                        lighting = resolved.lighting,
                        highContrast = appearance.highContrast,
                    )
                }
            }
        }
    }
}
