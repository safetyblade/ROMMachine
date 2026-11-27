package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.game.ArcControlMode
import com.swordfish.lemuroid.app.mobile.feature.game.CartdeckConsoleShell
import com.swordfish.lemuroid.app.mobile.feature.game.CartdeckHardwareSeats
import com.swordfish.lemuroid.app.mobile.feature.game.CartdeckLayoutMode
import com.swordfish.lemuroid.app.mobile.feature.game.CartdeckViewportPolicy
import com.swordfish.lemuroid.app.mobile.feature.game.ControlsOrientation
import com.swordfish.lemuroid.app.mobile.feature.game.GripArcProfileStore
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcBounds
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcControlGroup
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcHand
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcLayoutProfile
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcModuleGeometry
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcPositionSolver
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcResolution
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcSettingsStore
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcSoloProfiles
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcSoloSolver
import com.swordfish.lemuroid.app.mobile.feature.game.pivotArcHardwareSeatKey
import com.swordfish.lemuroid.app.mobile.feature.game.rememberGripArcProfile
import com.swordfish.lemuroid.app.mobile.feature.game.rememberPivotArcSettings
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.app.shared.game.DisplayEngineOptions
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.layouts.LocalWiiNunchukPointerStick
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import dagger.Lazy
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun nativePreviewController(system: SystemID): TouchControllerID? =
    when (system) {
        SystemID.XBOX -> TouchControllerID.XBOX
        SystemID.XBOX360 -> TouchControllerID.XBOX360
        SystemID.PLAYSTATION_2, SystemID.PLAYSTATION_3 -> TouchControllerID.PSX_DUALSHOCK
        SystemID.WII_U -> TouchControllerID.WII_CLASSIC
        else -> null
    }

