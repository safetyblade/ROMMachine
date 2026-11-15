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
    init { require(engine in setOf("armsx3", "rpcs3-libretro")) }
    val root = File(files, "native/ps3/$engine")
    val guest = File(root, if (engine == "armsx3") "guest" else "system/rpcs3")
    val backups = File(root, "backups")
    val cache = File(root, "cache")
    private val profileFile = File(root, "profiles.properties")
    data class Profile(val id: String, val name: String, val selected: Boolean)
    data class Save(val name: String, val title: String, val titleId: String?, val modified: Long, val bytes: Long)

    private fun properties(): Properties = Properties().also { if (profileFile.isFile) profileFile.inputStream().use(it::load) }
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
        val id = (1..99999999).first { p.getProperty("user.%08d".format(java.util.Locale.US, it)) == null }
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
    @Synchronized fun rename(id: String, name: String) {
        require(name.isNotBlank() && name.length <= 64)
        val p = properties()
        require(p.containsKey("user.$id"))
        File(user(id), "localusername").writeText(name.trim())
        p.setProperty("user.$id", name.trim())
        write(p)
    }
    @Synchronized fun removeProfile(id: String, confirmed: Boolean) {
        require(confirmed && profiles().size > 1)
        val p = properties()
        require(p.containsKey("user.$id"))
        // Archive the actual user, never relabel or overwrite another user's save directory.
        check(backups.mkdirs() || backups.isDirectory)
        check(user(id).renameTo(File(backups, "profile-$id-${UUID.randomUUID()}")))
        p.remove("user.$id")
        if (p.getProperty("selected") == id) p.setProperty("selected", p.stringPropertyNames().first { it.startsWith("user.") }.removePrefix("user."))
        write(p)
    }
    private fun save(userId: String, name: String): File {
        require(Regex("[A-Za-z0-9_.-]{1,128}").matches(name) && name !in setOf(".", ".."))
        val base = File(user(userId), "savedata").canonicalFile
        return File(base, name).canonicalFile.also { require(it.parentFile == base) }
    }
    fun saves(id: String): List<Save> = File(user(id), "savedata").listFiles().orEmpty().filter { it.isDirectory && !java.nio.file.Files.isSymbolicLink(it.toPath()) }.mapNotNull { dir ->
        // Save SFO category is SD and not accepted as launchable game metadata.
        Save(dir.name, dir.name, Regex("[A-Z]{4}[0-9]{5}").find(dir.name)?.value, dir.lastModified(), usage(dir))
    }.sortedByDescending { it.modified }
    fun usage(directory: File = root): Long = directory.walkTopDown().onEnter {
        !java.nio.file.Files.isSymbolicLink(it.toPath())
    }.filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }.sumOf { it.length() }
    fun export(id: String, name: String, output: OutputStream) {
        val directory = save(id, name)
        require(directory.isDirectory)
        ZipOutputStream(output).use { zip ->
            directory.walkTopDown().onEnter { !java.nio.file.Files.isSymbolicLink(it.toPath()) }.filter { it.isFile }.forEach {
                require(!java.nio.file.Files.isSymbolicLink(it.toPath()))
                zip.putNextEntry(ZipEntry("$name/" + it.relativeTo(directory).invariantSeparatorsPath))
                it.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
    @Synchronized fun restore(id: String, name: String, input: InputStream, confirmed: Boolean) {
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
                    if (entry.isDirectory) check(destination.mkdirs() || destination.isDirectory) else {
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
            require(File(pending, "PARAM.SFO").isFile && File(pending, "PARAM.PFD").isFile) { "Not a complete PS3 save directory" }
            check(backups.mkdirs() || backups.isDirectory)
            val previous = File(backups, "$id-$name-${UUID.randomUUID()}")
            if (target.exists()) check(target.renameTo(previous))
            check(target.parentFile!!.mkdirs() || target.parentFile!!.isDirectory)
            if (!pending.renameTo(target)) {
                if (previous.exists()) check(previous.renameTo(target))
                error("Restore failed; previous save retained")
            }
        } finally { pending.deleteRecursively() }
    }
    @Synchronized fun deleteSave(id: String, name: String, confirmed: Boolean) {
        require(confirmed)
        check(backups.mkdirs() || backups.isDirectory)
        check(save(id, name).renameTo(File(backups, "$id-$name-${UUID.randomUUID()}")))
    }
    fun firmwareInstalled(): Boolean = File(guest, "dev_flash/sys/external/liblv2.sprx").isFile
}
