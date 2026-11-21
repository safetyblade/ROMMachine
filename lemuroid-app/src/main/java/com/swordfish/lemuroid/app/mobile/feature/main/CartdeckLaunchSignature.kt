package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.swordfish.lemuroid.app.shared.startup.CartdeckIntroFraming
import com.swordfish.lemuroid.app.shared.startup.CartdeckStartupVideo

/** Preview, adjust and save framing without editing or transcoding the owner's intro clip. */
@Composable
internal fun CartdeckLaunchSignature(
    onFinished: () -> Unit,
    onAdjustingChanged: (Boolean) -> Unit,
) {
    val currentFinish = rememberUpdatedState(onFinished)
    val context = LocalContext.current
    val editing = remember { mutableStateOf(false) }
    val framing = remember { mutableStateOf(CartdeckIntroFraming.load(context)) }
    val player =
        remember(context) {
            CartdeckStartupVideo.create(context) { currentFinish.value() }
        }
    val update: (CartdeckIntroFraming) -> Unit = { next ->
        framing.value = next.constrained()
        player.previewFraming(framing.value)
    }
    val closeEditor = {
        player.persistFraming()
        editing.value = false
        player.resumeAfterFraming()
        onAdjustingChanged(false)
    }

    BackHandler {
        if (editing.value) {
            closeEditor()
        } else {
            currentFinish.value()
        }
    }
    DisposableEffect(player) {
        onDispose {
            onAdjustingChanged(false)
            player.stopPlayback()
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF13131A))) {
        AndroidView(factory = { player }, modifier = Modifier.fillMaxSize())
        if (editing.value) {
            Column(
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .background(Color(0xE813131A))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Startup video framing", color = Color.White)
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { update(framing.value.copy(fit = true)) }) {
                        Text(if (framing.value.fit) "✓ Fit whole video" else "Fit whole video")
                    }
                    TextButton(onClick = { update(framing.value.copy(fit = false)) }) {
                        Text(if (!framing.value.fit) "✓ Fill screen" else "Fill screen")
                    }
                }
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { update(framing.value.copy(zoom = framing.value.zoom - 0.1f)) }) {
                        Text("Zoom −")
                    }
                    Text("${(framing.value.zoom * 100).toInt()}%", color = Color.White)
                    TextButton(onClick = { update(framing.value.copy(zoom = framing.value.zoom + 0.1f)) }) {
                        Text("Zoom +")
                    }
                }
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { update(framing.value.copy(offsetY = framing.value.offsetY - 0.05f)) }) {
                        Text("↑")
                    }
                    TextButton(onClick = { update(framing.value.copy(offsetX = framing.value.offsetX - 0.05f)) }) {
                        Text("←")
                    }
                    TextButton(onClick = { update(framing.value.copy(offsetY = framing.value.offsetY + 0.05f)) }) {
                        Text("↓")
                    }
                    TextButton(onClick = { update(framing.value.copy(offsetX = framing.value.offsetX + 0.05f)) }) {
                        Text("→")
                    }
                }
                Row(horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = { update(CartdeckIntroFraming.defaultFor(context)) }) { Text("Reset") }
                    TextButton(onClick = closeEditor) { Text("Save & play") }
                    TextButton(onClick = { currentFinish.value() }) { Text("Skip intro") }
                }
            }
        } else {
            TextButton(
                onClick = {
                    player.pauseForFraming()
                    editing.value = true
                    onAdjustingChanged(true)
                },
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Text("Frame", color = Color(0xFFF8F1F7))
            }
            TextButton(
                onClick = { currentFinish.value() },
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Text("Skip intro", color = Color(0xFFF8F1F7))
            }
        }
    }
}
