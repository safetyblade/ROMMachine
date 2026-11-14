package com.swordfish.lemuroid.app.shared.game.ps2next

import android.content.Context
import android.os.Build
import android.system.Os
import android.system.OsConstants
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import java.io.File
import java.util.Properties
import java.util.zip.ZipInputStream

object Armsx2Engine {
    const val MANIFEST = "cartdeck/armsx2/armsx2.properties"
    val engineeringBuild: Boolean
        get() =
            BuildConfig.DEBUG && (
                BuildConfig.P1_EMU_ENABLED ||
                    BuildConfig.APPLICATION_ID == "dev.prestwich.cartdeck.ps2lab.debug"
            )

    fun pageSize(): Int = if (Os.sysconf(OsConstants._SC_PAGESIZE) >= 16384) 16384 else 4096

    fun coreName(): String = if (pageSize() == 16384) "libemucore_16k.so" else "libemucore_4k.so"

    fun store(files: File) =
        XboxEngineStore(
            File(files, "native-engines/armsx2"),
            coreName(),
            setOf(coreName(), "libc++_shared.so", "liblibrashader_capi.so"),
            "armsx2.properties",
        )

    fun install(context: Context): XboxEngineStore.Selection {
        check(engineeringBuild) { "ARMSX2 requires the opt-in PS2 engineering build" }
        check(Build.VERSION.SDK_INT >= 29 && "arm64-v8a" in Build.SUPPORTED_ABIS) {
            "ARMSX2 requires Android ARM64 API 29 or newer"
        }
        val props = Properties().apply { context.assets.open(MANIFEST).use { load(it) } }
        check(props.getProperty("host.page_size") == pageSize().toString()) {
            "Install the PS2 APK for this device's page model"
        }
        val notices = context.cacheDir.resolve("armsx2-notices.zip")
        context.assets.open("cartdeck/armsx2/armsx2-licenses.zip").use {
                input ->
            notices.outputStream().use { output -> input.copyTo(output) }
        }
        check(XboxEngineStore.hash(notices) == props.getProperty("notices.sha256")) { "ARMSX2 notices hash mismatch" }
        val resourcesDigest = props.getProperty("resources.sha256")
        require(resourcesDigest.matches(Regex("[0-9a-f]{64}")))
        val resourceArchive = File(context.filesDir, "native-engine-assets/armsx2/$resourcesDigest.zip")
        check(resourceArchive.parentFile!!.isDirectory || resourceArchive.parentFile!!.mkdirs())
        if (!resourceArchive.isFile || XboxEngineStore.hash(resourceArchive) != resourcesDigest) {
            val pending = File(resourceArchive.parentFile, ".$resourcesDigest.pending")
            context.assets.open("cartdeck/armsx2/armsx2-resources.zip").use { input ->
                java.io.FileOutputStream(pending).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            check(XboxEngineStore.hash(pending) == resourcesDigest) { "ARMSX2 resources hash mismatch" }
            check(pending.renameTo(resourceArchive))
            check(resourceArchive.setReadOnly())
        }
        return store(
            context.filesDir,
        ).install(File(context.applicationInfo.nativeLibraryDir), props, notices.readBytes())
    }

    fun resources(
        context: Context,
        paths: Armsx2Storage.Paths,
        properties: Properties,
    ) {
        val digest = properties.getProperty("resources.sha256")
        require(digest.matches(Regex("[0-9a-f]{64}")))
        val marker = File(paths.resources, ".cartdeck-source-sha256")
        if (marker.isFile && marker.readText() == digest) return
        val archive = File(context.filesDir, "native-engine-assets/armsx2/$digest.zip")
        check(XboxEngineStore.hash(archive) == digest) { "ARMSX2 resources hash mismatch" }
        val pending = File(paths.gameRoot, ".resources.pending")
        check(!pending.exists() || pending.deleteRecursively())
        check(pending.mkdirs())
        var total = 0L
        var count = 0
        ZipInputStream(archive.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                check(++count <= 10000)
                val target = File(pending, entry.name).canonicalFile
                check(target.path.startsWith(pending.canonicalPath + File.separator))
                if (entry.isDirectory) {
                    check(target.isDirectory || target.mkdirs())
                } else {
                    check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                    target.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            check(total <= 256L * 1024 * 1024) { "ARMSX2 resources exceed limits" }
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
        }
        check(File(pending, "GameIndex.yaml").isFile && File(pending, "shaders").isDirectory)
        File(pending, marker.name).writeText(digest)
        if (paths.resources.exists()) {
            val backup = File(paths.gameRoot, ".resources.previous-${System.currentTimeMillis()}")
            check(paths.resources.renameTo(backup)) { "Cannot preserve previous emulator resources" }
        }
        check(pending.renameTo(paths.resources))
    }
}
