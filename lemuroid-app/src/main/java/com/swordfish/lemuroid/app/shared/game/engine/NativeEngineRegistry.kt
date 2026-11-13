package com.swordfish.lemuroid.app.shared.game.engine

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.swordfish.lemuroid.app.shared.game.EmulatorCompatibilityPreferences
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import java.io.File
import java.util.Properties

/** Describes actual integrated implementations. Unbuilt upstream projects are never selectable engines. */
object NativeEngineRegistry {
    enum class Host { LIBRETRO, HAKUX_SDL3, XENIA_ANDROID }

    data class Engine(
        val id: String,
        val name: String,
        val system: SystemID,
        val host: Host,
        val core: CoreID?,
        val formats: Set<String>,
        val minimumAndroid: Int,
        val architectures: Set<String>,
        val minimumGles: Int,
        val minimumVulkan: Int,
        val firmware: List<String>,
        val supportAssets: String?,
        val stateNamespace: String,
        val saveCompatibility: String,
        val controllers: String,
        val limitations: String,
        val manifest: String? = null,
        val config: SystemCoreConfig? = null,
        val cpuRequirements: String = "Android ABI-compatible core; JIT checked at initialization",
        val audioRequirements: String = "Integrated host audio; verify initialization at runtime",
        val performance: String = "No device benchmark; retain engine-specific settings",
        val rollback: String = "Retain previous versions; never convert engine-specific states",
    )

    data class Hardware(
        val android: Int,
        val architectures: Set<String>,
        val gles: Int,
        val vulkan: Int,
    )

    data class Eligibility(val eligible: Boolean, val reason: String? = null)

    fun engines(system: GameSystem): List<Engine> {
        if (system.nativeBackend == "xenia") {
            return listOf(
                Engine(
                    "xenia-android", "Xenia Edge", system.id, Host.XENIA_ANDROID, null,
                    system.supportedExtensions.toSet(), 29, setOf("arm64-v8a"), 0, 0x00401000,
                    emptyList(), "Pinned XenDroid native ARM64 runtime", "native/xbox360",
                    "Persistent Xbox 360 content; no save states or automatic save conversion",
                    "Xbox 360 dual analog, ABXY, LB/RB, LT/RT, Start/Back; Guide opens CARTDECK menu",
                    "System Vulkan; independentBlend required. Xclipse gameplay is a separate gate. No save states.",
                    "cartdeck/xenia.properties",
                ),
            )
        }
        if (system.nativeBackend == "hakux") {
            return listOf(
                Engine(
                    "hakux", "hakuX", system.id, Host.HAKUX_SDL3, null,
                    system.supportedExtensions.toSet(), 28, setOf("arm64-v8a"), 0, 0x00401000,
                    emptyList(), "Integrated FEX, SDL3 and hakuX dependencies",
                    "native/xbox", "Persistent virtual HDD; no cross-engine save-state conversion",
                    "Xbox pad, analog sticks/triggers, Black/White, Start/Back",
                    "Stock Vulkan driver. GPU compatibility varies; first-frame verification required. No save states.",
                    "cartdeck/hakux.properties",
                ),
            )
        }
        val configs = system.systemCoreConfigs.toMutableList()
        val modern =
            when (system.id) {
                SystemID.PSX -> CoreID.PCSX_REARMED_MODERN
                SystemID.PSP -> CoreID.PPSSPP_MODERN
                else -> null
            }
        if (modern != null && configs.none { it.coreID == modern } && configs.isNotEmpty()) {
            configs += configs.first().copy(coreID = modern, statesVersion = configs.first().statesVersion + 1)
        }
        return configs.map { config -> describe(system, config) }
    }

    private fun describe(
        system: GameSystem,
        config: SystemCoreConfig,
    ): Engine {
        val core = config.coreID
        val native = core in setOf(CoreID.FLYCAST_MODERN, CoreID.DOLPHIN_MODERN, CoreID.PLAY_MODERN)
        val modern = native || core in setOf(CoreID.PCSX_REARMED_MODERN, CoreID.PPSSPP_MODERN)
        return Engine(
            core.coreName, core.coreDisplayName, system.id, Host.LIBRETRO, core,
            system.supportedExtensions.toSet(),
            if (native) {
                28
            } else if (modern) {
                24
            } else {
                21
            },
            if (modern) setOf("arm64-v8a") else config.supportedOnlyArchitectures.orEmpty(),
            when (core) {
                CoreID.PLAY_MODERN -> 0x00030002
                CoreID.FLYCAST_MODERN, CoreID.DOLPHIN_MODERN -> 0x00030000
                else -> 0
            },
            0,
            config.requiredBIOSFiles + config.regionalBIOSFiles.values,
            when (core) {
                CoreID.DOLPHIN_MODERN -> "Matching Dolphin Sys; separate Wii/GameCube User storage"
                CoreID.PPSSPP_MODERN -> "Matching PPSSPP assets"
                else -> null
            },
            "${system.id.dbname}/${config.stateNamespace}",
            "Engine state format is isolated; memory cards/battery saves remain in their existing system locations",
            "Existing ${system.id.dbname} controller profile (${config.controllerConfigs.size} port configurations)",
            when (core) {
                CoreID.PLAY_MODERN -> "GLES 3.2, BIOS-free Play!, variable compatibility; no states."
                CoreID.DOLPHIN_MODERN -> "Shared Wii/GameCube binary; separate input and storage."
                CoreID.FLYCAST_MODERN -> "Complete disc families required; CDI settings preserved."
                else -> "Core-specific compatibility; renderer negotiation remains enforced by the existing host."
            },
            when (core) {
                CoreID.FLYCAST_MODERN -> "cartdeck/flycast.properties"
                CoreID.DOLPHIN_MODERN -> "cartdeck/dolphin-modern.properties"
                CoreID.PLAY_MODERN -> "cartdeck/play-modern.properties"
                CoreID.PCSX_REARMED_MODERN -> "cartdeck/pcsx-rearmed-modern.properties"
                CoreID.PPSSPP_MODERN -> "cartdeck/ppsspp-modern.properties"
                else -> null
            },
            config,
        )
    }

