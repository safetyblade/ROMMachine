package com.swordfish.lemuroid.lib.saves.safesave

import java.util.UUID

/**
 * SAVE-A01 first activation — local SafeSave metadata model.
 *
 * Identity rule (from JR01 architecture and issue #290): a save group is bound
 * only to an immutable cartridge UUID supplied explicitly by the caller.
 * Ownership is never inferred from a bare filename, a game title, or a
 * savedata directory name.
 */
enum class SafeSaveVerificationState {
    STAGED,
    VERIFIED,
    FAILED,
}

data class SafeSaveFileRecord(
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
)

data class SafeSaveSnapshotRecord(
    val snapshotId: String,
    val cartridgeUuid: String,
    val platform: String,
    val engineId: String,
    val engineVersion: String,
    val saveType: String,
    val groupName: String,
    val saveTimestamp: Long,
    val captureTime: Long,
    val verificationState: SafeSaveVerificationState,
    val totalBytes: Long,
    val fileCount: Int,
    val files: List<SafeSaveFileRecord>,
)

data class SafeSaveJournalRecord(
    val opId: String,
    val kind: String,
    val snapshotId: String?,
    val stage: String,
    val result: String,
    val timestamp: Long,
)

/** Metadata index. Save bytes never live in index rows; they live on disk. */
interface SafeSaveIndex {
    fun registerCapture(
        snapshot: SafeSaveSnapshotRecord,
        completion: SafeSaveJournalRecord,
    )

    fun snapshot(snapshotId: String): SafeSaveSnapshotRecord?

    fun snapshotsForCartridge(cartridgeUuid: String): List<SafeSaveSnapshotRecord>

    fun snapshotsForGroup(groupName: String): List<SafeSaveSnapshotRecord>

    fun appendJournal(record: SafeSaveJournalRecord)

    fun journal(): List<SafeSaveJournalRecord>
}

fun requireCartridgeUuid(value: String): String {
    val parsed =
        try {
            UUID.fromString(value)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Cartridge identity must be an immutable UUID", error)
        }
    require(parsed.toString() == value.lowercase()) { "Cartridge UUID must be canonical" }
    return value.lowercase()
}
