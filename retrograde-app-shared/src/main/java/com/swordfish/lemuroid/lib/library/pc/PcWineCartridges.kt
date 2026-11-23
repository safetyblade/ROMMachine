package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Explicit PE32 import, never heuristic scanning. Same identity as other managed PC cartridges. */
object PcWineCartridges {
    private const val MARKER = "wine-payload.sha256"
    private const val PAYLOAD = "CARTWINE.EXE"
    private const val LIMIT = 512L * 1024 * 1024

    fun isWine(entry: PcCartridgeStore.Entry): Boolean = PcWineFolderBundle.isFolder(entry) || marker(entry) != null

    private fun marker(entry: PcCartridgeStore.Entry): String? {
        val parent = checkNotNull(entry.content.parentFile)
        val file = File(parent, MARKER)
        if (!file.exists()) return null
        check(file.canonicalFile == File(parent.canonicalFile, MARKER) && file.length() == 64L)
        return file.readText(Charsets.US_ASCII).also { check(it.matches(Regex("[a-f0-9]{64}"))) }
    }

    fun importExecutable(
        root: File,
        title: String,
        input: InputStream,
        checkCancellation: () -> Unit = {},
    ): PcCartridgeStore.Entry {
        val clean = title.trim().take(120)
        require(clean.isNotEmpty() && clean.none { it.isISOControl() })
        val parent = File(root, "pc-cartridges-v1")
        check(parent.isDirectory || parent.mkdirs())
        val id = UUID.randomUUID()
        val stage = File(parent, ".$id.import")
        check(stage.mkdir())
        try {
            val payload = File(stage, "payload.tmp")
            val sha = copyBounded(input, payload, checkCancellation)
            require(PcWineExecution.inspectExecutable(payload) == PcWineExecution.Architecture.X86) {
                "This native Wine candidate supports PE32 only"
            }
            val archive = File(stage, "$id.dosz")
            archive.outputStream().use { output ->
                ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(ZipEntry(PAYLOAD))
                    payload.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    zip.finish()
                    zip.flush()
                    output.fd.sync()
                }
            }
            check(payload.delete())
            write(File(stage, "title.txt"), clean.toByteArray(Charsets.UTF_8))
            write(File(stage, MARKER), sha.toByteArray(Charsets.US_ASCII))
            check(archive.setReadOnly())
            checkCancellation()
            val dest = File(parent, id.toString())
            check(!dest.exists() && stage.renameTo(dest))
            return checkNotNull(PcCartridgeStore(root).resolve(File(dest, "$id.dosz")))
        } finally {
            if (stage.exists()) stage.deleteRecursively()
        }
    }

    /** Never overwrite a working installation or accept an arbitrary host execution path. */
    fun prepareExecutable(
        entry: PcCartridgeStore.Entry,
        workspace: PcWorkspace,
    ): File {
        if (PcWineFolderBundle.isFolder(entry)) return PcWineFolderBundle.prepareExecutable(entry, workspace)
        val expected = checkNotNull(marker(entry))
        workspace.prepareWritableDirectories()
        val content = File(workspace.runtime, "content")
        check(content.isDirectory || content.mkdirs())
        check(content.canonicalFile == File(workspace.runtime.canonicalFile, "content"))
        val target = File(content, PAYLOAD)
        check(!Files.isSymbolicLink(target.toPath()))
        if (!target.exists()) {
            val staged = File.createTempFile(".wine-", ".tmp", content)
            try {
                ZipFile(entry.content).use { zip ->
                    val item = checkNotNull(zip.getEntry(PAYLOAD))
                    require(item.size in 1..LIMIT)
                    zip.getInputStream(item).use { input ->
                        check(copyBounded(input, staged) {} == expected) { "Wine cartridge payload digest mismatch" }
                    }
                }
                require(PcWineExecution.inspectExecutable(staged) == PcWineExecution.Architecture.X86)
                Files.move(staged.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } finally {
                staged.delete()
            }
        }
        // Detect unexpected changes without ever replacing the working file.
        check(target.isFile && target.canonicalFile == File(content.canonicalFile, PAYLOAD))
        check(target.inputStream().use { digest(it) } == expected) { "Wine executable changed; retained for recovery" }
        return target
    }

    private fun write(
        target: File,
        data: ByteArray,
    ) {
        target.outputStream().use {
            it.write(data)
            it.fd.sync()
        }
    }

    private fun copyBounded(
        input: InputStream,
        target: File,
        cancel: () -> Unit,
    ): String {
        val hash = MessageDigest.getInstance("SHA-256")
        var size = 0L
        target.outputStream().use { output ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                cancel()
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                size += count
                check(size <= LIMIT && target.usableSpace > 128L * 1024 * 1024) {
                    "Not enough space or executable too large"
                }
                output.write(buffer, 0, count)
                hash.update(buffer, 0, count)
            }
            output.fd.sync()
        }
        return hex(hash)
    }

    private fun digest(input: InputStream): String {
        val hash = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(128 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count > 0) hash.update(buffer, 0, count)
        }
        return hex(hash)
    }

    private fun hex(hash: MessageDigest): String = hash.digest().joinToString("") { "%02x".format(it) }
}
