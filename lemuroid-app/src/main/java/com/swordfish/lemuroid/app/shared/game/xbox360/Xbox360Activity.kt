package com.swordfish.lemuroid.app.shared.game.xbox360

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.mobile.feature.game.NativeRendererAudit
import com.swordfish.lemuroid.app.mobile.feature.game.NativeViewportPolicy
import com.swordfish.lemuroid.app.mobile.feature.game.XboxGameControls
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuPauseGate
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameplayMenuCommand
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameplayMenuHostAdapter
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameplayMenuHostKind
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameplayMenuHostProfiles
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.NativeControllerMappingSession
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxGameMenu
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxMenuCapabilities
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxMenuControls
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.menuFeedback
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.resumeFailureFeedback
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.CartdeckLoadingScreen
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.NativeReportCheckpoint
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.GameplayControllerPresence
import com.swordfish.lemuroid.app.shared.input.handle
import com.swordfish.lemuroid.app.shared.input.observeControllerMenuEdges
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.inputevents.InputEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import xendroid.compose.Emulator as XeniaEmulator

/** One guest per isolated process. Compose owns only CARTDECK controls and the shared pause menu. */
class Xbox360Activity : ComponentActivity(), SurfaceHolder.Callback {
    private val main = Handler(Looper.getMainLooper())
    private val pauseGate = GameMenuPauseGate()
    private val menuHost by lazy {
        GameplayMenuHostAdapter(
            GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.XENIA),
            pause = { requireNotNull(emulator).pause() },
            resume = { requireNotNull(emulator).resume() },
        )
    }
    private var menuPauseFeedback by mutableStateOf("Menu open. Engine pause is not confirmed.")
    private val viewportAudit = NativeRendererAudit.Tracker(NativeViewportPolicy.Host.XBOX360)
    private val touch = XboxPadState()
    private val hardware = XboxPadState()
    private val grip = XboxPadState()
    private val gripSlots = mutableMapOf<Int, Set<Int>>()
    private var emulator: XeniaEmulator? = null
    private lateinit var engine: XboxEngineStore.Selection
    private lateinit var surface: SurfaceView
    private var contentReady = false
    private var surfaceReady = false
    private var booted = false
    private var subsystemsReady = false
    private val reportCheckpoint = NativeReportCheckpoint()
    private var foreground = false
    private val preparation = com.swordfish.lemuroid.app.shared.game.LaunchPreparation()
    private var leaving = false
    private var menuOpen by mutableStateOf(false)

    // Explicit Advanced > Diagnostics opt-in only; no gameplay overlay or automatic capture.
    private var xgfxDiagnosticsEnabled by mutableStateOf(false)
    private var pipelineABActive = Xbox360PipelineAB.Mode.NORMAL
    private var pipelineABQueued by mutableStateOf(Xbox360PipelineAB.Mode.NORMAL)
    private var message by mutableStateOf("Preparing Xbox 360 content. Open the menu to cancel.")
    private var failed = false
    private var physicalConnected by mutableStateOf(false)
    private val presence by lazy { GameplayControllerPresence(this) }
    private lateinit var profile: Xbox360Profiles.Selection
    private val settings by lazy { TouchControllerSettingsManager(SharedPreferencesHelper.getSharedPreferences(this)) }
    private val poll =
        object : Runnable {
            override fun run() {
                if (leaving || !booted) return
                runCatching {
                    val native = emulator!!.cartdeck_report()
                    val errorCode =
                        Regex("native_error=0x([0-9a-fA-F]+)").find(native)
                            ?.groupValues?.get(1)?.toLongOrNull(16)
                    if (errorCode?.let {
                            it != 0L
                        } == true
                    ) {
                        fail("Xbox 360 native startup failed. Copy the report from Advanced.")
                    }
                    val flags = Regex("stages=0x([0-9a-fA-F]+)").find(native)?.groupValues?.get(1)?.toIntOrNull(16) ?: 0
                    if (flags and 2 != 0 && !subsystemsReady) {
                        subsystemsReady = true
                        syncPause()
                    }
                    if (flags and 8 != 0) message = ""
                    if (!failed && reportCheckpoint.due(android.os.SystemClock.elapsedRealtime(), flags.toString())) {
                        saveReport(native)
                    }
                }.onFailure { fail("Xbox 360 diagnostic interface failed. Return to the shelf before retrying.") }
                if (!leaving) main.postDelayed(this, 1000)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pipelineABQueued =
            if (BuildConfig.DEBUG) Xbox360PipelineAB.queued(this) else Xbox360PipelineAB.Mode.NORMAL
        observeControllerMenuEdges(this, menuEdge)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        val root = FrameLayout(this)
        surface = SurfaceView(this).also { it.holder.addCallback(this) }
        root.addView(surface, FrameLayout.LayoutParams(-1, -1))
        val overlay =
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    Box(Modifier.fillMaxSize()) {
                        XboxGameControls(
                            ::onTouchInput,
                            settings,
                            TouchControllerID.XBOX360,
                            physicalControllerConnected = physicalConnected,
                            presentationAspect =
                                NativeViewportPolicy.presentationAspect(NativeViewportPolicy.Host.XBOX360),
                            game =
                                intent.getSerializableExtra(
                                    "cartdeck.game",
                                ) as? com.swordfish.lemuroid.lib.library.db.entity.Game,
                            onGripPressed = { slot, pressed, codes ->
                                if (pressed && !menuOpen && this@Xbox360Activity.foreground) {
                                    gripSlots[slot] = codes
                                } else {
                                    gripSlots.remove(
                                        slot,
                                    )
                                }
                                grip.clear()
                                gripSlots.values.flatten().toSet().forEach { grip.key(it, true) }
                                sendInputs()
                            },
                        ) { viewport ->
                            viewportAudit.request(viewport.width(), viewport.height())
                            if (viewport.width() > 0 && viewport.height() > 0) {
                                val old = surface.layoutParams as FrameLayout.LayoutParams
                                if (old.width != viewport.width() || old.height != viewport.height() ||
                                    old.leftMargin != viewport.left || old.topMargin != viewport.top
                                ) {
                                    surface.layoutParams =
                                        FrameLayout.LayoutParams(viewport.width(), viewport.height()).apply {
                                            leftMargin = viewport.left
                                            topMargin = viewport.top
                                        }
                                }
                            }
                        }
                        if (message.isNotEmpty() && !menuOpen && !failed) {
                            CartdeckLoadingScreen(
                                message,
                                title =
                                    intent.getStringExtra(
                                        "cartdeck.gameTitle",
                                    ),
                                onCancel = {
                                    leave()
                                },
                                cancelling = preparation.cancelling.collectAsState().value,
                            )
                        }
                        if (menuOpen && failed) {
                            com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuTheme {
                                com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuPanel(
                                    intent.getStringExtra("cartdeck.gameTitle").orEmpty(),
                                    "Xbox 360",
                                    "Recovery",
                                    true,
                                    ::leave,
                                    ::leave,
                                ) {
                                    androidx.compose.material3.Text(message)
                                    com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink(
                                        "Copy Report",
                                        onClick = ::copyReport,
                                    )
                                    com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink(
                                        "Return to Shelf",
                                        onClick = ::leave,
                                    )
                                }
                            }
                        }
                        if (menuOpen && !failed) {
                            XboxGameMenu(
                                game =
                                    intent.getSerializableExtra(
                                        "cartdeck.game",
                                    ) as? com.swordfish.lemuroid.lib.library.db.entity.Game,
                                title = intent.getStringExtra("cartdeck.gameTitle").orEmpty(),
                                engineInfo = if (::engine.isInitialized) engine.id else "Engine unavailable",
                                capabilities =
                                    XboxMenuCapabilities(
                                        BuildConfig.DEBUG,
                                        false,
                                        !failed &&
                                            runCatching {
                                                Xbox360Engine.store(filesDir).canRollback()
                                            }.getOrDefault(false),
                                    ),
                                pauseFeedback = menuPauseFeedback,
                                canResume = !failed && !leaving,
                                onResume = ::closeMenu,
                                onReturnToShelf = ::leave,
                                onCopyReport = ::copyReport,
                                performanceCaptureEnabled = xgfxDiagnosticsEnabled,
                                onTogglePerformanceCapture =
                                    if (BuildConfig.DEBUG) ::togglePerformanceCapture else null,
                                pipelineABActive = if (BuildConfig.DEBUG) pipelineABActive.description else null,
                                pipelineABQueued = pipelineABQueued.description,
                                onQueuePipelineAB = if (BuildConfig.DEBUG) ::queuePipelineAB else null,
                                onProbe = {},
                                onPreviousEngine = { if (Xbox360Engine.store(filesDir).rollback()) leave() },
                                appearance = null,
                                console = "Xbox 360",
                                storageHelp =
                                    message.ifEmpty {
                                        "Xbox 360 saves in-game to persistent console storage. " +
                                            "Emulator save states are not supported. " +
                                            "Presentation is not gameplay acceptance."
                                    },
                            ) {
                                XboxMenuControls(settings, TouchControllerID.XBOX360, physicalMapping = {
                                    physicalMapping.Panel()
                                }) { orientation, edited ->
                                    lifecycleScope.launch {
                                        settings.storeSettings(
                                            TouchControllerID.XBOX360,
                                            orientation,
                                            edited,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        root.addView(overlay, ViewGroup.LayoutParams(-1, -1))
        setContentView(root)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Compose handles Back within a submenu. This is the Activity
                    // fallback for opening or dismissing the top-level menu.
                    if (!contentReady) {
                        leave()
                    } else if (menuOpen) {
                        closeMenu()
                    } else {
                        openMenu()
                    }
                }
            },
        )
        preparation.start(lifecycleScope) launch@{
            try {
                val staged =
                    withContext(Dispatchers.IO) {
                        GameProcessLock.acquireExclusive(applicationContext)
                        engine = Xbox360Engine.store(filesDir).selected()
                            ?: error("Verified Xbox 360 engine is unavailable")
                        saveReport("stage=ENGINE_SELECTED; native_engine_loaded=false")
                        profile = Xbox360Profiles.prepare(this@Xbox360Activity, Xbox360Engine.data(filesDir))
                        val jobContext = coroutineContext
                        Xbox360Content.stage(
                            this@Xbox360Activity,
                            Uri.parse(intent.getStringExtra("cartdeck.gameUri")),
                        ) {
                            jobContext.ensureActive()
                        }
                    }
                if (leaving) return@launch
                // System.load uses immutable verified package paths; never use downloaded APK code or a custom driver.
                System.load(engine.core.absolutePath)
                emulator =
                    XeniaEmulator().apply {
                        setup_context(this@Xbox360Activity)
                        val data = Xbox360Engine.data(filesDir)
                        pipelineABActive =
                            if (BuildConfig.DEBUG) {
                                Xbox360PipelineAB.consume(this@Xbox360Activity)
                            } else {
                                Xbox360PipelineAB.Mode.NORMAL
                            }
                        pipelineABQueued = Xbox360PipelineAB.Mode.NORMAL
                        setup_launch_args(
                            arrayOf(
                                "--storage_root=${data.absolutePath}",
                                "--content_root=${File(data, "content").absolutePath}",
                                "--cache_root=${File(data, "cache").absolutePath}",
                                "--config=${File(data, "config/xenia.config.toml").absolutePath}",
                                "--log_file=${File(data, "logs/xenia.log").absolutePath}",
                                "--hid=android", "--vulkan_lib_path=",
                                "--logged_profile_slot_0_xuid=${profile.xuid}",
                            ) + listOfNotNull(pipelineABActive.launchOption),
                        )
                        setup_game_path(staged.absolutePath)
                    }
                contentReady = true
                bootIfReady()
            } catch (
                cancelled: kotlinx.coroutines.CancellationException,
            ) {
                throw cancelled
            } catch (
                error: Throwable,
            ) {
                fail(
                    Xbox360Reports.safeError(error) + "\n" +
                        "Xbox 360 content or engine could not be opened. " +
                        "Check folder permission, content format and available storage. Saves were retained.",
                )
            }
        }
    }

    private fun bootIfReady() {
        if (!contentReady || !surfaceReady || booted || leaving) return
        emulator!!.setup_surface(surface.holder.surface)
        booted = true
        emulator!!.boot()
        saveReport("stage=ENGINE_LOADED; native_boot_requested=true; gameplay=UNVERIFIED")
        main.post(poll)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        if (booted) emulator?.setup_surface(holder.surface) else bootIfReady()
        syncPause()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        viewportAudit.observe(width, height)
        if (booted) emulator?.change_surface(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        viewportAudit.observe(0, 0)
        surfaceReady = false
        syncPause()
        if (booted) emulator?.setup_surface(null)
    }

    override fun onResume() {
        super.onResume()
        physicalMapping.start()
        foreground = true
        presence.start { devices ->
            menuEdge.retainDevices(devices.map { it.id }.toSet())
            physicalMapping.retainDevices(devices.map { it.id }.toSet())
            releaseInputs()
            physicalConnected = devices.isNotEmpty()
            if (physicalConnected) {
                touch.clear()
                grip.clear()
                gripSlots.clear()
                sendInputs()
            }
        }
        syncPause()
    }

    override fun onPause() {
        physicalMapping.stop()
        menuEdge.clear()
        foreground = false
        presence.stop()
        syncPause()
        super.onPause()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            message = "Android reports critical memory pressure. Return to the shelf before retrying."
            openMenu()
        }
    }

    private fun syncPause() {
        if (!subsystemsReady) return
        pauseGate.transition(foreground && surfaceReady, menuOpen, failed, leaving)?.let { shouldPause ->
            if (shouldPause) releaseInputs()
            val outcome =
                menuHost.dispatch(
                    if (shouldPause) GameplayMenuCommand.PAUSE else GameplayMenuCommand.RESUME,
                )
            if (shouldPause) {
                menuPauseFeedback = outcome.menuFeedback()
            } else {
                val failure = outcome.resumeFailureFeedback()
                if (failure != null && !leaving) {
                    // Keep the same guest session visible for retry or recovery.
                    menuPauseFeedback = failure
                    menuOpen = true
                    pauseGate.retryResumeAfterFailure()
                }
            }
        }
    }

    private fun openMenu() {
        if (!contentReady && !failed) {
            leave()
            return
        }
        if (menuOpen || leaving) return
        menuOpen = true
        releaseInputs()
        syncPause()
    }

    private fun closeMenu() {
        physicalMapping.discard()
        if (!menuOpen || failed || leaving) return
        menuOpen = false
        syncPause()
    }

    private fun fail(reason: String) {
        failed = true
        message = reason
        openMenu()
        saveReport("failure=$reason; gameplay=UNVERIFIED")
    }

    private fun releaseInputs() {
        touch.clear()
        hardware.clear()
        grip.clear()
        gripSlots.clear()
        if (booted) sendInputs()
    }

    private fun sendInputs() {
        if (!booted) return
        val base = Xbox360Pad.merged(hardware, touch)
        val accessory = Xbox360Pad.values(grip)
        IntArray(
            24,
        ) { i ->
            if (kotlin.math.abs(base[i]) >= kotlin.math.abs(accessory[i])) base[i] else accessory[i]
        }.forEachIndexed {
                index,
                value,
            ->
            emulator?.key_event(index, value != 0, value)
        }
    }

    private fun onTouchInput(events: List<InputEvent>) {
        if (leaving || menuOpen || !foreground) return
        events.forEach { event ->
            when (event) {
                is InputEvent.Button ->
                    if (event.id == KeyEvent.KEYCODE_BUTTON_MODE) {
                        if (!event.pressed) openMenu()
                    } else {
                        touch.key(event.id, event.pressed)
                    }
                is InputEvent.DiscreteDirection -> touch.direction(event.id, event.direction.x, event.direction.y)
                is InputEvent.ContinuousDirection -> touch.direction(event.id, event.direction.x, event.direction.y)
            }
        }
        if (!menuOpen) sendInputs()
    }

    private val physicalMapping by lazy {
        NativeControllerMappingSession(
            this,
            lifecycleScope,
            TouchControllerID.XBOX360,
            cartridge =
                (
                    intent.getSerializableExtra(
                        "cartdeck.game",
                    ) as? com.swordfish.lemuroid.lib.library.db.entity.Game
                )?.cartridgeId,
        ) { device ->
            presence.port(device.id)?.takeIf {
                it == 0
            }
        }
    }

    private val menuEdge = ControllerMenuEdge()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isCanceled) {
            menuEdge.clear()
            physicalMapping.cancelInput()
            releaseInputs()
            return true
        }
        if (physicalMapping.shortcut(event, {
                if (menuOpen) closeMenu() else openMenu()
            }, ::dispatchKeyEvent)
        ) {
            return true
        }
        if (message.isNotEmpty() && !failed) {
            if (event.keyCode in setOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_MODE)) {
                if (menuEdge.handle(event)) leave()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        val mappedKey = physicalMapping.gameplayKey(event)
        if (event.keyCode in setOf(KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_MENU) ||
            mappedKey == KeyEvent.KEYCODE_BUTTON_MODE
        ) {
            if (menuEdge.handle(event)) {
                if (menuOpen) closeMenu() else openMenu()
            }
            return true
        }
        if (menuOpen && physicalMapping.captureKey(event)) return true
        if (!menuOpen && !physicalMapping.acceptsGameplay(event)) return true
        if (menuOpen || !foreground) return super.dispatchKeyEvent(event)
        if (hardware.key(mappedKey, event.action == KeyEvent.ACTION_DOWN)) {
            sendInputs()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (menuOpen) {
            physicalMapping.observeMotion(event)
            return true
        }
        if (!physicalMapping.acceptsMotion(event)) return true
        if (!menuOpen && foreground && event.isFromSource(android.view.InputDevice.SOURCE_JOYSTICK)) {
            hardware.motion({ physicalMapping.axis(event, it) }, event.device)
            sendInputs()
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun saveReport(native: String) {
        val info =
            if (::engine.isInitialized) {
                "engine=${engine.id}; revision=${engine.properties.getProperty(
                    "revision",
                )}; sha256=${engine.properties.getProperty(
                    "core.sha256",
                )}; build_id=${engine.properties.getProperty("native.build_id")}"
            } else {
                "engine=unavailable"
            }
        val memory = android.app.ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memory)
        val rssBytes =
            runCatching {
                val pages =
                    java.io.File("/proc/self/statm").bufferedReader().use { reader ->
                        reader.readLine().split(' ').filter { it.isNotBlank() }[1].toLong()
                    }
                pages * android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE)
            }.getOrNull()
        val thermal =
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                (getSystemService(POWER_SERVICE) as android.os.PowerManager).currentThermalStatus.toString()
            } else {
                "UNAVAILABLE"
            }
        val report =
            "CARTDECK ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}); Xbox 360\n" +
                "device=${android.os.Build.MODEL}; api=${android.os.Build.VERSION.SDK_INT}; " +
                "abi=${android.os.Build.SUPPORTED_ABIS.joinToString()}; available_memory=${memory.availMem}\n" +
                "process_rss_bytes=${rssBytes ?: "UNAVAILABLE"}; thermal_status=$thermal; " +
                "native_heap_bytes=${android.os.Debug.getNativeHeapAllocatedSize()}\n" +
                "$info\n" +
                "pipeline_ab_scope=GLOBAL_XBOX360_ONE_LAUNCH; " +
                "pipeline_ab_active=${pipelineABActive.name}; " +
                "pipeline_ab_launch_option=${pipelineABActive.launchOption ?: "ENGINE_DEFAULT"}\n" +
                "$native\n${viewportAudit.summary()}\n" +
                "failure=${if (failed) message else "NONE_RECORDED"}\n" +
                "content_and_saves=RETAINED; automatic_restart=false; " +
                "exit=${if (leaving) "USER_REQUESTED" else "NOT_OBSERVED"}\n" +
                "Startup, JIT compilation and Vulkan presentation do not establish meaningful graphics or gameplay."
        Xbox360Reports.write(filesDir, report)
    }

    private fun queuePipelineAB(mode: Xbox360PipelineAB.Mode) {
        if (!BuildConfig.DEBUG || !menuOpen || failed || leaving) return
        runCatching { Xbox360PipelineAB.queue(this, mode) }
            .onSuccess { pipelineABQueued = mode }
            .onFailure { menuPauseFeedback = "Could not prepare the Xbox 360 pipeline A/B test." }
    }

    private fun togglePerformanceCapture() {
        if (!BuildConfig.DEBUG || !booted || failed || leaving || !menuOpen) return
        val next = !xgfxDiagnosticsEnabled
        runCatching {
            requireNotNull(emulator).cartdeck_set_xgfx_diagnostics(next)
        }.onSuccess {
            xgfxDiagnosticsEnabled = next
        }.onFailure {
            menuPauseFeedback = "Xbox 360 diagnostic capture is unavailable in this engine."
        }
    }

    private fun copyReport() {
        val native = runCatching { emulator?.cartdeck_report() }.getOrNull()
        if (native != null) saveReport(native)
        (
            getSystemService(
                CLIPBOARD_SERVICE,
            ) as ClipboardManager
        ).setPrimaryClip(ClipData.newPlainText("CARTDECK Xbox 360", Xbox360Reports.read(filesDir)))
    }

    private fun leave() {
        if (leaving) return
        runCatching { emulator?.cartdeck_set_xgfx_diagnostics(false) }
        xgfxDiagnosticsEnabled = false
        leaving = true
        preparation.requestCancellation()
        releaseInputs()
        syncPause()
        main.removeCallbacksAndMessages(null)
        lifecycleScope.launch {
            preparation.awaitCleanup()
            saveReport(runCatching { emulator?.cartdeck_report() }.getOrNull() ?: "stage=HOST_UNAVAILABLE")
            val result =
                Intent().putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME, intent.getSerializableExtra("cartdeck.game"))
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
            setResult(
                if (failed) BaseGameActivity.RESULT_ERROR else Activity.RESULT_OK,
                result.apply {
                    if (failed) putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR, message)
                },
            )
            finish()
        }
    }

    override fun onDestroy() {
        presence.stop()
        main.removeCallbacksAndMessages(null)
        if (booted) {
            releaseInputs()
            emulator?.pause()
            emulator?.setup_surface(null)
        }
        super.onDestroy()
        // Upstream runtime is single-run. Only this isolated guest process exits after bounded shutdown.
        // No guest directory is cleared and no automatic relaunch is attempted.
        Process.killProcess(Process.myPid())
    }
}
