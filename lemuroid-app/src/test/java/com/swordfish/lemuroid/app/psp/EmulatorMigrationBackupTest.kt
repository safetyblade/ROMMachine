package com.swordfish.lemuroid.app.psp

import com.swordfish.lemuroid.lib.saves.EmulatorMigrationBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EmulatorMigrationBackupTest {
    @Test
    fun snapshotRetainsOriginalAndChecksum() {
        val root = Files.createTempDirectory("emulator-snapshot").toFile()
        try {
            val existingSaves = File(root, "active/saves").apply { mkdirs() }
            File(existingSaves, "playstation.srm").writeText("previous working progress")
            val backup = File(root, "snapshots/psx")
            EmulatorMigrationBackup.preserve(
                backup,
                listOf(EmulatorMigrationBackup.Source(existingSaves, "saves")),
            )
            assertEquals("previous working progress", File(backup, "saves/playstation.srm").readText())
            assertTrue(File(backup, ".complete").readText().contains("saves/playstation.srm"))
            assertEquals("previous working progress", File(existingSaves, "playstation.srm").readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun backupIsImmutableWhenActiveSaveChanges() {
        val root = Files.createTempDirectory("emulator-immutable").toFile()
        try {
            val saves = File(root, "active").apply { mkdirs() }
            val source = File(saves, "game.srm").apply { writeText("old progress") }
            val backup = File(root, "backups/psx")
            val sources = listOf(EmulatorMigrationBackup.Source(saves, "saves"))
            EmulatorMigrationBackup.preserve(backup, sources)
            source.writeText("new game progress")
            EmulatorMigrationBackup.preserve(backup, sources)
            assertEquals("old progress", File(backup, "saves/game.srm").readText())
            assertEquals("new game progress", source.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun incompleteSnapshotMustNotBeReplaced() {
        val root = Files.createTempDirectory("emulator-incomplete").toFile()
        try {
            val backup = File(root, "backups/psp").apply { mkdirs() }
            val unfinished = File(backup, "incomplete.txt").apply { writeText("do not delete") }
            val outcome = runCatching { EmulatorMigrationBackup.preserve(backup, emptyList()) }
            assertTrue(outcome.isFailure)
            assertTrue(unfinished.exists())
            assertFalse(File(backup, ".complete").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun emptyFirstTimeLibraryCanBeSafelySnapshotted() {
        val root = Files.createTempDirectory("emulator-fresh").toFile()
        try {
            val backup = File(root, "backups/psp")
            EmulatorMigrationBackup.preserve(
                backup,
                listOf(EmulatorMigrationBackup.Source(File(root, "nonexistent"), "saves")),
            )
            assertTrue(File(backup, ".complete").isFile)
        } finally {
            root.deleteRecursively()
        }
    }
}
