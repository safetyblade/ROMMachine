package com.swordfish.lemuroid.app.shared.startup

import android.content.Context
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import com.swordfish.lemuroid.R

/**
 * Approved Loading2 clip. Android owns decode and lifecycle; no per-frame Compose work.
 * Fills its viewport with centred aspect-preserving crop so the centre C remains visible.
 * The library and saves are untouched and continue initialising beneath the overlay.
 */
internal object CartdeckStartupVideo {
    const val MAX_DURATION_MS = 12_500L

    fun create(
        context: Context,
        onFinished: () -> Unit,
    ): VideoView =
        VideoView(context).apply {
            setBackgroundColor(Color.rgb(19, 19, 26))
            setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.cartdeck_signature_startup}"))
            setOnPreparedListener { player ->
                player.isLooping = false
                player.setVolume(0f, 0f)
                player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                start()
            }
            setOnCompletionListener { onFinished() }
            setOnErrorListener { _, _, _ ->
                onFinished()
                true
            }
        }
}
