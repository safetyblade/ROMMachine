/* hakuX JNI ABI adapter for CARTDECK. GPL-3.0-or-later.
 * The class name is retained for the pinned upstream JNI exports; no hakuX frontend is imported. */
@file:Suppress("ktlint:standard:package-name") // Exact upstream JNI ABI namespace.

package com.rfandango.haku_x

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.swordfish.lemuroid.app.mobile.feature.game.XboxGameControls
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState
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
    private val poll =
        object : Runnable {
            override fun run() {
                if (!ready || leaving) return
                val fatal = nativeGetFatal()
                when {
                    fatal != null -> {
                        nativeSetPaused(true)
                        menu =
                            AlertDialog.Builder(this@NativeCoreActivity)
                                .setTitle("CARTDECK · Xbox could not continue")
                                .setMessage(fatal + "\nYour Xbox HDD and saves have been retained.")
                                .setCancelable(false)
                                .setPositiveButton("Return to shelf") { _, _ -> leave(fatal) }
                                .show()
                        return
                    }
                    nativeGetRelaunch() -> {
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
                    nativeGetQuit() -> {
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
        mLayout?.addView(
            controls,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        main.postDelayed(poll, 400)
    }

    override fun onResume() {
        super.onResume()
        paused = false
        if (ready) {
            nativeSetPaused(menu?.isShowing == true)
            main.removeCallbacks(poll)
            main.postDelayed(poll, 400)
        }
    }

    override fun onPause() {
        paused = true
        main.removeCallbacks(poll)
        releaseTouch()
        hardware.clear()
        if (ready) {
            sendHardware()
            nativeSetPaused(true)
        }
        super.onPause()
    }

    private fun showMenu() {
        if (!ready || leaving || menu?.isShowing == true) return
        releaseTouch()
        nativeSetPaused(true)
        menu =
            AlertDialog.Builder(this).setTitle("CARTDECK · Xbox")
                .setMessage(intent.getStringExtra("cartdeck.gameTitle"))
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
