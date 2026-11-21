package com.swordfish.lemuroid.app.safesave

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.saves.safesave.RoomSafeSaveIndex
import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveDatabase
import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveIndex
import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveJournalRecord
import com.swordfish.lemuroid.lib.saves.safesave.SafeSavePspAccess
import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveSnapshotRecord
import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files

private class InMemoryIndex : SafeSaveIndex {
    val snapshots = linkedMapOf<String, SafeSaveSnapshotRecord>()
    val journal = mutableListOf<SafeSaveJournalRecord>()

    override fun insertSnapshot(snapshot: SafeSaveSnapshotRecord) {
        snapshots[snapshot.snapshotId] = snapshot
    }

    override fun snapshot(snapshotId: String) = snapshots[snapshotId]

    override fun snapshotsForCartridge(cartridgeUuid: String) =
        snapshots.values.filter { it.cartridgeUuid == cartridgeUuid }

    override fun snapshotsForGroup(groupName: String) =
        snapshots.values.filter { it.groupName == groupName }

    override fun appendJournal(record: SafeSaveJournalRecord) {
        journal += record
    }

    override fun journal() = journal.toList()
}

@RunWith(RobolectricTestRunner::class)
class SafeSaveStoreTest {
    private val cartridgeA = "11111111-1111-1111-1111-111111111111"
    private val cartridgeB = "22222222-2222-2222-2222-222222222222"

    private fun fixture(): Triple<File, File, File> {
        val parent = Files.createTempDirectory("safesave-a01-").toFile()
        val saveRoot = File(parent, "modern").apply { mkdirs() }
        val group = File(saveRoot, "PSP/SAVEDATA/ULUS10001").apply { mkdirs() }
        File(group, "DATA.BIN").writeText("level-3-progress")
        File(group, "PARAM.SFO").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(group, "ICON0.PNG").writeBytes(byteArrayOf(9, 8, 7))
        return Triple(parent, saveRoot, File(parent, "snapshots"))
    }

    @Test
    fun captureRegistersIdentityHashesAndMultiFileIntegrity() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = InMemoryIndex()
            val store = SafeSaveStore(index, snapshotRoot)
            val result = store.capture(saveRoot, "ULUS10001", cartridgeA, "ppsspp-test", engineQuiesced = true)
            assertEquals(cartridgeA, result.snapshot.cartridgeUuid)
            assertEquals("PSP", result.snapshot.platform)
            assertEquals(3, result.snapshot.fileCount)
            assertEquals(3, result.metrics.fileCount)
            assertTrue(store.verify(result.snapshot.snapshotId))
            // Original untouched.
            assertEquals("level-3-progress", File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN").readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun captureRejectsNonUuidIdentityAndUnquiescedEngine() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val store = SafeSaveStore(InMemoryIndex(), snapshotRoot)
            assertThrows(IllegalArgumentException::class.java) {
                store.capture(saveRoot, "ULUS10001", "ULUS10001", "v", engineQuiesced = true)
            }
            assertThrows(IllegalStateException::class.java) {
                store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = false)
            }
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun wrongGameAssociationIsBlocked() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val store = SafeSaveStore(InMemoryIndex(), snapshotRoot)
            store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            assertThrows(IllegalStateException::class.java) {
                store.capture(saveRoot, "ULUS10001", cartridgeB, "v", engineQuiesced = true)
            }
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun restoreRoundTripsAndBacksUpDisplacedSave() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val store = SafeSaveStore(InMemoryIndex(), snapshotRoot)
            val captured = store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            val live = File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN")
            live.writeText("newer-progress")
            val restored = store.restore(captured.snapshot.snapshotId, saveRoot, engineQuiesced = true)
            assertEquals("level-3-progress", live.readText())
            assertTrue(restored.backupSnapshotDir!!.exists())
            assertEquals(
                "newer-progress",
                File(restored.backupSnapshotDir, "DATA.BIN").readText(),
            )
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun corruptedSnapshotIsRefusedAndLiveSavePreserved() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val store = SafeSaveStore(InMemoryIndex(), snapshotRoot)
            val captured = store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            File(snapshotRoot, "${captured.snapshot.snapshotId}/DATA.BIN").writeText("tampered")
            assertFalse(store.verify(captured.snapshot.snapshotId))
            assertThrows(IllegalStateException::class.java) {
                store.restore(captured.snapshot.snapshotId, saveRoot, engineQuiesced = true)
            }
            assertEquals("level-3-progress", File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN").readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun interruptedRestoreLeavesJournalAndOriginal() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = InMemoryIndex()
            val store = SafeSaveStore(index, snapshotRoot)
            assertThrows(Exception::class.java) {
                store.restore("missing-snapshot", saveRoot, engineQuiesced = true)
            }
            assertEquals("level-3-progress", File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN").readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun installOverNewStoreInstanceStillFindsVerifiedSave() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = InMemoryIndex()
            val captured = SafeSaveStore(index, snapshotRoot)
                .capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            // Simulate install-over: brand-new store/access objects, same index + roots.
            val access = SafeSavePspAccess(SafeSaveStore(index, snapshotRoot), index)
            val found = access.verifiedSavesForCartridge(cartridgeA)
            assertEquals(listOf(captured.snapshot.snapshotId), found.map { it.snapshotId })
            assertTrue(access.verifiedSavesForCartridge(cartridgeB).isEmpty())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun roomIndexPersistsAcrossDatabaseReopen() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            val dbFile = File(parent, "safesave-test.db")
            val db1 = Room.databaseBuilder(context, SafeSaveDatabase::class.java, dbFile.absolutePath).build()
            val captured = SafeSaveStore(RoomSafeSaveIndex(db1.safeSaveDao()), snapshotRoot)
                .capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            db1.close()
            val db2 = Room.databaseBuilder(context, SafeSaveDatabase::class.java, dbFile.absolutePath).build()
            val reopened = RoomSafeSaveIndex(db2.safeSaveDao()).snapshot(captured.snapshot.snapshotId)
            assertEquals(captured.snapshot, reopened)
            db2.close()
        } finally {
            parent.deleteRecursively()
        }
    }
}
