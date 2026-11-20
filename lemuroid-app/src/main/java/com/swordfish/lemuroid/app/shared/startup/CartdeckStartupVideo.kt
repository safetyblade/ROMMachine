package com.swordfish.lemuroid.app.shared.startup

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.swordfish.lemuroid.R

/**
 * Plays the approved owner-supplied intro without drawing an opaque VideoView over its frames.
 * A centred SIGNATURE mark remains visible until Android confirms the first decoded frame.
 * If decoding cannot start, return to the shelf; never leave a black blocking overlay.
 */
internal object CartdeckStartupVideo {
    const val MAX_DURATION_MS = 12_500L
    private const val FIRST_FRAME_TIMEOUT_MS = 6_000L

    fun create(
        context: Context,
        onFinished: () -> Unit,
    ): CartdeckStartupPlayer = CartdeckStartupPlayer(context, onFinished, FIRST_FRAME_TIMEOUT_MS)
}

internal class CartdeckStartupPlayer(
    context: Context,
    private val onFinished: () -> Unit,
    firstFrameTimeoutMs: Long,
) : FrameLayout(context), TextureView.SurfaceTextureListener {
    private val texture = TextureView(context)
    private val placeholder =
        ImageView(context).apply {
            setImageResource(R.drawable.cartdeck_mark_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
    private var mediaPlayer: MediaPlayer? = null
    private var videoSurface: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var firstFrameRendered = false
    private var stopped = false
    private var finished = false

    init {
        setBackgroundColor(Color.rgb(19, 19, 26))
        addView(
            texture,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        val markSize = (148f * resources.displayMetrics.density).toInt()
        addView(placeholder, LayoutParams(markSize, markSize, Gravity.CENTER))
        texture.surfaceTextureListener = this
        texture.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateCentreCrop() }
        postDelayed(
            {
                if (!firstFrameRendered && !stopped) {
                    Log.w("CartdeckStartupVideo", "First video frame timed out; opening shelf")
                    finishOnce()
                }
            },
            firstFrameTimeoutMs,
        )
    }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        if (stopped || mediaPlayer != null) return
        val surface = Surface(surfaceTexture)
        videoSurface = surface
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setDataSource(
                context,
                Uri.parse("android.resource://${context.packageName}/${R.raw.cartdeck_signature_startup}"),
            )
            player.setSurface(surface)
            player.isLooping = false
            player.setVolume(0f, 0f)
            player.setOnVideoSizeChangedListener { _, newWidth, newHeight ->
                videoWidth = newWidth
                videoHeight = newHeight
                updateCentreCrop()
            }
            player.setOnPreparedListener { prepared ->
                if (!stopped) {
                    videoWidth = prepared.videoWidth
                    videoHeight = prepared.videoHeight
                    updateCentreCrop()
                    prepared.start()
                }
            }
            player.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && !stopped) {
                    firstFrameRendered = true
                    placeholder.visibility = View.GONE
                }
                false
            }
            player.setOnCompletionListener { finishOnce() }
            player.setOnErrorListener { _, what, extra ->
                Log.w("CartdeckStartupVideo", "Decoder error what=$what extra=$extra; opening shelf")
                finishOnce()
                true
            }
            player.prepareAsync()
        } catch (error: Exception) {
            Log.w("CartdeckStartupVideo", "Unable to prepare startup video; opening shelf", error)
            finishOnce()
        }
    }

    /** Preserve clip aspect, centre its C, crop only outside the available display. */
    private fun updateCentreCrop() {
        val viewWidth = texture.width.toFloat()
        val viewHeight = texture.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return
        val videoAspect = videoWidth.toFloat() / videoHeight
        val viewAspect = viewWidth / viewHeight
        val horizontalScale = if (videoAspect > viewAspect) videoAspect / viewAspect else 1f
        val verticalScale = if (videoAspect < viewAspect) viewAspect / videoAspect else 1f
        texture.setTransform(
            Matrix().apply {
                setScale(horizontalScale, verticalScale, viewWidth / 2f, viewHeight / 2f)
            },
        )
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        updateCentreCrop()
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        stopPlayback()
        return true
    }

    private fun finishOnce() {
        if (finished) return
        finished = true
        stopPlayback()
        post { onFinished() }
    }

    fun stopPlayback() {
        if (stopped) return
        stopped = true
        mediaPlayer?.release()
        mediaPlayer = null
        videoSurface?.release()
        videoSurface = null
    }

    override fun onDetachedFromWindow() {
        stopPlayback()
        super.onDetachedFromWindow()
    }
}
