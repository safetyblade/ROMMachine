package com.swordfish.lemuroid.app.shared.game.ps3

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.Properties

object Ps3Engine {
    const val ARMSX3_REVISION = "d396f0b6b8372789e6423a4f4d242cab4c5135eb"
    const val RPCS3_REVISION = "993a9914a05361e6b3afe0487c463d9cb5f5b12b"
    const val ACTIVITY = "com.swordfish.lemuroid.app.debug.ps3.Ps3RuntimeActivity"

    fun binary(id: String): String =
        when (id) {
            "armsx3" -> "libarmsx3-core.so"
            "rpcs3-libretro" -> "librpcs3_libretro.so"
            else -> error("Unknown PS3 engine")
        }

    fun readiness(
        context: Context,
        id: String,
    ): String? {
        val manifest =
            runCatching {
                Properties().apply { context.assets.open("cartdeck/ps3/$id.properties").use(::load) }
            }.getOrNull() ?: return "PS3 engine is not packaged in this build. Your games and saves are retained."
        val revision = if (id == "armsx3") ARMSX3_REVISION else RPCS3_REVISION
        if (manifest.getProperty("revision") != revision) return "PS3 engine provenance does not match the pinned source."
        val file = File(context.applicationInfo.nativeLibraryDir, binary(id))
        if (!file.isFile) return "PS3 native engine is missing."
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val b = ByteArray(65536)
            while (true) {
                val n = input.read(b)
                if (n < 0) break
                digest.update(b, 0, n)
            }
        }
        if (digest.digest().joinToString("") {
                "%02x".format(it)
            } != manifest.getProperty("core.sha256")
        ) {
            return "PS3 engine integrity check failed."
        }
        return null
    }
}
