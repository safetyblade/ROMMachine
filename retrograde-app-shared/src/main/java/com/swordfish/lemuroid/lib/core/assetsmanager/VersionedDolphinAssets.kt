package com.swordfish.lemuroid.lib.core.assetsmanager

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipInputStream

/** Immutable engine-specific Sys trees. Memory cards remain outside this store. */
object VersionedDolphinAssets {
    private const val METADATA = ".cartdeck-dolphin.properties"
    private const val FILE_INDEX = ".cartdeck-sys-files.properties"

    @Synchronized
    fun installAndSelect(
        manifestStream: InputStream,
        currentCore: File,
        selectedCore: File,
        openArchive: () -> InputStream,
        systemRoot: File,
        expectedCurrentRevision: String = DolphinAssetBundleInstaller.SOURCE_REVISION,
    ): File {
        val current = Properties().apply { manifestStream.use { load(it) } }
        val currentRevision = requiredRevision(current)
        val currentHash = requiredHash(current, "core.sha256")
        val selectedHash = selectedCore.inputStream().use(::hash)
        val selectedVersion = selectedCore.parentFile!!.name
        require(selectedVersion.matches(Regex("[a-f0-9]{12}-[a-f0-9]{12}"))) { "Invalid Dolphin core version" }
        val directory = File(systemRoot, "engine-versions/$selectedVersion")
        val metadataFile = File(directory, METADATA)
        val selectedIsCurrent = selectedVersion == version(currentRevision, currentHash) && selectedHash == currentHash
        if (selectedIsCurrent && !metadataFile.isFile) {
            val bytes = java.io.ByteArrayOutputStream().also { current.store(it, null) }.toByteArray()
            DolphinAssetBundleInstaller.install(
                bytes.inputStream(),
                currentCore,
                openArchive,
                directory,
                expectedCurrentRevision,
            )
            // Build the index from the verified APK archive, never from mutable installed files.
            val index = Properties()
            openArchive().use { stream ->
                ZipInputStream(stream).use { archive ->
                    while (true) {
                        val entry = archive.nextEntry ?: break
                        if (!entry.isDirectory) {
                            check(index.setProperty(entry.name, hash(archive)) == null) { "Duplicate Dolphin resource" }
                        }
                        archive.closeEntry()
                    }
                }
            }
            val indexFile = File(directory, FILE_INDEX)
            atomicProperties(indexFile, index)
            current.setProperty("files.sha256", indexFile.inputStream().use(::hash))
            // Metadata commits the verified resource index only after complete extraction.
            atomicProperties(metadataFile, current)
        }
        check(metadataFile.isFile) { "Selected Dolphin engine has no retained matching Sys resources" }
        val installed = Properties().apply { metadataFile.inputStream().use { load(it) } }
        check(requiredHash(installed, "core.sha256") == selectedHash) { "Dolphin engine/resource checksum mismatch" }
        check(version(requiredRevision(installed), selectedHash) == selectedVersion) {
            "Dolphin engine/resource revision mismatch"
        }
        val indexFile = File(directory, FILE_INDEX)
        check(indexFile.inputStream().use(::hash) == requiredHash(installed, "files.sha256")) {
            "Dolphin resource index damaged"
        }
        val index = Properties().apply { indexFile.inputStream().use { load(it) } }
        val sys = File(directory, "dolphin-emu/Sys").canonicalFile
        check(index.isNotEmpty()) { "Empty Dolphin resource index" }
        index.forEach { (name, expected) ->
            val file = File(sys, name.toString()).canonicalFile
            check(file.path.startsWith(sys.path + File.separator)) { "Unsafe Dolphin resource path" }
            check(file.isFile && file.inputStream().use(::hash) == expected) { "Dolphin resource damaged: $name" }
        }
        return directory
    }

    private fun version(
        revision: String,
        coreHash: String,
    ) = "${revision.take(12)}-${coreHash.take(12)}"

    private fun requiredRevision(properties: Properties): String =
        requireNotNull(properties.getProperty("upstream.revision")).also {
            require(it.matches(Regex("[a-f0-9]{40}"))) { "Invalid Dolphin source revision" }
        }

    private fun requiredHash(
        properties: Properties,
        key: String,
    ): String =
        requireNotNull(properties.getProperty(key)).also {
            require(it.matches(Regex("[a-f0-9]{64}"))) { "Invalid Dolphin checksum: $key" }
        }

    private fun atomicProperties(
        destination: File,
        properties: Properties,
    ) {
        val pending = File(destination.parentFile, ".${destination.name}.pending")
        try {
            pending.outputStream().use { properties.store(it, null) }
            check(pending.renameTo(destination)) { "Cannot commit Dolphin resource metadata" }
        } finally {
            pending.delete()
        }
    }

    private fun hash(stream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
}
