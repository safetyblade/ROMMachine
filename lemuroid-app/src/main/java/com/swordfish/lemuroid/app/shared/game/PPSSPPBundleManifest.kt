package com.swordfish.lemuroid.app.shared.game

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Properties

/** Provenance and integrity of the paired core/assets shipped inside the signed APK. */
internal class PPSSPPBundleManifest(input: InputStream, expectedRevision: String) {
    private val properties = Properties().apply { input.use { load(it) } }

    init {
        check(properties.getProperty("revision") == expectedRevision) { "PPSSPP source revision mismatch" }
        listOf("core.sha256", "assets.sha256", "compat.sha256").forEach { key ->
            check(properties.getProperty(key)?.matches(Regex("[a-f0-9]{64}")) == true) {
                "Missing or invalid PPSSPP integrity field: $key"
            }
        }
    }

    fun verify(
        key: String,
        input: InputStream,
    ) {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val size = stream.read(buffer)
                if (size < 0) break
                digest.update(buffer, 0, size)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        check(actual == properties.getProperty(key)) { "PPSSPP integrity check failed: $key" }
    }

    fun verify(
        key: String,
        file: File,
    ) = verify(key, file.inputStream())
}
