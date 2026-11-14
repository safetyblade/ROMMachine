package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import com.swordfish.lemuroid.lib.library.covers.CustomCoverManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider
import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.lemuroid.lib.storage.RomFiles
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.lib.storage.StorageProvider
import com.swordfish.lemuroid.lib.storage.StorageProviderRegistry
import dagger.Lazy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LibraryRefreshRoundTripTest {
    @Test fun repeatedRefreshAddsAndRemovesGamesButPreservesPlayerMetadataOnStableDocumentRename() =
        runBlocking {
            withLibrary { library, db, provider ->
                val first = file("original.gba", "first")
                provider.files = listOf(first)
                assertEquals(1, library.indexLibrary().added)
                val original = db.gameDao().selectShelfGames().first().single()
                db.gameDao().update(
                    original.copy(
                        customName = "My game",
                        customCoverUri = "file:///my-cover.png",
                        isFavorite = true,
                        lastPlayedAt = 123,
                        // Simulate a wall-clock correction after the prior scan.
                        lastIndexedAt = System.currentTimeMillis() + 60_000,
                    ),
                )
                provider.files = listOf(first.copy(name = "renamed.GBA"), file("second.gba", "second"))
                val added = library.indexLibrary()
                assertEquals(1, added.added)
                assertEquals(0, added.removed)
                val renamed = db.gameDao().selectByFileUri(first.uri.toString())!!
                assertEquals(original.id, renamed.id)
                assertEquals("renamed.GBA", renamed.fileName)
                assertEquals("My game", renamed.customName)
                assertEquals("file:///my-cover.png", renamed.customCoverUri)
                assertEquals(true, renamed.isFavorite)
                assertEquals(123L, renamed.lastPlayedAt)
                assertEquals(0, library.indexLibrary().added)
                provider.files = listOf(first.copy(name = "renamed.GBA"))
                assertEquals(1, library.indexLibrary().removed)
                assertEquals(original.id, db.gameDao().selectShelfGames().first().single().id)
            }
        }

    @Test fun incompleteDirectoryEnumerationNeverPrunesPreviouslyIndexedGames() =
        runBlocking {
            withLibrary { library, db, provider ->
                provider.files = listOf(file("saved.gba", "saved"))
                library.indexLibrary()
                val original = db.gameDao().selectShelfGames().first().single()
                provider.files = emptyList()
                provider.failAfterEnumeration = true
                assertThrows(IOException::class.java) { runBlocking { library.indexLibrary() } }
                assertEquals(original, db.gameDao().selectShelfGames().first().single())
            }
        }

    @Test fun missingCueTrackPreservesOldCartridgeAndRestoredFamilyDoesNotDuplicateIt() =
        runBlocking {
            withLibrary { library, db, provider ->
                val cue = file("Game.CUE", "cue")
                val track1 = file("track1.bin", "track1")
                val track2 = file("track2.bin", "track2")
                provider.contents[cue.uri.toString()] = "FILE \"track1.bin\" BINARY\nFILE \"track2.bin\" BINARY"
                provider.files = listOf(cue, track1, track2)
                library.indexLibrary()
                val original = db.gameDao().selectShelfGames().first().single()
                assertEquals(cue.uri.toString(), original.fileUri)
                provider.files = listOf(cue, track1)
                library.indexLibrary()
                assertEquals(original.id, db.gameDao().selectByFileUri(cue.uri.toString())!!.id)
                provider.files = listOf(cue, track1, track2)
                library.indexLibrary()
                assertEquals(original.id, db.gameDao().selectShelfGames().first().single().id)
            }
        }

    @Test fun failedEnumerationDoesNotPublishPreparedNewGamesOrRenameExistingOnes() =
        runBlocking {
            withLibrary { library, db, provider ->
                val source = file("original.gba", "original")
                provider.files = listOf(source)
                library.indexLibrary()
                val original = db.gameDao().selectShelfGames().first().single()
                provider.files = listOf(source.copy(name = "renamed.gba")) +
                    (1..250).map { file("new$it.gba", "new$it") }
                provider.failAfterEnumeration = true
                assertThrows(IOException::class.java) { runBlocking { library.indexLibrary() } }
                assertEquals(listOf(original), db.gameDao().selectShelfGames().first())
            }
        }

    @Test fun failureInsideCommitRollsBackInsertsUpdatesAndCleanupTogether() =
        runBlocking {
            withLibrary { library, db, provider ->
                provider.files = listOf(file("original.gba", "original"))
                library.indexLibrary()
                val original = db.gameDao().selectShelfGames().first().single()
                provider.files = listOf(file("replacement.gba", "replacement"))
                assertThrows(IOException::class.java) {
                    runBlocking {
                        library.indexLibrary { phase ->
                            if (phase == "database_cleanup") throw IOException("Injected commit failure")
                        }
                    }
                }
                assertEquals(listOf(original), db.gameDao().selectShelfGames().first())
                assertEquals(1, library.indexLibrary().added)
            }
        }

    @Test fun fullRemovalRetiresIdentityAndVerifiedSafReimportRestoresPlayerData() =
        runBlocking {
            withLibrary { library, db, provider ->
                val originalSource =
                    BaseStorageFile(
                        "Original.gba",
                        100,
                        Uri.parse("content://com.android.externalstorage.documents/tree/old/document/primary%3AC2A"),
                    )
                provider.files = listOf(originalSource)
                library.indexLibrary()
                val original = db.gameDao().selectShelfGames().first().single()
                val edited =
                    original.copy(
                        customName = "My original cart",
                        customCoverUri = "file:///private/cover.png",
                        isFavorite = true,
                        lastPlayedAt = 123L,
                    )
                db.gameDao().update(edited)
                provider.files = emptyList()
                assertEquals(1, library.indexLibrary().removed)
                assertEquals(0, db.gameDao().selectShelfGames().first().size)
                val retired = db.retiredCartridgeDao().selectAll().single()
                assertEquals(edited.cartridgeId, retired.cartridgeId)
                assertEquals(edited.fileUri, retired.fileUri)
                assertEquals("My original cart", retired.displayName())
                val returned =
                    originalSource.copy(
                        name = "Moved.gba",
                        uri = Uri.parse("content://com.android.externalstorage.documents/tree/new/document/primary%3AC2A"),
                    )
                provider.files = listOf(returned)
                library.indexLibrary()
                val restored = db.gameDao().selectShelfGames().first().single()
                assertEquals(original.cartridgeId, restored.cartridgeId)
                assertEquals(returned.uri.toString(), restored.fileUri)
                assertEquals("My original cart", restored.customName)
                assertEquals("file:///private/cover.png", restored.customCoverUri)
                assertEquals(true, restored.isFavorite)
                assertEquals(123L, restored.lastPlayedAt)
                assertEquals(original.saveFileName(), restored.saveFileName())
                assertEquals(0, db.retiredCartridgeDao().selectAll().size)
                library.indexLibrary()
                assertEquals(1, db.gameDao().selectShelfGames().first().size)
            }
        }

    @Test fun sameNameDifferentDocumentCannotStealRetiredIdentity() =
        runBlocking {
            withLibrary { library, db, provider ->
                val original =
                    BaseStorageFile(
                        "Same.gba",
                        100,
                        Uri.parse("content://com.android.externalstorage.documents/document/primary%3Aoriginal"),
                    )
                provider.files = listOf(original)
                library.indexLibrary()
                val preservedId = db.gameDao().selectShelfGames().first().single().cartridgeId
                provider.files = emptyList()
                library.indexLibrary()
                provider.files =
                    listOf(
                        original.copy(
                            uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3Aother"),
                        ),
                    )
                library.indexLibrary()
                val fresh = db.gameDao().selectShelfGames().first().single()
                org.junit.Assert.assertNotEquals(preservedId, fresh.cartridgeId)
                assertEquals(preservedId, db.retiredCartridgeDao().selectAll().single().cartridgeId)
            }
        }

    private suspend fun withLibrary(block: suspend (LemuroidLibrary, RetrogradeDatabase, FakeProvider) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db = Room.inMemoryDatabaseBuilder(context, RetrogradeDatabase::class.java).allowMainThreadQueries().build()
        val provider = FakeProvider()
        val metadata =
            object : GameMetadataProvider {
                override suspend fun retrieveMetadata(storageFile: StorageFile) =
                    GameMetadata(storageFile.extensionlessName, "gba", storageFile.name, null, null)
            }
        val library =
            LemuroidLibrary(
                db,
                Lazy { StorageProviderRegistry(context, setOf(provider)) },
                Lazy { metadata },
                BiosManager(DirectoriesManager(context)),
                CustomCoverManager(context),
            )
        try {
            block(library, db, provider)
        } finally {
            db.close()
        }
    }

    private fun file(
        name: String,
        document: String,
    ) = BaseStorageFile(name, 100, Uri.parse("content://test/$document"))

    private class FakeProvider : StorageProvider {
        var files = emptyList<BaseStorageFile>()
        var failAfterEnumeration = false
        val contents = mutableMapOf<String, String>()
        override val id = "refresh-test"
        override val name = id
        override val uriSchemes = listOf("content")
        override val prefsFragmentClass = null
        override val enabledByDefault = true

        override fun listBaseStorageFiles() =
            flow {
                emit(files)
                if (failAfterEnumeration) throw IOException("Provider disconnected")
            }

        override fun getStorageFile(baseStorageFile: BaseStorageFile) =
            StorageFile(baseStorageFile.name, baseStorageFile.size, uri = baseStorageFile.uri)

        override fun getInputStream(uri: Uri) = ByteArrayInputStream(contents[uri.toString()].orEmpty().toByteArray())

        override fun getGameRomFiles(
            game: Game,
            dataFiles: List<DataFile>,
            allowVirtualFiles: Boolean,
        ): RomFiles = error("Not used by indexing")
    }
}
