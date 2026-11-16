package com.swordfish.lemuroid.app.debug

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.app.mobile.feature.game.NativeRendererAudit
import com.swordfish.lemuroid.app.mobile.feature.game.NativeViewportPolicy
import com.swordfish.lemuroid.app.mobile.feature.game.XboxGameControls
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink
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
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2BiosRecovery
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Content
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Engine
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2PadMapping
import com.swordfish.lemuroid.app.shared.game.ps2next.Armsx2Storage
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState
import com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360Pad
import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.GameplayControllerPresence
import com.swordfish.lemuroid.app.shared.input.handle
import com.swordfish.lemuroid.app.shared.input.observeControllerMenuEdges
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.inputevents.InputEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.iefriends.pcsx2.NativeApp
import java.io.File
import java.io.FileOutputStream

/** CARTDECK's opt-in in-process ARMSX2 host; Play! remains independently selectable. */
class Armsx2RuntimeProbeActivity : ComponentActivity(), SurfaceHolder.Callback {
    private lateinit var surface: SurfaceView
    private var game: Game? = null
    private var paths: Armsx2Storage.Paths? = null
    private var engine: XboxEngineStore.Selection? = null
    private var image: File? = null
    private var loaded = false
    private var ready = false
    private var started = false
    private var hostForeground = false
    private val preparation = com.swordfish.lemuroid.app.shared.game.LaunchPreparation()
    private var leaving = false
    private var failed = false
    private var missingBios = false
    private val biosSetupMode: Boolean get() = intent.getBooleanExtra(Armsx2BiosRecovery.SETUP, false)
    private var vm: Thread? = null
    private var menu by mutableStateOf(false)
    private val pauseGate = GameMenuPauseGate()
    private val menuHost by lazy {
        GameplayMenuHostAdapter(
            GameplayMenuHostProfiles.capabilities(GameplayMenuHostKind.ARMSX2),
            pause = { NativeApp.pause() },
            resume = { NativeApp.resume() },
        )
    }
    private var menuPauseFeedback by mutableStateOf("Menu open. Engine pause is not confirmed.")
    private val viewportAudit = NativeRendererAudit.Tracker(NativeViewportPolicy.Host.ARMSX2)
    private var connected by mutableStateOf(false)
    private var message by mutableStateOf("Preparing PS2 cartridge. Open the game menu to cancel.")
    private var report = "ARMSX2_PREPARING; gameplay=UNVERIFIED"
    private val hardware = XboxPadState()
    private val touch = XboxPadState()
    private val grip = XboxPadState()
    private val slots = mutableMapOf<Int, Set<Int>>()
    private val presence by lazy { GameplayControllerPresence(this) }
    private val settings by lazy {
        TouchControllerSettingsManager(getSharedPreferences("armsx2-controller-layout", MODE_PRIVATE))
    }

