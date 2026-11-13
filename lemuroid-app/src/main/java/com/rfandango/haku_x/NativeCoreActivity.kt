/* hakuX JNI ABI adapter for CARTDECK. GPL-3.0-or-later.
 * The class name is retained for the pinned upstream JNI exports; no hakuX frontend is imported. */
@file:Suppress("ktlint:standard:package-name") // Exact upstream JNI ABI namespace.

package com.rfandango.haku_x

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.mobile.feature.game.XboxGameControls
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.app.shared.game.xbox.FramePixelSummary
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.app.shared.game.xbox.XboxLaunchWatchdog
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPixelProbeCapture
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import gg.padkit.inputevents.InputEvent
import org.libsdl.app.SDLActivity
import java.io.File

/** SDL owns the Vulkan surface and audio. A dedicated same-package process contains native crashes. */
class NativeCoreActivity : SDLActivity() {
    private var disc = ""
    private lateinit var engine: XboxEngineStore.Selection
    private var startupFailure: Throwable? = null
    private var ready = false
    private var leaving = false
    private var paused = false
    private var menu: AlertDialog? = null
    private val main = Handler(Looper.getMainLooper())
    private val hardware = XboxPadState()
    private val touch = XboxPadState()
    private val watchdog = XboxLaunchWatchdog()
    private var lastPoll = 0L
    private var presentationRecorded = false
    private var pixelProbeRequested = false
    private var surfacePixelReport = "surface_pixel_probe=disabled"
    private var controlOverlay: ComposeView? = null
    private var pixelCapture: XboxPixelProbeCapture? = null
    private var pixelCaptureStarted = 0L
    private val finishPixelCapture =
        object : Runnable {
            override fun run() {
                val capture = pixelCapture ?: return
                if (leaving || paused || isFinishing) {
                    capture.cancel()
                    pixelCapture = null
                    surfacePixelReport += "\ncapture_status=CANCELED"
                    return
                }
                // This reads bounded CPU diagnostic text; it schedules no new GPU readbacks.
                val native = gpuReport()
                val elapsed = android.os.SystemClock.elapsedRealtime() - pixelCaptureStarted
                val result = capture.observe(elapsed, native)
                if (result == null) {
                    main.postDelayed(this, 250)
                } else {
                    surfacePixelReport += "\ncapture_status=${result.status}; native_pairs=${result.nativePairs}/3; " +
                        "samples do not establish gameplay"
                    pixelCapture = null
                    copyDiagnosticSnapshot(native)
                }
            }
        }
    private val poll =
        object : Runnable {
            override fun run() {
                if (!ready || leaving) return
                if (menu?.isShowing == true) {
                    lastPoll = 0L
                    main.postDelayed(this, 400)
                    return
                }
                val now = android.os.SystemClock.elapsedRealtime()
                val elapsed =
                    if (lastPoll == 0L || menu?.isShowing == true) {
                        0L
                    } else {
                        (now - lastPoll).coerceIn(
                            0L,
                            2000L,
                        )
                    }
                lastPoll = now
                val shown = nativeGetFrameShown()
                val bootState = runCatching { nativeGetBootState() }.getOrDefault(0)
                val relaunch = nativeGetRelaunch()
                val quit = nativeGetQuit()
                val verified = engine.properties.getProperty("cartdeck.presentation") == "vk-present-guest-v1"
                val timeout =
                    watchdog.poll(
                        elapsed,
                        if (relaunch || quit) 0 else bootState,
                        shown && verified,
                        shown && !verified,
                    )
                if (watchdog.presented && !presentationRecorded) {
                    registryGame()?.let { game ->
                        runCatching { EngineSelectionPreferences(applicationContext).recordPresentation(game) }
                    }
                    presentationRecorded = true
                }
                val fatal = nativeGetFatal() ?: timeout
                if (fatal != null) {
                    showFailure(fatal)
                    return
                }
                when {
                    relaunch -> {
                        val now = android.os.SystemClock.elapsedRealtime()
                        val window = intent.getLongExtra("cartdeck.relaunchWindow", now)
                        val count = if (now - window < 30000) intent.getIntExtra("cartdeck.relaunchCount", 0) else 0
                        if (count >= 5) {
                            nativeSetPaused(true)
                            menu =
                                AlertDialog.Builder(this@NativeCoreActivity)
                                    .setTitle("CARTDECK · Xbox")
                                    .setMessage(
                                        "This title is repeatedly restarting. " +
                                            "Return to the shelf before trying another engine.",
                                    )
                                    .setPositiveButton("Return to shelf") { _, _ ->
                                        leave("Repeated Xbox title restarts")
                                    }
                                    .setCancelable(false).show()
                            return
                        }
                        startActivity(
                            Intent(
                                this@NativeCoreActivity,
                                XboxRelaunchActivity::class.java,
                            ).putExtras(intent)
                                .putExtra("cartdeck.relaunchWindow", if (count == 0) now else window)
                                .putExtra("cartdeck.relaunchCount", count + 1)
                                .putExtra("cartdeck.relaunch", true)
                                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT),
                        )
                        leave(forwarded = true)
                        return
                    }
                    quit -> {
                        leave()
                        return
                    }
                }
                main.postDelayed(this, 400)
            }
        }

    override fun loadLibraries() {
        startupFailure?.let { throw IllegalStateException(it.message, it) }
        System.load(engine.sdl.absolutePath)
        System.load(engine.core.absolutePath)
    }

    override fun getMainSharedObject(): String = engine.core.absolutePath

    override fun getLibraries(): Array<String> = arrayOf("SDL3", "hakuxcore")

    override fun getArguments(): Array<String> =
        arrayOf(
            disc, File(filesDir, "native/xbox/hdd").absolutePath,
            "aspect=auto", "scale=1", "pads=1", "asyncpipe=1",
        ) +
            if (intent.getBooleanExtra("cartdeck.relaunch", false)) {
                arrayOf("relaunch")
            } else {
                emptyArray()
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        val preparation =
            runCatching {
                GameProcessLock.acquireExclusive(applicationContext)
                engine =
                    requireNotNull(XboxEngineStore(File(filesDir, "native-engines/xbox")).selected()) {
                        "No verified Xbox engine is installed"
                    }
                registryGame()?.let { game ->
                    runCatching {
                        val system = GameSystem.findById(game.systemId)
                        val descriptor = NativeEngineRegistry.engines(system).single()
                        EngineSelectionPreferences(applicationContext).beginLaunch(game, descriptor)
                    }
                }
                val uri = Uri.parse(requireNotNull(intent.getStringExtra("cartdeck.gameUri")))
                require(uri.scheme == "content" || uri.scheme == "file") { "Invalid Xbox disc location" }
                val descriptor =
                    requireNotNull(contentResolver.openFileDescriptor(uri, "r")) { "Xbox disc access was lost" }
                disc = "fd:${descriptor.detachFd()}"
                check(File(filesDir, "native/xbox/hdd").isDirectory) { "Xbox data storage is unavailable" }
            }
        startupFailure = preparation.exceptionOrNull()
        super.onCreate(savedInstanceState)
        if (SDLActivity.mBrokenLibraries) {
            if (startupFailure == null) {
                startupFailure = IllegalStateException("Xbox native runtime could not load on this device")
            }
            return
        }
        if (preparation.isFailure) {
            AlertDialog.Builder(this).setTitle("CARTDECK · Xbox")
                .setMessage(preparation.exceptionOrNull()?.message ?: "Cannot open game")
                .setPositiveButton("Return to shelf") { _, _ -> leave() }.setCancelable(false).show()
            return
        }
        mLayout?.setBackgroundColor(Color.BLACK)
        ready = true
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = showMenu()
            },
        )
        nativeSetPads(1)
        val controls =
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    XboxGameControls(::onTouchInput) { viewport ->
                        val current = mSurface?.layoutParams as? RelativeLayout.LayoutParams
                        if (
                            viewport.width() > 0 && viewport.height() > 0 &&
                            (
                                current == null || current.width != viewport.width() ||
                                    current.height != viewport.height() ||
                                    current.leftMargin != viewport.left || current.topMargin != viewport.top
                            )
                        ) {
                            mSurface?.layoutParams =
                                RelativeLayout.LayoutParams(viewport.width(), viewport.height()).apply {
                                    leftMargin = viewport.left
                                    topMargin = viewport.top
                                }
                        }
                    }
                }
            }
        controlOverlay = controls
        mLayout?.addView(
            controls,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        main.postDelayed(poll, 400)
    }

    override fun onResume() {
        super.onResume()
        paused = false
        lastPoll = 0L
        if (ready) {
            nativeSetPaused(menu?.isShowing == true)
            main.removeCallbacks(poll)
            main.postDelayed(poll, 400)
        }
    }

    override fun onPause() {
        cancelPixelCapture()
        paused = true
        lastPoll = 0L
        main.removeCallbacks(poll)
        releaseTouch()
        hardware.clear()
        if (ready) {
            sendHardware()
            nativeSetPaused(true)
        }
        super.onPause()
    }

    private fun registryGame(): Game? = intent.getSerializableExtra("cartdeck.game") as? Game

    private fun gpuReport(): String =
        runCatching { nativeGetGpuReport() }.getOrDefault("GPU report unavailable in previous engine")

    private fun presentationContract(): String =
        engine.properties.getProperty("cartdeck.presentation", "legacy acquired image")

    private fun diagnosticReport(
        reason: String,
        capturedGpuReport: String? = null,
    ): String {
        var safeReason = reason.take(2000).replace(filesDir.absolutePath, "[app storage]")
        listOf(disc, intent.getStringExtra("cartdeck.gameUri"), intent.getStringExtra("cartdeck.gameTitle"))
            .filterNotNull().filter { it.isNotBlank() }.forEach { safeReason = safeReason.replace(it, "[content]") }
        return "Xbox launch: $safeReason\n" +
            "Engine: ${engine.id}\n" +
            "Device: ${android.os.Build.MODEL}; Android ${android.os.Build.VERSION.SDK_INT}\n" +
            "Stage: ${watchdog.stage}; guest frame presented: ${watchdog.presented}\n" +
            "Native failure category: ${nativeGetFatalKind()}\n" +
            "${capturedGpuReport ?: gpuReport()}\n" +
            "Presentation probe: ${presentationContract()}\n" +
            "$surfacePixelReport\n" +
            "HDD and saves retained. No automatic relaunch was attempted."
    }

    private fun copyDiagnosticSnapshot(capturedGpuReport: String? = null) {
        val report = diagnosticReport("Manual diagnostic snapshot; not an engine failure", capturedGpuReport)
        runCatching { File(filesDir, "native/xbox/last-launch-report.txt").writeText(report) }
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("CARTDECK Xbox diagnostic", report))
        android.widget.Toast.makeText(this, "Engine/GPU report copied", android.widget.Toast.LENGTH_SHORT).show()
    }

    /** One explicit debug action; no continuous readback or image file export. */
    private fun requestPixelStageProbe() {
        if (!BuildConfig.DEBUG || pixelProbeRequested || leaving) return
        if (engine.properties.getProperty("cartdeck.pixelProbe") != "async-grid-v1") return
        pixelProbeRequested = true
        surfacePixelReport = "surface_pixel_probe=requested"
        if (runCatching { nativeRequestPixelProbe() }.isFailure) {
            surfacePixelReport = "surface_pixel_probe=native probe unavailable in this engine"
            copyDiagnosticSnapshot()
            return
        }
        pixelCapture = XboxPixelProbeCapture()
        pixelCaptureStarted = android.os.SystemClock.elapsedRealtime()
        menu?.dismiss() // Resume the existing guest; never relaunch or change its graphics settings.
        main.post(finishPixelCapture)
        main.postDelayed({ captureSurfacePixels() }, 1800)
    }

    private fun captureSurfacePixels() {
        if (leaving || isFinishing || paused || pixelCapture == null) return
        val surface = mSurface
        if (Build.VERSION.SDK_INT < 24 || surface == null || !surface.holder.surface.isValid) {
            surfacePixelReport = "surface_pixel_probe=surface unavailable"
            pixelCapture?.surfaceResult(false)
            return
        }
        val visible = android.graphics.Rect()
        val clipped = surface.getGlobalVisibleRect(visible)
        val overlay = controlOverlay
        val layers =
            "surface=${surface.width}x${surface.height} shown=${surface.isShown} " +
                "alpha=${surface.alpha} z=${surface.z} visible=$clipped:$visible; " +
                "controls=${overlay?.width}x${overlay?.height} alpha=${overlay?.alpha} opaque=${overlay?.isOpaque}"
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        var completed = false
        main.postDelayed({
            if (!completed && !leaving) {
                completed = true
                if (pixelCapture?.surfaceResult(false) == true) {
                    surfacePixelReport = "surface_pixel_probe=callback timeout; $layers"
                }
            }
        }, 3000)
        try {
            PixelCopy.request(surface, bitmap, { result ->
                try {
                    if (completed || pixelCapture?.surfaceResult(result == PixelCopy.SUCCESS) != true) return@request
                    completed = true
                    surfacePixelReport =
                        if (result == PixelCopy.SUCCESS) {
                            val pixels = IntArray(32 * 32)
                            bitmap.getPixels(pixels, 0, 32, 0, 0, 32, 32)
                            "surface_pixel_probe={${FramePixelSummary.argb(pixels)}}; $layers"
                        } else {
                            "surface_pixel_probe=PixelCopy result $result; $layers"
                        }
                } finally {
                    bitmap.recycle()
                }
            }, main)
        } catch (error: Exception) {
            completed = true
            bitmap.recycle()
            if (pixelCapture?.surfaceResult(false) == true) {
                surfacePixelReport = "surface_pixel_probe=request unavailable; $layers"
            }
        }
    }

    private fun cancelPixelCapture() {
        pixelCapture?.cancel()
        if (pixelCapture != null) surfacePixelReport += "\ncapture_status=CANCELED"
        pixelCapture = null
        main.removeCallbacks(finishPixelCapture)
    }

    private fun showFailure(reason: String) {
        cancelPixelCapture()
        registryGame()?.let { game ->
            runCatching { EngineSelectionPreferences(applicationContext).recordFailure(game, watchdog.stage) }
        }
        nativeSetPaused(true)
        val report = diagnosticReport(reason)
        runCatching { File(filesDir, "native/xbox/last-launch-report.txt").writeText(report) }
        android.util.Log.e("CARTDECK-Xbox", report)
        menu =
            AlertDialog.Builder(this)
                .setTitle(if (watchdog.presented) "CARTDECK · Xbox stopped" else "CARTDECK · Xbox not launched")
                .setMessage(report)
                .setCancelable(false)
                .setNeutralButton("Copy report") { _, _ ->
                    getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("CARTDECK Xbox launch", report))
                    showFailure(reason)
                }
                .setNegativeButton("Previous engine") { _, _ -> rollbackEngine() }
                .setPositiveButton("Return to shelf") { _, _ -> leave(report) }
                .show()
        menu?.getButton(AlertDialog.BUTTON_NEGATIVE)?.isEnabled =
            runCatching { XboxEngineStore(File(filesDir, "native-engines/xbox")).canRollback() }.getOrDefault(false)
    }

    private fun showMenu() {
        if (!ready || leaving || menu?.isShowing == true) return
        releaseTouch()
        nativeSetPaused(true)
        menu =
            AlertDialog.Builder(this).setTitle("CARTDECK · Xbox")
                .setMessage(intent.getStringExtra("cartdeck.gameTitle"))
                .setView(
                    android.widget.LinearLayout(this).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        addView(
                            android.widget.Button(this@NativeCoreActivity).apply {
                                text = "Copy engine/GPU report"
                                setOnClickListener { copyDiagnosticSnapshot() }
                            },
                        )
                        if (BuildConfig.DEBUG) {
                            addView(
                                android.widget.Button(this@NativeCoreActivity).apply {
                                    val supported =
                                        engine.properties.getProperty("cartdeck.pixelProbe") == "async-grid-v1"
                                    text =
                                        if (supported) {
                                            "Probe blank viewport (debug)"
                                        } else {
                                            "Probe unavailable in retained engine"
                                        }
                                    isEnabled = supported && !pixelProbeRequested
                                    setOnClickListener { requestPixelStageProbe() }
                                },
                            )
                        }
                    },
                )
                .setPositiveButton("Resume") { _, _ -> }
                .setNeutralButton("Previous engine") { _, _ -> rollbackEngine() }
                .setNegativeButton("Return to shelf") { _, _ -> leave() }
                .setOnDismissListener { if (!paused && !leaving) nativeSetPaused(false) }
                .show()
        menu?.getButton(AlertDialog.BUTTON_NEUTRAL)?.isEnabled =
            XboxEngineStore(File(filesDir, "native-engines/xbox")).canRollback()
    }

    private fun rollbackEngine() {
        val store = XboxEngineStore(File(filesDir, "native-engines/xbox"))
        if (!store.rollback()) return
        // Resume in a fresh process; do not load a second FEX runtime in this process.
        startActivity(
            Intent(this, XboxRelaunchActivity::class.java).putExtras(intent)
                .putExtra("cartdeck.relaunch", false)
                .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT),
        )
        leave(forwarded = true)
    }

    @Deprecated("Android back opens the CARTDECK pause menu")
    override fun onBackPressed() = showMenu()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!ready) return super.dispatchKeyEvent(event)
        val systemBack =
            event.keyCode == KeyEvent.KEYCODE_BACK &&
                !event.isFromSource(android.view.InputDevice.SOURCE_GAMEPAD)
        if (systemBack || event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.action == KeyEvent.ACTION_UP) showMenu()
            return true
        }
        if (hardware.key(event.keyCode, event.action == KeyEvent.ACTION_DOWN)) {
            sendHardware()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (ready && event.isFromSource(android.view.InputDevice.SOURCE_JOYSTICK)) {
            hardware.motion { axis -> event.getAxisValue(axis) }
            sendHardware()
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun onTouchInput(events: List<InputEvent>) {
        if (!ready || leaving) return
        events.forEach { event ->
            when (event) {
                is InputEvent.Button -> {
                    if (event.id == KeyEvent.KEYCODE_BUTTON_MODE) {
                        if (!event.pressed) showMenu()
                    } else {
                        touch.key(event.id, event.pressed)
                    }
                }
                is InputEvent.DiscreteDirection -> touch.direction(event.id, event.direction.x, event.direction.y)
                is InputEvent.ContinuousDirection -> touch.direction(event.id, event.direction.x, event.direction.y)
            }
        }
        sendTouch()
    }

    private fun releaseTouch() {
        touch.clear()
        if (ready) sendTouch()
    }

    private fun sendTouch() {
        var faceMask = 0
        for (index in 0..5) if (((touch.analog ushr (index * 8)) and 255) != 0L) faceMask = faceMask or (1 shl index)
        nativeSetOverlayPad(
            touch.buttons,
            faceMask,
            touch.lx / 32767f,
            -touch.ly / 32767f,
            touch.rx / 32767f,
            -touch.ry / 32767f,
            ((touch.analog ushr 48) and 255) / 255f,
            ((touch.analog ushr 56) and 255) / 255f,
        )
    }

    private fun sendHardware() =
        nativeSetPad(
            0,
            hardware.buttons,
            hardware.analog,
            hardware.lx,
            hardware.ly,
            hardware.rx,
            hardware.ry,
        )

    private fun leave(
        error: String? = null,
        forwarded: Boolean = false,
    ) {
        if (leaving) return
        leaving = true
        main.removeCallbacksAndMessages(null)
        if (!forwarded) {
            val result =
                Intent()
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME, intent.getSerializableExtra("cartdeck.game"))
                    .putExtra(
                        BaseGameActivity.PLAY_GAME_RESULT_LEANBACK,
                        intent.getBooleanExtra("cartdeck.leanback", false),
                    )
                    .putExtra(
                        BaseGameActivity.PLAY_GAME_RESULT_SESSION_DURATION,
                        android.os.SystemClock.elapsedRealtime() -
                            intent.getLongExtra(
                                "cartdeck.startTime", android.os.SystemClock.elapsedRealtime(),
                            ),
                    )
            if (error == null) {
                setResult(Activity.RESULT_OK, result)
            } else {
                setResult(
                    BaseGameActivity.RESULT_ERROR,
                    result.putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR, error),
                )
            }
        }
        finish()
    }

    override fun finish() {
        if (!leaving) {
            leave(startupFailure?.message ?: "The Xbox native runtime stopped unexpectedly")
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        releaseTouch()
        // SDL sends QUIT; upstream currently terminates its dedicated process rather than draining the core thread.
        super.onDestroy()
        // Upstream FEX/native state is single-run. Never kill CARTDECK's library process.
        Process.killProcess(Process.myPid())
    }

    companion object {
        @JvmStatic external fun nativeGetFatal(): String?

        @JvmStatic external fun nativeGetFatalKind(): Int

        @JvmStatic external fun nativeGetFrameShown(): Boolean

        @JvmStatic external fun nativeGetBootState(): Int

        @JvmStatic external fun nativeGetGpuReport(): String

        @JvmStatic external fun nativeRequestPixelProbe()

        @JvmStatic external fun nativeGetRelaunch(): Boolean

        @JvmStatic external fun nativeGetQuit(): Boolean

        @JvmStatic external fun nativeSetPads(mask: Int)

        @JvmStatic external fun nativeSetPaused(paused: Boolean)

        @JvmStatic external fun nativeSetOverlayPad(
            buttons: Int,
            analogMask: Int,
            lx: Float,
            ly: Float,
            rx: Float,
            ry: Float,
            lt: Float,
            rt: Float,
        )

        @JvmStatic external fun nativeSetPad(
            player: Int,
            buttons: Int,
            analog: Long,
            lx: Int,
            ly: Int,
            rx: Int,
            ry: Int,
        )
    }
}
