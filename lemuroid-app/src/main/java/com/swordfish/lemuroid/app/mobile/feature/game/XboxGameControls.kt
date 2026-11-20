package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.constraintlayout.compose.ConstraintLayout
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.rememberDisplaySettings
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberGameplayAppearance
import com.swordfish.lemuroid.app.shared.game.DisplaySettingsStore
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
    presentationAspect: Float = 4f / 3f,
    onViewport: (AndroidRect) -> Unit,
) {
    val context = LocalContext.current
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
        val hardwareBorder = border == DisplaySettingsStore.Border.STANDARD
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
        val arc = game?.let { rememberGripArcProfile(it).value }
        val manifest =
            remember(game) { if (game != null) runCatching { GripArcManifest.load(context) }.getOrNull() else null }
        val inventory =
            remember(
                controllerId,
            ) { com.swordfish.touchinput.radial.settings.NativeControlInventory.forController(controllerId) }
        val viewport = video.value
        val origin = root.value
        val placement =
            remember(arc, manifest, inventory, viewport, origin, constraints, density) {
                if (arc?.mode == ArcControlMode.STANDARD_GRIPARC && manifest != null && inventory != null &&
                    viewport != null && origin != null
                ) {
                    val scale = density.density
                    val safe =
                        PivotArcBounds(
                            insets.getLeft(density, androidx.compose.ui.unit.LayoutDirection.Ltr) / scale,
                            insets.getTop(density) / scale,
                            (
                                constraints.maxWidth -
                                    insets.getRight(density, androidx.compose.ui.unit.LayoutDirection.Ltr)
                            ) / scale,
                            (constraints.maxHeight - insets.getBottom(density)) / scale,
                        )
                    GripArcCombinedLayout.resolveDockFirst(
                        manifest,
                        inventory,
                        safe,
                        PivotArcBounds(
                            (viewport.left - origin.left) / scale,
                            (viewport.top - origin.top) / scale,
                            (viewport.right - origin.left) / scale,
                            (viewport.bottom - origin.top) / scale,
                        ),
                        arc.side,
                        arc.dx,
                        arc.dy,
                    )?.takeIf { it.complete }
                } else {
                    null
                }
            }
        var fallbackShown by androidx.compose.runtime.remember(game) { androidx.compose.runtime.mutableStateOf(false) }
        val unavailable =
            arc != null && arc.mode != ArcControlMode.STANDARD &&
                (arc.mode != ArcControlMode.STANDARD_GRIPARC || placement == null) && viewport != null
        if (unavailable && !fallbackShown) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { fallbackShown = true },
                title = { androidx.compose.material3.Text("Standard controls for this session") },
                text = {
                    androidx.compose.material3.Text(
                        "This native controller layout cannot fit the selected accessory mode " +
                            "with every input and 48dp targets. Saved cartridge settings are preserved.",
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        fallbackShown = true
                    }) { androidx.compose.material3.Text("Continue") }
                },
            )
        }
        PadKit(
            Modifier.fillMaxSize().onGloballyPositioned { root.value = it.boundsInRoot() },
            onInputEvents = onInput,
        ) {
            if (border != DisplaySettingsStore.Border.OFF) {
                CartdeckConsoleShell(
                    tokens = tokens,
                    videoBounds = video.value,
                    rootBounds = root.value,
                    isLandscape = landscape,
                    controlBounds = bounds.values.toList(),
                    patternId = resolved.patternId,
                    stampId = resolved.stampId,
                    decal = resolved.decal,
                    // Native hosts currently provide baseline lighting, not core-feedback events.
                    lighting = resolved.lighting,
                    highContrast = appearance.highContrast,
                    minimal = !hardwareBorder,
                )
            }
            if (hardwareBorder) {
                CartdeckHardwareSeats(
                    rootBounds = root.value,
                    videoBounds = video.value,
                    inputBounds = bounds,
                    tokens = tokens,
                    edition = edition,
                    isDualScreen = false,
                    customColours = resolved.customColours,
                    lighting = resolved.lighting,
                    highContrast = appearance.highContrast,
                )
            }
            CompositionLocalProvider(
                LocalLemuroidPadTheme provides theme,
                LocalCartdeckControlBounds provides observe,
            ) {
                val dedicatedHardwareLayout =
                    landscape && hardwareBorder && (arc == null || arc.mode == ArcControlMode.STANDARD)
                ConstraintLayout(
                    modifier = Modifier.fillMaxSize(),
                    constraintSet =
                        GameScreenLayout.buildConstraintSet(
                            landscape,
                            !dedicatedHardwareLayout,
                            CartdeckViewportPolicy.PortraitMode.STANDARD,
                            hardwareLandscape = dedicatedHardwareLayout,
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
                    if (placement == null && !physicalControllerConnected && !isTv) {
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
                if (placement != null && arc != null && !physicalControllerConnected && !isTv) {
                    GripArcCombinedControls(placement)
                    GripArcAccessory(
                        placement.dock,
                        arc,
                        tokens,
                        false,
                        false,
                        onGripPressed,
                        drawHousing = hardwareBorder,
                        lighting = resolved.lighting,
                        highContrast = appearance.highContrast,
                    )
                }
            }
        }
    }
}
