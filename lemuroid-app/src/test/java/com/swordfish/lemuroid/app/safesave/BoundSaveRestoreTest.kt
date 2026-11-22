package com.swordfish.lemuroid.app.safesave

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.saves.safesave.BoundSaveRestore
import com.swordfish.lemuroid.lib.saves.safesave.CartSaveArchive
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class BoundSaveRestoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun replacesManagedFilesAndRetainsAnOriginalCopy() {
        val destination = destination().apply { writeText("old state") }
        val source = source()
        val restore = BoundSaveRestore(context)
        restore.restore(listOf(replacement(destination, source)))
        assertEquals("restored state", destination.readText())
        assertFalse(BoundSaveRestore.recoveryPending(context))
        val history = File(context.noBackupFilesDir, "safesave/restore").listFiles().orEmpty()
        assertEquals("old state", File(history.single { it.name.startsWith("previous-") }, "original-0").readText())
    }

    @Test
    fun refusesChangedPayloadBeforeWritingAnyDestination() {
        val first = destination().apply { writeText("original one") }
        val second = destination().apply { writeText("original two") }
        val source = source()
        val invalid = replacement(second, source)
        source.appendText("changed")
        val valid = replacement(first, source)
        assertThrows(IllegalArgumentException::class.java) {
            BoundSaveRestore(context).restore(listOf(valid, invalid))
        }
        assertEquals("original one", first.readText())
        assertEquals("original two", second.readText())
        assertFalse(BoundSaveRestore.recoveryPending(context))
    }

    @Test
    fun interruptedActivationRollsBackReplacedAndNewSlots() {
        val replaced = destination().apply { writeText("restored state") }
        val newSlot = destination().apply { writeText("new state") }
        val pending = File(context.noBackupFilesDir, "safesave/restore/pending").apply { mkdirs() }
        val original = File(pending, "original-0").apply { writeText("old state") }
        val originalArtifact = CartSaveArchive.artifact(CartSaveArchive.Kind.MANUAL_STATE, original)
        val replacedHash = CartSaveArchive.artifact(CartSaveArchive.Kind.MANUAL_STATE, replaced).sha256
        val newSlotHash = CartSaveArchive.artifact(CartSaveArchive.Kind.MANUAL_STATE, newSlot).sha256
        val journal =
            JSONArray()
                .put(
                    JSONObject()
                        .put("destination", replaced.path)
                        .put("priorHash", originalArtifact.sha256)
                        .put("priorSize", originalArtifact.byteLength)
                        .put("replacementHash", replacedHash),
                ).put(
                    JSONObject()
                        .put("destination", newSlot.path)
                        .put("priorHash", JSONObject.NULL)
                        .put("priorSize", 0L)
                        .put("replacementHash", newSlotHash),
                )
        File(pending, "journal.json").writeText(journal.toString())

        BoundSaveRestore(context).recover()

        assertEquals("old state", replaced.readText())
        assertFalse(newSlot.exists())
        assertFalse(BoundSaveRestore.recoveryPending(context))
    }

    private fun destination(): File =
        File(DirectoriesManager(context).getStatesDirectory().canonicalFile, "cartridges/${UUID.randomUUID()}/slot1")
            .apply { parentFile?.mkdirs() }

    private fun source(): File =
        File.createTempFile("state-", ".bin", context.cacheDir.canonicalFile).apply { writeText("restored state") }

    private fun replacement(
        destination: File,
        source: File,
    ): BoundSaveRestore.Replacement =
        BoundSaveRestore.Replacement(
            destination,
            source,
            CartSaveArchive.artifact(CartSaveArchive.Kind.MANUAL_STATE, source),
        )
}
