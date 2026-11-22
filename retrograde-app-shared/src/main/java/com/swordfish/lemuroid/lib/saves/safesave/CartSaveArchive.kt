package com.swordfish.lemuroid.lib.saves.safesave

import com.swordfish.lemuroid.lib.mods.ModArchive
import com.swordfish.lemuroid.lib.mods.ModFiles
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Portable snapshot transport. Archive metadata grants no authority over live storage. */
object CartSaveArchive {
    @Serializable
    enum class Kind { GAME_SAVE, MANUAL_STATE, AUTOSAVE, STATE_METADATA, STATE_PREVIEW, MEMORY_CARD, VMU }

    @Serializable
    enum class Scope { GAME, CONSOLE, PLAYER, ALL_PLAYERS, CARD_BANK }

    @Serializable
    data class Player(val id: String, val exportAlias: String? = null)

    @Serializable
    data class Source(val appSchema: Int, val player: Player?, val deviceLabel: String? = null)

    @Serializable
    data class Exclusion(val engineId: String, val reason: String)

    @Serializable
    data class Coverage(val verified: Int, val skipped: Int, val skippedReasons: List<Exclusion>)

    @Serializable
    data class Artifact(
        val artifactId: String,
        val saveKind: Kind,
        val path: String,
        val byteLength: Long,
        val sha256: String,
        val slot: String? = null,
        val relativePath: String? = null,
    )

    @Serializable
    data class Group(
        val groupId: String,
        val scope: Scope,
        val platformId: String,
        val engineId: String,
        val engineVersion: String,
        val linkedCartridgeIds: List<String>,
        val artifacts: List<Artifact>,
        val nativeUserRef: String? = null,
        val ownerId: String? = null,
    )

    @Serializable
    data class Manifest(
        val snapshotId: String,
        val createdAtUtc: String,
        val scope: Scope,
        val source: Source,
        val coverage: Coverage,
        val saveGroups: List<Group>,
        val format: String = FORMAT,
        val schemaVersion: Int = 1,
    )

    private const val FORMAT = "cartdeck.safesave"
    private const val MANIFEST_BYTES = 1024 * 1024
    private const val MAX_ENTRIES = 4096
    private const val MAX_PAYLOAD_BYTES = 500L * 1024 * 1024
    const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024
    private val utcTimestamp = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z")
    private val shaPattern = Regex("[a-f0-9]{64}")
    private val json = Json { encodeDefaults = true }
    private val zipInspector =
        ModArchive(
            ModArchive.Limits(entries = MAX_ENTRIES, compressedBytes = MAX_ARCHIVE_BYTES),
        )

