package com.swordfish.lemuroid.lib.core

import android.content.Context
import android.os.Build
import android.util.Log
import com.swordfish.lemuroid.lib.library.CoreID
import java.io.File
import java.io.IOException
import java.util.Properties

/** Signed-APK provenance plus immutable local versions; saves never live in this store. */
object NativeCoreProvisioner {
    private fun manifestPath(coreID: CoreID): String =
        when (coreID) {
            CoreID.FLYCAST_MODERN -> "cartdeck/flycast.properties"
            CoreID.DOLPHIN_MODERN -> "cartdeck/dolphin-modern.properties"
            CoreID.PLAY_MODERN -> "cartdeck/play-modern.properties"
            else -> throw IllegalArgumentException("Not a versioned native engine: $coreID")
        }

    private fun root(context: Context) = File(context.filesDir, "native-engine-versions")

    /** A failed revision stays disabled until explicit reset or a different APK engine revision. */
    @Synchronized
    fun select(
        context: Context,
        coreID: CoreID,
    ): File {
        check(Build.VERSION.SDK_INT >= 28) { "This native engine requires Android 9 or newer" }
        val properties =
            Properties().apply {
                context.assets.open(manifestPath(coreID)).use { load(it) }
            }
        val revision = properties.getProperty("revision") ?: properties.getProperty("upstream.revision")
        require(revision != null && revision.matches(Regex("[a-fA-F0-9]{40}"))) { "Missing native source revision" }
        val sha = properties.getProperty("core.sha256")?.lowercase()
        require(sha != null && sha.matches(Regex("[a-f0-9]{64}"))) { "Missing native core checksum" }
        val platform = coreID.coreName
        val version = "${revision.take(12)}-${sha.take(12)}"
        val binary = File(context.applicationInfo.nativeLibraryDir, coreID.libretroFileName)
        val selected =
            selectVersion(root(context), platform, version, binary, sha) { error ->
                Log.e("CARTDECK_NATIVE", "Native engine integrity/install failed: $platform $version", error)
            }
        return selected
    }

    /** Pure file-based selection is also exercised by JVM corruption/rollback tests. */
    @Synchronized
    fun selectVersion(
        storeRoot: File,
        platform: String,
        version: String,
        binary: File,
        sha: String,
        onInstallFailure: (Exception) -> Unit = {},
    ): File {
        require(platform.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
        require(version.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
        val store = VersionedCoreStore(storeRoot)
        val failureDirectory = File(File(storeRoot, platform), "failed-versions")
        val failed = File(failureDirectory, version)
        if (!failed.isFile) {
            try {
                store.install(platform, version, binary, sha)
                val pinned =
                    File(
                        File(storeRoot, platform),
                        "selected.version",
                    ).takeIf { it.isFile }?.readText()?.trim()
                check(store.activate(platform, pinned ?: version)) { "Cannot activate verified native engine" }
            } catch (error: Exception) {
                recordFailure(failureDirectory, version)
                onInstallFailure(error)
                // Selection below independently rechecks both stored versions. Never load
                // an unverified APK binary or delete the previous engine/save directories.
            }
        }
        val selected = store.select(platform)
        check(selected != null && !File(failureDirectory, selected.version).isFile) {
            "Native engine disabled after launch failure; select another engine or install a newer revision"
        }
        return selected.coreFile
    }

    /** Call only after confirmed native launch failure, never for an ordinary lifecycle exit. */
    @Synchronized
    fun rollback(
        context: Context,
        coreID: CoreID,
    ): Boolean {
        manifestPath(coreID) // Restrict this API to the versioned native engines.
        val storeRoot = root(context)
        val store = VersionedCoreStore(storeRoot)
        val failedVersion = store.select(coreID.coreName)?.version ?: return false
        recordFailure(File(File(storeRoot, coreID.coreName), "failed-versions"), failedVersion)
        return store.rollback(coreID.coreName)
    }

    /** Manual engine rollback keeps the selected version pinned across launches. */
    @Synchronized
    fun choosePrevious(
        context: Context,
        coreID: CoreID,
    ): Boolean {
        manifestPath(coreID)
        return choosePrevious(root(context), coreID.coreName)
    }

    @Synchronized
    fun choosePrevious(
        storeRoot: File,
        platform: String,
    ): Boolean {
        require(platform.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
        val store = VersionedCoreStore(storeRoot)
        if (!store.rollback(platform)) return false
        val selection = store.select(platform) ?: return false
        writeAtomic(File(File(storeRoot, platform), "selected.version"), selection.version)
        return true
    }

    /** Explicitly select/retry the engine shipped in this APK. */
    @Synchronized
    fun chooseCurrent(
        context: Context,
        coreID: CoreID,
    ) {
        manifestPath(coreID)
        chooseCurrent(root(context), coreID.coreName)
    }

    @Synchronized
    fun chooseCurrent(
        storeRoot: File,
        platform: String,
    ) {
        require(platform.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}")))
        val pointer = File(File(storeRoot, platform), "selected.version")
        check(!pointer.exists() || pointer.delete()) { "Cannot reset engine selection" }
        val failed = File(File(storeRoot, platform), "failed-versions")
        check(!failed.exists() || failed.deleteRecursively()) { "Cannot reset native engine failure" }
    }

    /** Save states are scoped to the actual selected immutable engine version. */
    @Synchronized
    fun stateVersion(
        context: Context,
        coreID: CoreID,
    ): Int {
        val version = select(context, coreID).parentFile!!.name
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(version.toByteArray(Charsets.UTF_8))
        return ((digest[0].toInt() and 0x7f) shl 24) or
            ((digest[1].toInt() and 0xff) shl 16) or
            ((digest[2].toInt() and 0xff) shl 8) or (digest[3].toInt() and 0xff)
    }

    private fun writeAtomic(
        target: File,
        value: String,
    ) {
        check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs()) { "Cannot create engine preference" }
        val temporary = File(target.parentFile, ".${target.name}.pending")
        try {
            temporary.writeText("$value\n")
            if (!temporary.renameTo(target)) throw IOException("Cannot update native engine preference")
        } finally {
            temporary.delete()
        }
    }

    private fun recordFailure(
        directory: File,
        version: String,
    ) {
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create failure history" }
        val temporary = File(directory, ".failed.pending")
        try {
            temporary.writeText("$version\n")
            if (!temporary.renameTo(File(directory, version))) throw IOException("Cannot record failed native engine")
        } finally {
            temporary.delete()
        }
    }

    /** Explicit retry is a user action; an ordinary relaunch does not clear failure history. */
    @Synchronized
    fun resetFailure(
        context: Context,
        coreID: CoreID,
    ) {
        manifestPath(coreID)
        val failed = File(File(root(context), coreID.coreName), "failed-versions")
        check(!failed.exists() || failed.deleteRecursively()) { "Cannot reset native engine failure" }
    }
}
