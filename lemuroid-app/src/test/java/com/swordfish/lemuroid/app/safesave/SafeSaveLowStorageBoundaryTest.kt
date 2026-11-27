package com.swordfish.lemuroid.app.safesave

import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveIndex
import com.swordfish.lemuroid.lib.saves.safesave.SafeSaveJournalRecord
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

private class BoundaryRecordingIndex : SafeSaveIndex {
    val snapshots = linkedMapOf<String, SafeSaveSnapshotRecord>()
    val journal = mutableListOf<SafeSaveJournalRecord>()

    override fun registerCapture(
        snapshot: SafeSaveSnapshotRecord,
        completion: SafeSaveJournalRecord,
    ) {
        snapshots[snapshot.snapshotId] = snapshot
        journal += completion
    }

    override fun snapshot(snapshotId: String) = snapshots[snapshotId]

    override fun snapshotsForCartridge(cartridgeUuid: String) =
        snapshots.values.filter { it.cartridgeUuid == cartridgeUuid }

    override fun snapshotsForGroup(groupName: String) = snapshots.values.filter { it.groupName == groupName }

    override fun appendJournal(record: SafeSaveJournalRecord) {
        journal += record
    }

    override fun journal() = journal.toList()
}

/**
 * SafeSave low-storage guard, boundary proof.
 *
 * SafeSaveMetricsAndJournalTest already proves a zero-space fixture refuses both
 * capture and restore. These tests pin the refusal to the snapshot size itself:
 * capture must refuse when free space is below the snapshot size, restore must
 * refuse when free space is below twice the snapshot size, and a refused
 * operation must leave no partial staging behind in either root.
 */
@RunWith(RobolectricTestRunner::class)
class SafeSaveLowStorageBoundaryTest {
    private val cartridgeA = "11111111-1111-1111-1111-111111111111"

    private fun fixture(): Triple<File, File, File> {
        val parent = Files.createTempDirectory("safesave-low-storage-").toFile()
        val saveRoot = File(parent, "modern").apply { mkdirs() }
        val group = File(saveRoot, "PSP/SAVEDATA/ULUS10001").apply { mkdirs() }
        File(group, "DATA.BIN").writeText("level-3-progress")
        File(group, "PARAM.SFO").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(group, "ICON0.PNG").writeBytes(byteArrayOf(9, 8, 7))
        return Triple(parent, saveRoot, File(parent, "snapshots"))
    }

    // Fixture totals: 16 + 4 + 3 bytes across 3 files.
    private val fixtureBytes = 23L

    @Test
    fun captureRefusesWhenFreeSpaceIsBelowSnapshotSize() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = BoundaryRecordingIndex()
            val store = SafeSaveStore(index, snapshotRoot, availableBytes = { fixtureBytes - 1 })
            val error =
                assertThrows(IllegalStateException::class.java) {
                    store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
                }
            assertEquals("Insufficient storage for SafeSave operation", error.message)
            assertTrue(index.snapshots.isEmpty())
            assertTrue(index.journal.isEmpty())
            // No partial staging: the snapshot root was never even created.
            assertFalse(snapshotRoot.exists())
            assertEquals(
                "level-3-progress",
                File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN").readText(),
            )
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun restoreRefusesWhenFreeSpaceIsBelowTwiceSnapshotSize() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = BoundaryRecordingIndex()
            val captured =
                SafeSaveStore(index, snapshotRoot)
                    .capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            val liveFile = File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN")
            liveFile.writeText("newer-live-progress")
            val priorJournal = index.journal.toList()
            val store =
                SafeSaveStore(index, snapshotRoot, availableBytes = { fixtureBytes * 2 - 1 })
            val error =
                assertThrows(IllegalStateException::class.java) {
                    store.restore(captured.snapshot.snapshotId, saveRoot, engineQuiesced = true)
                }
            assertEquals("Insufficient storage for SafeSave operation", error.message)
            assertEquals("newer-live-progress", liveFile.readText())
            assertTrue(store.verify(captured.snapshot.snapshotId))
            assertEquals(priorJournal, index.journal)
            // No partial staging: no restore staging or displaced dirs beside
            // the live group, and no backup dir beside the stored snapshot.
            assertEquals(
                listOf("ULUS10001"),
                File(saveRoot, "PSP/SAVEDATA").list()?.toList(),
            )
            assertEquals(
                listOf(captured.snapshot.snapshotId),
                snapshotRoot.list()?.toList(),
            )
        } finally {
            parent.deleteRecursively()
        }
    }
}
