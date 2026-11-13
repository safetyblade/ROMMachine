package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager

/** Hosted in the existing SDL Activity's ComposeView; no engine or Activity launch. */
@Composable
internal fun XboxGameMenu(
    title: String,
    engineInfo: String,
    capabilities: XboxMenuCapabilities,
    onResume: () -> Unit,
    onReturnToShelf: () -> Unit,
    onCopyReport: () -> Unit,
    onProbe: () -> Unit,
    onPreviousEngine: () -> Unit,
    appearance: ShelfAppearance? = null,
    controls: @Composable () -> Unit,
) {
    GameMenuTheme(appearance) {
        val nav = rememberNavController()
        val route =
            nav.currentBackStackEntryAsState().value?.destination?.route
                ?.let { GameMenuRoute.findByRoute(it) } ?: GameMenuRoute.HOME
        val page =
            when (route) {
                GameMenuRoute.HOME -> "Paused"
                GameMenuRoute.CONTROLS -> "Controls"
                GameMenuRoute.TROUBLESHOOTING -> "Troubleshooting"
                else -> "Advanced"
            }
        GameMenuPanel(
            title,
            "Xbox",
            page,
            route.canGoBack(),
            onBack = { if (!nav.popBackStack()) onResume() },
            onResume = onResume,
        ) {
            NavHost(nav, startDestination = GameMenuRoute.HOME.route, modifier = Modifier.fillMaxSize()) {
                composable(GameMenuRoute.HOME) {
                    SharedGameMenuHome(capabilities.home) { action ->
                        when (action) {
                            GameMenuAction.RESUME -> onResume()
                            GameMenuAction.CONTROLS -> nav.navigateToRoute(GameMenuRoute.CONTROLS)
                            GameMenuAction.ADVANCED -> nav.navigateToRoute(GameMenuRoute.ADVANCED)
                            GameMenuAction.RETURN_TO_SHELF -> onReturnToShelf()
                            else -> Unit // Not advertised by this backend.
                        }
                    }
                }
                composable(GameMenuRoute.CONTROLS) { controls() }
                composable(GameMenuRoute.ADVANCED) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text("Current engine", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                        Text(engineInfo, Modifier.padding(horizontal = 16.dp))
                        GameMenuLink("Copy engine/GPU report", onClick = onCopyReport)
                        if (capabilities.rollback) GameMenuLink("Previous Engine", onClick = onPreviousEngine)
                        GameMenuLink("Troubleshooting") { nav.navigateToRoute(GameMenuRoute.TROUBLESHOOTING) }
                    }
                }
                composable(GameMenuRoute.TROUBLESHOOTING) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Copy the engine/GPU report from Advanced when requesting help. " +
                                "Xbox saves in-game to its virtual HDD. Emulator save states are not available.",
                            Modifier.padding(16.dp),
                        )
                        if (capabilities.showProbe) GameMenuLink("Probe blank viewport (debug)", onClick = onProbe)
                    }
                }
            }
        }
    }
}

/** Edits the same settings manager observed by the live PadKit overlay. */
@Composable
internal fun XboxMenuControls(
    manager: TouchControllerSettingsManager,
    onSave: (TouchControllerSettingsManager.Orientation, TouchControllerSettingsManager.Settings) -> Unit,
) {
    var orientation by remember { mutableStateOf(TouchControllerSettingsManager.Orientation.PORTRAIT) }
    val density = LocalDensity.current
    val insets = WindowInsets.displayCutout
    val settings by remember(manager, orientation, density, insets) {
        manager.observeSettings(TouchControllerID.XBOX, orientation, density, insets)
    }.collectAsState(TouchControllerSettingsManager.Settings())
    var edited by remember(settings, orientation) { mutableStateOf(settings) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Touch controls", style = MaterialTheme.typography.titleMedium)
        GameMenuLink(
            if (orientation == TouchControllerSettingsManager.Orientation.PORTRAIT) "Portrait" else "Landscape",
        ) {
            orientation =
                if (orientation == TouchControllerSettingsManager.Orientation.PORTRAIT) {
                    TouchControllerSettingsManager.Orientation.LANDSCAPE
                } else {
                    TouchControllerSettingsManager.Orientation.PORTRAIT
                }
        }
        Text("Size")
        Slider(
            edited.scale.coerceIn(0f, 1f),
            { edited = edited.copy(scale = it) },
            modifier = Modifier.semantics { contentDescription = "Control size" },
            onValueChangeFinished = { onSave(orientation, edited) },
        )
        Text("Horizontal spacing")
        Slider(
            edited.marginX.coerceIn(0f, 1f),
            { edited = edited.copy(marginX = it) },
            modifier = Modifier.semantics { contentDescription = "Horizontal spacing" },
            onValueChangeFinished = { onSave(orientation, edited) },
        )
        Text("Vertical spacing")
        Slider(
            edited.marginY.coerceIn(0f, 1f),
            { edited = edited.copy(marginY = it) },
            modifier = Modifier.semantics { contentDescription = "Vertical spacing" },
            onValueChangeFinished = { onSave(orientation, edited) },
        )
    }
}
