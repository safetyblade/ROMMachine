package com.swordfish.lemuroid.app.shared.game.xbox

import java.io.File
import java.security.MessageDigest
import java.util.Properties

/** An atomic, immutable package selection. Engine rollback never deletes or replaces console data. */
class XboxEngineStore(
    private val root: File,
    private val coreName: String = "libhakuxcore.so",
    private val requiredLibraries: Set<String> = setOf("libhakuxcore.so", "libSDL3.so"),
    private val manifestName: String = "hakux.properties",
) {
    data class Selection(
        val id: String,
        val directory: File,
        val properties: Properties,
        private val coreName: String = "libhakuxcore.so",
    ) {
        val core get() = File(directory, coreName)
        val sdl get() = File(directory, "libSDL3.so")
    }

    fun install(
        nativeDirectory: File,
        properties: Properties,
        notices: ByteArray,
    ): Selection {
        val revision = properties.getProperty("revision")
        require(revision.matches(Regex("[0-9a-f]{40}"))) { "Invalid Xbox engine revision" }
        val coreHash = properties.getProperty("core.sha256")
        require(coreHash.matches(Regex("[0-9a-f]{64}"))) { "Invalid Xbox engine hash" }
        val id = revision + "-" + coreHash.take(12)
        check(root.isDirectory || root.mkdirs())
        val target = File(File(root, "versions"), id)
        if (!target.exists()) {
            val pending = File(target.parentFile, ".$id.pending")
            check(!pending.exists() || pending.deleteRecursively())
            check(pending.mkdirs())
            try {
                libraries(properties).forEach { name ->
                    val source = File(nativeDirectory, name)
                    check(hash(source) == properties.getProperty("library.$name.sha256")) {
                        "Xbox native dependency failed verification: $name"
                    }
                    val output = File(pending, name)
                    source.copyTo(output)
                    check(output.setReadOnly()) { "Cannot protect Xbox engine binary" }
                }
                File(pending, "hakux-licenses.zip").writeBytes(notices)
                File(
                    pending,
                    manifestName,
                ).outputStream().use { properties.store(it, "Verified CARTDECK Xbox package") }
                check(pending.renameTo(target)) { "Cannot finish Xbox engine installation" }
            } finally {
                pending.deleteRecursively()
            }
        }
        val installed = verified(id)
        if (installed == null) {
            // A damaged immutable cache must not make its intact previous package unreachable.
            // Keep the failed bytes for diagnosis and select only independently verified files.
            val fallback = selected() ?: error("Stored Xbox engine package is corrupt")
            writePointer("active", fallback.id)
            return fallback
        }
        val current = pointer("active")
        // An explicit rollback stays selected across launches until a newer APK package arrives.
        if (pointer("packaged") != id) {
            if (current != null && verified(current) != null && current != id) writePointer("previous", current)
            writePointer("active", id)
            writePointer("packaged", id)
        } else if (selected() == null) {
            writePointer("active", id)
        }
        return selected() ?: installed
    }

    fun selected(): Selection? =
        listOfNotNull(
            pointer("active"),
            pointer("previous"),
        ).distinct().firstNotNullOfOrNull(::verified)

    fun canRollback(): Boolean = pointer("previous")?.let(::verified)?.id?.let { it != selected()?.id } == true

    fun rollback(): Boolean {
        val previous = pointer("previous")?.let(::verified) ?: return false
        val current = selected()
        if (previous.id == current?.id) return false
        writePointer("active", previous.id)
        if (current != null && current.id != previous.id) writePointer("previous", current.id)
        return true
    }

    private fun verified(id: String): Selection? {
        if (!validId(id)) return null
        return runCatching {
            val dir = File(File(root, "versions"), id)
            val props = Properties().apply { File(dir, manifestName).inputStream().use { load(it) } }
            check(props.getProperty("revision") + "-" + props.getProperty("core.sha256").take(12) == id)
            libraries(props).forEach {
                    name ->
                check(hash(File(dir, name)) == props.getProperty("library.$name.sha256"))
            }
            Selection(id, dir, props, coreName)
        }.getOrNull()
    }

    private fun libraries(props: Properties): List<String> {
        val names = props.getProperty("library.names").split(",")
        require(names.distinct().size == names.size && names.all { it.matches(Regex("lib[A-Za-z0-9_]+\\.so")) })
        require(names.containsAll(requiredLibraries))
        require(props.getProperty("library.$coreName.sha256") == props.getProperty("core.sha256"))
        return names
    }

    private fun pointer(name: String): String? =
        File(root, "$name.version").takeIf {
            it.isFile
        }?.readText()?.trim()?.takeIf(::validId)

    private fun writePointer(
        name: String,
        id: String,
    ) {
        require(validId(id))
        check(root.isDirectory || root.mkdirs())
        val temp = File(root, ".$name.pending")
        temp.writeText(id)
        check(temp.renameTo(File(root, "$name.version"))) { "Cannot select Xbox engine version" }
    }

    private fun validId(id: String) = id.matches(Regex("[0-9a-f]{40}-[0-9a-f]{12}"))

    companion object {
        fun hash(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
