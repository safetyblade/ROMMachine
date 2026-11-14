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
import androidx.compose.runtime.produceState
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
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfStore
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartdeckEditionPreset
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.resolveCartdeckEdition
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.touchinput.radial.CartdeckPadPalette
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
    onViewport: (AndroidRect) -> Unit,
) {
    val context = LocalContext.current
    val appearance =
        produceState(ShelfAppearance(), context) {
            value = ShelfStore(context).read().appearance
        }.value
    val edition =
        when (appearance.edition.lowercase()) {
            "pink" -> CartdeckEditionPreset.PINK
            "green" -> CartdeckEditionPreset.GREEN
            else -> CartdeckEditionPreset.ORIGINAL
        }
    val preset = remember(edition) { context.resolveCartdeckEdition(edition) }
    val tokens =
        remember(preset, appearance) {
            fun role(
                name: String,
                fallback: androidx.compose.ui.graphics.Color,
            ) = appearance.custom[name]?.let { androidx.compose.ui.graphics.Color(it.toInt()) } ?: fallback
            preset.copy(
                chassis = role("chassis", preset.chassis), secondary = role("secondary", preset.secondary),
                bezel = role("bezel", preset.bezel), glass = role("glass", preset.glass),
                button = role("button", preset.button), accent = role("accent", preset.accent),
                onButton = role("onButton", preset.onButton), onChassis = role("onChassis", preset.onChassis),
                focus = role("focus", preset.focus),
            )
        }
    val theme =
        remember(tokens) {
            LemuroidPadTheme(
                CartdeckPadPalette(
                    chassis = tokens.chassis,
                    trim = tokens.secondary,
                    bezel = tokens.bezel,
                    button = tokens.button,
                    accent = tokens.accent,
                    onButton = tokens.onButton,
                    focus = tokens.focus,
                    highContrast = appearance.highContrast,
                ),
            )
        }
    val manager =
        settingsManager ?: remember {
            TouchControllerSettingsManager(SharedPreferencesHelper.getSharedPreferences(context))
        }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = constraints.maxWidth > constraints.maxHeight
        // SDL owns the guest surface beneath this transparent Compose overlay.
        // The maximised path must not instantiate PadKit's virtual controller.
        if (landscape && physicalControllerConnected) {
            CartdeckMaximisedFrame(tokens, onViewport)
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
            CartdeckConsoleShell(tokens, video.value, root.value, landscape, bounds.values.toList())
            CartdeckHardwareSeats(
                rootBounds = root.value,
                videoBounds = video.value,
                inputBounds = bounds,
                tokens = tokens,
                edition = edition,
                isDualScreen = false,
                customColours = appearance.custom.isNotEmpty(),
            )
            CompositionLocalProvider(
                LocalLemuroidPadTheme provides theme,
                LocalCartdeckControlBounds provides observe,
            ) {
                ConstraintLayout(
                    modifier = Modifier.fillMaxSize(),
                    constraintSet =
                        GameScreenLayout.buildConstraintSet(
                            landscape,
                            true,
                            CartdeckViewportPolicy.PortraitMode.STANDARD,
                        ),
                ) {
                    Box(
                        Modifier.layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW).onGloballyPositioned {
                            val rect = it.boundsInRoot()
                            video.value = rect
                            onViewport(
                                AndroidRect(
                                    rect.left.toInt(),
                                    rect.top.toInt(),
                                    rect.right.toInt(),
                                    rect.bottom.toInt(),
                                ),
                            )
                        },
                    )
                    if (placement == null) {
                        val config = TouchControllerID.getConfig(controllerId)
                        config.leftComposable(
                            this@PadKit,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_LEFT_PAD),
                            settings,
                        )
                        config.rightComposable(
                            this@PadKit,
                            Modifier.layoutId(GameScreenLayout.CONSTRAINTS_RIGHT_PAD),
                            settings,
                        )
                    }
                }
                if (placement != null && arc != null) {
                    GripArcCombinedControls(placement)
                    GripArcAccessory(placement.dock, arc, tokens, false, false, onGripPressed)
                }
            }
        }
    }
}
