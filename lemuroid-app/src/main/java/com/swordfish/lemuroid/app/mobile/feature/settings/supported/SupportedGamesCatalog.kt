package com.swordfish.lemuroid.app.mobile.feature.settings.supported

import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import java.io.File
import java.util.Locale

/**
 * Display metadata is deliberately derived from the same GameSystem registry used by
 * CARTDECK's scanner. The small note/alias mapping does not define supported formats.
 */
internal data class SupportedGameEntry(
    val system: GameSystem,
    val displayName: String,
    val extensions: List<String>,
    val searchAliases: String,
    val noteResId: Int?,
)

internal data class GameGuideRuntime(
    val installedCoreFiles: Set<String>,
    val installedFirmwareFiles: Set<String>,
    val deviceAbis: Set<String>,
    val nativeManifests: Set<String> = emptySet(),
)

internal enum class GameGuideSetup {
    CHECKING,
    DEVICE_UNSUPPORTED,
    ENGINE_MISSING,
    FIRMWARE_MISSING,
    ENGINE_PRESENT,
}

internal object SupportedGamesCatalog {
    fun entries(context: Context): List<SupportedGameEntry> =
        GameSystem.all()
            .map { system ->
                SupportedGameEntry(
                    system = system,
                    displayName = context.getString(system.titleResId),
                    extensions = system.supportedExtensions.distinct().sorted(),
                    searchAliases = aliases(system.id),
                    noteResId = note(system.id),
                )
            }.sortedBy { it.displayName.lowercase(Locale.ROOT) }

    fun search(
        entries: List<SupportedGameEntry>,
        query: String,
    ): List<SupportedGameEntry> {
        val term = query.trim().lowercase(Locale.ROOT).removePrefix(".")
        if (term.isEmpty()) return entries
        return entries.filter { entry ->
            entry.displayName.lowercase(Locale.ROOT).contains(term) ||
                entry.searchAliases.contains(term) ||
                entry.extensions.any { it.lowercase(Locale.ROOT).contains(term) }
        }
    }

    fun status(
        entry: SupportedGameEntry,
        runtime: GameGuideRuntime?,
    ): GameGuideSetup {
        if (runtime == null) return GameGuideSetup.CHECKING
        if (entry.system.nativeBackend == "cemu") {
            // Catalogue and shelf discovery intentionally precede a verified
            // production Vulkan frontend. An isolated Cemu test SO is not a
            // selectable gameplay engine.
            return if ("arm64-v8a" in runtime.deviceAbis) {
                GameGuideSetup.ENGINE_MISSING
            } else {
                GameGuideSetup.DEVICE_UNSUPPORTED
            }
        }
        if (entry.system.nativeBackend == "xenia") {
            if ("arm64-v8a" !in runtime.deviceAbis) return GameGuideSetup.DEVICE_UNSUPPORTED
            return if (
                "libcartdeck_xenia.so" in runtime.installedCoreFiles &&
                "cartdeck/xenia.properties" in runtime.nativeManifests
            ) {
                GameGuideSetup.ENGINE_PRESENT
            } else {
                GameGuideSetup.ENGINE_MISSING
            }
        }
        if (entry.system.nativeBackend == "hakux") {
            if ("arm64-v8a" !in runtime.deviceAbis) return GameGuideSetup.DEVICE_UNSUPPORTED
            return if (setOf("libhakuxcore.so", "libSDL3.so").all { it in runtime.installedCoreFiles }) {
                GameGuideSetup.ENGINE_PRESENT
            } else {
                GameGuideSetup.ENGINE_MISSING
            }
        }
        val candidates =
            entry.system.systemCoreConfigs.filter { config ->
                val allowedArchitectures = config.supportedOnlyArchitectures
                allowedArchitectures == null ||
                    allowedArchitectures.any { it in runtime.deviceAbis }
            }
        if (candidates.isEmpty()) return GameGuideSetup.DEVICE_UNSUPPORTED

        // GameLoader searches nativeLibraryDir and the app's filesDir for these same binaries.
        val installed =
            candidates.firstOrNull { it.coreID.libretroFileName in runtime.installedCoreFiles }
                ?: return GameGuideSetup.ENGINE_MISSING

        val missingRequired = installed.requiredBIOSFiles.any { it !in runtime.installedFirmwareFiles }
        val missingRegional =
            installed.regionalBIOSFiles.isNotEmpty() &&
                installed.regionalBIOSFiles.values.none { it in runtime.installedFirmwareFiles }
        if (missingRequired || missingRegional) return GameGuideSetup.FIRMWARE_MISSING
        return GameGuideSetup.ENGINE_PRESENT
    }

    /**
     * Read only. Run on Dispatchers.IO, never on the Compose render thread.
     * No core download, scan, storage mutation or network request is triggered here.
     */
    fun currentRuntime(context: Context): GameGuideRuntime {
        val coreNames = CoreID.values().map { it.libretroFileName }.toSet() + setOf("libhakuxcore.so", "libSDL3.so")
        val searchRoots =
            listOf(
                File(context.applicationInfo.nativeLibraryDir),
                context.filesDir,
            ).filter { it.exists() }
        val installed =
            searchRoots.asSequence()
                .flatMap { it.walkTopDown() }
                .filter { it.isFile && it.name in coreNames }
                .map { it.name }
                .toSet()
        val firmware =
            File(context.filesDir, "system").listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }
                ?.toSet()
                .orEmpty()
        val manifests =
            listOf("cartdeck/xenia.properties")
                .filter { path ->
                    runCatching { context.assets.open(path).use { it.read() } }.isSuccess
                }.toSet()
        return GameGuideRuntime(installed, firmware, Build.SUPPORTED_ABIS.toSet(), manifests)
    }

    private fun aliases(id: SystemID): String =
        when (id) {
            SystemID.PSX -> "ps1 psx playstation one"
            SystemID.PSP -> "psp playstation portable"
            SystemID.GB -> "gb game boy"
            SystemID.GBC -> "gbc game boy color"
            SystemID.GBA -> "gba game boy advance"
            SystemID.N64 -> "n64 nintendo 64"
            SystemID.NDS -> "nds ds nintendo ds"
            SystemID.NINTENDO_3DS -> "3ds nintendo 3ds"
            SystemID.WII_U -> "wiiu wii u nintendo wii u cemu wua wud wux rpx"
            SystemID.XBOX360 -> "x360 xbox360 xbox 360 microsoft xenia xex xcp zar"
            SystemID.SNES -> "snes super nintendo"
            SystemID.NES -> "nes nintendo"
            SystemID.GENESIS -> "genesis mega drive"
            SystemID.SEGACD -> "sega cd mega cd"
            SystemID.PC_ENGINE -> "turbografx 16 pc engine"
            SystemID.MAME2003PLUS -> "mame arcade"
            SystemID.FBNEO -> "finalburn neo arcade"
            else -> ""
        }

    private fun note(id: SystemID): Int? =
        when (id) {
            SystemID.PSX -> R.string.cartdeck_formats_note_psx
            SystemID.SEGACD -> R.string.cartdeck_formats_note_segacd
            SystemID.LYNX -> R.string.cartdeck_formats_note_lynx
            SystemID.NDS -> R.string.cartdeck_formats_note_nds
            SystemID.FBNEO, SystemID.MAME2003PLUS -> R.string.cartdeck_formats_note_arcade
            SystemID.DOS -> R.string.cartdeck_formats_note_dos
            SystemID.ATARI7800, SystemID.PC_ENGINE -> R.string.cartdeck_formats_note_bin
            else -> null
        }
}
