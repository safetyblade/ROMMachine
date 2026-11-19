package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

/**
 * Single-player bridge until PROF-01 supplies the authoritative active profile session.
 * The generated ID must be adopted by the default Manager, not replaced during migration.
 * No save paths, Room tables, account service or Android backup are used.
 */
internal class GemRatingLocalStore private constructor(
    val session: GemRatingRepository.Session,
    val repository: GemRatingRepository,
    private val root: File,
) {
    fun previousBackup(): GemRatingRepository.Snapshot? {
        repository.snapshot(session)
        val saved = readExisting(AtomicFile(File(root, "${session.profileId}.pre-restore.json"))) ?: return null
        return GemProfileBackup.decode(saved.encodeToByteArray()).also { require(it.profileId == session.profileId) }
    }

    companion object {
        private val lock = Any()

        fun open(context: Context): GemRatingLocalStore =
            synchronized(lock) {
                val root = File(context.noBackupFilesDir, "gem-ratings")
                val ownerFile = AtomicFile(File(root, "legacy-manager-id"))
                val owner = readExisting(ownerFile)
                val id =
                    if (owner == null) {
                        val retained = if (root.exists()) requireNotNull(root.listFiles()) else emptyArray()
                        check(retained.none { it.name.contains(".json") }) { "Legacy rating owner identity missing" }
                        UUID.randomUUID().toString().also { writeVerified(ownerFile, it) }
                    } else {
                        owner
                    }
                GemRatingRepository.requireUuid(id)
                val session = GemRatingRepository.Session(id, GemRatingRepository.Role.MANAGER)
                val repository = GemRatingRepository({ session }, FileStorage(root, context.applicationContext))
                GemRatingLocalStore(session, repository, root)
            }

        private fun readExisting(file: AtomicFile): String? =
            try {
                file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (failure: FileNotFoundException) {
                val base = file.baseFile
                if (base.exists() || File(base.path + ".bak").exists() || File(base.path + ".new").exists()) {
                    throw failure
                }
                null
            }

        private fun writeVerified(
            file: AtomicFile,
            content: String,
        ) {
            val stream = file.startWrite()
            try {
                stream.write(content.toByteArray(Charsets.UTF_8))
                file.finishWrite(stream)
            } catch (failure: Throwable) {
                file.failWrite(stream)
                throw failure
            }
            check(readExisting(file) == content) { "Rating storage verification failed" }
        }

        private class FileStorage(
            private val root: File,
            private val context: Context,
        ) : GemRatingRepository.Storage {
            private fun file(profileId: String): AtomicFile {
                GemRatingRepository.requireUuid(profileId)
                return AtomicFile(File(root, "$profileId.json"))
            }

            override fun read(profileId: String): GemRatingRepository.Snapshot {
                val data = readExisting(file(profileId))
                val snapshot =
                    data?.let { Json.decodeFromString(GemRatingRepository.Snapshot.serializer(), it) }
                        ?: GemRatingRepository.Snapshot(profileId)
                if (snapshot.preferences != null) return snapshot
                val discovery = context.getSharedPreferences("gem_discovery_v1", Context.MODE_PRIVATE)
                val shelf = context.getSharedPreferences("gem_shelf_presentation_v1", Context.MODE_PRIVATE)
                val recent =
                    discovery.getString("$profileId:recent", "").orEmpty().split('\n')
                        .filter { it.length in 1..256 && it.none(Char::isISOControl) }.distinct().takeLast(20)
                return snapshot.copy(
                    preferences =
                        GemRatingRepository.Preferences(
                            wildcardPercent = discovery.getInt("$profileId:wildcard", 30).coerceIn(20, 50),
                            recentSurprises = recent,
                            showShelfRatings = shelf.getBoolean("$profileId:showRatings", false),
                        ),
                )
            }

            override fun write(snapshot: GemRatingRepository.Snapshot) {
                val previous = read(snapshot.profileId)
                val data = Json.encodeToString(GemRatingRepository.Snapshot.serializer(), snapshot)
                writeVerified(file(snapshot.profileId), data)
                if (
                    previous.records != snapshot.records ||
                    previous.preferences?.showShelfRatings != snapshot.preferences?.showShelfRatings
                ) {
                    GemShelfPresentation.changed()
                }
            }

            override fun backup(snapshot: GemRatingRepository.Snapshot) {
                GemRatingRepository.requireUuid(snapshot.profileId)
                val target = AtomicFile(File(root, "${snapshot.profileId}.pre-restore.json"))
                writeVerified(target, GemProfileBackup.encode(snapshot).decodeToString())
            }
        }
    }
}
