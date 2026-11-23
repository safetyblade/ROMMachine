package com.swordfish.lemuroid.lib.saves.safesave

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction

/**
 * SAVE-A01: separately versioned SafeSave metadata database.
 *
 * This is intentionally NOT a migration of [com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase]:
 * the main database stays at Room v13 untouched, so rollback of this feature
 * never requires a main-database downgrade.
 *
 * PENDING ARCHITECTURE APPROVAL: Daniel's approval of the separate-database
 * decision is still outstanding (see SAVE-A01 PR). This class does not claim it.
 */
@Entity(tableName = "safesave_snapshots", indices = [Index("cartridgeUuid"), Index("groupName")])
data class SafeSaveSnapshotEntity(
    @androidx.room.PrimaryKey val snapshotId: String,
    val cartridgeUuid: String,
    val platform: String,
    val engineId: String,
    val engineVersion: String,
    val saveType: String,
    val groupName: String,
    val saveTimestamp: Long,
    val captureTime: Long,
    val verificationState: String,
    val totalBytes: Long,
    val fileCount: Int,
)

@Entity(
    tableName = "safesave_files",
    primaryKeys = ["snapshotId", "relativePath"],
    indices = [Index("snapshotId")],
)
data class SafeSaveFileEntity(
    val snapshotId: String,
    val relativePath: String,
    val sizeBytes: Long,
    val sha256: String,
)

@Entity(tableName = "safesave_journal")
data class SafeSaveJournalEntity(
    @androidx.room.PrimaryKey val opId: String,
    val kind: String,
    val snapshotId: String?,
    val stage: String,
    val result: String,
    val timestamp: Long,
)

@Dao
interface SafeSaveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSnapshot(entity: SafeSaveSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFiles(files: List<SafeSaveFileEntity>)

    @Transaction
    fun registerSnapshot(
        snapshot: SafeSaveSnapshotEntity,
        files: List<SafeSaveFileEntity>,
        completion: SafeSaveJournalEntity,
    ) {
        check(!conflictingOwner(snapshot.groupName, snapshot.cartridgeUuid)) {
            "Savedata group is already bound to a different cartridge"
        }
        insertSnapshot(snapshot)
        insertFiles(files)
        insertJournal(completion)
    }

    @Query("SELECT EXISTS(SELECT 1 FROM safesave_snapshots WHERE groupName = :group AND cartridgeUuid != :owner)")
    fun conflictingOwner(
        group: String,
        owner: String,
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertJournal(entity: SafeSaveJournalEntity)

    @Query("SELECT * FROM safesave_snapshots WHERE snapshotId = :snapshotId")
    fun snapshot(snapshotId: String): SafeSaveSnapshotEntity?

    @Query("SELECT * FROM safesave_files WHERE snapshotId = :snapshotId ORDER BY relativePath")
    fun files(snapshotId: String): List<SafeSaveFileEntity>

    @Query("SELECT * FROM safesave_snapshots WHERE cartridgeUuid = :cartridgeUuid ORDER BY captureTime")
    fun snapshotsForCartridge(cartridgeUuid: String): List<SafeSaveSnapshotEntity>

    @Query("SELECT * FROM safesave_snapshots WHERE groupName = :groupName ORDER BY captureTime")
    fun snapshotsForGroup(groupName: String): List<SafeSaveSnapshotEntity>

    @Query("SELECT * FROM safesave_journal ORDER BY timestamp")
    fun journal(): List<SafeSaveJournalEntity>
}

@Database(
    entities = [SafeSaveSnapshotEntity::class, SafeSaveFileEntity::class, SafeSaveJournalEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SafeSaveDatabase : RoomDatabase() {
    abstract fun safeSaveDao(): SafeSaveDao

    companion object {
        const val DB_NAME = "safesave"

        fun create(context: Context): SafeSaveDatabase =
            Room.databaseBuilder(context, SafeSaveDatabase::class.java, DB_NAME).build()
    }
}

/** Room-backed [SafeSaveIndex]. Stores metadata only; save bytes stay on disk. */
class RoomSafeSaveIndex(private val dao: SafeSaveDao) : SafeSaveIndex {
    override fun registerCapture(
        snapshot: SafeSaveSnapshotRecord,
        completion: SafeSaveJournalRecord,
    ) {
        dao.registerSnapshot(
            SafeSaveSnapshotEntity(
                snapshotId = snapshot.snapshotId,
                cartridgeUuid = snapshot.cartridgeUuid,
                platform = snapshot.platform,
                engineId = snapshot.engineId,
                engineVersion = snapshot.engineVersion,
                saveType = snapshot.saveType,
                groupName = snapshot.groupName,
                saveTimestamp = snapshot.saveTimestamp,
                captureTime = snapshot.captureTime,
                verificationState = snapshot.verificationState.name,
                totalBytes = snapshot.totalBytes,
                fileCount = snapshot.fileCount,
            ),
            snapshot.files.map {
                SafeSaveFileEntity(snapshot.snapshotId, it.relativePath, it.sizeBytes, it.sha256)
            },
            completion.toEntity(),
        )
    }

    override fun snapshot(snapshotId: String): SafeSaveSnapshotRecord? {
        val entity = dao.snapshot(snapshotId) ?: return null
        return entity.toRecord(dao.files(snapshotId))
    }

    override fun snapshotsForCartridge(cartridgeUuid: String): List<SafeSaveSnapshotRecord> =
        dao.snapshotsForCartridge(cartridgeUuid).map { it.toRecord(dao.files(it.snapshotId)) }

    override fun snapshotsForGroup(groupName: String): List<SafeSaveSnapshotRecord> =
        dao.snapshotsForGroup(groupName).map { it.toRecord(dao.files(it.snapshotId)) }

    override fun appendJournal(record: SafeSaveJournalRecord) {
        dao.insertJournal(record.toEntity())
    }

    private fun SafeSaveJournalRecord.toEntity() =
        SafeSaveJournalEntity(opId, kind, snapshotId, stage, result, timestamp)

    override fun journal(): List<SafeSaveJournalRecord> =
        dao.journal().map { SafeSaveJournalRecord(it.opId, it.kind, it.snapshotId, it.stage, it.result, it.timestamp) }

    private fun SafeSaveSnapshotEntity.toRecord(files: List<SafeSaveFileEntity>) =
        SafeSaveSnapshotRecord(
            snapshotId = snapshotId,
            cartridgeUuid = cartridgeUuid,
            platform = platform,
            engineId = engineId,
            engineVersion = engineVersion,
            saveType = saveType,
            groupName = groupName,
            saveTimestamp = saveTimestamp,
            captureTime = captureTime,
            verificationState = SafeSaveVerificationState.valueOf(verificationState),
            totalBytes = totalBytes,
            fileCount = fileCount,
            files = files.map { SafeSaveFileRecord(it.relativePath, it.sizeBytes, it.sha256) },
        )
}
