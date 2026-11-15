package com.swordfish.lemuroid.app.debug.ps3

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
import com.swordfish.touchinput.radial.settings.TouchControllerID
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
                                check(pending.renameTo(File(storage.guest, "PS3UPDAT.PUP")))
                            } else {
                                val fd =
                                    contentResolver.openFileDescriptor(uri, "r")
                                        ?: error("Firmware cannot be opened")
                                check(Ps3Native.install(fd.detachFd(), true)) {
                                    "PS3 firmware validation or installation failed"
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
                            TouchControllerID.PSX_DUALSHOCK,
                            connected,
                            game,
                        ) { viewport ->
                            if (viewport.width() > 0 && viewport.height() > 0) {
                                surface.layoutParams =
                                    FrameLayout.LayoutParams(viewport.width(), viewport.height()).apply {
                                        leftMargin = viewport.left
                                        topMargin = viewport.top
                                    }
                            }
                        }
                        if (loading && !menu) {
                            CartdeckLoadingScreen(message)
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
                                    ),
                                onResume = {
                                    if (!failure) {
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
                                XboxMenuControls(settings, TouchControllerID.PSX_DUALSHOCK) { orientation, edit ->
                                    lifecycleScope.launch {
                                        settings.storeSettings(
                                            TouchControllerID.PSX_DUALSHOCK,
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
                while (!leaving && loaded) {
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
                    if (retro) report += withContext(Dispatchers.IO) { Ps3Libretro.nativeReport() }
                    withContext(Dispatchers.IO) { File(storage.root, "last-report.txt").writeText(report) }
                    message = com.armsx3.core.ProgressRepository.message.ifBlank { "Compiling PS3 modules" }
                    val presented = retro && Regex("presented_image_callbacks=[1-9][0-9]*").containsMatchIn(report)
                    if (framePeriod > 0 || presented) loading = false
                    if (booting && state == 5) error("PS3 guest entered a frozen state")
                }
            }.onFailure(::fail)
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
                    if (retro) {
                        if (media.isDirectory) {
                            val direct = File(media, "PARAM.SFO")
                            media = if (direct.isFile) direct else File(media, "PS3_GAME/PARAM.SFO")
                        }
                        check(media.isFile) { "PS3 executable metadata is missing" }
                        val result =
                            Ps3Libretro.nativeRun(
                                surface.holder.surface,
                                storage.root.path,
                                media.path,
                                0,
                                requireNotNull(coreLibrary).path,
                                media.parentFile!!.path,
                            )
                        if (!leaving) error(result)
                        return@withContext
                    }
                    if (media.extension.equals("pkg", true)) {
                        check(
                            Ps3Native.install(media.inputStream().fdCompat(), false),
                        ) { "PS3 package installation failed" }
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
        if (loaded && booting) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { if (retro) Ps3Libretro.nativePause(value) else Ps3Native.pause(value) }
            }
        }
    }

    private fun release() {
        pressed.clear()
        sticks.clear()
        if (loaded) {
            if (retro) {
                for (id in 0..15) Ps3Libretro.nativeButton(id, false)
                for (axis in 0..3) Ps3Libretro.nativeAxis(axis, 0)
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
                    key(0, event.id, event.pressed)
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
            key(0, KeyEvent.KEYCODE_DPAD_LEFT, x < -.5f)
            key(0, KeyEvent.KEYCODE_DPAD_RIGHT, x > .5f)
            key(0, KeyEvent.KEYCODE_DPAD_UP, y > .5f)
            key(0, KeyEvent.KEYCODE_DPAD_DOWN, y < -.5f)
        } else {
            axis(0, source - 1, x, y)
        }
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
        val keys = pressed[port].orEmpty()
        val first = mapOf(109 to 1, 106 to 2, 107 to 4, 108 to 8, 19 to 16, 22 to 32, 20 to 64, 21 to 128)
        val second = mapOf(104 to 1, 105 to 2, 102 to 4, 103 to 8, 100 to 16, 97 to 32, 96 to 64, 99 to 128)
        val a = sticks[port] ?: intArrayOf(128, 128, 128, 128)
        if (retro) {
            if (port != 0) return
            val codes = intArrayOf(97, 99, 109, 108, 19, 20, 21, 22, 96, 100, 102, 103, 104, 105, 106, 107)
            codes.forEachIndexed { id, code -> Ps3Libretro.nativeButton(id, code in keys) }
            a.forEachIndexed { id, value ->
                Ps3Libretro.nativeAxis(id, ((value - 128) * 258).coerceIn(-32767, 32767))
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode in setOf(KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_BUTTON_MODE)) {
            if (event.action == KeyEvent.ACTION_UP) openMenu()
            return true
        }
        if (!menu && event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            val port = presence.connected().indexOfFirst { it.id == event.deviceId }.coerceIn(0, 6)
            key(port, event.keyCode, event.action == KeyEvent.ACTION_DOWN)
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
        presence.start {
            connected = it.isNotEmpty()
            release()
        }
        pause(menu)
        boot()
    }

    override fun onPause() {
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

    private fun leave() {
        if (leaving) return
        leaving = true
        release()
        lifecycleScope.launch {
            if (loaded) {
                withContext(
                    Dispatchers.IO,
                ) { runCatching { if (retro) Ps3Libretro.nativeStop() else Ps3Native.stop() } }
            }
            NativeSessionJournal.finish(this@Ps3RuntimeActivity)
            setResult(
                if (failure) BaseGameActivity.RESULT_ERROR else Activity.RESULT_OK,
                Intent().putExtra(
                    BaseGameActivity.PLAY_GAME_RESULT_GAME,
                    game,
                ).putExtra(BaseGameActivity.PLAY_GAME_RESULT_ERROR, if (failure) message else null),
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
