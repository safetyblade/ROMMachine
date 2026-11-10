package com.swordfish.lemuroid.app.mobile.feature.settings.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.swordfish.lemuroid.app.mobile.feature.game.CartdeckLayoutMode
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcBounds
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcHand
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcLayoutProfile
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcPositionSolver
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcResolution
import com.swordfish.lemuroid.app.mobile.feature.game.PivotArcSettingsStore
import com.swordfish.lemuroid.app.mobile.feature.game.rememberPivotArcSettings
import com.swordfish.lemuroid.lib.library.SystemID

/** Settings → Layout. Intentional global choice, with no per-game mutations. */
@Composable
fun PivotArcSettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember(context) { PivotArcSettingsStore(context) }
    val current = rememberPivotArcSettings().value
    var adjusting by remember { mutableStateOf(false) }
    val baseline = remember(context) { runCatching { PivotArcLayoutProfile.load(context) }.getOrNull() }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Layout", style = MaterialTheme.typography.headlineSmall)
        Text("One global control layout across cartridges.", style = MaterialTheme.typography.bodyMedium)
        LayoutChoice(
            "Standard",
            "Original controls, positions and button mappings",
            current.mode == CartdeckLayoutMode.STANDARD,
        ) { store.save(current.copy(mode = CartdeckLayoutMode.STANDARD)) }
        LayoutChoice(
            "One Hand Mode",
            "PivotArc · Candidate C",
            current.mode == CartdeckLayoutMode.ONE_HAND,
        ) { store.save(current.copy(mode = CartdeckLayoutMode.ONE_HAND)) }
        if (current.mode == CartdeckLayoutMode.ONE_HAND) {
            HorizontalDivider()
            Text("PivotArc", style = MaterialTheme.typography.titleMedium)
            LayoutChoice("Right Hand", "Owner-approved Candidate C arrangement", current.hand == PivotArcHand.RIGHT) {
                store.save(current.copy(hand = PivotArcHand.RIGHT))
            }
            LayoutChoice("Left Hand", "Mirror positions; keep game and input identities unchanged", current.hand == PivotArcHand.LEFT) {
                store.save(current.copy(hand = PivotArcHand.LEFT))
            }
            Button(onClick = { adjusting = true }, enabled = baseline != null) { Text("Adjust Position") }
            TextButton(onClick = { store.resetPosition() }) { Text("Reset Position") }
            Text(
                "GB, GBC, GBA and SNES are the initial controller profiles. Other systems use their existing Standard controls with an explanation. Saved PivotArc preference is retained.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (adjusting && baseline != null) {
        var x by remember(adjusting) { mutableFloatStateOf(current.offsetXdp) }
        var y by remember(adjusting) { mutableFloatStateOf(current.offsetYdp) }
        val density = LocalDensity.current
        val preview =
            PivotArcPositionSolver.resolve(
                baseline = baseline,
                active = PivotArcLayoutProfile.activeFor(SystemID.GBA).orEmpty(),
                safe = PivotArcBounds(8f, 8f, 385f, 844f),
                viewport = PivotArcBounds(22f, 84f, 371f, 344f),
                leftHanded = current.hand == PivotArcHand.LEFT,
                requestedDx = x,
                requestedDy = y,
                isLandscape = false,
            )
        Dialog(onDismissRequest = { adjusting = false }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Adjust Position", style = MaterialTheme.typography.titleLarge)
                    Text("Drag the group, or use the sliders. This reference preview is clamped again to the real gameplay viewport on launch.", style = MaterialTheme.typography.bodySmall)
                    val primary = MaterialTheme.colorScheme.primary
                    val surface = MaterialTheme.colorScheme.surfaceVariant
                    val outline = MaterialTheme.colorScheme.outline
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                            .pointerInput(density) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    x = (x + drag.x / density.density).coerceIn(-200f, 200f)
                                    y = (y + drag.y / density.density).coerceIn(-200f, 200f)
                                }
                            },
                    ) {
                        val sx = size.width / 393f
                        val sy = size.height / 852f
                        drawRoundRect(surface, topLeft = Offset(8 * sx, 8 * sy), size = Size(377 * sx, 836 * sy))
                        drawRect(outline.copy(alpha = 0.35f), topLeft = Offset(22 * sx, 84 * sy), size = Size(349 * sx, 260 * sy))
                        if (preview is PivotArcResolution.Fits) {
                            preview.placement.controls.forEach { (key, r) ->
                                val c = if (key == "D") primary else primary.copy(alpha = 0.75f)
                                drawRoundRect(c, topLeft = Offset(r.left * sx, r.top * sy), size = Size(r.width * sx, r.height * sy))
                            }
                        }
                    }
                    if (preview is PivotArcResolution.Unavailable) {
                        Text(preview.reason, color = MaterialTheme.colorScheme.error)
                    }
                    Text("Horizontal: ${x.toInt()}dp")
                    Slider(value = x.coerceIn(-200f, 200f), onValueChange = { x = it }, valueRange = -200f..200f)
                    Text("Vertical: ${y.toInt()}dp")
                    Slider(value = y.coerceIn(-200f, 200f), onValueChange = { y = it }, valueRange = -200f..200f)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { x = 0f; y = 0f }) { Text("Reset") }
                        TextButton(onClick = { adjusting = false }) { Text("Cancel") }
                        Button(
                            onClick = {
                                if (preview is PivotArcResolution.Fits) {
                                    store.save(current.copy(offsetXdp = preview.placement.appliedDx, offsetYdp = preview.placement.appliedDy))
                                    adjusting = false
                                }
                            },
                            enabled = preview is PivotArcResolution.Fits,
                        ) { Text("Apply") }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutChoice(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
