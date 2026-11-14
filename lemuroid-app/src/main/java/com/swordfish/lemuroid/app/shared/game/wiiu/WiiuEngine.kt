package com.swordfish.lemuroid.app.shared.game.wiiu

import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import java.io.File
import java.util.Properties

/** Complete core/host pairs use the existing immutable, hashed engine store. */
object WiiuEngine {
    const val CORE = "libcemu_libretro_android.so"
    const val HOST = "libcartdeckwiiuvulkanhost.so"
    const val MANIFEST = "cartdeck/cemu.properties"
    const val UPSTREAM = "444c585202302a7f4b319bafb4cf72156a8cac8b"
    val engineeringBuild: Boolean
        get() = BuildConfig.DEBUG && BuildConfig.APPLICATION_ID == "dev.prestwich.cartdeck.wiiulab.debug"

    fun store(files: File) =
        XboxEngineStore(
            File(files, "native-engines/wiiu"),
            CORE,
            setOf(CORE, HOST),
            "cemu.properties",
        )

    fun install(context: Context): XboxEngineStore.Selection {
        check(engineeringBuild) { "Cemu is available only in the opt-in Wii U engineering build" }
        check(Build.VERSION.SDK_INT >= 28 && "arm64-v8a" in Build.SUPPORTED_ABIS) {
            "Cemu requires Android ARM64 API 28 or newer"
        }
        val properties = Properties().apply { context.assets.open(MANIFEST).use { load(it) } }
        check(properties.getProperty("upstream.revision") == UPSTREAM) { "Unexpected Cemu source revision" }
        return store(context.filesDir).install(
            File(context.applicationInfo.nativeLibraryDir),
            properties,
            context.assets.open("cartdeck/cemu-licenses.zip").use { it.readBytes() },
        )
    }
}
