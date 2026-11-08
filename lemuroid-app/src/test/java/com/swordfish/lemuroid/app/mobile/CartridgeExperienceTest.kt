package com.swordfish.lemuroid.app.mobile

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesScreen
import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesViewModel
import com.swordfish.lemuroid.app.mobile.feature.games.GamesScreen
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModel
import com.swordfish.lemuroid.app.mobile.feature.home.HomeScreen
import com.swordfish.lemuroid.app.mobile.feature.home.HomeViewModel
import com.swordfish.lemuroid.app.mobile.feature.main.CartridgeDetail
import com.swordfish.lemuroid.app.mobile.feature.search.SearchScreen
import com.swordfish.lemuroid.app.mobile.feature.search.SearchViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeSpine
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeSystemLabel
import com.swordfish.lemuroid.lib.library.MetaSystemID
import com.swordfish.lemuroid.lib.library.covers.CustomCoverManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.dao.GameSearchDao
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeExperienceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val game = cartridge(1, "Atlas", "gb")

    private fun cartridge(
        id: Int,
        title: String,
        system: String,
    ) = Game(
        id = id,
        title = title,
        systemId = system,
        fileName = "$id.gb",
        fileUri = "content://test/$id",
        developer = null,
        coverFrontUrl = null,
        lastIndexedAt = 0,
    )

    private fun home(
        state: HomeViewModel.UIState,
        onAdd: () -> Unit = {},
        onSelect: (Game) -> Unit = {},
    ) {
        compose.setContent {
            CartridgeLibraryTheme {
                HomeScreen(
                    state = state,
                    onGameClicked = onSelect,
                    onGameLongClick = {},
                    onOpenCoreSelection = {},
                    onEnableNotificationsClicked = {},
                    onEnableMicrophoneClicked = {},
                    onSetDirectoryClicked = onAdd,
                )
            }
        }
    }

    private fun detail(
        selected: Game = game,
        open: (Game) -> Unit = {},
        back: () -> Unit = {},
        change: (Game, String) -> Unit = { _, _ -> },
        reset: (Game) -> Unit = {},
    ) {
        compose.setContent {
            CartridgeLibraryTheme { CartridgeDetail(selected, back, open, change, reset) }
        }
    }

    @Test fun emptyLibraryAddsCartridgesThroughCallback() {
        var imports = 0
        home(HomeViewModel.UIState(indexInProgress = false, showNoGamesCard = true), onAdd = { imports++ })
        compose.onNodeWithText("Add Cartridges").performClick()
        assertEquals(1, imports)
    }

    @Test fun scanProgressKeepsLibraryVisibleAndPreventsDuplicateImports() {
        home(HomeViewModel.UIState(indexInProgress = true, discoveryGames = listOf(game)))
        compose.onNodeWithText("Add Cartridges").assertIsNotEnabled()
        compose.onNodeWithText("Adding cartridges… Your library stays available.").assertExists()
        compose.onNodeWithContentDescription("Atlas, GB").assertExists()
    }

    @Test fun populatedShelfShowsMultipleSystemsWithIdenticalSelectionContract() {
        var selected: Game? = null
        val second = cartridge(2, "Orbit", "gba")
        home(HomeViewModel.UIState(indexInProgress = false, discoveryGames = listOf(game, second)), onSelect = {
            selected = it
        })
        compose.onNodeWithContentDescription("Atlas, GB").assertExists()
        compose.onNodeWithContentDescription("Orbit, GBA").performClick()
        assertEquals(second, selected)
    }

    @Test fun longTitleRemainsAvailableToAccessibility() {
        val title = "An exceptionally long original cartridge title with a complete accessible name"
        val long = cartridge(3, title, "gbc")
        compose.setContent { CartridgeLibraryTheme { CartridgeSpine(long, {}) } }
        compose.onNodeWithContentDescription("$title, GBC").assertExists()
    }

    @Test fun selectFrontCoverDoesNotLaunchAndOpenDispatchesOnce() {
        var selected by mutableStateOf(false)
        var launches = 0
        compose.setContent {
            CartridgeLibraryTheme {
                if (selected) {
                    CartridgeDetail(game, { selected = false }, { launches++ }, { _, _ -> }, {})
                } else {
                    CartridgeSpine(game, { selected = true })
                }
            }
        }
        compose.onNodeWithContentDescription("Atlas, GB").performClick()
        compose.onNodeWithText("Open").performScrollTo().assertIsEnabled()
        assertEquals(0, launches)
        compose.onNodeWithText("Open").performClick()
        compose.onNodeWithText("Open").assertIsNotEnabled().performClick()
        assertEquals(1, launches)
    }

    @Test fun backReturnsToShelfWithoutLaunch() {
        var backs = 0
        var launches = 0
        detail(open = { launches++ }, back = { backs++ })
        compose.onNodeWithText("Back to shelf").performClick()
        assertEquals(1, backs)
        assertEquals(0, launches)
    }

    @Test fun coverPickerCancelDoesNotWriteArtwork() {
        var writes = 0
        detail(change = { _, _ -> writes++ })
        compose.onNodeWithText("Change Cover").performScrollTo().performClick()
        val activity = shadowOf(compose.activity)
        val request = activity.nextStartedActivityForResult
        assertEquals(Intent.ACTION_GET_CONTENT, request.intent.action)
        activity.receiveResult(request.intent, Activity.RESULT_CANCELED, null)
        compose.waitForIdle()
        assertEquals(0, writes)
    }

    @Test fun resetCoverUsesExistingCallbackAndPreservesGameIdentity() {
        var reset: Game? = null
        val custom = game.copy(customCoverUri = "file:///local-cover.png")
        detail(selected = custom, reset = { reset = it })
        compose.onNodeWithText("Reset Cover").performScrollTo().performClick()
        assertEquals(game.id, reset?.id)
        assertEquals(game.fileUri, reset?.fileUri)
    }

    @Test fun originalFallbackNeedsNoArtworkAndDoesNotOfferFalseResume() {
        detail()
        compose.onNodeWithText("Atlas").assertExists()
        compose.onNodeWithText("Reset Cover").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Resume").assertDoesNotExist()
    }

    @Test fun continueRetainsExistingRecentRecord() {
        var selected: Game? = null
        home(HomeViewModel.UIState(indexInProgress = false, recentGames = listOf(game)), onSelect = { selected = it })
        compose.onNodeWithText("Continue", substring = true).performClick()
        assertEquals(game, selected)
    }

    @Test fun narrowLandscapeDetailStillScrollsToActions() {
        compose.setContent {
            CartridgeLibraryTheme {
                Box(Modifier.requiredSize(300.dp, 240.dp)) {
                    CartridgeDetail(game, {}, {}, { _, _ -> }, {})
                }
            }
        }
        compose.onNodeWithText("Open").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Change Cover").performScrollTo().assertIsEnabled()
    }

    @Test fun customCoverManagerPersistsAndResetsOnlyTheSelectedCover() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val source = File(context.cacheDir, "original-atlas.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
            val manager = CustomCoverManager(context)
            val uri = manager.importCover(8675309, Uri.fromFile(source))
            val persisted = File(Uri.parse(uri).path!!)
            assertTrue(persisted.exists())
            assertTrue(persisted.readBytes().contentEquals(source.readBytes()))
            assertEquals(manager.getCoversDirectory(), CustomCoverManager(context).getCoversDirectory())
            manager.deleteCover(8675309)
            assertFalse(persisted.exists())
            assertTrue(source.exists())
        }

    @Test
    fun shelfPositionSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val games = (1..20).map { cartridge(it, "Cartridge $it", "gb") }
        restoration.setContent {
            CartridgeLibraryTheme {
                HomeScreen(
                    state = HomeViewModel.UIState(indexInProgress = false, discoveryGames = games),
                    onGameClicked = {},
                    onGameLongClick = {},
                    onOpenCoreSelection = {},
                    onEnableNotificationsClicked = {},
                    onEnableMicrophoneClicked = {},
                    onSetDirectoryClicked = {},
                )
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(15)
        compose.onNodeWithContentDescription("Cartridge 16, GB").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Cartridge 16, GB").assertExists()
    }

    @Test
    fun largeTextKeepsTitleAndExplicitOpenAccessible() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.8f)) {
                CartridgeLibraryTheme { CartridgeDetail(game, {}, {}, { _, _ -> }, {}) }
            }
        }
        compose.onNodeWithText("Atlas").assertExists()
        compose.onNodeWithText("Open").performScrollTo().assertIsEnabled()
    }

    @Test
    fun chosenCoverDispatchesExistingRecordAndUriExactlyOnce() {
        var changes = 0
        var selected: Game? = null
        var cover: String? = null
        detail(change = { record, uri ->
            changes++
            selected = record
            cover = uri
        })
        compose.onNodeWithText("Change Cover").performScrollTo().performClick()
        val activity = shadowOf(compose.activity)
        val request = activity.nextStartedActivityForResult
        activity.receiveResult(
            request.intent,
            Activity.RESULT_OK,
            Intent().setData(Uri.parse("content://test/artwork")),
        )
        compose.waitForIdle()
        assertEquals(1, changes)
        assertEquals(game, selected)
        assertEquals("content://test/artwork", cover)
    }

    @Test fun favouritesUsesSharedSelection() = verifyRouteSelection("favourites")

    @Test fun systemsUsesSharedSelection() = verifyRouteSelection("systems")

    @Test fun searchUsesSharedSelection() = verifyRouteSelection("search")

    private fun verifyRouteSelection(route: String) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db =
            Room.inMemoryDatabaseBuilder(context, RetrogradeDatabase::class.java)
                .addCallback(GameSearchDao.CALLBACK)
                .allowMainThreadQueries()
                .build()
        val store = ViewModelStore()
        val favourite = game.copy(isFavorite = true)
        db.gameDao().insert(listOf(favourite))
        var selected: Game? = null
        var selections = 0
        val select: (Game) -> Unit = {
            selected = it
            selections++
        }
        val favourites = ViewModelProvider(store, FavoritesViewModel.Factory(db))[FavoritesViewModel::class.java]
        val systems = ViewModelProvider(store, GamesViewModel.Factory(db, MetaSystemID.GB))[GamesViewModel::class.java]
        val search = ViewModelProvider(store, SearchViewModel.Factory(db))[SearchViewModel::class.java]
        compose.setContent {
            CartridgeLibraryTheme {
                when (route) {
                    "favourites" ->
                        FavoritesScreen(
                            viewModel = favourites,
                            onGameClick = select,
                            onGameLongClick = {},
                        )
                    "systems" ->
                        GamesScreen(
                            viewModel = systems,
                            onGameClick = select,
                            onGameLongClick = {},
                            onGameFavoriteToggle = { _, _ -> },
                        )
                    else ->
                        SearchScreen(
                            viewModel = search,
                            searchQuery = "Atlas",
                            onGameClick = select,
                            onGameLongClick = {},
                            onGameFavoriteToggle = { _, _ -> },
                            onResetSearchQuery = {},
                        )
                }
            }
        }
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Atlas, GB").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Atlas, GB").performClick()
            assertEquals(favourite, selected)
            assertEquals(1, selections)
        } finally {
            store.clear()
            db.close()
        }
    }

    @Test fun badgesPreserveSystemIds() {
        assertEquals("GB", cartridgeSystemLabel("gb"))
        assertEquals("GBC", cartridgeSystemLabel("gbc"))
        assertEquals("GBA", cartridgeSystemLabel("gba"))
        assertEquals("PS1", cartridgeSystemLabel("psx"))
    }
}
