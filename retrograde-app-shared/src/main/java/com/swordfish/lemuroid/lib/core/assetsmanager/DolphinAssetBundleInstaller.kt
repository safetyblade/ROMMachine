package com.swordfish.lemuroid.lib.core.assetsmanager

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipInputStream

/** Pure file installer used by Android provisioning and JVM integration tests. */
object DolphinAssetBundleInstaller {
    const val SOURCE_REVISION = "32c61f9d0e933331bb7087ba57e96883241b4960"
    private const val MARKER = ".cartdeck-asset-sha256"

    @Synchronized
    fun install(
        manifestStream: InputStream,
        core: File,
        openArchive: () -> InputStream,
        systemDirectory: File,
        expectedRevision: String = SOURCE_REVISION,
    ): File {
        val manifest = Properties().apply { manifestStream.use { load(it) } }
        check(manifest.getProperty("upstream.revision") == expectedRevision) { "Dolphin revision mismatch" }

        fun expected(key: String): String =
            requireNotNull(manifest.getProperty(key)).also {
                check(it.matches(Regex("[0-9a-f]{64}"))) { "Invalid Dolphin bundle checksum" }
            }
        val expectedCore = expected("core.sha256")
        val expectedAssets = expected("assets.sha256")
        core.inputStream().use { check(sha256(it) == expectedCore) { "Dolphin core integrity failure" } }
        val destination = File(systemDirectory, "dolphin-emu/Sys")
        PPSSPPAssetArchiveInstaller.recoverInterruptedInstallation(destination)
        // APK stream is verified on every launch; partial installs cannot gain a valid marker.
        openArchive().use { check(sha256(it) == expectedAssets) { "Dolphin Sys integrity failure" } }
        if (File(destination, MARKER).run { isFile && readText() == expectedAssets }) return systemDirectory
        openArchive().use { input ->
            ZipInputStream(input).use { archive ->
                PPSSPPAssetArchiveInstaller.install(archive, destination) { staging ->
                    check(File(staging, "GC").isDirectory && File(staging, "GameSettings").isDirectory) {
                        "Incomplete Dolphin Sys archive"
                    }
                    File(staging, MARKER).writeText(expectedAssets)
                }
            }
        }
        return systemDirectory
    }

    private fun sha256(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val length = stream.read(buffer)
            if (length < 0) break
            digest.update(buffer, 0, length)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
