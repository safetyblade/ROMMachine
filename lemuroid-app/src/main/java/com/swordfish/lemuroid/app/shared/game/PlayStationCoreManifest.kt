package com.swordfish.lemuroid.app.shared.game

import java.io.InputStream
import java.security.MessageDigest
import java.util.Properties

/** Verifies the pinned core against provenance shipped in the same signed APK. */
internal object PlayStationCoreManifest {
    const val SOURCE_REVISION = "7b0e8f8fadfbce3d170d195b9d9eecaabfa08c8e"
    const val ASSET_PATH = "cartdeck/pcsx-rearmed-modern.properties"

    fun verify(
        manifest: InputStream,
        core: InputStream,
    ): Boolean =
        core.use { binary ->
            val properties = Properties().apply { manifest.use { load(it) } }
            if (properties.getProperty("revision") != SOURCE_REVISION) return@use false
            val expected = properties.getProperty("core.sha256") ?: return@use false
            if (!expected.matches(Regex("[a-f0-9]{64}"))) return@use false
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val size = binary.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            actual == expected
        }
}
