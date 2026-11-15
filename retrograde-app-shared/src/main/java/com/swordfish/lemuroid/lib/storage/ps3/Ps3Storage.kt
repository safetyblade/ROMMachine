package com.swordfish.lemuroid.lib.storage.ps3

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Engine roots never alias. Firmware, HDD, caches, profiles and backups survive app updates. */
class Ps3Storage(files: File, engine: String) {
    init {
        require(engine in setOf("armsx3", "rpcs3-libretro"))
    }

    val root = File(files, "native/ps3/$engine")
    val guest = File(root, if (engine == "armsx3") "guest" else "system/rpcs3")
    val backups = File(root, "backups")
    val cache = File(root, "cache")
    private val profileFile = File(root, "profiles.properties")

    data class Profile(val id: String, val name: String, val selected: Boolean)

    data class Save(val name: String, val title: String, val titleId: String?, val modified: Long, val bytes: Long)

    private fun properties(): Properties =
        Properties().also { p ->
            if (profileFile.isFile) profileFile.inputStream().use(p::load)
            File(guest, "dev_hdd0/home").listFiles().orEmpty().filter {
                it.isDirectory && Regex("[0-9]{8}").matches(it.name) && it.name != "00000000"
            }.sortedBy { it.name }.forEach { directory ->
                if (!p.containsKey("user.${directory.name}")) {
                    val username =
                        File(directory, "localusername").takeIf { it.length() in 1..1024 }
                            ?.readText()?.trim()?.take(64)?.takeIf { it.isNotBlank() } ?: "PS3 user ${directory.name}"
                    p.setProperty("user.${directory.name}", username)
                }
            }
            if (p.getProperty("selected") == null) {
                p.stringPropertyNames().filter { it.startsWith("user.") }.minOrNull()?.let {
                    p.setProperty("selected", it.removePrefix("user."))
                }
            }
        }

    private fun write(p: Properties) {
        check(root.mkdirs() || root.isDirectory)
        val pending = File(root, "profiles.pending")
        pending.outputStream().use { p.store(it, "CARTDECK PS3 virtual users") }
        check(pending.renameTo(profileFile)) { "Could not commit profile settings" }
    }

    private fun user(id: String): File {
        require(Regex("[0-9]{8}").matches(id) && id != "00000000")
        return File(guest, "dev_hdd0/home/$id")
    }

    @Synchronized fun profiles(): List<Profile> {
        val p = properties()
        return p.stringPropertyNames().filter { it.startsWith("user.") }.sorted().map {
            val id = it.removePrefix("user.")
            user(id)
            Profile(id, p.getProperty(it), p.getProperty("selected") == id)
        }
    }

    @Synchronized fun create(name: String): Profile {
        require(name.isNotBlank() && name.length <= 64)
        val p = properties()
        val id =
            (1..99999999).first { p.getProperty("user.%08d".format(java.util.Locale.US, it)) == null }
                .let { "%08d".format(java.util.Locale.US, it) }
        val directory = user(id)
        check(directory.mkdirs() || directory.isDirectory)
        File(directory, "localusername").writeText(name.trim())
        p.setProperty("user.$id", name.trim())
        if (p.getProperty("selected") == null) p.setProperty("selected", id)
        write(p)
        return profiles().single { it.id == id }
    }

    @Synchronized fun select(id: String) {
        val p = properties()
        require(p.containsKey("user.$id"))
        p.setProperty("selected", id)
        write(p)
    }

    @Synchronized fun rename(
        id: String,
        name: String,
    ) {
        require(name.isNotBlank() && name.length <= 64)
        val p = properties()
        require(p.containsKey("user.$id"))
        File(user(id), "localusername").writeText(name.trim())
        p.setProperty("user.$id", name.trim())
        write(p)
    }

    @Synchronized fun removeProfile(
        id: String,
        confirmed: Boolean,
    ) {
        require(confirmed && profiles().size > 1)
        val p = properties()
        require(p.containsKey("user.$id"))
        // Archive the actual user, never relabel or overwrite another user's save directory.
        check(backups.mkdirs() || backups.isDirectory)
        check(user(id).renameTo(File(backups, "profile-$id-${UUID.randomUUID()}")))
        p.remove("user.$id")
        if (p.getProperty("selected") == id) {
            val remaining = p.stringPropertyNames().first { it.startsWith("user.") }.removePrefix("user.")
            p.setProperty("selected", remaining)
        }
        write(p)
    }

