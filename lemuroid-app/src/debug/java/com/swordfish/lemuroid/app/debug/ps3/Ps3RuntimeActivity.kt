package com.swordfish.lemuroid.app.debug.ps3

import com.swordfish.lemuroid.app.shared.input.ControllerMenuEdge
import com.swordfish.lemuroid.app.shared.input.handle
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Process
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import com.swordfish.lemuroid.app.mobile.feature.game.XboxGameControls
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuLink
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxGameMenu
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxMenuCapabilities
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.XboxMenuControls
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.CartdeckLoadingScreen
import com.swordfish.lemuroid.app.shared.game.GameProcessLock
import com.swordfish.lemuroid.app.shared.game.NativeSessionJournal
import com.swordfish.lemuroid.app.shared.game.ps3.Ps3Content
import com.swordfish.lemuroid.app.shared.game.ps3.Ps3Engine
import com.swordfish.lemuroid.app.shared.input.GameplayControllerPresence
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.ps3.Ps3Storage
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.inputevents.InputEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** One cartridge, one isolated process; native engine owns real Vulkan and Oboe execution. */
class Ps3RuntimeActivity : ComponentActivity(), SurfaceHolder.Callback {
    private lateinit var surface: SurfaceView
    private lateinit var storage: Ps3Storage
    private var game: Game? = null

