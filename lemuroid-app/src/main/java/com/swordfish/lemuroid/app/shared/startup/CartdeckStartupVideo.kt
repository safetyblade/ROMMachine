package com.swordfish.lemuroid.app.shared.startup

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
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

    private var shownInProcess = false

    fun claimColdStart(): Boolean {
        if (shownInProcess) return false
        shownInProcess = true
        return true
    }

    fun isSkipKey(keyCode: Int): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            -> true
            else -> false
        }

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
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val audioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()
    private val focusListener =
        AudioManager.OnAudioFocusChangeListener { change ->
            if (change != AudioManager.AUDIOFOCUS_GAIN && hasAudioFocus && !stopped) finishOnce()
        }
    private var focusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private val soundPreferences = CartdeckStartupSound.preferences(context)
    private val soundListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> updateVolume() }
    private var prepared = false
    private var endFramingProgress = 0f
    private var mediaPlayer: MediaPlayer? = null
    private var videoSurface: Surface? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var firstFrameRendered = false
    private var adjustingFraming = false
    private var framing = CartdeckIntroFraming.load(context)
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
        soundPreferences.registerOnSharedPreferenceChangeListener(soundListener)
        texture.surfaceTextureListener = this
        texture.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateFraming() }
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
        if (Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f ||
            (Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled())
        ) {
            postDelayed({ finishOnce() }, 500L)
            return
        }
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
            player.setAudioAttributes(audioAttributes)
            player.setOnVideoSizeChangedListener { _, newWidth, newHeight ->
                videoWidth = newWidth
                videoHeight = newHeight
                updateFraming()
            }
            player.setOnPreparedListener { prepared ->
                if (!stopped) {
                    videoWidth = prepared.videoWidth
                    videoHeight = prepared.videoHeight
                    updateFraming()
                    this.prepared = true
                    updateVolume()
                    if (!adjustingFraming) prepared.start()
                }
            }
            player.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && !stopped) {
                    firstFrameRendered = true
                    placeholder.visibility = View.GONE
                }
                false
            }
            player.setOnCompletionListener { if (!adjustingFraming) finishOnce() }
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

    /** Fit reveals all of the clip; Fill crops. Offset and zoom remain in safe bounds. */
    private fun updateFraming() {
        val viewWidth = texture.width.toFloat()
        val viewHeight = texture.height.toFloat()
        if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return
        val videoAspect = videoWidth.toFloat() / videoHeight
        val viewAspect = viewWidth / viewHeight
        val safe = framing.constrained()
        val horizontalScale =
            if (safe.fit) {
                minOf(1f, videoAspect / viewAspect)
            } else {
                maxOf(1f, videoAspect / viewAspect)
            }
        val verticalScale =
            if (safe.fit) {
                minOf(1f, viewAspect / videoAspect)
            } else {
                maxOf(1f, viewAspect / videoAspect)
            }
        val progress = if (adjustingFraming) 0f else endFramingProgress
        val (endScaleX, endScaleY) = cartdeckIntroEndScale(videoAspect, viewAspect)
        texture.setTransform(
            Matrix().apply {
                setScale(
                    horizontalScale * safe.zoom * (1f - progress) + endScaleX * progress,
                    verticalScale * safe.zoom * (1f - progress) + endScaleY * progress,
                    viewWidth / 2f,
                    viewHeight / 2f,
                )
                if (progress == 0f) {
                    postTranslate(safe.offsetX * viewWidth, safe.offsetY * viewHeight)
                } else {
                    postTranslate(
                        safe.offsetX * viewWidth * (1f - progress),
                        safe.offsetY * viewHeight * (1f - progress),
                    )
                }
            },
        )
    }

    fun previewFraming(next: CartdeckIntroFraming) {
        framing = next.constrained()
        updateFraming()
    }

    fun persistFraming() {
        CartdeckIntroFraming.save(context, framing)
    }

    fun pauseForFraming() {
        adjustingFraming = true
        if (prepared) mediaPlayer?.pause()
        updateFraming()
        releaseAudioFocus()
    }

    fun resumeAfterFraming() {
        adjustingFraming = false
        mediaPlayer?.let { player ->
            if (prepared && !stopped) {
                updateFraming()
                updateVolume()
                player.start()
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int,
    ) {
        updateFraming()
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        val player = mediaPlayer ?: return
        if (!prepared || adjustingFraming || stopped) return
        val progress = ((player.currentPosition - 7_900) / 900f).coerceIn(0f, 1f)
        if (progress != endFramingProgress) {
            endFramingProgress = progress
            updateFraming()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility != View.VISIBLE && mediaPlayer != null) finishOnce()
    }

    @Suppress("DEPRECATION")
    private fun updateVolume() {
        val player = mediaPlayer ?: return
        if (!prepared || adjustingFraming || stopped) return
        val gain = CartdeckStartupSound.gain(soundPreferences)
        if (gain == 0f) {
            player.setVolume(0f, 0f)
            releaseAudioFocus()
            return
        }
        if (!hasAudioFocus) {
            val result =
                if (Build.VERSION.SDK_INT >= 26) {
                    val request =
                        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes(audioAttributes)
                            .setOnAudioFocusChangeListener(focusListener)
                            .setWillPauseWhenDucked(true)
                            .build()
                    focusRequest = request
                    audioManager.requestAudioFocus(request)
                } else {
                    audioManager.requestAudioFocus(
                        focusListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
                    )
                }
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        val effectiveGain = if (hasAudioFocus) gain else 0f
        player.setVolume(effectiveGain, effectiveGain)
    }

    @Suppress("DEPRECATION")
    private fun releaseAudioFocus() {
        if (!hasAudioFocus) return
        hasAudioFocus = false
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest?.let(audioManager::abandonAudioFocusRequest)
            focusRequest = null
        } else {
            audioManager.abandonAudioFocus(focusListener)
        }
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        if (!stopped) finishOnce()
        return true
    }

    private fun finishOnce() {
        if (finished) return
        finished = true
        stopPlayback()
        android.os.Handler(android.os.Looper.getMainLooper()).post { onFinished() }
    }

    fun stopPlayback() {
        if (stopped) return
        stopped = true
        soundPreferences.unregisterOnSharedPreferenceChangeListener(soundListener)
        mediaPlayer?.release()
        releaseAudioFocus()
        mediaPlayer = null
        videoSurface?.release()
        videoSurface = null
    }

    override fun onDetachedFromWindow() {
        stopPlayback()
        super.onDetachedFromWindow()
    }
}

// The decoded end card occupies the central 50% of the source width and 66% of its height.
internal fun cartdeckIntroEndScale(
    videoAspect: Float,
    viewAspect: Float,
): Pair<Float, Float> {
    val fitX = minOf(1f, videoAspect / viewAspect)
    val fitY = minOf(1f, viewAspect / videoAspect)
    val zoomToFill = 1f / minOf(fitX, fitY)
    val targetZoom = if (viewAspect < 1f) 1.85f else zoomToFill
    val safeZoom = minOf(targetZoom, 0.90f / (fitX * 0.50f), 0.90f / (fitY * 0.66f))
    return fitX * safeZoom to fitY * safeZoom
}
