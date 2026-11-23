package com.swordfish.lemuroid.lib.library.pc

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Frozen source folder, immutable cartridge identity and persistent writable game files. */
object PcWineFolderBundle {
    data class Source(val path: String, val open: () -> InputStream)

    private data class Record(val path: String, val size: Long, val sha: String)

    private const val MANIFEST = "wine-folder-v1.tsv"
    private const val HEADER = "CARTDECK_FOLDER_V1"
    private const val MAX_FILES = 4096
    private const val MAX_SINGLE = 512L * 1024 * 1024
    private const val MAX_TOTAL = 2L * 1024 * 1024 * 1024
    private const val RESERVE = 256L * 1024 * 1024
    private val forbidden =
        setOf("CON", "PRN", "AUX", "NUL") +
            (1..9).flatMap { listOf("COM" + it, "LPT" + it) }

    fun validPath(name: String): String {
        require(name.length in 1..1024 && !name.startsWith("/") && '\\' !in name)
        val parts = name.split("/")
        require(parts.size <= 12 && parts.all { it.length in 1..128 })
        for (part in parts) {
            require(
                part != "." && part != ".." && !part.endsWith(".") && !part.endsWith(" ") &&
                    part.none { it.isISOControl() || it in "<>:\"|?*\\\t" },
            ) { "Unsafe PC folder filename" }
            require(part.substringBefore('.').uppercase(Locale.ROOT) !in forbidden)
        }
        return name
    }

    fun isFolder(entry: PcCartridgeStore.Entry): Boolean = File(entry.content.parentFile, MANIFEST).isFile

    fun importFolder(
        filesRoot: File,
        title: String,
        files: List<Source>,
        selectedExecutable: String,
        checkCancellation: () -> Unit = {},
    ): PcCartridgeStore.Entry {
        val clean = title.trim().take(120)
        require(clean.isNotBlank() && clean.none { it.isISOControl() })
        require(files.isNotEmpty() && files.size <= MAX_FILES)
        val selected = validPath(selectedExecutable)
        require(selected.endsWith(".exe", ignoreCase = true))
        val names = files.map { validPath(it.path) }
        require(names.map { it.lowercase(Locale.ROOT) }.toSet().size == names.size)
        val realSelection = names.single { it.equals(selected, ignoreCase = true) }
        val root = File(filesRoot, "pc-cartridges-v1")
        check(root.isDirectory || root.mkdirs())
        val id = UUID.randomUUID()
        val stage = File(root, "." + id + ".import")
        check(stage.mkdir())
        try {
            var total = 0L
            val records = mutableListOf<Record>()
            val archive = File(stage, id.toString() + ".dosz")
            archive.outputStream().use { output ->
                ZipOutputStream(output).use { zip ->
                    for (source in files.sortedBy { it.path.lowercase(Locale.ROOT) }) {
                        checkCancellation()
                        val path = validPath(source.path)
                        zip.putNextEntry(ZipEntry("payload/" + path))
                        val digest = MessageDigest.getInstance("SHA-256")
                        var size = 0L
                        source.open().use { input ->
                            val buffer = ByteArray(128 * 1024)
                            while (true) {
                                checkCancellation()
                                val n = input.read(buffer)
                                if (n < 0) break
                                if (n == 0) continue
                                size += n
                                total += n
                                check(size <= MAX_SINGLE && total <= MAX_TOTAL)
                                check(stage.usableSpace > RESERVE) { "Insufficient PC import storage" }
                                zip.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                            }
                        }
                        zip.closeEntry()
                        records += Record(path, size, hex(digest))
                    }
                    zip.finish()
                    zip.flush()
                    output.fd.sync()
                }
            }
            val chosen = checkNotNull(records.singleOrNull { it.path == realSelection })
            val probe = File(stage, "probe.tmp")
            try {
                ZipFile(archive).use { zip ->
                    zip.getInputStream(checkNotNull(zip.getEntry("payload/" + chosen.path))).use { input ->
                        probe.outputStream().use { input.copyTo(it) }
                    }
                }
                require(PcWineExecution.inspectExecutable(probe) == PcWineExecution.Architecture.X86) {
                    "The current Wine runtime accepts PE32 only"
                }
            } finally {
                probe.delete()
            }
            val manifest =
                buildString {
                    append(HEADER).append('\t').append(chosen.path).append('\n')
                    for (record in records) {
                        append(record.path).append('\t').append(record.size).append('\t')
                        append(record.sha).append('\n')
                    }
                }
            write(File(stage, MANIFEST), manifest.toByteArray(Charsets.UTF_8))
            write(File(stage, "title.txt"), clean.toByteArray(Charsets.UTF_8))
            check(archive.setReadOnly())
            checkCancellation()
            val published = File(root, id.toString())
            check(!published.exists() && stage.renameTo(published))
            return checkNotNull(PcCartridgeStore(filesRoot).resolve(File(published, id.toString() + ".dosz")))
        } finally {
            if (stage.exists()) stage.deleteRecursively()
        }
    }

