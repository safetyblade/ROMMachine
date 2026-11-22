package com.swordfish.lemuroid.app.safesave

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.saves.safesave.BoundSaveRestore
import com.swordfish.lemuroid.lib.saves.safesave.CartSaveArchive
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
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