    @Volatile private var runActive = false
    private var loaded = false
    private var ready = false
    private var booting = false
    private var leaving = false
    private var foreground = false
    private var failure = false
    private var menu by mutableStateOf(false)
    private var loading by mutableStateOf(true)
    private var connected by mutableStateOf(false)
    private var message by mutableStateOf("Preparing PlayStation 3")
    private var coreLibrary: File? = null
    private var activeRevision = ""
    private var report = "PS3_PREPARING; gameplay=UNVERIFIED"
    private val touchKeys = mutableSetOf<Int>()
    private val touchSticks = intArrayOf(128, 128, 128, 128)
    private val gripSlots = mutableMapOf<Int, Set<Int>>()
    private val rawPressed = mutableMapOf<Int, MutableSet<Int>>()
    private val remaps = mutableMapOf<Int, Map<Int, Int>>()
    private val menuShortcuts = mutableMapOf<Int, Set<Int>>()
    private val inputManager by lazy {
        com.swordfish.lemuroid.app.shared.input.InputDeviceManager(
            this,
            dagger.Lazy { com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper.getSharedPreferences(this) },
        )
    }
    private val pressed = mutableMapOf<Int, MutableSet<Int>>()
    private val sticks = mutableMapOf<Int, IntArray>()
    private val presence by lazy { GameplayControllerPresence(this) }
    private val settings by lazy {
        TouchControllerSettingsManager(getSharedPreferences("ps3-controller-layout", MODE_PRIVATE))
    }
    private var engine = "armsx3"
    private val retro: Boolean get() = engine == "rpcs3-libretro"
    private val firmware =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null && loaded && !booting) {
                lifecycleScope.launch {
                    loading = true
                    menu = false
                    message = "Validating and installing PS3 firmware"
                    runCatching {
                        withContext(Dispatchers.IO) {
                            if (retro) {
                                check(storage.guest.mkdirs() || storage.guest.isDirectory)
                                val pending = File(storage.guest, ".firmware.pending")
                                contentResolver.openInputStream(uri)?.use { input ->
                                    pending.outputStream().use { output ->
                                        val buffer = ByteArray(65536)
                                        var total = 0L
                                        while (true) {
                                            val count = input.read(buffer)
                                            if (count < 0) break
                                            total += count
                                            require(total <= 1073741824) { "Firmware exceeds 1 GiB" }
                                            output.write(buffer, 0, count)
                                        }
                                    }
                                } ?: error("Firmware cannot be opened")
                                val header = pending.inputStream().use { it.readNBytesCompat(8) }
                                require(
                                    header.size == 8 && header.copyOfRange(0, 5).toString(Charsets.US_ASCII) == "SCEUF",
                                ) {
                                    "Not a PS3 PUP firmware file"
                                }
                                if (!storage.hasPendingFirmwareReplacement()) storage.beginFirmwareReplacement()
                                check(pending.renameTo(File(storage.guest, "PS3UPDAT.PUP")))
                            } else {
                                val fd =
                                    contentResolver.openFileDescriptor(uri, "r")
                                        ?: error("Firmware cannot be opened")
                                storage.beginFirmwareReplacement()
                                storage.markFirmwareInstallStarted()
                                var installed = false
                                try {
                                    installed = Ps3Native.install(fd.detachFd(), true)
                                    check(installed) { "PS3 firmware validation or installation failed" }
                                } finally {
                                    storage.finishFirmwareReplacement(installed)
                                }
                            }
                        }
                    }.onSuccess {
                        message = "Firmware accepted. Return to Shelf and reopen to complete native validation."
                    }
                        .onFailure { fail(it) }
                    loading = false
                    menu = true
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
        engine = intent.getStringExtra("cartdeck.engine") ?: "armsx3"
        storage = Ps3Storage(filesDir, engine)
        surface = SurfaceView(this).also { it.holder.addCallback(this) }
        val root = FrameLayout(this)
        root.addView(surface, FrameLayout.LayoutParams(-1, -1))
        root.addView(
            ComposeView(this).apply {
                setContent {
                    Box(Modifier.fillMaxSize()) {
                        XboxGameControls(
                            ::touch,
                            settings,
                            Ps3Engine.CONTROLLER,
                            connected,
                            game,
                            onGripPressed = { slot, down, codes ->
                                if (down && !menu && this@Ps3RuntimeActivity.foreground) {
                                    gripSlots[slot] = codes
                                } else {
                                    gripSlots.remove(
                                        slot,
                                    )
                                }
                                send(0)
                            },
                        ) { viewport ->
                            if (viewport.width() > 0 && viewport.height() > 0) {
                                surface.layoutParams =
                                    FrameLayout.LayoutParams(viewport.width(), viewport.height()).apply {
                                        leftMargin = viewport.left
                                        topMargin = viewport.top
                                    }
                            }
                        }
                        if (loading && !menu) CartdeckLoadingScreen(message)
                        if (!menu && (loading || connected)) {
                            androidx.compose.material3.TextButton(
                                onClick = ::openMenu,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd),
                            ) { androidx.compose.material3.Text("Menu") }
                        }
                        if (menu) {
                            XboxGameMenu(
                                title = game?.displayName() ?: "PlayStation 3 setup", engineInfo = "$engine · $report",
                                capabilities =
                                    XboxMenuCapabilities(
                                        false,
                                        false,
                                        Ps3Engine.store(filesDir, engine).canRollback(),
                                        resume = game != null && !failure,
                                    ),
                                onResume = {
                                    if (!failure && game != null) {
                                        menu = false
                                        pause(false)
                                        boot()
                                    }
                                },
                                onReturnToShelf = ::leave,
                                onCopyReport = {
                                    (
                                        getSystemService(
                                            CLIPBOARD_SERVICE,
                                        ) as ClipboardManager
                                    ).setPrimaryClip(ClipData.newPlainText("PS3 engine report", report))
                                }, onProbe = {}, onPreviousEngine = {
                                    if (Ps3Engine.store(filesDir, engine).rollback()) {
                                        message = "Previous engine selected. Reopen from Shelf; saves retained."
                                    }
                                }, console = "PlayStation 3",
                                storageHelp =
                                    "PS3 saves use the selected virtual user's HDD. " +
                                        "Save states are unavailable. $message",
                                recovery = {
                                    GameMenuLink("PS3 setup, profiles and save management") {
                                        // The same in-app setup screen used by main settings.
                                        startActivity(
                                            Intent(
                                                this@Ps3RuntimeActivity,
                                                Ps3ManagementActivity::class.java,
                                            ).putExtra("engine", engine),
                                        )
                                    }
                                    if (!booting) {
                                        GameMenuLink(
                                            "Import PS3 firmware",
                                        ) { firmware.launch(arrayOf("*/*")) }
                                    }
                                },
                            ) {
                                XboxMenuControls(settings, Ps3Engine.CONTROLLER) { orientation, edit ->
                                    lifecycleScope.launch {
                                        settings.storeSettings(
                                            Ps3Engine.CONTROLLER,
                                            orientation,
                                            edit,
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
                override fun handleOnBackPressed() = openMenu()
            },
        )
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    GameProcessLock.acquireExclusive(this@Ps3RuntimeActivity)
                    Ps3Engine.readiness(this@Ps3RuntimeActivity, engine)?.let { error(it) }
                    val selected = Ps3Engine.install(this@Ps3RuntimeActivity, engine)
                    coreLibrary = selected.core
                    activeRevision = selected.properties.getProperty("revision")
                    if (storage.profiles().isEmpty()) storage.create("Player 1")
                    val profile = storage.profiles().first { it.selected }
                    check(storage.guest.mkdirs() || storage.guest.isDirectory)
                    if (storage.firmwareInstallInterrupted()) {
                        storage.finishFirmwareReplacement(false)
                        val pup = File(storage.guest, "PS3UPDAT.PUP")
                        if (pup.exists()) check(pup.renameTo(File(storage.root, "interrupted-PS3UPDAT.PUP")))
                    }
                    val config = File(storage.guest, "config").apply { mkdirs() }
                    val path = storage.guest.canonicalPath.replace("'", "''") + "/"
                    File(config, "vfs.yml").writeText("\"$(EmulatorDir)\": '$path'\n")
                    NativeSessionJournal.begin(this@Ps3RuntimeActivity, "PS3", storage.root)
                    if (retro) {
                        File(storage.guest, "cartdeck-user.txt").writeText(profile.id)
                        Ps3Libretro.load()
                        Ps3Libretro.nativePrepare()
                    } else {
                        Ps3Native.loadBridge()
                        check(Ps3Native.load(requireNotNull(coreLibrary).path))
                        check(Ps3Native.initialize(storage.guest.path, profile.id)) { "PS3 initialization failed" }
                        Thread({ Ps3Native.process(false) }, "cartdeck-ps3-main-processor").start()
                        Thread({ Ps3Native.process(true) }, "cartdeck-ps3-compiler").start()
                    }
                    loaded = true
                }
                ready = storage.firmwareInstalled() || (retro && File(storage.guest, "PS3UPDAT.PUP").isFile)
                if (!ready) {
                    loading = false
                    message = "PS3 firmware is missing. Import firmware in the game menu or PlayStation 3 setup."
                    menu = true
                } else if (game != null) {
                    boot()
                } else {
                    loading = false
                    menu = true
                }
                while (!leaving && loaded && !failure) {
                    delay(500)
                    val state = withContext(Dispatchers.IO) { if (retro) -1 else Ps3Native.state() }
                    val framePeriod = withContext(Dispatchers.IO) { if (retro) 0L else Ps3Native.framePeriod() }
                    report =
                        buildString {
                            appendLine("engine=$engine")
                            appendLine("source=$activeRevision")
                            appendLine("frame_period_ns=$framePeriod")
                            appendLine("native_state=$state")
                            appendLine("firmware=${storage.firmwareInstalled()}")
                            appendLine("profile=${storage.profiles().firstOrNull { it.selected }?.id}")
                            appendLine("gameplay=UNVERIFIED")
                        }
                    if (retro) {
                        report += withContext(Dispatchers.IO) { Ps3Libretro.nativeReport() }
                        withContext(Dispatchers.IO) { completeFirmwareInstall(report) }
                    }
                    withContext(Dispatchers.IO) { File(storage.root, "last-report.txt").writeText(report) }
                    message = com.armsx3.core.ProgressRepository.message.ifBlank { "Compiling PS3 modules" }
                    val presented = retro && Regex("presented_image_callbacks=[1-9][0-9]*").containsMatchIn(report)
                    if (framePeriod > 0 || presented) loading = false
                    if (booting && state == 5) error("PS3 guest entered a frozen state")
                }
            }.onFailure(::fail)
        }
    }

    private fun completeFirmwareInstall(nativeReport: String) {
        if (!storage.hasPendingFirmwareReplacement()) return
        val status =
            Regex("(?m)^firmware_install_status=(-?[0-9]+)$")
                .find(nativeReport)?.groupValues?.get(1)?.toIntOrNull() ?: return
        if (status == 0) return
        storage.finishFirmwareReplacement(status == 1)
        if (status < 0) {
            val pup = File(storage.guest, "PS3UPDAT.PUP")
            if (pup.exists()) check(pup.renameTo(File(storage.root, "rejected-PS3UPDAT.PUP")))
            error("Native PS3 firmware installation failed; previous firmware was restored")
        }
    }

    private fun boot() {
        if (game == null || !loaded || !ready || booting || !foreground ||
            !surface.holder.surface.isValid || menu
        ) {
            return
        }
        booting = true
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (!retro) check(Ps3Native.surface(surface.holder.surface, 0, surface.width, surface.height))
                    val current = game ?: error("Open a PS3 cartridge from Shelf")
                    var media = Ps3Content.stage(this@Ps3RuntimeActivity, current, storage.root)
                    var packageTitle: String? = null
                    var packageReceipt: File? = null
                    if (media.extension.equals("pkg", true)) {
                        packageTitle = com.swordfish.lemuroid.lib.storage.ps3.Ps3Metadata.pkg(
                            media.inputStream().use { it.readNBytesCompat(128) }, media.length(),
                        ) ?: error("Invalid PS3 package")
                        val digest = java.security.MessageDigest.getInstance("SHA-256")
                        media.inputStream().use { input ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                digest.update(buffer, 0, count)
                            }
                        }
                        val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                        packageReceipt = File(storage.root, "installed-packages/$hash")
                        val installed = File(storage.guest, "dev_hdd0/game/$packageTitle/PARAM.SFO")
                        if (packageReceipt.isFile && installed.isFile &&
                            File(installed.parentFile, "USRDIR/EBOOT.BIN").isFile
                        ) {
                            media = if (retro) installed else File(installed.parentFile, "USRDIR/EBOOT.BIN")
                        }
                    }
                    if (retro) {
                        if (media.isDirectory) {
                            val direct = File(media, "PARAM.SFO")
                            media = if (direct.isFile) direct else File(media, "PS3_GAME/PARAM.SFO")
                        }
                        check(media.isFile) { "PS3 executable metadata is missing" }
                        if (storage.hasPendingFirmwareReplacement()) storage.markFirmwareInstallStarted()
                        runActive = true
                        val result =
                            try {
                                Ps3Libretro.nativeRun(
                                    surface.holder.surface,
                                    storage.root.path,
                                    media.path,
                                    0,
                                    requireNotNull(coreLibrary).path,
                                    media.parentFile!!.path,
                                )
                            } finally {
                                try {
                                    completeFirmwareInstall(Ps3Libretro.nativeReport())
                                } finally {
                                    runActive = false
                                }
                            }
                        if (!leaving && packageTitle != null &&
                            Regex("(?m)^package_installed=1$").containsMatchIn(Ps3Libretro.nativeReport())
                        ) {
                            val installed = File(storage.guest, "dev_hdd0/game/$packageTitle/PARAM.SFO")
                            check(installed.isFile && File(installed.parentFile, "USRDIR/EBOOT.BIN").isFile) {
                                "Package installed additional content; open its base game cartridge."
                            }
                            packageReceipt?.let {
                                check(it.parentFile!!.mkdirs() || it.parentFile!!.isDirectory)
                                it.writeText("native_package_install=SUCCESS")
                            }
                            withContext(Dispatchers.Main) { leave(reopen = true) }
                            return@withContext
                        }
                        if (!leaving) error(result)
                        return@withContext
                    }
                    if (media.extension.equals("pkg", true)) {
                        check(
                            Ps3Native.install(media.inputStream().fdCompat(), false),
                        ) { "PS3 package installation failed" }
                        packageReceipt?.let {
                            check(it.parentFile!!.mkdirs() || it.parentFile!!.isDirectory)
                            it.writeText("native_package_install=SUCCESS")
                        }
                        val id =
                            com.swordfish.lemuroid.lib.storage.ps3.Ps3Metadata.pkg(
                                media.inputStream().use {
                                    it.readNBytesCompat(128)
                                },
                                media.length(),
                            ) ?: error("Invalid PS3 package")
                        media = File(storage.guest, "dev_hdd0/game/$id/USRDIR/EBOOT.BIN")
                    }
                    val result = Ps3Native.boot(media.path)
                    check(result == 0) {
                        "PS3 boot failed (native result $result). " +
                            "Check firmware, media decryption and required user licences."
                    }
                }
            }.onFailure(::fail)
        }
    }

    private fun fail(error: Throwable) {
        failure = true
        loading = false
        message = error.message?.take(1024) ?: "PS3 engine failed; saves were retained."
        report = message
        menu = true
    }

    private fun openMenu() {
        menu = true
        release()
        pause(true)
    }

    private fun pause(value: Boolean) {
        if (loaded && booting && !leaving) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { if (retro) Ps3Libretro.nativePause(value) else Ps3Native.pause(value) }
            }
        }
    }

    private fun release() {
        pressed.clear()
        sticks.clear()
        touchKeys.clear()
        touchSticks.fill(128)
        gripSlots.clear()
        rawPressed.clear()
        if (loaded) {
            if (retro) {
                for (port in 0..6) for (id in 0..15) Ps3Libretro.nativeButton(port, id, false)
                for (port in 0..6) for (axis in 0..3) Ps3Libretro.nativeAxis(port, axis, 0)
            } else {
                for (port in 0..6) Ps3Native.pad(port, 0, 0, 128, 128, 128, 128)
            }
        }
    }

    private fun touch(events: List<InputEvent>) {
        if (menu || !foreground) return
        for (event in events) when (event) {
            is InputEvent.Button ->
                if (event.id == KeyEvent.KEYCODE_BUTTON_MODE) {
                    if (!event.pressed) openMenu()
                } else {
                    if (event.pressed) touchKeys.add(event.id) else touchKeys.remove(event.id)
                    send(0)
                }
            is InputEvent.ContinuousDirection -> touchDirection(event.id, event.direction.x, event.direction.y)
            is InputEvent.DiscreteDirection -> touchDirection(event.id, event.direction.x, event.direction.y)
        }
    }

    private fun touchDirection(
        source: Int,
        x: Float,
        y: Float,
    ) {
        if (source == 0) {
            val directions =
                mapOf(
                    KeyEvent.KEYCODE_DPAD_LEFT to (x < -.5f),
                    KeyEvent.KEYCODE_DPAD_RIGHT to (x > .5f),
                    KeyEvent.KEYCODE_DPAD_UP to (y > .5f),
                    KeyEvent.KEYCODE_DPAD_DOWN to (y < -.5f),
                )
            directions.forEach { (code, down) -> if (down) touchKeys.add(code) else touchKeys.remove(code) }
        } else if (source in 1..2) {
            touchSticks[(source - 1) * 2] = (128 + x * 127).toInt().coerceIn(0, 255)
            touchSticks[(source - 1) * 2 + 1] = (128 - y * 127).toInt().coerceIn(0, 255)
        }
        send(0)
    }

    private fun key(
        port: Int,
        code: Int,
        down: Boolean,
    ) {
        val keys = pressed.getOrPut(port) { mutableSetOf() }
        if (down) keys.add(code) else keys.remove(code)
        send(port)
    }

    private fun axis(
        port: Int,
        stick: Int,
        x: Float,
        y: Float,
    ) {
        if (stick !in 0..1) return
        val a = sticks.getOrPut(port) { intArrayOf(128, 128, 128, 128) }
        a[stick * 2] = (128 + x * 127).toInt().coerceIn(0, 255)
        a[stick * 2 + 1] = (128 - y * 127).toInt().coerceIn(0, 255)
        send(port)
    }

    private fun send(port: Int) {
        if (!loaded || !booting || menu) return
        val keys = pressed[port].orEmpty() + if (port == 0) touchKeys + gripSlots.values.flatten() else emptySet()
        val first = mapOf(109 to 1, 106 to 2, 107 to 4, 108 to 8, 19 to 16, 22 to 32, 20 to 64, 21 to 128)
        val second = mapOf(104 to 1, 105 to 2, 102 to 4, 103 to 8, 100 to 16, 97 to 32, 96 to 64, 99 to 128)
        val hardware = sticks[port] ?: intArrayOf(128, 128, 128, 128)
        val a =
            IntArray(4) { index ->
                if (port == 0 && kotlin.math.abs(touchSticks[index] - 128) > kotlin.math.abs(hardware[index] - 128)) {
                    touchSticks[index]
                } else {
                    hardware[index]
                }
            }
        if (retro) {
            val codes = intArrayOf(97, 99, 109, 108, 19, 20, 21, 22, 96, 100, 102, 103, 104, 105, 106, 107)
            codes.forEachIndexed { id, code -> Ps3Libretro.nativeButton(port, id, code in keys) }
            a.forEachIndexed { id, value ->
                Ps3Libretro.nativeAxis(port, id, ((value - 128) * 258).coerceIn(-32767, 32767))
            }
            return
        }
        Ps3Native.pad(
            port,
            keys.fold(0) { b, k -> b or (first[k] ?: 0) },
            keys.fold(0) { b, k -> b or (second[k] ?: 0) },
            a[0],
            a[1],
            a[2],
            a[3],
        )
    }

    private val menuEdge = ControllerMenuEdge()

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode in setOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_MODE)) {
            if (menuEdge.handle(event)) {
                if (menu) closeMenu() else openMenu()
            }
            return true
        }
        if (!menu && event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            val port = presence.connected().indexOfFirst { it.id == event.deviceId }.coerceIn(0, 6)
            val raw = rawPressed.getOrPut(event.deviceId) { mutableSetOf() }
            val down = event.action == KeyEvent.ACTION_DOWN
            if (down) raw.add(event.keyCode) else raw.remove(event.keyCode)
            val combo = menuShortcuts[event.deviceId].orEmpty()
            if (combo.isNotEmpty() && raw.containsAll(combo)) {
                openMenu()
                return true
            }
            key(port, remaps[event.deviceId]?.get(event.keyCode) ?: event.keyCode, down)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (!menu && event.isFromSource(InputDevice.SOURCE_JOYSTICK)) {
            val port = presence.connected().indexOfFirst { it.id == event.deviceId }.coerceIn(0, 6)
            axis(port, 0, event.getAxisValue(MotionEvent.AXIS_X), -event.getAxisValue(MotionEvent.AXIS_Y))
            val right = com.swordfish.lemuroid.app.shared.input.ControllerHardwareProfile.rightStickAxes(event.device)
            axis(port, 1, event.getAxisValue(right.first), -event.getAxisValue(right.second))
            key(port, KeyEvent.KEYCODE_DPAD_LEFT, event.getAxisValue(MotionEvent.AXIS_HAT_X) < -.5f)
            key(port, KeyEvent.KEYCODE_DPAD_RIGHT, event.getAxisValue(MotionEvent.AXIS_HAT_X) > .5f)
            key(port, KeyEvent.KEYCODE_DPAD_UP, event.getAxisValue(MotionEvent.AXIS_HAT_Y) < -.5f)
            key(port, KeyEvent.KEYCODE_DPAD_DOWN, event.getAxisValue(MotionEvent.AXIS_HAT_Y) > .5f)
            key(port, KeyEvent.KEYCODE_BUTTON_L2, event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > .5f)
            key(port, KeyEvent.KEYCODE_BUTTON_R2, event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > .5f)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onResume() {
        super.onResume()
        foreground = true
        presence.start { devices ->
            connected = devices.isNotEmpty()
            release()
            lifecycleScope.launch {
                remaps.clear()
                menuShortcuts.clear()
                for (device in devices) {
                    remaps[device.id] =
                        inputManager.getCurrentBindings(device).mapKeys { it.key.keyCode }.mapValues {
                            when (it.value.keyCode) {
                                KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
                                KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
                                KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
                                KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
                                else -> it.value.keyCode
                            }
                        }
                    menuShortcuts[device.id] =
                        inputManager.getCurrentShortcuts(device).firstOrNull {
                            it.type == com.swordfish.lemuroid.app.shared.settings.GameShortcutType.MENU
                        }?.keys.orEmpty()
                }
            }
        }
        pause(menu)
        boot()
    }

    override fun onPause() {
        menuEdge.clear()
        foreground = false
        release()
        pause(true)
        presence.stop()
        super.onPause()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (loaded) {
            if (retro) {
                Ps3Libretro.nativeSurface(holder.surface)
            } else {
                Ps3Native.surface(holder.surface, 0, surface.width, surface.height)
            }
        }
        boot()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int,
    ) {
        if (loaded) {
            if (retro) {
                Ps3Libretro.nativeSurface(holder.surface)
            } else {
                Ps3Native.surface(holder.surface, 1, width, height)
            }
        }
        boot()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        release()
        if (loaded) {
            if (retro) {
                Ps3Libretro.nativeSurface(null)
            } else {
                Ps3Native.surface(null, 2, 0, 0)
            }
        }
    }

    private fun leave(reopen: Boolean = false) {
        if (leaving) return
        leaving = true
        release()
        lifecycleScope.launch {
            val stopped = java.util.concurrent.atomic.AtomicBoolean(!loaded)
            if (loaded) {
                Thread({
                    runCatching {
                        if (retro) {
                            Ps3Libretro.nativeStop()
                            while (runActive) Thread.sleep(50)
                        } else {
                            Ps3Native.stop()
                        }
                    }.onSuccess { stopped.set(true) }
                }, "cartdeck-ps3-shutdown").apply { isDaemon = true }.start()
            }
            val deadline = android.os.SystemClock.elapsedRealtime() + 15000
            while (!stopped.get() && android.os.SystemClock.elapsedRealtime() < deadline) delay(50)
            if (stopped.get()) {
                NativeSessionJournal.finish(this@Ps3RuntimeActivity)
            } else {
                failure = true
                message =
                    "PS3 native shutdown timed out. Existing in-game saves were retained; check the engine report."
                report = "PS3_SHUTDOWN_TIMEOUT; native_save_flush=UNVERIFIED"
                withContext(Dispatchers.IO) { File(storage.root, "last-report.txt").writeText(report) }
            }
            setResult(
                if (failure) BaseGameActivity.RESULT_ERROR else Activity.RESULT_OK,
                Intent().putExtra(
                    BaseGameActivity.PLAY_GAME_RESULT_GAME,
                    game,
                ).putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR, if (failure) message else null)
                    .putExtra("cartdeck.reopen", reopen && !failure)
                    .putExtra("cartdeck.reopen.engine", engine),
            )
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) Process.killProcess(Process.myPid())
    }

    private fun java.io.FileInputStream.fdCompat(): Int = use { android.os.ParcelFileDescriptor.dup(it.fd).detachFd() }

    private fun java.io.InputStream.readNBytesCompat(size: Int): ByteArray {
        val b = ByteArray(size)
        var p = 0
        while (p < size) {
            val n = read(b, p, size - p)
            if (n < 0) break
            p += n
        }
        return b.copyOf(p)
    }
}
