package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
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
        val initial = compose.onNodeWithContentDescription("Atlas, GB").getUnclippedBoundsInRoot()
        compose.runOnIdle { font.value = 2f }
        assertEquals(initial, compose.onNodeWithContentDescription("Atlas, GB").getUnclippedBoundsInRoot())
    }
}