@Composable
internal fun UniversalControls(
    game: Game?,
    core: SystemCoreConfig? = null,
    activeController: String? = null,
    onLiveController: ((String) -> Unit)? = null,
    orientationSupported: Boolean = true,
    physical: @Composable () -> Unit = {},
    advanced: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val global by rememberPivotArcSettings()
    val profile = game?.let { rememberGripArcProfile(it).value }
    val store = remember(context) { GripArcProfileStore(context) }
    val globalStore = remember(context) { PivotArcSettingsStore(context) }
    val manager =
        remember(context) {
            ControllerConfigsManager(Lazy { SharedPreferencesHelper.getSharedPreferences(context) })
        }
    var example by remember { mutableStateOf(SystemID.GBA) }
    val system = game?.let { SystemID.entries.firstOrNull { system -> system.dbname == it.systemId } } ?: example
    val config = core ?: remember(system) { GameSystem.findById(system.dbname).systemCoreConfigs.firstOrNull() }
    val controllers = config?.controllerConfigs?.get(0).orEmpty()
    var nativeName by remember(game?.cartridgeId, config, activeController) { mutableStateOf(activeController) }
    LaunchedEffect(game?.cartridgeId, config, activeController) {
        if (activeController == null && config != null) {
            nativeName = manager.getControllerConfigs(system, config, game)[0]?.name
        }
    }
    val controller =
        (controllers.firstOrNull { it.name == nativeName } ?: controllers.firstOrNull())?.touchControllerID
            ?: nativePreviewController(system)
    val defaultMode =
        if (global.mode == CartdeckLayoutMode.ONE_HAND) ArcControlMode.PIVOTARC else ArcControlMode.STANDARD
    val selected = profile?.mode ?: defaultMode
    val inheritsLayout = game != null && !store.hasSelection(game)
    var previewLandscape by remember { mutableStateOf(false) }
    var openPhysical by remember { mutableStateOf(false) }
    var openAdvanced by remember { mutableStateOf(false) }
    var openProgrammer by remember { mutableStateOf(false) }
    var examplesOpen by remember { mutableStateOf(false) }
    var feedback by remember(game?.cartridgeId) { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (game == null) {
            Text("Default controls", style = MaterialTheme.typography.titleMedium)
            GameMenuLink("Preview system · ${stringResource(GameSystem.findById(system.dbname).titleResId)}") {
                examplesOpen = !examplesOpen
            }
            if (examplesOpen) {
                SystemID.entries.forEach {
                    GameMenuLink(stringResource(GameSystem.findById(it.dbname).titleResId)) {
                        example = it
                        examplesOpen = false
                    }
                }
            }
        }
        if (controllers.size > 1) {
            var chooseNative by remember { mutableStateOf(false) }
            val title = if (system == SystemID.WII) "Wii controller" else "Console controller"
            GameMenuLink(title) { chooseNative = true }
            controllers.firstOrNull { it.touchControllerID == controller }?.let {
                Text(stringResource(it.displayName))
            }
            if (chooseNative) {
                AlertDialog(
                    onDismissRequest = { chooseNative = false },
                    title = { Text(title) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            controllers.forEach { option ->
                                GameMenuLink(stringResource(option.displayName)) {
                                    chooseNative = false
                                    if (onLiveController != null && system == SystemID.WII) {
                                        onLiveController(option.name)
                                    } else if (config != null) {
                                        scope.launch {
                                            if (manager.selectController(system, config, 0, option.name, game)) {
                                                nativeName = option.name
                                                feedback =
                                                    if (onLiveController != null) {
                                                        "Controller saved. Restart the game to apply."
                                                    } else {
                                                        "Controller saved."
                                                    }
                                            } else {
                                                feedback = "Could not save controller."
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { chooseNative = false }) { Text("Cancel") } },
                )
            }
        }
        val pointerMode =
            if (controller in setOf(TouchControllerID.WII_NUNCHUK, TouchControllerID.WII_SIDEWAYS) && config != null) {
                WiiPointerControls(game, config) { feedback = it }
            } else {
                "2"
            }
        Text("Preview orientation", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!previewLandscape, { previewLandscape = false }, { Text("Preview portrait") })
            FilterChip(previewLandscape, { previewLandscape = true }, { Text("Preview landscape") })
        }
        val supportedPivot =
            controller != null && (
                PivotArcSoloProfiles.forMode(system, controller) != null ||
                    PivotArcSoloProfiles.forNativeHost(system, controller) != null ||
                    PivotArcLayoutProfile.activeFor(system) != null
            )
        for (pair in ArcControlMode.entries.chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (mode in pair) {
                    ControllerLayoutCard(
                        modifier = Modifier.weight(1f),
                        mode = mode,
                        selected = mode == selected,
                        inheritsLayout = inheritsLayout,
                        system = system,
                        controller = controller,
                        landscape = previewLandscape,
                        supportedPivot = supportedPivot,
                        pointerMode = pointerMode,
                        onApply = {
                            if (game != null) {
                                val saved = store.saveLayout(game, mode)
                                feedback = if (saved) "Layout applied." else "Could not save layout."
                            } else {
                                val layout =
                                    if (mode.hasPivot) CartdeckLayoutMode.ONE_HAND else CartdeckLayoutMode.STANDARD
                                globalStore.save(globalStore.read().copy(mode = layout))
                                feedback = "Default layout applied."
                            }
                        },
                    )
                }
            }
        }
        if (game != null && store.hasSelection(game)) {
            GameMenuLink("Use global layout default") {
                feedback = if (store.useGlobalLayout(game)) "Using the global layout." else "Could not save layout."
            }
        }
        if (orientationSupported && (game == null || config != null)) {
            var choice by remember(game?.cartridgeId) { mutableStateOf(game?.let(store::orientationOverride)) }
            var orientationOpen by remember { mutableStateOf(false) }
            GameMenuLink("Gameplay orientation · ${(choice ?: global.orientation).name.lowercase()}") {
                orientationOpen = !orientationOpen
            }
            if (orientationOpen) {
                for (orientation in ControlsOrientation.entries) {
                    val active = (choice ?: global.orientation) == orientation
                    GameMenuLink(
                        orientation.name.lowercase().replaceFirstChar(Char::uppercaseChar) +
                            if (active) " · Selected" else "",
                    ) {
                        if (game == null) {
                            globalStore.save(globalStore.read().copy(orientation = orientation))
                            feedback = "Default orientation saved."
                        } else if (store.saveOrientation(game, orientation)) {
                            choice = orientation
                            feedback = "Orientation saved. Applies the next time you open the game."
                        }
                    }
                }
                if (game != null && choice != null) {
                    GameMenuLink("Use global orientation") {
                        if (store.saveOrientation(game, null)) choice = null
                    }
                }
            }
        }
        feedback?.let { Text(it) }
        if (game != null && profile?.mode?.hasGrip == true) {
            GameMenuLink("Programmable controls") { openProgrammer = !openProgrammer }
            if (openProgrammer) GripArcEditorScreen(game, embedded = true)
        }
        GameMenuLink("Physical controller · Map and test") { openPhysical = !openPhysical }
        if (openPhysical) physical()
        if (advanced != null) {
            GameMenuLink("Advanced") { openAdvanced = !openAdvanced }
            if (openAdvanced) advanced()
        }
    }
}

@Composable
private fun ControllerLayoutCard(
    modifier: Modifier,
    mode: ArcControlMode,
    selected: Boolean,
    inheritsLayout: Boolean,
    system: SystemID,
    controller: TouchControllerID?,
    landscape: Boolean,
    supportedPivot: Boolean,
    pointerMode: String,
    onApply: () -> Unit,
) {
    val (title, description) =
        when (mode) {
            ArcControlMode.STANDARD -> "Standard" to "Classic controls"
            ArcControlMode.PIVOTARC -> "PivotARC" to "On-the-go play"
            ArcControlMode.STANDARD_GRIPARC -> "GripARC" to "Power-user controls"
            ArcControlMode.PIVOTARC_GRIPARC -> "PivotARC + GripARC" to "One-handed play with extra functions"
        }
    val available = !mode.hasGrip && controller != null && (!mode.hasPivot || (supportedPivot && !landscape))
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description)
            if (selected && inheritsLayout) Text("Selected via global default")
            when {
                mode.hasGrip -> Text("Awaiting an approved hardware layout for this controller.")
                controller == null -> Text("Touch preview unavailable for this engine.")
                mode.hasPivot && !supportedPivot -> Text("PivotARC is unavailable for this controller.")
                mode.hasPivot && landscape -> Text("PivotARC uses Standard controls in landscape.")
                else -> ControlsLayoutPreview(system, controller, mode, landscape, pointerMode)
            }
            if (available || selected) {
                TextButton(enabled = available && (!selected || inheritsLayout), onClick = onApply) {
                    val action =
                        when {
                            !selected -> "Use $title"
                            inheritsLayout -> "Keep for this cartridge"
                            else -> "Selected"
                        }
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun WiiPointerControls(
    game: Game?,
    config: SystemCoreConfig,
    feedback: (String) -> Unit,
): String {
    val key = "dolphin_ir_mode"
    val options = config.exposedSettings.firstOrNull { it.key == key }?.values.orEmpty()
    if (options.isEmpty()) return "2"
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val namespace = DisplayEngineOptions.namespace(SystemID.WII.dbname, config)
    val preferenceKey = CoreVariablesManager.computeSharedPreferenceKey(key, SystemID.WII.dbname)
    val preferences = remember(context) { SharedPreferencesHelper.getSharedPreferences(context) }
    val store = remember(context) { DisplaySettingsStore(context) }
    val allowed = remember(config) { options.map { it.key }.toSet() }
    var value by remember(game?.cartridgeId, config) { mutableStateOf("2") }
    LaunchedEffect(game?.cartridgeId, config) {
        value =
            withContext(Dispatchers.IO) {
                store.readSafely(game?.cartridgeId).effective.engineOptions["$namespace:$key"]
                    ?: preferences.getString(preferenceKey, "2") ?: "2"
            }
    }
    Text("Wii pointer", style = MaterialTheme.typography.titleMedium)
    for (option in options) {
        GameMenuLink(stringResource(option.titleId) + if (option.key == value) " · Selected" else "") {
            scope.launch {
                val saved =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            if (game != null) {
                                store.setEngineOption(game.cartridgeId, namespace, key, option.key, allowed)
                                true
                            } else {
                                preferences.edit().putString(preferenceKey, option.key).commit()
                            }
                        }.getOrDefault(false)
                    }
                if (saved) value = option.key
                feedback(if (saved) "Wii pointer saved." else "Could not save Wii pointer.")
            }
        }
    }
    return value
}

@Composable
private fun ControlsLayoutPreview(
    system: SystemID,
    controller: TouchControllerID,
    mode: ArcControlMode,
    landscape: Boolean,
    pointerMode: String,
) {
    val context = LocalContext.current
    val settings by rememberPivotArcSettings()
    val width = if (landscape) 740 else 393
    val height = if (landscape) 360 else 852
    val root = PivotArcBounds(0f, 0f, width.toFloat(), height.toFloat())
    val viewport =
        if (landscape) {
            PivotArcBounds(180f, 15f, 560f, 300f)
        } else if (CartdeckViewportPolicy.portraitMode(system) == CartdeckViewportPolicy.PortraitMode.DUAL_SCREEN) {
            PivotArcBounds(18f, 18f, 375f, 410f)
        } else {
            val (w, h) = CartdeckViewportPolicy.portraitApertureRatio(system, 852f).split(':').map(String::toFloat)
            PivotArcBounds(22f, 84f, 371f, 84f + 349f * h / w)
        }
    val standardNunchuk = mode == ArcControlMode.STANDARD && controller == TouchControllerID.WII_NUNCHUK && !landscape
    val profile =
        remember(system, controller, settings.pspAnalogPrimary, pointerMode, standardNunchuk) {
            if (standardNunchuk) {
                return@remember PivotArcSoloProfiles.wiiNunchuk(pointerMode != "2", fixedHolds = false)
            }

            PivotArcSoloProfiles.forMode(system, controller, settings.pspAnalogPrimary, pointerMode != "2")
                ?: PivotArcSoloProfiles.forNativeHost(system, controller)
        }
    val fit =
        remember(system, controller, mode, landscape, settings, profile) {
            if (!mode.hasPivot) {
                null
            } else {
                val safe = PivotArcBounds(8f, 32f, 385f, 820f)
                val left = settings.hand == PivotArcHand.LEFT
                if (profile != null) {
                    PivotArcSoloSolver.resolve(
                        profile,
                        safe,
                        viewport,
                        left,
                        settings.offsetXdp,
                        settings.offsetYdp,
                        landscape,
                    )
                } else {
                    PivotArcLayoutProfile.activeFor(system)?.let { active ->
                        PivotArcPositionSolver.resolve(
                            PivotArcLayoutProfile.load(context), active, safe, viewport, left,
                            settings.offsetXdp, settings.offsetYdp, landscape, requireHardware = true,
                        )
                    }
                }
            }
        }
    val placed =
        if (standardNunchuk && profile != null) {
            PivotArcSoloProfiles.standardWiiNunchuk(profile, PivotArcBounds(8f, 32f, 385f, 820f), viewport)
        } else {
            (fit as? PivotArcResolution.Fits)?.placement
        }
    if (mode.hasPivot && placed == null) {
        Text("PivotARC cannot fit this reference handset.")
        return
    }
    val native = remember(controller) { TouchControllerID.getConfig(controller) }
    val tokens = context.resolveCartdeckEdition(CartdeckEditionPreset.ORIGINAL)
    val touches = placed?.controls.orEmpty()
    Layout(
        modifier = Modifier.fillMaxWidth().height(if (landscape) 132.dp else 216.dp).clearAndSetSemantics {},
        content = {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.fillMaxSize().background(tokens.bezel)) {
                    val contoured = mode.hasPivot && system != SystemID.GBA
                    CartdeckConsoleShell(
                        tokens,
                        viewport.asRect(),
                        root.asRect(),
                        landscape,
                        touches.values.map { it.asRect() },
                        mountedPivotDeck = contoured,
                    )
                    if (placed != null) {
                        CartdeckHardwareSeats(
                            rootBounds = root.asRect(),
                            videoBounds = viewport.asRect(),
                            inputBounds =
                                touches.mapKeys {
                                    pivotArcHardwareSeatKey(
                                        it.key,
                                    )
                                }.mapValues { it.value.asRect() },
                            tokens = tokens,
                            edition = CartdeckEditionPreset.ORIGINAL,
                            isDualScreen = system == SystemID.NDS,
                            customColours = false,
                            pivotArc = mode.hasPivot,
                            measuredStandard = standardNunchuk,
                            contouredPivot = contoured,
                            pivotUtilities = profile?.hardwareUtilities ?: PivotArcModuleGeometry.legacyUtilityKeys,
                        )
                    }
                    CompositionLocalProvider(
                        LocalLemuroidPadTheme provides LemuroidPadTheme(),
                        LocalWiiNunchukPointerStick provides (pointerMode != "2"),
                    ) {
                        PadKit(
                            onInputEvents = {},
                            hapticFeedbackType = HapticFeedbackType.NONE,
                            simulatedState = remember { mutableStateOf(InputState()) },
                            simulatedControlIds = remember { mutableStateOf(emptySet()) },
                        ) {
                            if (placed == null) {
                                val position =
                                    if (landscape) {
                                        Modifier.align(Alignment.CenterStart)
                                    } else {
                                        Modifier.align(Alignment.BottomStart)
                                    }
                                native.leftComposable(this, position, TouchControllerSettingsManager.Settings())
                                val right =
                                    if (landscape) {
                                        Modifier.align(Alignment.CenterEnd)
                                    } else {
                                        Modifier.align(Alignment.BottomEnd)
                                    }
                                native.rightComposable(this, right, TouchControllerSettingsManager.Settings())
                            } else {
                                PivotArcControlGroup(
                                    placed,
                                    system,
                                    emptySet(),
                                    tokens.accent,
                                    {},
                                    touchController = controller,
                                    soloProfile = profile,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { children, constraints ->
        val w = width
        val h = height
        val preview = children.single().measure(Constraints.fixed(w, h))
        val scale = minOf(constraints.maxWidth.toFloat() / w, constraints.maxHeight.toFloat() / h)
        layout(constraints.maxWidth, constraints.maxHeight) {
            preview.placeWithLayer(
                ((constraints.maxWidth - w * scale) / 2f).toInt(),
                ((constraints.maxHeight - h * scale) / 2f).toInt(),
            ) {
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = scale
                scaleY = scale
            }
        }
    }
}

private fun PivotArcBounds.asRect() = Rect(left, top, right, bottom)
