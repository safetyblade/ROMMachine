package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

/** Real Android file/image code and pure responsive placement invariants. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartdeckPersonalDecalTest {
    @Test
    fun anchorPlacementLeavesGameAndControlsUntouched() {
        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(16f, 80f, 377f, 370f)
        val pads = listOf(Rect(20f, 590f, 182f, 819f), Rect(205f, 590f, 373f, 819f))
        val controls = listOf(video) + pads
        CartdeckPersonalDecal.anchors.keys.forEach { anchor ->
            val decal = CartdeckPersonalDecal(UUID.randomUUID().toString(), anchor)
            val seat = CartdeckDecalGeometry.place(frame, controls, decal, 98f, 8f)
            if (seat != null) {
                assertTrue(seat.left >= frame.left && seat.right <= frame.right)
                assertTrue(seat.top >= frame.top && seat.bottom <= frame.bottom)
                assertTrue(
                    controls.none { blocked ->
                        seat.left < blocked.right + 8f && seat.right > blocked.left - 8f &&
                            seat.top < blocked.bottom + 8f && seat.bottom > blocked.top - 8f
                    },
                )
            }
        }
        assertEquals(
            null,
            CartdeckDecalGeometry.place(
                frame,
                listOf(frame),
                CartdeckPersonalDecal(UUID.randomUUID().toString()),
                98f,
                8f,
            ),
        )
    }

    @Test
    fun landscapeTabletAndDualScreenPreserveTheMeasuredAperture() {
        for (frame in listOf(Rect(0f, 0f, 832f, 384f), Rect(0f, 0f, 1600f, 1200f))) {
            val game = Rect(frame.width * 0.15f, 12f, frame.width * 0.85f, frame.height - 12f)
            val seat =
                CartdeckDecalGeometry.place(
                    frame,
                    listOf(game),
                    CartdeckPersonalDecal(UUID.randomUUID().toString()),
                    98f,
                    8f,
                )
            if (seat != null) {
                assertFalse(
                    seat.left < game.right + 8f && seat.right > game.left - 8f &&
                        seat.top < game.bottom + 8f && seat.bottom > game.top - 8f,
                )
            }
        }
    }

    @Test
    fun invalidIdsOpacityAndPlacementNeverEnterPersistentAppearance() {
        val valid = CartdeckPersonalDecal(UUID.randomUUID().toString())
        assertEquals(valid, valid.validate())
        assertThrows(IllegalArgumentException::class.java) { valid.copy(assetId = "../../roms").validate() }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(anchor = "untrusted").validate() }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(opacity = Float.NaN).validate() }
        assertThrows(IllegalArgumentException::class.java) { valid.copy(scale = 2f).validate() }
        assertThrows(IllegalArgumentException::class.java) {
            ShelfLayout(appearance = ShelfAppearance(decal = valid.copy(rotation = 180f))).validate()
        }
    }

    @Test
    fun imageImportIsPrivateStagedAndCopiesCanShareTheSameArtwork() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val store = CartdeckDecalStore(app)
        val input = File(app.cacheDir, "cartdeck-test-decal.png")
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        input.outputStream().use { assertTrue(bmp.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bmp.recycle()
        val id = store.importImage(Uri.fromFile(input))
        try {
            val staging = File(app.cacheDir, "cartdeck/decal-pending/$id.png")
            val durable = File(app.filesDir, "cartdeck/decals/$id.png")
            assertTrue(staging.isFile)
            assertNotNull(store.readImage(id))
            store.promote(id)
            assertTrue(durable.isFile)
            assertFalse(staging.exists())
            val saved =
                ShelfAppearance(decal = CartdeckPersonalDecal(id)).saveNewProfile("Custom", 100L)
            val copied = saved.duplicateProfile(saved.profiles.first().id, now = 200L)
            assertEquals(id, copied.profiles.last().decal?.assetId)
            val recreated = decode(encode(ShelfLayout(appearance = copied))).appearance
            assertEquals(copied, recreated)
            durable.setLastModified(System.currentTimeMillis() - 12 * 60 * 1000L)
            store.pruneUnused(copied)
            assertTrue("Shared personal image is still referenced", durable.exists())
            store.pruneUnused(ShelfAppearance())
            assertFalse("Unreferenced image can be removed", durable.exists())
        } finally {
            store.discard(id)
            input.delete()
        }
    }
}
