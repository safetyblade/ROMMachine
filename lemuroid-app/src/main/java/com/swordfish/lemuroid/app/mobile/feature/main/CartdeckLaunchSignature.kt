package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.swordfish.lemuroid.app.shared.startup.CartdeckStartupVideo

/** Plays Loading2 exactly once on a fresh entry, never on orientation recreation or returning from gameplay. */
@Composable
internal fun CartdeckLaunchSignature(onFinished: () -> Unit) {
    val currentFinish = rememberUpdatedState(onFinished)
    val context = LocalContext.current
    val player =
        remember(context) {
            CartdeckStartupVideo.create(context) { currentFinish.value() }
        }

    BackHandler { currentFinish.value() }
    DisposableEffect(player) {
        onDispose { player.stopPlayback() }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF13131A))) {
        AndroidView(factory = { player }, modifier = Modifier.fillMaxSize())
        TextButton(
            onClick = { currentFinish.value() },
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Text("Skip intro", color = Color(0xFFF8F1F7))
        }
    }
}