    private fun save(
        userId: String,
        name: String,
    ): File {
        require(Regex("[A-Za-z0-9_.-]{1,128}").matches(name) && name !in setOf(".", ".."))
        val base = File(user(userId), "savedata").canonicalFile
        return File(base, name).canonicalFile.also { require(it.parentFile == base) }
    }

    fun saves(id: String): List<Save> =
        File(user(id), "savedata").listFiles().orEmpty().filter {
            it.isDirectory && !java.nio.file.Files.isSymbolicLink(it.toPath())
        }.map { dir ->
            val fields =
                File(dir, "PARAM.SFO").takeIf { it.length() in 20..1048576 }
                    ?.readBytes()?.let(Ps3Metadata::fields).orEmpty()
            // Save SFO category is SD and not accepted as launchable game metadata.
            Save(
                dir.name,
                fields["TITLE"] ?: dir.name,
                Regex("[A-Z]{4}[0-9]{5}").find(dir.name)?.value,
                dir.lastModified(),
                usage(dir),
            )
        }.sortedByDescending { it.modified }

    fun usage(directory: File = root): Long =
        directory.walkTopDown().onEnter {
            !java.nio.file.Files.isSymbolicLink(it.toPath())
        }.filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }.sumOf { it.length() }

    fun export(
        id: String,
        name: String,
        output: OutputStream,
    ) {
        val directory = save(id, name)
        require(directory.isDirectory)
        exportDirectory(directory, name, output)
    }

    private fun exportDirectory(
        directory: File,
        name: String,
        output: OutputStream,
    ) {
        ZipOutputStream(output).use { zip ->
            directory.walkTopDown().onEnter {
                !java.nio.file.Files.isSymbolicLink(it.toPath())
            }.filter { it.isFile }.forEach {
                require(!java.nio.file.Files.isSymbolicLink(it.toPath()))
                zip.putNextEntry(ZipEntry("$name/" + it.relativeTo(directory).invariantSeparatorsPath))
                it.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    @Synchronized fun restore(
        id: String,
        name: String,
        input: InputStream,
        confirmed: Boolean,
    ) {
        require(confirmed)
        val target = save(id, name)
        val pending = File(root, ".restore-${UUID.randomUUID()}")
        check(pending.mkdirs())
        try {
            var total = 0L
            val seen = mutableSetOf<String>()
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(seen.add(entry.name) && seen.size <= 4096)
                    require(entry.name.startsWith("$name/") && !entry.name.contains('\\'))
                    val relative = entry.name.removePrefix("$name/")
                    require(relative.split('/').none { it == ".." || it == "." })
                    val destination = File(pending, relative).canonicalFile
                    require(destination.toPath().startsWith(pending.canonicalFile.toPath()))
                    if (entry.isDirectory) {
                        check(destination.mkdirs() || destination.isDirectory)
                    } else {
                        check(destination.parentFile!!.mkdirs() || destination.parentFile!!.isDirectory)
                        destination.outputStream().use { out ->
                            val b = ByteArray(65536)
                            while (true) {
                                val n = zip.read(b)
                                if (n < 0) break
                                total += n
                                require(total <= 1073741824) { "Save backup exceeds 1 GiB" }
                                out.write(b, 0, n)
                            }
                        }
                    }
                }
            }
            require(File(pending, "PARAM.SFO").isFile && File(pending, "PARAM.PFD").isFile) {
                "Not a complete PS3 save directory"
            }
            check(backups.mkdirs() || backups.isDirectory)
            val previous = File(backups, "$id-$name-${UUID.randomUUID()}")
            if (target.exists()) check(target.renameTo(previous))
            check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
            if (!pending.renameTo(target)) {
                if (previous.exists()) check(previous.renameTo(target))
                error("Restore failed; previous save retained")
            }
        } finally {
            pending.deleteRecursively()
        }
    }

    data class Backup(val directory: String, val saveName: String, val modified: Long)

    fun savedBackups(id: String): List<Backup> {
        user(id)
        return backups.listFiles().orEmpty().filter {
            it.isDirectory && it.name.startsWith("$id-") && it.name.length > id.length + 38
        }.map { Backup(it.name, it.name.removePrefix("$id-").dropLast(37), it.lastModified()) }
            .sortedByDescending { it.modified }
    }

    @Synchronized fun restoreBackup(
        id: String,
        directory: String,
        confirmed: Boolean,
    ) {
        require(confirmed)
        val backup = savedBackups(id).single { it.directory == directory }
        val source = File(backups, backup.directory).canonicalFile
        require(source.parentFile == backups.canonicalFile)
        val pending = File(root, ".backup-${UUID.randomUUID()}.zip")
        try {
            pending.outputStream().use { exportDirectory(source, backup.saveName, it) }
            pending.inputStream().use { restore(id, backup.saveName, it, true) }
        } finally {
            pending.delete()
        }
    }

    @Synchronized fun deleteSave(
        id: String,
        name: String,
        confirmed: Boolean,
    ) {
        require(confirmed)
        check(backups.mkdirs() || backups.isDirectory)
        check(save(id, name).renameTo(File(backups, "$id-$name-${UUID.randomUUID()}")))
    }

    /** Firmware replacement never touches the HDD, profiles or caches. A failed install restores its predecessor. */
    @Synchronized fun beginFirmwareReplacement() {
        val record = File(root, "firmware-recovery.properties")
        check(!record.exists()) { "Finish or recover the previous firmware installation first" }
        val current = File(guest, "dev_flash")
        val previous = File(backups, "firmware-${UUID.randomUUID()}")
        check(root.mkdirs() || root.isDirectory)
        check(backups.mkdirs() || backups.isDirectory)
        val p = Properties()
        p.setProperty("previous", previous.name)
        p.setProperty("had_previous", current.exists().toString())
        p.setProperty("started", "false")
        record.outputStream().use { p.store(it, "Pending PS3 firmware replacement") }
        if (current.exists()) check(current.renameTo(previous)) { "Could not preserve current PS3 firmware" }
    }

    @Synchronized fun finishFirmwareReplacement(success: Boolean) {
        val record = File(root, "firmware-recovery.properties")
        if (!record.isFile) return
        val p = Properties().also { record.inputStream().use(it::load) }
        val name = p.getProperty("previous")
        require(Regex("firmware-[0-9a-f-]{36}").matches(name))
        val previous = File(backups, name)
        if (!success && (previous.exists() || p.getProperty("had_previous") != "true")) {
            val failed = File(guest, "dev_flash")
            if (failed.exists()) {
                val rejected = File(backups, "rejected-firmware-${UUID.randomUUID()}")
                check(failed.renameTo(rejected))
            }
            if (previous.exists()) {
                check(previous.renameTo(failed)) {
                    "Previous firmware remains in backups; retry recovery"
                }
            }
        }
        check(record.delete())
    }

    @Synchronized fun markFirmwareInstallStarted() {
        val record = File(root, "firmware-recovery.properties")
        val p = Properties().also { record.inputStream().use(it::load) }
        p.setProperty("started", "true")
        record.outputStream().use { p.store(it, "Native PS3 firmware installation started") }
    }

    fun firmwareInstallInterrupted(): Boolean {
        val record = File(root, "firmware-recovery.properties")
        return record.isFile && Properties().also { record.inputStream().use(it::load) }
            .getProperty("started") == "true"
    }

    @Synchronized fun importRap(
        id: String,
        name: String,
        bytes: ByteArray,
    ) {
        require(bytes.size == 16 && Regex("[A-Za-z0-9_-]{1,128}\\.rap", RegexOption.IGNORE_CASE).matches(name)) {
            "Select an original 16-byte RAP licence with its original filename"
        }
        val directory = File(user(id), "exdata").apply { check(mkdirs() || isDirectory) }
        val target = File(directory, name)
        val pending = File(directory, ".licence.pending")
        pending.outputStream().use { it.write(bytes) }
        if (target.exists()) {
            check(backups.mkdirs() || backups.isDirectory)
            target.copyTo(File(backups, "licence-${UUID.randomUUID()}-$name"))
        }
        check(pending.renameTo(target))
    }

    fun hasPendingFirmwareReplacement(): Boolean = File(root, "firmware-recovery.properties").isFile

    fun firmwareInstalled(): Boolean = File(guest, "dev_flash/sys/external/liblv2.sprx").isFile
}
