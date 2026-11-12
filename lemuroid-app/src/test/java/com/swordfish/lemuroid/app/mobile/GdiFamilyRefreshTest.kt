package com.swordfish.lemuroid.app.mobile

import android.net.Uri
import com.swordfish.lemuroid.lib.library.StorageFilesMerger
import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.StorageProvider
import com.swordfish.lemuroid.lib.storage.scanner.NextGenDiscIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GdiFamilyRefreshTest {
    private val descriptor = "2\n1 0 4 2352 \"Track 1.bin\" 0\n2 4500 0 2352 track02.raw 0"

    @Test
    fun incompleteFamilyKeepsPresentTracksUnderDescriptorWithoutClaimingPlayable() {
        val files = listOf(file("disc.GDI"), file("Track 1.bin"), file("unrelated.bin"))
        val groups = StorageFilesMerger.mergeDataFiles(provider(descriptor), files)
        assertEquals(setOf("disc.GDI", "unrelated.bin"), groups.map { it.primaryFile.name }.toSet())
        val disc = groups.single { it.primaryFile.name == "disc.GDI" }
        assertEquals(listOf("Track 1.bin"), disc.dataFiles.map { it.name })
        assertNull(
            NextGenDiscIdentity.probe(
                "disc.GDI",
                gdiText = descriptor,
                availableTrackNames = disc.dataFiles.map { it.name }.toSet(),
            ).platform,
        )
    }

    @Test
    fun restoringMissingTrackProducesOneCompleteDiscWithExactSourceNames() {
        val files = listOf(file("disc.gdi"), file("Track 1.bin"), file("track02.raw"))
        val groups = StorageFilesMerger.mergeDataFiles(provider(descriptor), files)
        assertEquals(1, groups.size)
        assertEquals(listOf("Track 1.bin", "track02.raw"), groups.single().dataFiles.map { it.name })
        assertEquals(
            NextGenDiscIdentity.Platform.DREAMCAST,
            NextGenDiscIdentity.probe(
                "disc.gdi",
                gdiText = descriptor,
                availableTrackNames = groups.single().dataFiles.map { it.name }.toSet(),
            ).platform,
        )
    }

    @Test(expected = IOException::class)
    fun unreadableDescriptorStillAbortsIncompleteScanInsteadOfPruningLibrary() {
        StorageFilesMerger.mergeDataFiles(provider(null), listOf(file("disc.gdi"), file("Track 1.bin")))
    }

    private fun file(name: String) = BaseStorageFile(name, 100, Uri.parse("content://fixture/$name"))

    private fun provider(text: String?): StorageProvider =
        Proxy.newProxyInstance(
            StorageProvider::class.java.classLoader,
            arrayOf(StorageProvider::class.java),
        ) { _, method, _ ->
            check(method.name == "getInputStream") { "Unexpected storage operation: ${method.name}" }
            text?.let { ByteArrayInputStream(it.toByteArray()) }
        } as StorageProvider
}
