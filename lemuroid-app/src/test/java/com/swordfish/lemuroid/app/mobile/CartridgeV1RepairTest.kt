package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfBoard
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeSpine
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeArtworkRequest
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeV1RepairTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val game =
        Game(
            id = 4,
            title = "Atlas",
            fileName = "atlas.gb",
            fileUri = "content://rom/4",
            systemId = "gb",
            developer = null,
            coverFrontUrl = "https://existing.invalid/atlas.png",
            lastIndexedAt = 0,
        )

    @Test fun verticalShelfStacksRowsAndRetainsFullAccessibleTitles() {
        val games = (1..12).map { game.copy(id = it, title = "Cartridge $it", fileUri = "content://rom/$it") }
        val layout =
            ShelfLayout().reconcile(
                games.associate {
                    shelfGameKey(it.systemId, it.fileUri) to it.displayName()
                },
            )
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(
                    layout,
                    games,
                    {},
                    {},
                )
            }
        }
        val first = compose.onNodeWithContentDescription("Cartridge 1, GB, spine facing").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithContentDescription("Cartridge 2, GB, spine facing").getUnclippedBoundsInRoot()
        val sixth = compose.onNodeWithContentDescription("Cartridge 6, GB, spine facing").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(second.left > first.left)
        org.junit.Assert.assertTrue(sixth.top > first.top)
        org.junit.Assert.assertTrue((first.right - first.left).value >= 48f)
    }

    @Test fun nativeDragSwapsIntoPersistentVacancyWithoutSelectingGame() {
        val identity = shelfGameKey(game.systemId, game.fileUri)
        val initial = ShelfLayout().reconcile(mapOf(identity to game.title)).insertSpace(ShelfLayout.UNSORTED)
        val layout = mutableStateOf(initial)
        var selections = 0
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(
                    layout.value,
                    listOf(game),
                    { selections++ },
                    {},
                    arrange = true,
                    onEdit = { layout.value = it(layout.value) },
                )
            }
        }
        val cartridge = compose.onNodeWithContentDescription("Atlas, GB, spine facing")
        val gap = compose.onNodeWithContentDescription("Reserved empty shelf space")
        val delta = gap.fetchSemanticsNode().boundsInRoot.center - cartridge.fetchSemanticsNode().boundsInRoot.center
        cartridge.performTouchInput { swipe(center, center + delta, 500) }
        compose.runOnIdle {
            assertEquals(0, selections)
            assertEquals(null, layout.value.sections.single().cells.first().gameKey)
            assertEquals(identity, layout.value.sections.single().cells[1].gameKey)
        }
    }

    @Test fun originalArtworkAndCustomPrecedenceSurviveReset() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val custom = game.copy(customCoverUri = "file:///private/custom.png")
        assertEquals(custom.customCoverUri, cartridgeArtworkRequest(context, custom, 1).data)
        assertEquals(game.coverFrontUrl, cartridgeArtworkRequest(context, game, 2).data)
        assertNotEquals(
            cartridgeArtworkRequest(context, custom, 1).memoryCacheKey,
            cartridgeArtworkRequest(context, custom, 2).memoryCacheKey,
        )
        assertEquals(coil.request.CachePolicy.ENABLED, cartridgeArtworkRequest(context, game, 2).diskCachePolicy)
        assertEquals(coil.request.CachePolicy.DISABLED, cartridgeArtworkRequest(context, custom, 2).memoryCachePolicy)
    }

    @Test fun physicalCartridgeDoesNotGrowAtTwoHundredPercentText() {
        val font = mutableStateOf(1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font.value)) {
                CartridgeLibraryTheme { CartridgeSpine(game, {}) }
            }
        }
        val initial = compose.onNodeWithContentDescription("Atlas, GB, spine facing").getUnclippedBoundsInRoot()
        compose.runOnIdle { font.value = 2f }
        assertEquals(
            initial,
            compose.onNodeWithContentDescription("Atlas, GB, spine facing").getUnclippedBoundsInRoot(),
        )
    }
}
