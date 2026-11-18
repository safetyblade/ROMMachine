package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.net.ssl.HttpsURLConnection

/** Version-level activation, independent of all personal and emulator storage. */
internal class GemCatalogueVersions(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "gem-catalogue-versions")
    private val pointer = AtomicFile(File(root, "state.json"))

    @Serializable
    data class Ref(val sequence: Long, val version: String, val digest: String)

    @Serializable
    private data class State(
        val schema: Int = 1,
        val active: Ref = BASELINE,
        val previous: Ref? = null,
        val highestSequence: Long = 0,
        val identities: Map<String, String> = emptyMap(),
    )

    data class Status(
        val active: String,
        val previous: String?,
        val loaded: String,
        val warning: String?,
        val signingConfigured: Boolean,
        val onlineConfigured: Boolean,
    )

    data class Candidate(
        val verified: GemSignedCatalogue.Verified,
        val expectedActive: Ref,
        val expectedHighest: Long,
        val added: Int,
        val retired: Int,
    )

    private fun asset(name: String): ByteArray? =
        try {
            app.assets.open(name).use { it.readBytes() }
        } catch (_: FileNotFoundException) {
            null
        }

    private fun signingKey(): PublicKey? =
        asset("gem/update-public-key.der")?.let {
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(it))
        }

    private fun endpoint(): URL? =
        asset("gem/update-origin.txt")?.decodeToString()?.trim()?.let {
            URL(it).also { url ->
                require(url.protocol == "https" && url.userInfo == null && url.host.isNotBlank())
            }
        }

    private fun state(): State {
        val stored =
            try {
                pointer.openRead().use { GemProfileBackup.read(it).decodeToString(throwOnInvalidSequence = true) }
            } catch (failure: FileNotFoundException) {
                if (pointer.baseFile.exists() || File(root, "state.json.bak").exists()) throw failure
                return State()
            }
        val result = Json.decodeFromString(State.serializer(), stored)
        require(result.schema == 1 && result.highestSequence >= 0)
        require(result.active.sequence in 0..result.highestSequence)
        require(result.previous == null || result.previous.sequence in 0..result.highestSequence)
        require(result.identities.size <= 200_000)
        require(result.identities.values.all { it.matches(HEX) })
        return result
    }

    private fun file(ref: Ref): File {
        require(ref.sequence > 0 && ref.digest.matches(HEX))
        return File(root, ref.digest + ".gemcatalogue")
    }

    private fun loadRef(ref: Ref): GemCataloguePackage.Loaded {
        if (ref == BASELINE) return GemProductionCatalogue.load(app)
        val bytes = file(ref).inputStream().use(GemSignedCatalogue::read)
        require(GemProfileBackup.digest(bytes) == ref.digest)
        val verified = GemSignedCatalogue.verify(bytes, requireNotNull(signingKey()))
        require(verified.sequence == ref.sequence && verified.catalogue.manifest.catalogueVersion == ref.version)
        return verified.catalogue
    }

    fun load(): GemCataloguePackage.Loaded =
        synchronized(lock) {
            val current = runCatching { state() }.getOrNull()
            if (current != null) {
                runCatching { loadRef(current.active) }.getOrNull()?.let { return@synchronized it }
                current.previous?.let {
                    runCatching { loadRef(it) }.getOrNull()?.let { loaded -> return@synchronized loaded }
                }
            }
            GemProductionCatalogue.load(app)
        }

    fun status(): Status =
        synchronized(lock) {
            val current = runCatching { state() }.getOrNull()
            val loaded = load().manifest.catalogueVersion
            Status(
                active = current?.active?.version ?: "Unavailable",
                previous = current?.previous?.version,
                loaded = loaded,
                warning =
                    if (current == null || loaded != current.active.version) {
                        "Active catalogue unavailable; using verified fallback. No player data changed."
                    } else {
                        null
                    },
                signingConfigured = runCatching { signingKey() != null }.getOrDefault(false),
                onlineConfigured = runCatching { signingKey() != null && endpoint() != null }.getOrDefault(false),
            )
        }

    fun preview(input: InputStream): Candidate =
        synchronized(lock) {
            val key = requireNotNull(signingKey()) { "Catalogue signing configuration not installed" }
            previewVerified(GemSignedCatalogue.verify(GemSignedCatalogue.read(input), key))
        }

    private fun previewVerified(verified: GemSignedCatalogue.Verified): Candidate {
        val current = state()
        require(verified.sequence > current.highestSequence) { "Update is not newer than accepted history" }
        val active = loadRef(current.active)
        val incoming = verified.catalogue.manifest
        require(incoming.catalogueVersion != current.active.version && incoming.catalogueVersion != BASELINE.version)
        val history = GemCatalogueVersionPolicy.extend(current.identities, active.manifest.releases)
        GemCatalogueVersionPolicy.extend(history, incoming.releases)
        val oldIds = active.manifest.releases.map { it.releaseId }.toSet()
        val newIds = incoming.releases.map { it.releaseId }.toSet()
        return Candidate(
            verified,
            current.active,
            current.highestSequence,
            (newIds - oldIds).size,
            (oldIds - newIds).size,
        )
    }

    fun activate(candidate: Candidate): String =
        synchronized(lock) {
            val current = state()
            check(current.active == candidate.expectedActive && current.highestSequence == candidate.expectedHighest) {
                "Catalogue changed; preview again"
            }
            val verified = GemSignedCatalogue.verify(candidate.verified.bytes, requireNotNull(signingKey()))
            previewVerified(verified)
            val active = loadRef(current.active)
            val history = GemCatalogueVersionPolicy.extend(current.identities, active.manifest.releases)
            val nextHistory = GemCatalogueVersionPolicy.extend(history, verified.catalogue.manifest.releases)
            val next = Ref(verified.sequence, verified.catalogue.manifest.catalogueVersion, verified.digest)
            write(AtomicFile(file(next)), verified.bytes)
            loadRef(next)
            save(
                State(
                    active = next,
                    previous = current.active,
                    highestSequence = next.sequence,
                    identities = nextHistory,
                ),
            )
            prune(next, current.active)
            next.version
        }

    private fun prune(
        active: Ref,
        previous: Ref,
    ) {
        val retained = setOf(active.digest, previous.digest)
        root.listFiles()?.forEach { file ->
            val digest = file.name.removeSuffix(".gemcatalogue")
            if (file.name.endsWith(".gemcatalogue") && digest.matches(HEX) && digest !in retained) {
                file.delete()
            }
        }
    }

    fun rollback(): String =
        synchronized(lock) {
            val current = state()
            val previous = requireNotNull(current.previous) { "No previous approved version" }
            loadRef(previous)
            save(current.copy(active = previous, previous = current.active))
            previous.version
        }

    /** Explicit user action only; never runs on startup, never follows redirects. */
    fun fetchPreview(): Candidate {
        val url = requireNotNull(endpoint()) { "Online source not configured" }
        requireNotNull(signingKey()) { "Catalogue signing configuration not installed" }
        val connection = url.openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            require(connection.responseCode == 200) { "Catalogue source unavailable" }
            require(connection.contentLengthLong <= GemSignedCatalogue.MAX_BYTES)
            return connection.inputStream.use(::preview)
        } finally {
            connection.disconnect()
        }
    }

    private fun save(state: State) {
        val bytes = Json.encodeToString(State.serializer(), state).encodeToByteArray()
        require(bytes.size <= GemProfileBackup.MAX_BYTES)
        write(pointer, bytes)
    }

    private fun write(
        file: AtomicFile,
        bytes: ByteArray,
    ) {
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (failure: Throwable) {
            file.failWrite(stream)
            throw failure
        }
        check(file.openRead().use { it.readBytes() }.contentEquals(bytes))
    }

    companion object {
        private val lock = Any()
        private val HEX = Regex("[a-f0-9]{64}")
        private val BASELINE = Ref(0, GemProductionCatalogue.VERSION, "")
    }
}
