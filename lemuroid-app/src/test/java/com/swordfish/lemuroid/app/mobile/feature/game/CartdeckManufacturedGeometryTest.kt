package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckLighting
import com.swordfish.lemuroid.app.mobile.feature.shelf.CartdeckPersonalDecal
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartdeckManufacturedGeometryTest {
    @Test
    fun crownTracksShareOneDatumAndMirrorAcrossPortraitChassis() {
        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(22f, 84f, 371f, 346f)
        val tracks = CartdeckManufacturedGeometry.channels(frame, video, emptyList(), 1f, false)
        val crown = tracks.filter { it.bounds.center.y == 16f }.map { it.bounds }
        assertEquals(3, crown.size)
        assertEquals(frame.center.x, crown[1].center.x)
        assertEquals(frame.width - crown[0].right, crown[2].left)
        assertEquals(frame.width - crown[0].left, crown[2].right)
        assertTrue(tracks.all { !CartdeckHardwareGeometry.intersects(it.bounds, video) })
    }

    @Test
    fun crampedOrObstructedSideTracksDisappearAsAPair() {
        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(22f, 84f, 371f, 346f)
        val leftPad = Rect(5f, 100f, 20f, 160f)
        val tracks = CartdeckManufacturedGeometry.channels(frame, video, listOf(leftPad), 1f, false)
        assertTrue(tracks.none { it.zone == CartdeckLighting.SIDE_RAILS && it.bounds.height > 10f })
        assertTrue(tracks.none { CartdeckHardwareGeometry.intersects(it.bounds, leftPad) })
        val ds = Rect(10f, 10f, 383f, 550f)
        val compact = CartdeckManufacturedGeometry.channels(frame, ds, emptyList(), 1f, true)
        assertTrue(compact.none { it.bounds.center.y == 16f })
        assertTrue(compact.none { CartdeckHardwareGeometry.intersects(it.bounds, ds) })
    }

    @Test
    fun glowNeverEscapesSafeHardwareOrTouchesVideoAndControls() {
        val frame = Rect(0f, 0f, 393f, 852f)
        val video = Rect(22f, 84f, 371f, 346f)
        val controls = listOf(Rect(30f, 400f, 180f, 700f))
        val tracks = CartdeckManufacturedGeometry.channels(frame, video, controls, 1f, false)
        val halos = tracks.mapNotNull { it.glowBounds }
        assertTrue(halos.isNotEmpty())
        assertTrue(halos.all { it.left >= 9f && it.top >= 9f && it.right <= 384f && it.bottom <= 843f })
        assertFalse(halos.any { CartdeckHardwareGeometry.intersects(it, video) })
        assertFalse(halos.any { halo -> controls.any { CartdeckHardwareGeometry.intersects(halo, it) } })
    }

    @Test
    fun landscapeEngravingIsPairedAndAvoidsControlsAndArtworkSeats() {
        val frame = Rect(0f, 0f, 852f, 393f)
        val video = Rect(230f, 8f, 622f, 385f)
        val controls = listOf(Rect(30f, 46f, 210f, 345f), Rect(642f, 46f, 822f, 345f))
        val decoration =
            CartdeckManufacturedGeometry.landscapeDecoration(
                frame, video, controls, 1f, CartdeckPersonalDecal.ANCHOR_LOWER_RIGHT,
            )
        assertEquals(4, decoration.bands.size)
        assertEquals(decoration.bands[0].width, decoration.bands[1].width)
        val art = listOfNotNull(decoration.stampSeat, decoration.decalSeat)
        assertEquals(2, art.size)
        assertTrue((decoration.bands + art).none { CartdeckHardwareGeometry.intersects(it, video) })
        assertTrue((decoration.bands + art).none { zone ->
            controls.any { CartdeckHardwareGeometry.intersects(it, zone) }
        })
        assertTrue(art.none { seat -> decoration.bands.any { CartdeckHardwareGeometry.intersects(it, seat) } })
    }

    @Test
    fun landscapeFitCapsOnlySessionScaleAndKeepsNativeRotationAndMargins() {
        val saved = TouchControllerSettingsManager.Settings(scale = 0.8f, rotation = 0.1f, marginX = 0.1f)
        val compact = GameScreenLayout.fitLandscapeHardwareSettings(saved, 393.dp)
        assertTrue(compact.scale < saved.scale)
        assertEquals(saved.rotation, compact.rotation)
        assertEquals(saved.marginX, compact.marginX)
        assertEquals(saved, GameScreenLayout.fitLandscapeHardwareSettings(saved, 800.dp))
    }

    @Test
    fun landscapeHousingUsesOnlyAvailableSideSpaceAndNeverMovesTheAperture() {
        val frame = Rect(0f, 0f, 852f, 393f)
        val video = Rect(150f, 22f, 702f, 333f)
        val controls = listOf(Rect(25f, 80f, 125f, 210f), Rect(727f, 80f, 827f, 210f))
        val panels = CartdeckManufacturedGeometry.sideHousings(frame, video, controls, 1f)
        assertEquals(2, panels.size)
        assertFalse(panels.any { CartdeckHardwareGeometry.intersects(it, video) })
        assertTrue(CartdeckManufacturedGeometry.sideHousings(frame, video, emptyList(), 1f).isEmpty())
        val maximised = Rect(8f, 8f, 844f, 385f)
        assertTrue(CartdeckManufacturedGeometry.sideHousings(frame, maximised, controls, 1f).isEmpty())
        assertFalse(CartdeckManufacturedGeometry.footerFits(frame.width, frame.height, maximised, controls, 1f))
    }
}
