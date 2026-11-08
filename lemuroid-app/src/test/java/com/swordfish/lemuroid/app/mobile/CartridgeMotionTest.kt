package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.swordfish.lemuroid.app.mobile.feature.main.CartridgeDetail
import com.swordfish.lemuroid.app.mobile.feature.main.CartridgePickup
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfAppearance
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeGeometry
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeFlight
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeMotionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val game = Game(1, "atlas.gb", "content://fixture/1", "Atlas", "gb", null, null, 0)

    @Test fun pickupStartsAtTheActualCellLiftsRotatesAndSettlesAtCover() {
        val firstRow = Rect(20f, 100f, 92f, 360f)
        val thirdRow = Rect(240f, 700f, 312f, 960f)
        val target = Rect(36f, 200f, 356f, 590f)
        assertEquals(firstRow, cartridgeFlight(firstRow, target, 0f).bounds)
        assertEquals(thirdRow, cartridgeFlight(thirdRow, target, 0f).bounds)
        assertTrue(cartridgeFlight(thirdRow, target, 0.1f).bounds.top < thirdRow.top)
        assertFalse(cartridgeFlight(thirdRow, target, 0.3f).showFront)
        assertTrue(cartridgeFlight(thirdRow, target, 0.8f).showFront)
        assertEquals(target, cartridgeFlight(thirdRow, target, 1f).bounds)
        assertEquals(0f, cartridgeFlight(thirdRow, target, 1f).rotationY)
        assertEquals(thirdRow, cartridgeFlight(thirdRow, target, -1f).bounds)
    }

    @Test fun disposingAnOldCellCannotEraseItsReplacementGeometry() {
        val geometry = CartridgeGeometry()
        val first = Any()
        val replacement = Any()
        val bounds = Rect(1f, 2f, 73f, 262f)
        geometry.update(1, first, Rect.Zero)
        geometry.update(1, replacement, bounds)
        geometry.remove(1, first)
        assertEquals(bounds, geometry.cells[1])
        geometry.remove(1, replacement)
        assertFalse(geometry.cells.containsKey(1))
    }

    @Test fun reducedMotionShowsActionsImmediatelyAndNeverAutoLaunches() {
        var opens = 0
        var closes = 0
        compose.setContent {
            CartridgeLibraryTheme(ShelfAppearance(reducedMotion = true)) {
                Box {
                    CartridgePickup(1, game, true, Rect(10f, 400f, 82f, 660f), { closes++ }) { front, close ->
                        CartridgeDetail(game, close, { opens++ }, { _, _ -> }, {}, frontModifier = front)
                    }
                }
            }
        }
        assertEquals(0, opens)
        compose.onNodeWithText("Open").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Open").performClick()
        assertEquals(1, opens)
        compose.onNodeWithText("Back to shelf").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, closes)
    }
}
