package com.swordfish.lemuroid.app.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.app.mobile.feature.game.XboxGameControls
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuTheme
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxGameMenu
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxMenuCapabilities
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxMenuControls
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuContent
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuEngine
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuPad
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuStorage
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuViewport
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState
import com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360Pad
import com.swordfish.lemuroid.app.shared.input.GameplayControllerPresence
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.inputevents.InputEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Opt-in in-app Cemu host. The ordinary APK cannot select or launch this debug process. */
class WiiuVulkanRuntimeProbeActivity : ComponentActivity(), SurfaceHolder.Callback {
    private lateinit var renderSurface: SurfaceView
    private var game: Game? = null
    private var selection: XboxEngineStore.Selection? = null
    private var media: File? = null
    private var sandbox: File? = null
    private var loaded = false
    private var started = false
    private var foreground = false
    private var leaving = false
    private var failed = false
    private var vmThread: Thread? = null
    private var menuOpen by mutableStateOf(false)
    private var physicalConnected by mutableStateOf(false)
    private var proController by mutableStateOf(false)
    private var frameAspect = 16f / 9f
    private var message by mutableStateOf("Preparing Wii U content. Open the game menu to cancel.")
    private var report = "WIIU_PREPARING; gameplay=UNVERIFIED"
    private val touch = XboxPadState()
    private val hardware = XboxPadState()
    private val grip = XboxPadState()
    private val gripSlots = mutableMapOf<Int, Set<Int>>()
    private val presence by lazy { GameplayControllerPresence(this) }

    // Reuse the established Nintendo Classic controls without changing Wii preferences.
    private val settings by lazy {
        TouchControllerSettingsManager(getSharedPreferences("wiiu-controller-layout", MODE_PRIVATE))
    }

    private external fun nativeRun(
        surface: Surface,
        sandbox: String,
        game: String,
        maxFrames: Int,
        coreLibrary: String,
        mediaDirectory: String,
    ): String

    private external fun nativeSurface(surface: Surface?)

    private external fun nativePrepare()

    private external fun nativeStop()

    private external fun nativePause(paused: Boolean)

    private external fun nativeReport(): String

    private external fun nativeController(pro: Boolean)

    private external fun nativeToggleScreen()

    private external fun nativeButton(
        retroId: Int,
        down: Boolean,
    )

    private external fun nativeAxis(
        axis: Int,
        value: Int,
    )

    private external fun nativePointer(
        x: Int,
        y: Int,
        down: Boolean,
    )

