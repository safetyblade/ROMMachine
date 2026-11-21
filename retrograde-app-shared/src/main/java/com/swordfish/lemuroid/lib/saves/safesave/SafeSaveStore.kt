package com.swordfish.lemuroid.lib.saves.safesave

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/**
 * SAVE-A01 local SafeSave capture/restore for one PSP savedata group.
 *
 * Contract (JR01 SafeSave V1 state machine):
 * DISCOVER -> QUIESCE -> STAGE -> HASH_VERIFY -> REGISTER -> COMPLETE, and
 * restore as DEST_BACKUP -> STAGE_RESTORE -> VERIFY_RESTORE -> ACTIVATE ->
 * JOURNAL. Originals are never silently overwritten; every failure is
 * journalled and reported truthfully, with the original data preserved.
 *
 * Synchronous on purpose: callers run it on an IO dispatcher. It performs no
 * startup scan and hashes only the one group being captured or restored.
 */
class SafeSaveStore(
    private val index: SafeSaveIndex,
    private val snapshotRoot: File,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    data class Metrics(val durationMs: Long, val fileCount: Int, val totalBytes: Long)

    data class CaptureResult(val snapshot: SafeSaveSnapshotRecord, val metrics: Metrics)

    data class RestoreResult(val snapshotId: String, val backupSnapshotDir: File?, val metrics: Metrics)

    fun capture(
        modernSaveRoot: File,
        groupName: String,
        cartridgeUuid: String,
        engineVersion: String,
        engineQuiesced: Boolean,
    ): CaptureResult {
        val started = clock()
        check(engineQuiesced) { "Engine must be closed/quiesced before SafeSave capture" }
        val uuid = requireCartridgeUuid(cartridgeUuid)
        // Wrong-game / ambiguous-association guard: a group already bound to a
        // different cartridge UUID is blocked, never reassigned implicitly.
        val conflicting = index.snapshotsForGroup(groupName).any { it.cartridgeUuid != uuid }
        check(!conflicting) { "Savedata group is already bound to a different cartridge" }

        val group = PspSavedataAdapter.discover(modernSaveRoot, groupName)
        requireFreeSpace(snapshotRoot, group.totalBytes)

        val snapshotId = UUID.randomUUID().toString()
        val opId = UUID.randomUUID().toString()
        val staging = File(snapshotRoot, ".staging-$snapshotId")
        val destination = File(snapshotRoot, snapshotId)
        try {
            check(staging.mkdirs()) { "Unable to stage SafeSave snapshot" }
            val records = group.files.map { source ->
                val relative = PspSavedataAdapter.relativePath(group.directory, source)
                val copy = File(staging, relative)
                check(copy.parentFile?.isDirectory == true || copy.parentFile?.mkdirs() == true) {
                    "Unable to stage SafeSave file"
                }
                source.copyTo(copy, overwrite = false)
                val sourceHash = sha256(source)
                check(sha256(copy) == sourceHash) { "SafeSave capture verification failed for $relative" }
                SafeSaveFileRecord(relative, source.length(), sourceHash)
            }
            val snapshot = SafeSaveSnapshotRecord(
                snapshotId = snapshotId,
                cartridgeUuid = uuid,
                platform = PspSavedataAdapter.PLATFORM,
                engineId = PspSavedataAdapter.ENGINE_ID,
                engineVersion = engineVersion,
                saveType = PspSavedataAdapter.SAVE_TYPE,
                groupName = groupName,
                saveTimestamp = group.latestModified,
                captureTime = clock(),
                verificationState = SafeSaveVerificationState.VERIFIED,
                totalBytes = group.totalBytes,
                fileCount = records.size,
                files = records,
            )
            check(staging.renameTo(destination)) { "Unable to activate SafeSave snapshot" }
            index.insertSnapshot(snapshot)
            index.appendJournal(SafeSaveJournalRecord(opId, "CAPTURE", snapshotId, "COMPLETE", "VERIFIED", clock()))
            return CaptureResult(snapshot, Metrics(clock() - started, records.size, group.totalBytes))
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (destination.exists() && index.snapshot(snapshotId) == null) destination.deleteRecursively()
            index.appendJournal(
                SafeSaveJournalRecord(opId, "CAPTURE", null, "FAILED", error.message ?: "capture failed", clock()),
            )
            throw error
        }
    }

    fun restore(
        snapshotId: String,
        modernSaveRoot: File,
        engineQuiesced: Boolean,
    ): RestoreResult {
        val started = clock()
        check(engineQuiesced) { "Engine must be closed/quiesced before SafeSave restore" }
        val snapshot = index.snapshot(snapshotId) ?: throw IOException("SafeSave snapshot not found: $snapshotId")
        check(snapshot.verificationState == SafeSaveVerificationState.VERIFIED) { "Snapshot is not verified" }
        val snapshotDir = File(snapshotRoot, snapshotId)
        // Corruption gate: verify stored bytes before touching the destination.
        snapshot.files.forEach { record ->
            val file = File(snapshotDir, record.relativePath)
            check(file.isFile && file.length() == record.sizeBytes && sha256(file) == record.sha256) {
                "Snapshot file failed verification: ${record.relativePath}"
            }
        }
        val savedataRoot = PspSavedataAdapter.savedataRoot(modernSaveRoot)
        val destination = File(savedataRoot, snapshot.groupName)
        requireFreeSpace(savedataRoot, snapshot.totalBytes * 2)

        val opId = UUID.randomUUID().toString()
        val backupDir = File(snapshotRoot, "backup-$opId-${snapshot.groupName}")
        val staging = File(savedataRoot, ".restore-staging-$opId")
        val displaced = File(savedataRoot, ".restore-displaced-$opId")
        var activated = false
        try {
            if (destination.exists()) {
                copyTreeVerified(destination, backupDir)
                index.appendJournal(
                    SafeSaveJournalRecord(opId, "RESTORE", snapshotId, "DEST_BACKUP", backupDir.name, clock()),
                )
            }
            snapshot.files.forEach { record ->
                val target = File(staging, record.relativePath)
                check(target.parentFile?.isDirectory == true || target.parentFile?.mkdirs() == true) {
                    "Unable to stage SafeSave restore"
                }
                File(snapshotDir, record.relativePath).copyTo(target, overwrite = false)
                check(sha256(target) == record.sha256) { "Restored file failed verification: ${record.relativePath}" }
            }
            if (destination.exists()) check(destination.renameTo(displaced)) { "Unable to displace current savedata" }
            check(staging.renameTo(destination)) { "Unable to activate SafeSave restore" }
            activated = true
            displaced.deleteRecursively()
            index.appendJournal(SafeSaveJournalRecord(opId, "RESTORE", snapshotId, "COMPLETE", "VERIFIED", clock()))
            return RestoreResult(snapshotId, if (backupDir.exists()) backupDir else null,
                Metrics(clock() - started, snapshot.fileCount, snapshot.totalBytes))
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (!activated && displaced.exists() && !destination.exists()) {
                displaced.renameTo(destination)
            }
            index.appendJournal(
                SafeSaveJournalRecord(
                    opId,
                    "RESTORE",
                    snapshotId,
                    "FAILED",
                    error.message ?: "restore failed",
                    clock(),
                ),
            )
            throw error
        }
    }

    /** Verify stored bytes of a snapshot without touching any live save. */
    fun verify(snapshotId: String): Boolean {
        val snapshot = index.snapshot(snapshotId) ?: return false
        val dir = File(snapshotRoot, snapshotId)
        return snapshot.files.all { record ->
            val file = File(dir, record.relativePath)
            file.isFile && file.length() == record.sizeBytes && sha256(file) == record.sha256
        }
    }

    private fun copyTreeVerified(source: File, destination: File) {
        source.walkTopDown().forEach { entry ->
            val relative = entry.relativeTo(source).path
            if (relative.isBlank()) return@forEach
            val copy = File(destination, relative)
            if (entry.isDirectory) {
                check(copy.isDirectory || copy.mkdirs()) { "Unable to back up savedata" }
            } else {
                check(copy.parentFile?.isDirectory == true || copy.parentFile?.mkdirs() == true) {
                    "Unable to back up savedata"
                }
                entry.copyTo(copy, overwrite = false)
                check(sha256(copy) == sha256(entry)) { "Destination backup verification failed" }
            }
        }
    }

    private fun requireFreeSpace(root: File, neededBytes: Long) {
        val probe = generateSequence(root) { it.parentFile }.firstOrNull { it.exists() } ?: return
        check(probe.usableSpace > neededBytes * 2) { "Insufficient storage for SafeSave operation" }
    }

    companion object {
        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }
    }
}