    private val importBios =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null && biosSetupMode) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        com.swordfish.lemuroid.app.shared.game.NativeSessionJournal.finish(
                            this@Armsx2RuntimeProbeActivity,
                        )
                    }
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            } else if (uri != null) {
                lifecycleScope.launch {
                    if (started && !failed) return@launch
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val root = requireNotNull(paths).bios
                            check(root.isDirectory || root.mkdirs())
                            val pending = File(root, ".bios.pending")
                            var size = 0L
                            contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(pending).use { output ->
                                    val buffer = ByteArray(8192)
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        size += count
                                        check(size <= 16L * 1024 * 1024) { "BIOS file exceeds import limit" }
                                        output.write(buffer, 0, count)
                                    }
                                    check(size > 0) { "Empty BIOS file" }
                                    output.fd.sync()
                                }
                            } ?: error("Cannot read selected BIOS")
                            check(loaded) { "Load the verified engine before BIOS validation" }
                            val fd =
                                android.os.ParcelFileDescriptor.open(
                                    pending,
                                    android.os.ParcelFileDescriptor.MODE_READ_ONLY,
                                ).detachFd()
                            check(
                                NativeApp.getBiosInfoFromFd(fd) != null,
                            ) { "The selected file is not a supported PS2 BIOS" }
                            check(pending.renameTo(File(root, "bios.bin")))
                        }
                    }.onSuccess {
                        message = "User BIOS imported. Return to Shelf and reopen this cartridge."
                        missingBios = false
                        if (biosSetupMode) {
                            failed = false
                            if (callingActivity == null) {
                                android.widget.Toast.makeText(
                                    this@Armsx2RuntimeProbeActivity,
                                    "PS2 BIOS imported. Automatic selection can now use ARMSX2.",
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                            }
                            report = "PS2_BIOS_IMPORTED; native_validation=PASS; memory_cards=RETAINED"
                            leave()
                        }
                    }.onFailure {
                        message = "BIOS import failed: ${it.message ?: it.javaClass.simpleName}. " +
                            "Memory cards were retained."
                        report = message
                        failed = true
                        openMenu()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeControllerMenuEdges(this, menuEdge)
        game = intent.getSerializableExtra("cartdeck.game") as? Game
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        surface = SurfaceView(this).also { it.holder.addCallback(this) }
        val root = FrameLayout(this)
        root.addView(surface, FrameLayout.LayoutParams(-1, -1))
        root.addView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    Box(Modifier.fillMaxSize()) {
                        XboxGameControls(
                            ::touchInput,
                            settings,
                            TouchControllerID.PSX_DUALSHOCK,
                            physicalControllerConnected = connected,
                            presentationAspect =
                                NativeViewportPolicy.presentationAspect(NativeViewportPolicy.Host.ARMSX2),
                            game = game,
                            onGripPressed = { slot, down, codes ->
                                if (down && hostForeground && !menu) slots[slot] = codes else slots.remove(slot)
                                grip.clear()
                                slots.values.flatten().toSet().forEach { grip.key(it, true) }
                                sendInput()
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
                        if (!menu && message.isNotEmpty() && !failed) {
                            CartdeckLoadingScreen(message, onCancel = {
                                leave()
                            }, cancelling = preparation.cancelling.collectAsState().value)
                        }
                        if (menu) {
                            XboxGameMenu(
                                game = game,
                                title = game?.displayName() ?: "PlayStation 2",
                                engineInfo = engine?.let { "ARMSX2 ${it.id}" } ?: "ARMSX2 startup needs attention",
                                capabilities =
                                    XboxMenuCapabilities(
                                        false,
                                        false,
                                        engine != null && Armsx2Engine.store(filesDir).canRollback(),
                                    ),
                                pauseFeedback = menuPauseFeedback,
                                canResume = !failed && !leaving,
                                onResume = ::closeMenu,
                                onReturnToShelf = ::leave,
                                onCopyReport = ::copyReport,
                                onProbe = {},
                                onPreviousEngine = { if (Armsx2Engine.store(filesDir).rollback()) leave() },
                                recovery = {
                                    if (failed) {
                                        Text(message, Modifier.padding(16.dp))
                                        GameMenuLink("Import PS2 BIOS") { importBios.launch(arrayOf("*/*")) }
                                        GameMenuLink("Retry cartridge") { retryArmsx2() }
                                    }
                                    GameMenuLink("Use Play! fallback and reopen") { usePlay() }
                                },
                                console = "PlayStation 2",
                                storageHelp = message.ifEmpty { "Independent ARMSX2 memory cards and in-game saves." },
                            ) {
                                GameMenuLink("Use Play! on next Open (separate memory cards)") {
                                    game?.let { current ->
                                        if (EngineSelectionPreferences(this@Armsx2RuntimeProbeActivity).setPreferred(
                                                GameSystem.findById(current.systemId),
                                                CoreID.PLAY_MODERN.coreName,
                                                current,
                                            )
                                        ) {
                                            leave()
                                        }
                                    }
                                }
                                GameMenuLink("Capture stock GPU and driver diagnostics") {
                                    lifecycleScope.launch {
                                        if (loaded) {
                                            withContext(Dispatchers.IO) {
                                                val output = File(requireNotNull(paths).gameRoot, "driver-report.json")
                                                if (NativeApp.writeSystemReport(output.absolutePath)) {
                                                    report += "\n" + output.readText().take(16384)
                                                }
                                            }
                                        }
                                    }
                                }
                                GameMenuLink("Renderer on next launch: switch Vulkan / OpenGL") {
                                    val prefs = getSharedPreferences("armsx2-runtime", MODE_PRIVATE)
                                    val next =
                                        if (prefs.getString("renderer", "Vulkan") == "Vulkan") "OpenGL" else "Vulkan"
                                    prefs.edit().putString("renderer", next).apply()
                                    message = "$next selected for next launch. Uses the stock system driver."
                                }
                                GameMenuLink(
                                    "Import user-owned PS2 BIOS after a failed launch",
                                ) { importBios.launch(arrayOf("*/*")) }
                                XboxMenuControls(settings, TouchControllerID.PSX_DUALSHOCK, physicalMapping = {
                                    physicalMapping.Panel()
                                }) { orientation, edited ->
                                    lifecycleScope.launch {
                                        settings.storeSettings(
                                            TouchControllerID.PSX_DUALSHOCK,
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
                    // Compose handles Back within a submenu. This is the Activity
                    // fallback for opening or dismissing the top-level menu.
                    if (!loaded) {
                        leave()
                    } else if (menu) {
                        closeMenu()
                    } else {
                        openMenu()
                    }
                }
            },
        )
        preparation.start(lifecycleScope) launch@{
            try {
                if (biosSetupMode) {
                    var attempts = 0
                    while (withContext(Dispatchers.IO) {
                            GameProcessLock.isHeldByAnotherProcess(this@Armsx2RuntimeProbeActivity)
                        } && attempts++ < 100
                        ) delay(100)
                }
                GameProcessLock.acquireExclusive(this@Armsx2RuntimeProbeActivity)
                val current = game
                check(biosSetupMode || current != null) { "Open a PS2 cartridge from Shelf" }
                val job = coroutineContext
                withContext(Dispatchers.IO) {
                    val data =
                        if (biosSetupMode) {
                            Armsx2Storage.setupPaths(filesDir)
                        } else {
                            Armsx2Storage.paths(filesDir, requireNotNull(current).cartridgeId)
                        }
                    paths = data
                    Armsx2Storage.createPrivateDirectories(data)
                    val selected = Armsx2Engine.install(this@Armsx2RuntimeProbeActivity)
                    engine = selected
                    if (!biosSetupMode) {
                        Armsx2Engine.resources(
                            this@Armsx2RuntimeProbeActivity,
                            data,
                            selected.properties,
                        )
                    }
                    if (!biosSetupMode) {
                        image =
                            Armsx2Content.stage(
                                this@Armsx2RuntimeProbeActivity,
                                Uri.parse(requireNotNull(current).fileUri),
                                current.cartridgeId,
                            ) {
                                job.ensureActive()
                            }
                    }
                    job.ensureActive()
                    com.swordfish.lemuroid.app.shared.game.NativeSessionJournal.begin(
                        this@Armsx2RuntimeProbeActivity,
                        "PS2",
                        data.gameRoot,
                    )
                    check(
                        NativeApp.loadSelected(selected.directory, Armsx2Engine.coreName()),
                    ) { NativeApp.loadStatus() }
                    loaded = true
                    if (biosSetupMode) return@withContext
                    val missing = Armsx2Storage.missingRequirements(data, requireNotNull(image))
                    missingBios = "BIOS" in missing
                    check(missing.isEmpty()) { "PS2 prerequisites missing: ${missing.joinToString()}" }
                    NativeApp.setupSdlHost(this@Armsx2RuntimeProbeActivity)
                    NativeApp.allowPrivateRoot(data.globalRoot)
                    NativeApp.setFreeSoftwareNotice(
                        getSharedPreferences(
                            "armsx2-runtime",
                            MODE_PRIVATE,
                        ).getBoolean("show_free_software_notice", false),
                    )
                    NativeApp.initialize(data.gameRoot.absolutePath, data.bios.absolutePath, Build.VERSION.SDK_INT)
                    job.ensureActive()
                    NativeApp.setCustomVulkanDriver("", "", "", "")
                    NativeApp.setAffinityMode(0) // Let the Samsung scheduler place threads; no Adreno assumptions.
                    val renderer = getSharedPreferences("armsx2-runtime", MODE_PRIVATE).getString("renderer", "Vulkan")
                    if (renderer == "OpenGL") NativeApp.renderOpenGL() else NativeApp.renderVulkan()
                    ready = true
                }
                if (biosSetupMode) {
                    message = "Select your user-owned PS2 BIOS. No game will be started."
                    importBios.launch(arrayOf("*/*"))
                    return@launch
                }
                startIfReady()
                while (!leaving) {
                    if (ready && started) {
                        report = "engine=${engine?.id}\ndriver_selection=STOCK\n" + NativeApp.getCustomDriverStatus() +
                            "\npresented_frames=${NativeApp.getPresentedFrameCount()}\n" + NativeApp.getPerfSnapshot() +
                            "\ngameplay=UNVERIFIED"
                        withContext(
                            Dispatchers.IO,
                        ) { File(requireNotNull(paths).gameRoot, "last-report.txt").writeText(report.take(16384)) }
                        if (NativeApp.getPresentedFrameCount() > 0 && !failed) {
                            message = ""
                            EngineSelectionPreferences(
                                this@Armsx2RuntimeProbeActivity,
                            ).recordPresentation(requireNotNull(current))
                        }
                    }
                    delay(1000)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                failed = true
                message =
                    if (missingBios) {
                        "PS2 BIOS is missing. Use Import PS2 BIOS to select your own BIOS file."
                    } else {
                        "ARMSX2 could not start: ${error.message ?: error.javaClass.simpleName}"
                    }
                val bootError = if (loaded) runCatching { NativeApp.getLastBootError() }.getOrDefault("") else ""
                report = "$message\n$bootError"
                openMenu()
            }
        }
    }

    private fun retryArmsx2() {
        game?.let { current ->
            if (EngineSelectionPreferences(this).setPreferred(
                    GameSystem.findById(current.systemId),
                    "armsx2-android",
                    current,
                )
            ) {
                reopenEngine = "armsx2-android"
                restart()
            }
        }
    }

    private fun usePlay() {
        game?.let { current ->
            if (EngineSelectionPreferences(this).setPreferred(
                    GameSystem.findById(current.systemId),
                    CoreID.PLAY_MODERN.coreName,
                    current,
                )
            ) {
                reopenEngine = CoreID.PLAY_MODERN.coreName
                restart()
            }
        }
    }

    private fun restart() {
        reopen = true
        leave()
    }

    private var reopen = false
    private var reopenEngine: String? = null

    private fun startIfReady() {
        val disc = image ?: return
        if (!ready || started || leaving || !hostForeground || menu || !surface.holder.surface.isValid) return
        NativeApp.onNativeSurfaceCreated()
        NativeApp.onNativeSurfaceChanged(surface.holder.surface, surface.width, surface.height)
        surface.display?.refreshRate?.let { NativeApp.setDisplayRefreshRate(it) }
        started = true
        vm =
            Thread({
                val outcome = runCatching { NativeApp.runVMThread(disc.absolutePath) }
                runOnUiThread {
                    if (!leaving) {
                        failed = true
                        report = "ARMSX2_VM_RETURNED=${outcome.getOrDefault(false)}\n" + NativeApp.getLastBootError()
                        message = "ARMSX2 session ended. Copy the report or return to Shelf."
                        openMenu()
                    }
                }
            }, "cartdeck-armsx2-vm").also { it.start() }
    }

    private fun touchInput(events: List<InputEvent>) {
        if (menu || !hostForeground || leaving) return
        events.forEach {
            when (it) {
                is InputEvent.Button ->
                    if (it.id == KeyEvent.KEYCODE_BUTTON_MODE) {
                        if (!it.pressed) openMenu()
                    } else {
                        touch.key(it.id, it.pressed)
                    }
                is InputEvent.DiscreteDirection -> touch.direction(it.id, it.direction.x, it.direction.y)
                is InputEvent.ContinuousDirection -> touch.direction(it.id, it.direction.x, it.direction.y)
            }
        }
        sendInput()
    }

    private fun sendInput() {
        if (!ready || !started) return
        val base = Xbox360Pad.merged(hardware, touch)
        val accessory = Xbox360Pad.values(grip)
        val pad =
            IntArray(
                24,
            ) { if (kotlin.math.abs(base[it]) >= kotlin.math.abs(accessory[it])) base[it] else accessory[it] }
        val keys =
            intArrayOf(
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_BUTTON_SELECT,
                KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_BUTTON_THUMBL, KeyEvent.KEYCODE_BUTTON_THUMBR,
            )
        keys.forEachIndexed { index, code ->
            Armsx2PadMapping.button(
                code,
                pad[index] != 0,
            )?.let { NativeApp.setPadButton(it.nativeCode, it.range, it.pressed) }
        }
        val values =
            Armsx2PadMapping.trigger(pad[14] / 255f, false).let { listOf(it) } +
                Armsx2PadMapping.trigger(pad[15] / 255f, true) +
                Armsx2PadMapping.stick((pad[16] + pad[18]) / 32767f, -(pad[17] + pad[19]) / 32767f, false) +
                Armsx2PadMapping.stick((pad[20] + pad[22]) / 32767f, -(pad[21] + pad[23]) / 32767f, true)
        values.forEach { NativeApp.setPadButton(it.nativeCode, it.range, it.pressed) }
    }

    private fun releaseInput() {
        hardware.clear()
        touch.clear()
        grip.clear()
        slots.clear()
        sendInput()
    }

    private fun syncPause() {
        if (!ready || !started) return
        pauseGate.transition(hostForeground, menu, failed, leaving)?.let { shouldPause ->
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
                    menu = true
                    pauseGate.retryResumeAfterFailure()
                }
            }
        }
    }

    private fun openMenu() {
        if (menu || leaving) return
        menu = true
        releaseInput()
        syncPause()
    }

    private fun closeMenu() {
        physicalMapping.discard()
        if (!menu || failed || leaving) return
        menu = false
        syncPause()
        startIfReady()
    }

    private val physicalMapping by lazy {
        NativeControllerMappingSession(this, lifecycleScope, TouchControllerID.PSX_DUALSHOCK) { device -> 0 }
    }

    private val menuEdge = ControllerMenuEdge()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isCanceled) {
            menuEdge.clear()
            physicalMapping.cancelInput()
            releaseInput()
            return true
        }
        val mappedKey = physicalMapping.gameplayKey(event)
        if (event.keyCode in setOf(KeyEvent.KEYCODE_BUTTON_MODE, KeyEvent.KEYCODE_MENU) ||
            mappedKey == KeyEvent.KEYCODE_BUTTON_MODE
        ) {
            if (menuEdge.handle(event)) {
                if (menu) closeMenu() else openMenu()
            }
            return true
        }
        if (menu && physicalMapping.captureKey(event)) return true
        if (!menu && !physicalMapping.acceptsGameplay(event)) return true
        if (!menu && hostForeground && event.action in setOf(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP) &&
            hardware.key(mappedKey, event.action == KeyEvent.ACTION_DOWN)
        ) {
            sendInput()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (menu) {
            physicalMapping.observeMotion(event)
            return true
        }
        if (!physicalMapping.acceptsMotion(event)) return true
        if (!menu && hostForeground && event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            hardware.motion({ event.getAxisValue(it) }, event.device)
            sendInput()
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (ready && started) NativeApp.onNativeSurfaceCreated()
        startIfReady()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        viewportAudit.observe(width, height)
        if (ready && started && holder.surface.isValid) NativeApp.onNativeSurfaceChanged(holder.surface, width, height)
        startIfReady()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        viewportAudit.observe(0, 0)
        releaseInput()
        if (ready) NativeApp.onNativeSurfaceDestroyed()
    }

    override fun onResume() {
        super.onResume()
        physicalMapping.start()
        hostForeground = true
        presence.start { devices ->
            menuEdge.retainDevices(devices.map { it.id }.toSet())
            physicalMapping.retainDevices(devices.map { it.id }.toSet())
            releaseInput()
            connected = devices.isNotEmpty()
        }
        syncPause()
        startIfReady()
    }

    override fun onPause() {
        physicalMapping.stop()
        menuEdge.clear()
        hostForeground = false
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
        ).setPrimaryClip(
            ClipData.newPlainText(
                "CARTDECK ARMSX2",
                (report + "\n" + viewportAudit.summary()).take(16384),
            ),
        )
    }

    private fun leave() {
        if (leaving) return
        leaving = true
        preparation.requestCancellation()
        releaseInput()
        lifecycleScope.launch {
            preparation.awaitCleanup()
            val shutdown =
                Thread(
                    { if (loaded && !biosSetupMode) runCatching { NativeApp.shutdown() } },
                    "cartdeck-armsx2-stop",
                ).also { it.start() }
            withContext(Dispatchers.IO) { shutdown.join(8000) }
            paths?.let { File(it.gameRoot, "last-report.txt").writeText(report.take(16384)) }
            if (!shutdown.isAlive) {
                com.swordfish.lemuroid.app.shared.game.NativeSessionJournal.finish(
                    this@Armsx2RuntimeProbeActivity,
                )
            }
            setResult(
                if (failed || shutdown.isAlive) BaseGameActivity.RESULT_ERROR else Activity.RESULT_OK,
                Intent().putExtra(Armsx2BiosRecovery.REQUIRED, missingBios)
                    .putExtra("cartdeck.reopen", reopen)
                    .putExtra("cartdeck.reopen.engine", reopenEngine)
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR, report)
                    .putExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME, game)
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
        presence.stop()
        super.onDestroy()
        Thread(
            {
                val stop =
                    Thread({
                        if (!leaving && loaded && !biosSetupMode) runCatching { NativeApp.shutdown() }
                    }).also { it.start() }
                stop.join(8000)
                Process.killProcess(Process.myPid())
            },
            "cartdeck-armsx2-process-stop",
        ).start()
    }
}
