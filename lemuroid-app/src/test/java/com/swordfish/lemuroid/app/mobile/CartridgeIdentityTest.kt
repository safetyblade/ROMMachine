package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveState
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeIdentityTest {
    private fun game(uri: String) = Game(
        fileName = "synthetic.gb", fileUri = uri, title = "Synthetic", systemId = "gb",
        developer = null, coverFrontUrl = null, lastIndexedAt = 1,
    )

    @Test fun sameNamesHaveDifferentIdsAndCopyPreservesIdentityAndAliases() {
        val first = game("content://fixture/a")
        val second = game("content://fixture/b")
        assertNotEquals(first.cartridgeId, second.cartridgeId)
        UUID.fromString(first.cartridgeId)
        val renamed = first.copy(fileName = "renamed.gb", fileUri = "content://fixture/moved")
        assertEquals(first.cartridgeId, renamed.cartridgeId)
        assertEquals(first.fileName, renamed.saveFileName())
        assertEquals(first.fileUri, renamed.legacySourceUri)
    }

    @Test fun installedSramAndStatesStillReadAfterSameDocumentRename() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val directories = DirectoriesManager(context)
        val game = game("content://fixture/rename").copy(legacyFileName = "installed-fixture.gb")
        val sram = byteArrayOf(5, 7, 11)
        val original = File(directories.getSavesDirectory(), "installed-fixture.srm")
        original.writeBytes(sram)
        val saves = SavesManager(directories)
        val states = StatesManager(directories)
        val core = CoreID.GAMBATTE
        val state = SaveState(byteArrayOf(13, 17), SaveState.Metadata())
        states.setAutoSave(game, core, state)
        states.setSlotSave(game, state, core, 0)
        val renamed = game.copy(fileName = "renamed.gb")
        assertArrayEquals(sram, saves.getSaveRAM(renamed, SystemCoreConfig(core, hashMapOf())))
        assertArrayEquals(state.state, states.getAutoSave(renamed, core)!!.state)
        assertArrayEquals(state.state, states.getSlotSave(renamed, core, 0)!!.state)
        assertArrayEquals(sram, original.readBytes())
    }

    @Test fun failedSramAndStateWritesReachTheCaller() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val blocked = File(context.cacheDir, "blocked-external-root").apply { writeText("sentinel") }
        val storageContext = object : android.content.ContextWrapper(context) {
            override fun getExternalFilesDir(type: String?): File = blocked
        }
        val directories = DirectoriesManager(storageContext)
        val game = game("content://fixture/failure").copy(legacyFileName = "blocked-fixture.gb")
        try {
            val ramFailure = runCatching { SavesManager(directories).setSaveRAM(game, byteArrayOf(1)) }.exceptionOrNull()
            val stateFailure = runCatching {
                StatesManager(directories).setAutoSave(game, CoreID.GAMBATTE, SaveState(byteArrayOf(1), SaveState.Metadata()))
            }.exceptionOrNull()
            assertTrue(ramFailure is IOException)
            assertTrue(stateFailure is IOException)
            assertEquals("sentinel", blocked.readText())
        } finally {
            blocked.delete()
        }
    }
}
