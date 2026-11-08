package com.swordfish.lemuroid.app.mobile.feature.main

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.swordfish.lemuroid.app.mobile.feature.shelf.LocalShelfAppearance
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeFront
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeSpine
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalCartridgeGeometry
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeFlight
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Same root as the source cell: no unrelated dialog-window coordinate origin. */
@Composable
fun CartridgePickup(
    gameId: Int,
    game: Game?,
    recordReady: Boolean,
    sourceBounds: Rect?,
    onDismiss: () -> Unit,
    artworkRevision: Int = 0,
    content: @Composable (frontModifier: Modifier, close: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val appearance = LocalShelfAppearance.current
    val configuration = LocalConfiguration.current
    val dimensions =
        listOf(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.fontScale,
            configuration.densityDpi,
        )
    val openedDimensions = remember(gameId) { dimensions }
    val systemReduced =
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
        ) == 0f
    val lowPower = (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
    val lowRam = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
    val immediate =
        appearance.reducedMotion || systemReduced || lowPower || lowRam ||
            sourceBounds == null || dimensions != openedDimensions
    val progress = remember(gameId) { Animatable(if (immediate) 1f else 0f) }
    var target by remember(gameId) { mutableStateOf<Rect?>(null) }
    var root by remember { mutableStateOf(Rect.Zero) }
    var closing by remember(gameId) { mutableStateOf(false) }
    val geometry = LocalCartridgeGeometry.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val focus = remember { FocusRequester() }
    var cardAttached by remember { mutableStateOf(false) }
    val settled = progress.value >= 0.999f && !closing
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                val stableSource = sourceBounds != null && geometry.cells[gameId] == sourceBounds
                if (!immediate && stableSource) progress.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
                onDismiss()
            }
        }
    }
    BackHandler(onBack = close)
    LaunchedEffect(gameId, target, game != null, recordReady && game == null, immediate) {
        if (immediate || (recordReady && game == null)) {
            progress.snapTo(1f)
        } else if (game != null && target != null && !closing) {
            if (progress.value == 0f) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            geometry.selectedId = gameId
            progress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(settled, cardAttached) { if (settled && cardAttached) focus.requestFocus() }
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize().onGloballyPositioned { root = it.boundsInRoot() }
            .background(Color.Black.copy(alpha = 0.65f * progress.value))
            .clickable(onClick = close).semantics { paneTitle = "Cartridge details" },
    ) {
        Surface(
            Modifier.align(Alignment.Center).widthIn(max = 560.dp).fillMaxWidth()
                .heightIn(max = maxHeight - 32.dp).padding(16.dp)
                .graphicsLayer { alpha = if (settled) 1f else 0f }
                .then(if (!settled) Modifier.clearAndSetSemantics {} else Modifier)
                .focusRequester(focus).focusable().clickable {}
                .onGloballyPositioned { cardAttached = true },
            shape = MaterialTheme.shapes.large,
        ) {
            content(Modifier.onGloballyPositioned { target = Rect(it.positionInRoot(), it.size.toSize()) }, close)
        }
        if (!settled) Box(Modifier.fillMaxSize().clickable {}.clearAndSetSemantics {})
        if (!settled && sourceBounds != null && target != null && game != null) {
            val destination = target!!
            val flight = cartridgeFlight(sourceBounds, destination, progress.value)
            val bounds = flight.bounds
            Box(
                Modifier.offset {
                    IntOffset(
                        (bounds.left - root.left).roundToInt(),
                        (bounds.top - root.top).roundToInt(),
                    )
                }
                    .requiredSize(with(density) { bounds.width.toDp() }, with(density) { bounds.height.toDp() })
                    .graphicsLayer {
                        rotationY = flight.rotationY
                        cameraDistance = 16f * density.density
                        shadowElevation = 20f
                    }
                    .clearAndSetSemantics {}.clickable {},
            ) {
                if (flight.showFront) {
                    CartridgeFront(
                        game,
                        artworkRevision = artworkRevision,
                        modifier =
                            Modifier.requiredSize(
                                with(density) { destination.width.toDp() },
                                with(density) { destination.height.toDp() },
                            )
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin(0f, 0f)
                                    scaleX = bounds.width / destination.width
                                    scaleY = bounds.height / destination.height
                                },
                    )
                } else {
                    CartridgeSpine(
                        game,
                        {},
                        Modifier.graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = bounds.width / sourceBounds.width
                            scaleY = bounds.height / sourceBounds.height
                        },
                        width = with(density) { sourceBounds.width.toDp() },
                        registerGeometry = false,
                    )
                }
            }
        }
    }
}