    fun prepareExecutable(
        entry: PcCartridgeStore.Entry,
        workspace: PcWorkspace,
    ): File {
        val (selected, records) = readManifest(entry)
        workspace.prepareWritableDirectories()
        val content = File(workspace.runtime, "content")
        if (!content.exists()) {
            val stage = File(workspace.runtime, ".folder-" + UUID.randomUUID() + ".tmp")
            check(stage.mkdir())
            try {
                ZipFile(entry.content).use { zip ->
                    val names = zip.entries().asSequence().filterNot { it.isDirectory }.map { it.name }.toList()
                    check(names.sorted() == records.map { "payload/" + it.path }.sorted()) {
                        "PC folder ZIP does not match its manifest"
                    }
                    for (record in records) {
                        val target = safeFile(stage, record.path)
                        check(target.parentFile.mkdirs() || target.parentFile.isDirectory)
                        val hash = MessageDigest.getInstance("SHA-256")
                        var size = 0L
                        zip.getInputStream(checkNotNull(zip.getEntry("payload/" + record.path))).use { input ->
                            target.outputStream().use { output ->
                                val buffer = ByteArray(128 * 1024)
                                while (true) {
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    if (n == 0) continue
                                    size += n
                                    check(size <= record.size)
                                    output.write(buffer, 0, n)
                                    hash.update(buffer, 0, n)
                                }
                                output.fd.sync()
                            }
                        }
                        check(size == record.size && hex(hash) == record.sha)
                    }
                }
                Files.move(stage.toPath(), content.toPath(), StandardCopyOption.ATOMIC_MOVE)
            } finally {
                if (stage.exists()) stage.deleteRecursively()
            }
        }
        check(content.isDirectory && !Files.isSymbolicLink(content.toPath()))
        val executable = safeFile(content, selected)
        check(executable.isFile && !Files.isSymbolicLink(executable.toPath()))
        require(PcWineExecution.inspectExecutable(executable) == PcWineExecution.Architecture.X86)
        // Never refresh writable game files on launch: modified saves and settings must survive.
        return executable
    }

    private fun readManifest(entry: PcCartridgeStore.Entry): Pair<String, List<Record>> {
        val file = File(entry.content.parentFile, MANIFEST)
        check(file.isFile && !Files.isSymbolicLink(file.toPath()) && file.length() in 80..(MAX_FILES * 1200L))
        val lines = file.readLines(Charsets.UTF_8)
        val header = lines.first().split('\t')
        check(header.size == 2 && header[0] == HEADER)
        val selected = validPath(header[1])
        val records =
            lines.drop(1).filter { it.isNotEmpty() }.map { line ->
                val parts = line.split('\t')
                check(parts.size == 3 && parts[2].matches(Regex("[a-f0-9]{64}")))
                val size = parts[1].toLong()
                check(size in 0..MAX_SINGLE)
                Record(validPath(parts[0]), size, parts[2])
            }
        check(records.isNotEmpty() && records.size <= MAX_FILES && records.sumOf { it.size } <= MAX_TOTAL)
        check(records.map { it.path.lowercase(Locale.ROOT) }.toSet().size == records.size)
        check(records.any { it.path == selected })
        return selected to records
    }

    private fun safeFile(
        root: File,
        relative: String,
    ): File {
        val file = File(root, validPath(relative))
        val base = root.canonicalFile.toPath()
        check(file.canonicalFile.toPath().startsWith(base) && file.canonicalFile.toPath() != base)
        var path = file.parentFile.toPath()
        while (path.startsWith(root.toPath())) {
            check(!Files.isSymbolicLink(path))
            if (path == root.toPath()) break
            path = path.parent
        }
        return file
    }

    private fun write(
        file: File,
        bytes: ByteArray,
    ) {
        file.outputStream().use {
            it.write(bytes)
            it.fd.sync()
        }
    }

    private fun hex(hash: MessageDigest): String = hash.digest().joinToString("") { "%02x".format(it) }
}
