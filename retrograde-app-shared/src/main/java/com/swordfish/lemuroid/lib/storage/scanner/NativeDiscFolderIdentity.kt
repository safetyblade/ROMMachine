package com.swordfish.lemuroid.lib.storage.scanner

import com.swordfish.lemuroid.lib.library.SystemID
import java.net.URLDecoder
import java.util.Locale

/**
 * A conservative fallback for native discs that cannot be seek-probed via a
 * phone's Storage Access Framework provider. This is not an extension-only
 * detector: a clear ROM folder and an enabled engine are both required.
 */
object NativeDiscFolderIdentity {
    private val extensions =
        mapOf(
            SystemID.DREAMCAST to setOf("gdi", "chd", "m3u", "cdi"),
            SystemID.GAMECUBE to setOf("gcm", "iso", "rvz", "gcz", "ciso", "tgc"),
            SystemID.PLAYSTATION_2 to setOf("iso", "chd", "cso", "isz"),
        )

    private val folders =
        mapOf(
            "dc" to SystemID.DREAMCAST,
            "dreamcast" to SystemID.DREAMCAST,
            "sega dreamcast" to SystemID.DREAMCAST,
            "gc" to SystemID.GAMECUBE,
            "gamecube" to SystemID.GAMECUBE,
            "game cube" to SystemID.GAMECUBE,
            "nintendo gamecube" to SystemID.GAMECUBE,
            "ps2" to SystemID.PLAYSTATION_2,
            "playstation 2" to SystemID.PLAYSTATION_2,
            "playstation2" to SystemID.PLAYSTATION_2,
            "sony playstation 2" to SystemID.PLAYSTATION_2,
        )

    /**
     * Positive disc signatures and old PS1/PSP serials always win. A verified
     * foreign disc (notably Wii) must never inherit the folder's console.
     */
    fun resolve(
        name: String,
        path: String?,
        enabled: Set<SystemID>,
        verified: SystemID? = null,
        legacy: SystemID? = null,
        foreignDisc: Boolean = false,
    ): SystemID? {
        if (verified != null) return verified
        if (legacy != null) return legacy
        if (foreignDisc) return null
        return infer(name, path, enabled)
    }

    fun infer(
        name: String,
        path: String?,
        enabled: Set<SystemID>,
    ): SystemID? {
        if (path.isNullOrBlank()) return null
        val extension = name.substringAfterLast('.', "").lowercase(Locale.US)
        val decodedPath =
            runCatching { URLDecoder.decode(path.replace("+", "%2B"), "UTF-8") }
                .getOrDefault(path)
        // Drop the filename before checking complete, exact directory names.
        // This supports both /tree/.../document/primary:/ROMs/PS2/game.iso
        // and ordinary /storage/emulated/0/ROMs/PS2/game.iso paths.
        val parent = decodedPath.replace('\\', '/').substringBeforeLast('/', "")
        if (parent.isBlank()) return null
        val candidates =
            parent.split('/')
                .map { segment ->
                    segment.substringAfterLast(':')
                        .replace('-', ' ')
                        .replace('_', ' ')
                        .trim()
                        .lowercase(Locale.US)
                }
                .mapNotNull { folders[it] }
                .distinct()
        if (candidates.size != 1) return null
        val system = candidates.single()
        return system.takeIf { it in enabled && extension in extensions.getValue(it) }
    }
}