    private val importResources =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    if (started && !failed) {
                        message = "Import resources after a failed launch, then reopen from Shelf."
                        return@launch
                    }
                    val job = coroutineContext
                    runCatching {
                        withContext(Dispatchers.IO) {
                            com.swordfish.lemuroid.app.shared.game.wiiu.WiiuResources.import(
                                this@WiiuVulkanRuntimeProbeActivity,
                                uri,
                                requireNotNull(sandbox),
                            ) { job.ensureActive() }
                        }
                    }.onSuccess { message = "User resources imported. Return to Shelf and reopen this cartridge." }
                        .onFailure { message = "Resource import failed. Cartridge NAND and game saves were retained." }
                }
            }
        }

    private val importKeys =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val root = requireNotNull(sandbox)
                            val target = File(root, "system/Cemu/keys.txt")
                            check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                            val bytes =
                                contentResolver.openInputStream(uri)?.use { input ->
                                    val output = java.io.ByteArrayOutputStream()
                                    val buffer = ByteArray(8192)
                                    while (output.size() <= 1024 * 1024) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        output.write(buffer, 0, count)
                                    }
                                    output.toByteArray()
                                }
                                    ?: error("Cannot read the selected key file")
                            check(
                                bytes.isNotEmpty() && bytes.size <= 1024 * 1024,
                            ) { "The selected key file is empty or too large" }
                            val pending = File(target.parentFile, ".keys.pending")
                            FileOutputStream(pending).use {
                                it.write(bytes)
                                it.fd.sync()
                            }
                            check(pending.renameTo(target)) { "Cannot store Wii U keys" }
                        }
                    }.onSuccess { message = "User keys imported. Return to Shelf and reopen this cartridge." }
                        .onFailure { message = "Key import failed. Existing game saves were retained." }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        game = intent.getSerializableExtra("cartdeck.game") as? Game
        proController = getSharedPreferences("wiiu-runtime", MODE_PRIVATE).getBoolean("pro.${game?.cartridgeId}", false)
        renderSurface = SurfaceView(this).also { it.holder.addCallback(this) }
        renderSurface.setOnTouchListener { view, event ->
            if (!loaded || !started || menuOpen || view.width <= 0 || view.height <= 0) return@setOnTouchListener false
            val pointer = WiiuViewport.pointer(event.x, event.y, view.width, view.height, frameAspect)
            nativePointer(
                pointer.x,
                pointer.y,
                pointer.inside && event.actionMasked in setOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE),
            )
            if (event.actionMasked == MotionEvent.ACTION_UP) view.performClick()
            true
        }
        val root = FrameLayout(this)
        root.addView(renderSurface, FrameLayout.LayoutParams(-1, -1))
        root.addView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    Box(Modifier.fillMaxSize()) {
                        XboxGameControls(
                            ::touchInput,
                            settings,
                            TouchControllerID.WII_CLASSIC,
                            physicalControllerConnected = physicalConnected,
                            game = game,
                            onGripPressed = { slot, pressed, codes ->
                                if (pressed && !menuOpen && foreground) {
                                    gripSlots[slot] = codes
                                } else {
                                    gripSlots.remove(
                                        slot,
                                    )
                                }
                                grip.clear()
                                gripSlots.values.flatten().toSet().forEach { grip.key(WiiuPad.touchKey(it), true) }
                                sendInput()
                            },
                        ) { viewport ->
                            if (viewport.width() > 0 && viewport.height() > 0) {
                                val old = renderSurface.layoutParams as FrameLayout.LayoutParams
                                if (old.width != viewport.width() || old.height != viewport.height() ||
                                    old.leftMargin != viewport.left || old.topMargin != viewport.top
                                ) {
                                    renderSurface.layoutParams =
                                        FrameLayout.LayoutParams(viewport.width(), viewport.height()).apply {
                                            leftMargin = viewport.left
                                            topMargin = viewport.top
                                        }
                                }
                            }
                        }
                        if (!menuOpen && message.isNotEmpty()) GameMenuTheme { Text(message) }
                        if (menuOpen) {
                            XboxGameMenu(
                                title = game?.displayName() ?: "Wii U",
                                engineInfo = selection?.id ?: "Engine unavailable",
                                capabilities =
                                    XboxMenuCapabilities(
                                        false,
                                        false,
                                        selection != null && WiiuEngine.store(filesDir).canRollback(),
                                    ),
                                onResume = {
                                    if (!failed) {
                                        menuOpen = false
                                        syncPause()
                                        startIfReady()
                                    }
                                },
                                onReturnToShelf = ::leave,
                                onCopyReport = ::copyReport,
                                onProbe = {},
                                onPreviousEngine = { if (WiiuEngine.store(filesDir).rollback()) leave() },
                                console = "Wii U",
                                storageHelp =
                                    message.ifEmpty {
                                        "Saves persist in this cartridge's NAND. " +
                                            "Resume uses in-game saves; save states are unavailable."
                                    },
                            ) {
                                GameMenuLink(
                                    "Screen: switch TV / GamePad on Resume",
                                ) { if (loaded) nativeToggleScreen() }
                                GameMenuLink(
                                    "Controller: ${if (proController) "Pro Controller" else "GamePad"} (next frame)",
                                ) {
                                    proController = !proController
                                    getSharedPreferences(
                                        "wiiu-runtime",
                                        MODE_PRIVATE,
                                    ).edit().putBoolean("pro.${game?.cartridgeId}", proController).apply()
                                    if (loaded) nativeController(proController)
                                }
                                GameMenuLink("Import user-owned Cemu resources folder") { importResources.launch(null) }
                                GameMenuLink(
                                    "Import user-owned keys.txt for next launch",
                                ) { importKeys.launch(arrayOf("*/*")) }
                                XboxMenuControls(settings, TouchControllerID.WII_CLASSIC) { orientation, edited ->
                                    lifecycleScope.launch {
                                        settings.storeSettings(
                                            TouchControllerID.WII_CLASSIC,
                                            orientation,
                                            edited,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            FrameLayout.LayoutParams(-1, -1),
        )
        setContentView(root)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    openMenu()
                }
            },
        )
        lifecycleScope.launch {
            try {
                GameProcessLock.acquireExclusive(this@WiiuVulkanRuntimeProbeActivity)
                val current = requireNotNull(game) { "Open a Wii U cartridge from the shelf" }
                val job = coroutineContext
                val prepared =
                    withContext(Dispatchers.IO) {
                        val data = WiiuStorage.cartridgeRoot(filesDir, current.cartridgeId)
                        sandbox = data
                        val engine = WiiuEngine.install(this@WiiuVulkanRuntimeProbeActivity)
                        check(data.isDirectory || data.mkdirs())
                        val image =
                            WiiuContent.stage(
                                this@WiiuVulkanRuntimeProbeActivity,
                                Uri.parse(current.fileUri),
                                current.cartridgeId,
                            ) {
                                job.ensureActive()
                            }
                        Triple(engine, data, image)
                    }
                selection = prepared.first
                sandbox = prepared.second
                media = prepared.third
                System.load(File(prepared.first.directory, WiiuEngine.HOST).absolutePath)
                loaded = true
                startIfReady()
                while (!leaving) {
                    if (loaded && started) {
                        report = nativeReport()
                        val width = Regex("image_width=(\\d+)").find(report)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val height = Regex("image_height=(\\d+)").find(report)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        if (width > 0 && height > 0) frameAspect = width.toFloat() / height
                        File(requireNotNull(sandbox), "last-report.txt").writeText(report.take(16384))
                        val frames =
                            Regex("presented_image_callbacks=(\\d+)").find(report)
                                ?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        if (frames > 0 && !failed) {
                            message = ""
                            com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences(
                                this@WiiuVulkanRuntimeProbeActivity,
                            ).recordPresentation(current)
                        }
                    }
                    delay(1000)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                failed = true
                message = "Wii U could not start (${error.javaClass.simpleName}). " +
                    "Check media, resources and storage. Saves were retained."
                report = message
                openMenu()
            }
        }
    }

    private fun startIfReady() {
        val engine = selection ?: return
        val image = media ?: return
        val root = sandbox ?: return
        if (!loaded || started || leaving || !foreground || menuOpen || !renderSurface.holder.surface.isValid) return
        started = true
        nativePrepare()
        nativeController(proController)
        vmThread =
            Thread({
                val result =
                    runCatching {
                        nativeRun(
                            renderSurface.holder.surface, root.absolutePath, image.absolutePath, 0,
                            engine.core.absolutePath, image.parentFile!!.canonicalPath,
                        )
                    }.getOrElse { "WIIU_NATIVE_FAILED:${it.javaClass.simpleName}" }
                runOnUiThread {
                    report = result
                    if (!leaving) {
                        failed = true
                        message = "Wii U session ended. Copy the engine report or return to the shelf."
                        openMenu()
                    }
                }
            }, "cartdeck-wiiu-vm").also { it.start() }
    }

    private fun touchInput(events: List<InputEvent>) {
        if (leaving || menuOpen || !foreground) return
        events.forEach { event ->
            when (event) {
                is InputEvent.Button ->
                    if (event.id == KeyEvent.KEYCODE_BUTTON_MODE) {
                        if (!event.pressed) openMenu()
                    } else {
                        touch.key(WiiuPad.touchKey(event.id), event.pressed)
                    }
                is InputEvent.DiscreteDirection -> touch.direction(event.id, event.direction.x, event.direction.y)
                is InputEvent.ContinuousDirection -> touch.direction(event.id, event.direction.x, event.direction.y)
            }
        }
        sendInput()
    }

    private fun sendInput() {
        if (!loaded || !started) return
        val base = Xbox360Pad.merged(hardware, touch)
        val accessory = Xbox360Pad.values(grip)
        val merged =
            IntArray(
                24,
            ) { if (kotlin.math.abs(base[it]) >= kotlin.math.abs(accessory[it])) base[it] else accessory[it] }
        WiiuPad.retroToXenia.forEachIndexed { retro, source -> nativeButton(retro, merged[source] != 0) }
        WiiuPad.analog(merged).forEachIndexed { index, value -> nativeAxis(index, value.coerceIn(-32767, 32767)) }
    }

    private fun releaseInput() {
        touch.clear()
        hardware.clear()
        grip.clear()
        gripSlots.clear()
        if (loaded) {
            nativePointer(0, 0, false)
            sendInput()
        }
    }

    private fun syncPause() {
        if (loaded) nativePause(!foreground || menuOpen || leaving || failed || !renderSurface.holder.surface.isValid)
    }

    private fun openMenu() {
        menuOpen = true
        releaseInput()
        syncPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode in setOf(KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_MENU)) {
            if (event.action == KeyEvent.ACTION_UP) openMenu()
            return true
        }
        if (!menuOpen && foreground && event.action in setOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP) &&
            hardware.key(event.keyCode, event.action == KeyEvent.ACTION_DOWN)
        ) {
            sendInput()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (!menuOpen && foreground && event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            hardware.motion({ event.getAxisValue(it) }, event.device)
            sendInput()
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (loaded && started) nativeSurface(holder.surface)
        syncPause()
        startIfReady()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        if (loaded && started) nativeSurface(holder.surface)
        syncPause()
        startIfReady()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseInput()
        if (loaded && started) nativeSurface(null)
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        presence.start { physicalConnected = it.isNotEmpty() }
        syncPause()
        startIfReady()
    }

    override fun onPause() {
        foreground = false
        presence.stop()
        releaseInput()
        syncPause()
        super.onPause()
    }

    private fun copyReport() {
        (
            getSystemService(
                CLIPBOARD_SERVICE,
            ) as ClipboardManager
        ).setPrimaryClip(ClipData.newPlainText("CARTDECK Wii U", report.take(16384)))
    }

    private fun leave() {
        if (leaving) return
        leaving = true
        releaseInput()
        if (loaded) nativeStop()
        lifecycleScope.launch {
            val stopped =
                withContext(Dispatchers.IO) {
                    vmThread?.join(8000)
                    vmThread?.isAlive != true
                }
            if (!stopped) {
                failed = true
                report += "\nWIIU_SHUTDOWN_TIMED_OUT"
            }
            sandbox?.let { File(it, "last-report.txt").writeText(report.take(16384)) }
            setResult(
                if (failed) BaseGameActivity.RESULT_ERROR else Activity.RESULT_OK,
                Intent().putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME, game)
                    .putExtra(
                        BaseGameActivity.PLAY_GAME_RESULT_LEANBACK,
                        intent.getBooleanExtra("cartdeck.leanback", false),
                    )
                    .putExtra(
                        BaseGameActivity.PLAY_GAME_RESULT_SESSION_DURATION,
                        SystemClock.elapsedRealtime() -
                            intent.getLongExtra("cartdeck.startTime", SystemClock.elapsedRealtime()),
                    ),
            )
            finish()
        }
    }

    override fun onDestroy() {
        if (loaded) nativeStop()
        presence.stop()
        super.onDestroy()
        Thread({
            vmThread?.join(8000)
            Process.killProcess(Process.myPid())
        }, "cartdeck-wiiu-shutdown").start()
    }
}
