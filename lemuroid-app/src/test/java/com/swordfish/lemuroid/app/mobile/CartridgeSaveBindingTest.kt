package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.CartridgeSaveBinding
import com.swordfish.lemuroid.lib.saves.SaveState
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeSaveBindingTest {
    private fun game(
        fileName: String,
        uri: String,
    ): Game =
        Game(
            fileName = fileName,
            fileUri = uri,
            title = "Synthetic",
            systemId = "gb",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 1,
        )

    private fun uniqueName() = "save-binding-${UUID.randomUUID()}.gb"

    private val config = SystemCoreConfig(CoreID.GAMBATTE, hashMapOf())

    @Test fun distinctCartridgesCannotReadOrOverwriteSameFilenameSram() =
        runBlocking {
            val dirs = DirectoriesManager(ApplicationProvider.getApplicationContext<Application>())
            val name = uniqueName()
            val first = game(name, "content://fixture/one")
            val second = game(name, "content://fixture/two")
            val legacy = File(dirs.getSavesDirectory(), "${name.substringBeforeLast('.')}.srm")
            legacy.writeBytes(byteArrayOf(9, 8, 7))
            val binding = CartridgeSaveBinding(dirs) { listOf(first, second) }
            val saves = SavesManager(dirs, binding)
            assertNull(saves.getSaveRAM(first, config))
            assertNull(saves.getSaveRAM(second, config))
            saves.setSaveRAM(first, byteArrayOf(1, 2, 3))
            saves.setSaveRAM(second, byteArrayOf(4, 5, 6))
            assertArrayEquals(byteArrayOf(1, 2, 3), saves.getSaveRAM(first, config))
            assertArrayEquals(byteArrayOf(4, 5, 6), saves.getSaveRAM(second, config))
            assertArrayEquals(byteArrayOf(9, 8, 7), legacy.readBytes())
            assertNotEquals(first.cartridgeId, second.cartridgeId)
        }

    @Test fun existingUniqueSramAndStatesSurviveUpgradeAndRename() =
        runBlocking {
            val dirs = DirectoriesManager(ApplicationProvider.getApplicationContext<Application>())
            val name = uniqueName()
            val original = game(name, "content://fixture/installed")
            val source = File(dirs.getSavesDirectory(), "${name.substringBeforeLast('.')}.srm")
            source.writeBytes(byteArrayOf(10, 11, 12))
            val state = SaveState(byteArrayOf(31, 32), SaveState.Metadata())
            val oldStates = StatesManager(dirs)
            oldStates.setAutoSave(original, CoreID.GAMBATTE, state)
            oldStates.setSlotSave(original, state, CoreID.GAMBATTE, 0)
            val binding = CartridgeSaveBinding(dirs) { listOf(original) }
            val saves = SavesManager(dirs, binding)
            val states = StatesManager(dirs, binding)
            val renamed = original.copy(fileName = "renamed.gb")
            assertArrayEquals(source.readBytes(), saves.getSaveRAM(renamed, config))
            assertArrayEquals(state.state, states.getAutoSave(renamed, CoreID.GAMBATTE)!!.state)
            assertArrayEquals(state.state, states.getSlotSave(renamed, CoreID.GAMBATTE, 0)!!.state)
            assertArrayEquals(byteArrayOf(10, 11, 12), source.readBytes())
            saves.setSaveRAM(renamed, byteArrayOf(90))
            assertArrayEquals(byteArrayOf(90), saves.getSaveRAM(original, config))
            assertArrayEquals(byteArrayOf(10, 11, 12), source.readBytes())
        }

    @Test fun ambiguousLegacyStatesRemainUnassignedAndFutureStatesAreIsolated() =
        runBlocking {
            val dirs = DirectoriesManager(ApplicationProvider.getApplicationContext<Application>())
            val name = uniqueName()
            val first = game(name, "content://fixture/state-one")
            val second = game(name, "content://fixture/state-two")
            val old = StatesManager(dirs)
            val legacyState = SaveState(byteArrayOf(60), SaveState.Metadata())
            old.setAutoSave(first, CoreID.GAMBATTE, legacyState)
            val binding = CartridgeSaveBinding(dirs) { listOf(first, second) }
            val states = StatesManager(dirs, binding)
            assertNull(states.getAutoSave(first, CoreID.GAMBATTE))
            assertNull(states.getAutoSave(second, CoreID.GAMBATTE))
            states.setAutoSave(first, CoreID.GAMBATTE, SaveState(byteArrayOf(1), SaveState.Metadata()))
            states.setAutoSave(second, CoreID.GAMBATTE, SaveState(byteArrayOf(2), SaveState.Metadata()))
            assertArrayEquals(byteArrayOf(1), states.getAutoSave(first, CoreID.GAMBATTE)!!.state)
            assertArrayEquals(byteArrayOf(2), states.getAutoSave(second, CoreID.GAMBATTE)!!.state)
            assertArrayEquals(legacyState.state, old.getAutoSave(first, CoreID.GAMBATTE)!!.state)
        }

    @Test fun incompleteOrConflictingCopyCannotBecomeAuthoritative() =
        runBlocking {
            val dirs = DirectoriesManager(ApplicationProvider.getApplicationContext<Application>())
            val name = uniqueName()
            val game = game(name, "content://fixture/recovery")
            val legacy = File(dirs.getSavesDirectory(), "${name.substringBeforeLast('.')}.srm")
            legacy.writeBytes(byteArrayOf(77, 78))
            val scopeHash =
                MessageDigest.getInstance("SHA-256").digest("shared".toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
            val candidate = File(dirs.getSavesDirectory(), "cartridges/${game.cartridgeId}/sram/$scopeHash/${legacy.name}")
            candidate.parentFile!!.mkdirs()
            candidate.writeBytes(byteArrayOf(99))
            val binding = CartridgeSaveBinding(dirs) { listOf(game) }
            val failure = runCatching { SavesManager(dirs, binding).getSaveRAM(game, config) }.exceptionOrNull()
            assertTrue(failure is IOException)
            assertArrayEquals(byteArrayOf(77, 78), legacy.readBytes())
            assertArrayEquals(byteArrayOf(99), candidate.readBytes())
            val marker = File(dirs.getSaveBindingsDirectory(), "${game.cartridgeId}/sram/$scopeHash.active")
            assertFalse(marker.exists())
            // Recovery requires clearing or explicitly resolving only the failed candidate,
            // never altering the original. A retry must continue to fail closed.
            assertTrue(runCatching { binding.sramFile(game, legacy, "shared") }.exceptionOrNull() is IOException)
        }
}