    fun hardware(context: Context): Hardware {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val vulkan =
            context.packageManager.systemAvailableFeatures.orEmpty()
                .filter { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                .maxOfOrNull { it.version } ?: 0
        return Hardware(
            Build.VERSION.SDK_INT,
            Build.SUPPORTED_ABIS.toSet(),
            activityManager.deviceConfigurationInfo.reqGlEsVersion,
            vulkan,
        )
    }

    /** Advertised hardware is a prerequisite only: EGL/Vulkan creation is still checked by each host. */
    fun hardwareEligibility(
        engine: Engine,
        hardware: Hardware,
    ): Eligibility =
        when {
            hardware.android < engine.minimumAndroid ->
                Eligibility(
                    false,
                    "Requires Android API ${engine.minimumAndroid} or newer",
                )
            engine.architectures.isNotEmpty() && engine.architectures.intersect(hardware.architectures).isEmpty() ->
                Eligibility(false, "Requires ${engine.architectures.joinToString()}")
            hardware.gles < engine.minimumGles ->
                Eligibility(
                    false,
                    "Requires OpenGL ES ${engine.minimumGles ushr 16}.${engine.minimumGles and 0xffff}",
                )
            hardware.vulkan < engine.minimumVulkan -> Eligibility(false, "Requires Vulkan 1.1")
            else -> Eligibility(true)
        }

    fun eligibility(
        context: Context,
        engine: Engine,
        extension: String? = null,
    ): Eligibility {
        val hardware = hardwareEligibility(engine, hardware(context))
        if (!hardware.eligible) return hardware
        if (extension != null && extension.lowercase() !in engine.formats) {
            return Eligibility(
                false,
                if (engine.host == Host.XENIA_ANDROID) {
                    "Xbox 360 supports verified ISO, XEX folders, ZAR and XCP. " +
                        "M3U disc switching is unavailable. Your cartridge and saves are retained."
                } else {
                    "This engine does not support this file format"
                },
            )
        }
        if (engine.core in setOf(CoreID.PCSX_REARMED_MODERN, CoreID.PPSSPP_MODERN)) {
            if (!EmulatorCompatibilityPreferences(context).available(engine.system)) {
                return Eligibility(false, "Matching engine and verified support assets are unavailable")
            }
        }
        val binaryName =
            when (engine.host) {
                Host.HAKUX_SDL3 -> "libhakuxcore.so"
                Host.XENIA_ANDROID -> "libcartdeck_xenia.so"
                else -> engine.core?.libretroFileName
            }
        // Legacy cores retain their existing verified install/provisioning path.
        if (engine.manifest != null) {
            val binary = binaryName?.let { File(context.applicationInfo.nativeLibraryDir, it) }
            if (binary == null || !binary.isFile || binary.length() == 0L) {
                return Eligibility(
                    false,
                    "Engine is not packaged in this build",
                )
            }
            if (engine.host != Host.HAKUX_SDL3 && version(context, engine) == null) {
                return Eligibility(
                    false,
                    "Engine provenance is unavailable",
                )
            }
        }
        return Eligibility(true)
    }

    /** APK manifests are authoritative; revision strings are never inferred from a display name. */
    fun version(
        context: Context,
        engine: Engine,
    ): String? =
        engine.manifest?.let { path ->
            runCatching {
                val manifest = Properties().apply { context.assets.open(path).use { load(it) } }
                val revision = manifest.getProperty("revision") ?: manifest.getProperty("upstream.revision")
                val digest = manifest.getProperty("core.sha256")
                val active =
                    if (engine.host == Host.XENIA_ANDROID) {
                        File(
                            context.filesDir,
                            "native-engines/xbox360/active.version",
                        ).takeIf { it.isFile }?.readText()?.trim()
                    } else if (engine.host == Host.HAKUX_SDL3) {
                        File(
                            context.filesDir,
                            "native-engines/xbox/active.version",
                        ).takeIf { it.isFile }?.readText()?.trim()
                    } else {
                        engine.core?.let { core ->
                            File(context.filesDir, "native-engine-versions/${core.coreName}/selected.version")
                                .takeIf { it.isFile }?.readText()?.trim()
                        }
                    }
                val bundled =
                    if (revision != null && digest != null) {
                        "${revision.take(12)}-${digest.take(12)}"
                    } else {
                        null
                    }
                val installed = active ?: bundled
                listOfNotNull(revision, digest, installed).takeIf { it.isNotEmpty() }?.joinToString(":")
            }.getOrNull()
        }
}
