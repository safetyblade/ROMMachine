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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360PipelineAB
import com.swordfish.lemuroid.app.shared.support.SupportActivity
import com.swordfish.lemuroid.app.shared.support.SupportReport
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
    console: String = "Xbox",
    storageHelp: String = "Xbox saves in-game to its virtual HDD. Emulator save states are not available.",
    pauseFeedback: String = "Menu open. This engine may continue running.",
    canResume: Boolean = true,
    game: com.swordfish.lemuroid.lib.library.db.entity.Game? = null,
    performanceCaptureEnabled: Boolean = false,
    onTogglePerformanceCapture: (() -> Unit)? = null,
    pipelineABActive: String? = null,
    pipelineABQueued: String = "Engine default",
    onQueuePipelineAB: ((Xbox360PipelineAB.Mode) -> Unit)? = null,
    recovery: @Composable () -> Unit = {},
    supportDiagnostic: (() -> String?)? = null,
    controls: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val safeResume = canResume && capabilities.resume
    GameMenuTheme(appearance) {
        val nav = rememberNavController()
        val route =
            nav.currentBackStackEntryAsState().value?.destination?.route
                ?.let { GameMenuRoute.findByRoute(it) } ?: GameMenuRoute.HOME
        val page =
            when (route) {
                GameMenuRoute.HOME -> "Game menu"
                GameMenuRoute.CONTROLS -> "Controls"
                GameMenuRoute.DISPLAY -> "Display"
                GameMenuRoute.STATES -> "Save & Load"
                GameMenuRoute.DIAGNOSTICS -> "Diagnostics"
                GameMenuRoute.TROUBLESHOOTING -> "Troubleshooting"
                else -> "Advanced"
            }
        GameMenuPanel(
            title,
            console,
            page,
            route.canGoBack(),
            onBack = {
                if (!nav.popBackStack()) {
                    if (safeResume) onResume() else onReturnToShelf()
                }
            },
            onResume = { if (safeResume) onResume() },
            statusMessage = null,
        ) {
            NavHost(nav, startDestination = GameMenuRoute.HOME.route, modifier = Modifier.fillMaxSize()) {
                composable(GameMenuRoute.HOME) {
                    SharedGameMenuHome(
                        capabilities = capabilities.home.copy(reporting = supportDiagnostic != null),
                        onAction = { action ->
                            when (action) {
                                GameMenuAction.RESUME -> if (safeResume) onResume()
                                GameMenuAction.CONTROLS -> nav.navigateToRoute(GameMenuRoute.CONTROLS)
                                GameMenuAction.DISPLAY -> nav.navigateToRoute(GameMenuRoute.DISPLAY)
                                GameMenuAction.STATES -> nav.navigateToRoute(GameMenuRoute.STATES)
                                GameMenuAction.REPORT_PROBLEM -> {
                                    val report =
                                        SupportReport.create(
                                            "gameplay",
                                            "Game problem",
                                            runCatching { supportDiagnostic?.invoke() }.getOrNull(),
                                            gameTitle = playerMenuTitle(title),
                                            system = console,
                                            engine = engineInfo,
                                        )
                                    context.startActivity(SupportActivity.intent(context, report))
                                }
                                GameMenuAction.ADVANCED -> nav.navigateToRoute(GameMenuRoute.ADVANCED)
                                GameMenuAction.RETURN_TO_SHELF -> onReturnToShelf()
                                else -> Unit // Not advertised by this backend.
                            }
                        },
                        canResume = safeResume,
                    )
                }
                composable(GameMenuRoute.CONTROLS) { controls() }
                composable(GameMenuRoute.DISPLAY) { DisplaySettingsScreen(game, libretroHost = false) }
                composable(GameMenuRoute.STATES) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(storageHelp, Modifier.padding(16.dp))
                        Text(
                            "Emulator save states are not available for this engine. Use the game's own save system.",
                            Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                composable(GameMenuRoute.ADVANCED) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Additional options",
                            Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!safeResume) recovery()
                        GameMenuLink("Diagnostics") { nav.navigateToRoute(GameMenuRoute.DIAGNOSTICS) }
                        if (capabilities.rollback) GameMenuLink("Previous Engine", onClick = onPreviousEngine)
                        GameMenuLink("Troubleshooting") { nav.navigateToRoute(GameMenuRoute.TROUBLESHOOTING) }
                    }
                }
                composable(GameMenuRoute.DIAGNOSTICS) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                        Text("Engine information", style = MaterialTheme.typography.titleMedium)
                        Text(engineInfo, style = MaterialTheme.typography.bodySmall)
                        Text(pauseFeedback, style = MaterialTheme.typography.bodySmall)
                        GameMenuLink("Copy engine/GPU report", onClick = onCopyReport)
                        if (pipelineABActive != null && onQueuePipelineAB != null) {
                            Text(
                                "Global Xbox 360 renderer/memory A/B (debug only). Active: $pipelineABActive. " +
                                    "Next launch: $pipelineABQueued. One launch only; settings automatically reset. " +
                                    "No game files, saves or shader caches are cleared.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            GameMenuLink("Next launch: test draw skipping OFF") {
                                onQueuePipelineAB(Xbox360PipelineAB.Mode.SKIP_OFF)
                            }
                            GameMenuLink("Next launch: test draw skipping ON") {
                                onQueuePipelineAB(Xbox360PipelineAB.Mode.SKIP_ON)
                            }
                            Text(
                                "XGFX-05 guest-memory ordering: disables early Vulkan upload hoisting " +
                                    "for one launch only. This may affect speed; " +
                                    "it does not stall pipeline compilation.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            GameMenuLink("Next launch: test ordered memory uploads") {
                                onQueuePipelineAB(Xbox360PipelineAB.Mode.ORDERED_UPLOADS)
                            }
                            GameMenuLink("Next launch: restore engine default") {
                                onQueuePipelineAB(Xbox360PipelineAB.Mode.NORMAL)
                            }
                        }
                        if (onTogglePerformanceCapture != null) {
                            Text(
                                "Frame diagnostics are off by default and never appear over gameplay. " +
                                    "Start capture here for a test, then return here to stop and copy the report.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            GameMenuLink(
                                if (performanceCaptureEnabled) {
                                    "Stop Xbox 360 frame capture"
                                } else {
                                    "Start Xbox 360 frame capture"
                                },
                                onClick = onTogglePerformanceCapture,
                            )
                        }
                    }
                }
                composable(GameMenuRoute.TROUBLESHOOTING) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Copy the engine/GPU report from Advanced when requesting help. " +
                                storageHelp,
                            Modifier.padding(16.dp),
                        )
                        if (safeResume) recovery()
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
    controllerId: TouchControllerID = TouchControllerID.XBOX,
    physicalMapping: @Composable () -> Unit = {},
    onSave: (TouchControllerSettingsManager.Orientation, TouchControllerSettingsManager.Settings) -> Unit,
) {
    var orientation by remember { mutableStateOf(TouchControllerSettingsManager.Orientation.PORTRAIT) }
    val density = LocalDensity.current
    val insets = WindowInsets.displayCutout
    val settings by remember(manager, orientation, density, insets) {
        manager.observeSettings(controllerId, orientation, density, insets)
    }.collectAsState(TouchControllerSettingsManager.Settings())
    var edited by remember(settings, orientation) { mutableStateOf(settings) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        physicalMapping()
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
