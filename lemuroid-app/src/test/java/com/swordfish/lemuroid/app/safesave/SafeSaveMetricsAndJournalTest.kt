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

private class RecordingIndex : SafeSaveIndex {
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
 * SAVE-A01 follow-up: the capture/restore metrics and journal contract.
 *
 * Original SAVE-A02 #369 reported capture/restore duration, file count and bytes as
 * unmeasured in-app evidence, and no test asserted the journal sequence.
 * SafeSaveStore takes an injected clock, so both are testable exactly:
 * a stepped clock must produce an exact duration, and every outcome —
 * success or failure — must leave the journal the state machine promises.
 */
@RunWith(RobolectricTestRunner::class)
class SafeSaveMetricsAndJournalTest {
    private val cartridgeA = "11111111-1111-1111-1111-111111111111"

    private fun fixture(): Triple<File, File, File> {
        val parent = Files.createTempDirectory("safesave-metrics-").toFile()
        val saveRoot = File(parent, "modern").apply { mkdirs() }
        val group = File(saveRoot, "PSP/SAVEDATA/ULUS10001").apply { mkdirs() }
        File(group, "DATA.BIN").writeText("level-3-progress")
        File(group, "PARAM.SFO").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(group, "ICON0.PNG").writeBytes(byteArrayOf(9, 8, 7))
        return Triple(parent, saveRoot, File(parent, "snapshots"))
    }

    // Fixture totals: 16 + 4 + 3 bytes across 3 files.
    private val fixtureBytes = 23L
    private val fixtureFiles = 3

    @Test
    fun captureMetricsUseInjectedClockAndReportTotals() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            // Capture reads the clock for: start, snapshot captureTime,
            // COMPLETE journal timestamp, then the metrics duration.
            val times = listOf(1_000L, 1_100L, 1_200L, 1_750L).iterator()
            val store = SafeSaveStore(RecordingIndex(), snapshotRoot) { times.next() }
            val result = store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            assertEquals(750L, result.metrics.durationMs)
            assertEquals(fixtureFiles, result.metrics.fileCount)
            assertEquals(fixtureBytes, result.metrics.totalBytes)
            assertEquals(1_100L, result.snapshot.captureTime)
            assertFalse(times.hasNext())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun restoreMetricsUseInjectedClockAndJournalStagesInOrder() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = RecordingIndex()
            val captured =
                SafeSaveStore(index, snapshotRoot)
                    .capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            // Restore reads the clock for: start, DEST_BACKUP journal,
            // ACTIVATING journal, COMPLETE journal, then metrics duration.
            val times = listOf(5_000L, 5_100L, 5_200L, 5_300L, 5_900L).iterator()
            val store = SafeSaveStore(index, snapshotRoot) { times.next() }
            val restored = store.restore(captured.snapshot.snapshotId, saveRoot, engineQuiesced = true)
            assertEquals(900L, restored.metrics.durationMs)
            assertEquals(fixtureFiles, restored.metrics.fileCount)
            assertEquals(fixtureBytes, restored.metrics.totalBytes)
            assertFalse(times.hasNext())
            val restoreStages = index.journal.filter { it.kind == "RESTORE" }.map { it.stage }
            assertEquals(listOf("DEST_BACKUP", "ACTIVATING", "COMPLETE"), restoreStages)
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun successfulCaptureJournalsExactlyOneCompleteEntry() {
        val (parent, saveRoot, snapshotRoot) = fixture()
        try {
            val index = RecordingIndex()
            val captured =
                SafeSaveStore(index, snapshotRoot)
                    .capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            assertEquals(1, index.journal.size)
            val entry = index.journal.single()
            assertEquals("CAPTURE", entry.kind)
            assertEquals("COMPLETE", entry.stage)
            assertEquals("VERIFIED", entry.result)
            assertEquals(captured.snapshot.snapshotId, entry.snapshotId)
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun failedCaptureJournalsFailedAndRegistersNoSnapshot() {
        val (parent, saveRoot, _) = fixture()
        try {
            // A snapshot root that is a regular file makes staging fail
            // inside the capture try block, exercising the failure path.
            val blockedRoot = File(parent, "snapshots-blocked").apply { writeText("not a directory") }
            val index = RecordingIndex()
            val store = SafeSaveStore(index, blockedRoot)
            assertThrows(IllegalStateException::class.java) {
                store.capture(saveRoot, "ULUS10001", cartridgeA, "v", engineQuiesced = true)
            }
            assertTrue(index.snapshots.isEmpty())
            assertEquals(1, index.journal.size)
            val entry = index.journal.single()
            assertEquals("CAPTURE", entry.kind)
            assertEquals("FAILED", entry.stage)
            assertEquals(null, entry.snapshotId)
            assertEquals("not a directory", blockedRoot.readText())
            // The live save was never touched.
            assertEquals("level-3-progress", File(saveRoot, "PSP/SAVEDATA/ULUS10001/DATA.BIN").readText())
        } finally {
            parent.deleteRecursively()
        }
    }

    @Test
    fun verifyReturnsFalseForUnknownOrMalformedSnapshotIds() {
        val (parent, _, snapshotRoot) = fixture()
        try {
            val store = SafeSaveStore(RecordingIndex(), snapshotRoot)
            assertFalse(store.verify("33333333-3333-3333-3333-333333333333"))
            assertFalse(store.verify("not-a-uuid"))
        } finally {
            parent.deleteRecursively()
        }
    }
}