    /** Write to a fresh private staging file. Publish only after [inspect] succeeds. */
    fun write(
        destination: File,
        manifest: Manifest,
        payloads: Map<String, File>,
        cancelled: () -> Boolean = { false },
    ) {
        val artifacts = validate(manifest)
        require(payloads.keys == artifacts.map { it.artifactId }.toSet())
        val metadata = json.encodeToString(Manifest.serializer(), manifest).encodeToByteArray()
        require(metadata.size <= MANIFEST_BYTES)
        check(destination.createNewFile()) { "SafeSave staging file already exists" }
        try {
            ZipOutputStream(destination.outputStream().buffered()).use { zip ->
                // Bounded reader rejects high compression ratios, including legitimate blank cards.
                zip.setLevel(Deflater.NO_COMPRESSION)
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(metadata)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("manifest.sha256"))
                zip.write(hash(metadata).encodeToByteArray())
                zip.closeEntry()
                for (artifact in artifacts) {
                    val source = payloads.getValue(artifact.artifactId)
                    require(source.isFile && source.absoluteFile == source.canonicalFile)
                    zip.putNextEntry(ZipEntry(artifact.path))
                    source.inputStream().use { input ->
                        val actual =
                            digest(input, artifact.byteLength, cancelled) { bytes, count ->
                                zip.write(bytes, 0, count)
                            }
                        require(actual == artifact.sha256) { "Snapshot changed while packaging it" }
                    }
                    zip.closeEntry()
                }
            }
            require(inspect(destination, cancelled) == manifest)
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        }
    }

    /** Stream and verify every payload; a manifest checksum alone is insufficient. */
    fun inspect(
        archive: File,
        cancelled: () -> Boolean = { false },
    ): Manifest {
        val entries = zipInspector.inspect(archive).associateBy { it.path }
        require(entries.values.none { it.directory })
        return ZipFile(archive).use { zip ->
            fun bytes(
                name: String,
                limit: Int,
            ): ByteArray {
                val entry = entries[name] ?: error("Missing SafeSave metadata")
                require(entry.size in 1L..limit.toLong())
                return zip.getInputStream(zip.getEntry(name)).use { input ->
                    val data = ByteArray(entry.size.toInt())
                    java.io.DataInputStream(input).readFully(data)
                    require(input.read() == -1)
                    data
                }
            }
            val raw = bytes("manifest.json", MANIFEST_BYTES)
            require(hash(raw) == bytes("manifest.sha256", 64).decodeToString())
            val text = raw.decodeToString(throwOnInvalidSequence = true)
            requireBoundedJson(text)
            val manifest = json.decodeFromString(Manifest.serializer(), text)
            val artifacts = validate(manifest)
            require(entries.keys == artifacts.map { it.path }.toSet() + setOf("manifest.json", "manifest.sha256"))
            for (artifact in artifacts) {
                require(entries.getValue(artifact.path).size == artifact.byteLength)
                val actual =
                    zip.getInputStream(zip.getEntry(artifact.path)).use {
                        digest(it, artifact.byteLength, cancelled)
                    }
                require(actual == artifact.sha256) { "A SafeSave payload failed verification" }
            }
            manifest
        }
    }

    /** Extract to a private sandbox, after verifying the complete archive. */
    fun extractVerified(
        archive: File,
        stagingParent: File,
        expected: Manifest,
    ): File {
        require(inspect(archive) == expected) { "The selected backup changed after preview" }
        val payloads = expected.saveGroups.flatMap { it.artifacts }.map { it.path }.toSet()
        return zipInspector.extract(archive, stagingParent, {
            it in payloads || it == "manifest.json" || it == "manifest.sha256"
        })
    }

    fun artifact(
        kind: Kind,
        file: File,
        relativePath: String? = null,
        slot: String? = null,
    ): Artifact {
        val id = UUID.randomUUID().toString()
        val size = file.length()
        require(file.isFile && file.absoluteFile == file.canonicalFile && size in 0..MAX_PAYLOAD_BYTES)
        return Artifact(id, kind, "artifacts/$id/content.bin", size, ModFiles.sha256(file), slot, relativePath)
    }

    private fun validate(manifest: Manifest): List<Artifact> {
        require(manifest.format == FORMAT && manifest.schemaVersion == 1)
        requireCartridgeUuid(manifest.snapshotId)
        require(manifest.source.appSchema > 0)
        manifest.source.player?.let { requireCartridgeUuid(it.id) }
        require(utcTimestamp.matches(manifest.createdAtUtc))
        require(manifest.saveGroups.size in 1..MAX_ENTRIES - 2)
        require(manifest.coverage.verified == manifest.saveGroups.size)
        require(manifest.coverage.skipped == manifest.coverage.skippedReasons.size)
        require(manifest.coverage.skipped in 0..MAX_ENTRIES)
        val groups = mutableSetOf<String>()
        val identifiers = mutableSetOf<String>()
        val artifacts = mutableListOf<Artifact>()
        var bytes = 0L
        for (group in manifest.saveGroups) {
            require(groups.add(requireCartridgeUuid(group.groupId)))
            require(group.nativeUserRef == null) { "Native account identifiers cannot be shared" }
            group.ownerId?.let { requireCartridgeUuid(it) }
            require(group.platformId.length in 1..64 && group.engineId.length in 1..64)
            require(group.engineVersion.length in 1..128)
            require(group.linkedCartridgeIds.isNotEmpty() && group.linkedCartridgeIds.size <= MAX_ENTRIES)
            group.linkedCartridgeIds.forEach { requireCartridgeUuid(it) }
            require(group.artifacts.isNotEmpty())
            val names = mutableSetOf<String>()
            for (artifact in group.artifacts) {
                require(identifiers.add(requireCartridgeUuid(artifact.artifactId)))
                require(artifact.path == "artifacts/${artifact.artifactId}/content.bin")
                require(artifact.byteLength in 0..MAX_PAYLOAD_BYTES - bytes)
                require(shaPattern.matches(artifact.sha256))
                artifact.relativePath?.let {
                    require(ModFiles.relativePath(it) == it && names.add(it.lowercase(java.util.Locale.ROOT)))
                }
                require(artifacts.size < MAX_ENTRIES - 2)
                bytes += artifact.byteLength
                artifacts += artifact
            }
        }
        return artifacts
    }

    private fun requireBoundedJson(text: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        for (char in text) {
            if (quoted) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    quoted = false
                }
            } else {
                when (char) {
                    '"' -> quoted = true
                    '{', '[' -> require(++depth <= 24)
                    '}', ']' -> require(--depth >= 0)
                }
            }
        }
        require(depth == 0 && !quoted)
    }

    private fun digest(
        input: InputStream,
        expectedBytes: Long,
        cancelled: () -> Boolean,
        copy: (ByteArray, Int) -> Unit = { _, _ -> },
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        var read = 0L
        while (true) {
            check(!cancelled()) { "SafeSave operation cancelled" }
            val count = input.read(buffer)
            if (count == -1) break
            read += count
            require(read <= expectedBytes) { "SafeSave payload exceeds its declared size" }
            digest.update(buffer, 0, count)
            copy(buffer, count)
        }
        require(read == expectedBytes) { "Incomplete SafeSave payload" }
        return hex(digest.digest())
    }

    private fun hash(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
