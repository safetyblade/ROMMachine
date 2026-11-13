package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
                manager.observeSettings(TouchControllerID.XBOX, orientation, density, insets)
            }.collectAsState(TouchControllerSettingsManager.Settings()).value
        val root = remember { mutableStateOf<Rect?>(null) }
        val video = remember { mutableStateOf<Rect?>(null) }
        val bounds = remember(landscape) { mutableStateMapOf<String, Rect>() }
        val observe: (String, Rect) -> Unit =
            remember(bounds) {
                { name, rect -> if (bounds[name] != rect) bounds[name] = rect }
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
                    val config = TouchControllerID.getConfig(TouchControllerID.XBOX)
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
        }
    }
}
