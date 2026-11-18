package com.swordfish.lemuroid.app.mobile

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
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
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfCell
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfLayout
import com.swordfish.lemuroid.app.mobile.feature.shelf.ShelfSection
import com.swordfish.lemuroid.app.mobile.feature.shelf.shelfGameKey
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
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
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").assertExists()
    }

    @Test fun populatedShelfShowsMultipleSystemsWithIdenticalSelectionContract() {
        var selected: Game? = null
        val second = cartridge(2, "Orbit", "gba")
        home(HomeViewModel.UIState(indexInProgress = false, discoveryGames = listOf(game, second)), onSelect = {
            selected = it
        })
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").assertExists()
        compose.onNodeWithContentDescription("Orbit, GBA, spine facing").performClick()
        assertEquals(second, selected)
    }

    @Test fun longTitleRemainsAvailableToAccessibility() {
        val title = "An exceptionally long original cartridge title with a complete accessible name"
        val long = cartridge(3, title, "gbc")
        compose.setContent { CartridgeLibraryTheme { CartridgeSpine(long, {}) } }
        compose.onNodeWithContentDescription("$title, GBC, spine facing").assertExists()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun capturesCartridgeFrontAndProgressiveSetup() {
        detail(cartridge(2, "Xbox fixture", "xbox"))
        val directory = File("build/reports/cartridge-setup").apply { mkdirs() }

        fun capture(
            view: View,
            name: String,
        ) {
            compose.runOnIdle {
                assertTrue("Capture target must be laid out", view.width > 0 && view.height > 0)
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    // Robolectric has no real Choreographer/window compositor for
                    // captureToImage's OnDraw/PixelCopy handshake. Draw the actual
                    // native-graphics decor view, including the separate sheet dialog.
                    view.draw(Canvas(bitmap))
                    val colors =
                        (0 until 10).flatMap { y ->
                            (0 until 10).map { x -> bitmap.getPixel(x * bitmap.width / 10, y * bitmap.height / 10) }
                        }.toSet()
                    assertTrue("Capture must contain rendered UI, not a blank bitmap", colors.size > 1)
                    File(directory, name).outputStream().use {
                        assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                } finally {
                    bitmap.recycle()
                }
            }
        }
        capture(compose.activity.window.decorView, "front.png")
        openSetup()
        val dialog = ShadowDialog.getLatestDialog()
        assertTrue("Setup must have a visible native dialog", dialog?.isShowing == true)
        capture(dialog.window!!.decorView, "setup.png")
    }

    private fun settleSheet() {
        // Advance virtual animation time, never wall-clock sleep. Sheet anchors
        // and hit targets must settle before injecting the next pointer gesture.
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
    }

    private fun openSetup() {
        compose.onNodeWithText("Setup").assertIsDisplayed().performClick()
        settleSheet()
        compose.onNodeWithTag("cartridge_setup").assertIsDisplayed()
    }

    private fun openArtworkSetup() {
        openSetup()
        // Robolectric's Compose modal sheet uses a separate Android window:
        // pointer injection into its child text can hit the underlying activity.
        // Invoke the button's real accessibility OnClick action to exercise
        // the same navigation callback, then verify the new section renders.
        compose.onNodeWithTag("cartridge_setup_section_Cartridge artwork")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        settleSheet()
        compose.onNodeWithText("Back to Setup").assertExists()
        compose.onNodeWithText("Upload / replace image").assertExists()
    }

    @Test fun singleXboxEngineDoesNotOfferFakeAlternatives() {
        compose.setContent {
            CartridgeLibraryTheme { CartridgeDetail(cartridge(2, "Xbox fixture", "xbox"), {}, {}, { _, _ -> }, {}) }
        }
        compose.onNodeWithText("Open").assertIsDisplayed()
        openSetup()
        compose.onNodeWithTag("cartridge_setup_section_Emulator")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        settleSheet()
        compose.onNodeWithText("Back to Setup").assertExists()
        compose.onNodeWithText("hakuX").assertExists()
        compose.onNodeWithText("Automatic / reset to recommended").assertDoesNotExist()
        compose.onNodeWithText("Use console preference").assertDoesNotExist()
        compose.onNodeWithText("Use previous version").assertDoesNotExist()
        compose.onNodeWithText("Fallback policy for this console").assertDoesNotExist()
    }

    @Test fun cartridgeFrontKeepsTechnicalControlsBehindSetupWithoutChangingChoices() {
        detail()
        compose.onNodeWithText("Open").assertIsEnabled()
        compose.onNodeWithText("Upload / replace image").assertDoesNotExist()
        compose.onNodeWithText("Automatic / reset to recommended").assertDoesNotExist()
        openSetup()
        compose.onNodeWithText("Emulator").assertExists()
        compose.onNodeWithText("Controls").assertExists()
        compose.onNodeWithText("Cartridge artwork").assertExists()
        compose.onNodeWithText("Close Setup").performClick()
        compose.onNodeWithText("Open").assertIsEnabled()
        compose.onNodeWithText("Upload / replace image").assertDoesNotExist()
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
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").performClick()
        compose.onNodeWithText("Open").assertIsDisplayed().assertIsEnabled()
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
        openArtworkSetup()
        compose.onNodeWithText("Upload / replace image")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
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
        openArtworkSetup()
        compose.onNodeWithText("Reset Cover")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(game.id, reset?.id)
        assertEquals(game.fileUri, reset?.fileUri)
    }

    @Test fun originalFallbackNeedsNoArtworkAndDoesNotOfferFalseResume() {
        detail()
        assertTrue(
            "Cartridge title must remain accessible",
            compose.onAllNodesWithText("Atlas").fetchSemanticsNodes().isNotEmpty(),
        )
        openArtworkSetup()
        compose.onNodeWithText("Reset Cover").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Resume").assertDoesNotExist()
    }

    @Test fun continueRetainsExistingRecentRecord() {
        var selected: Game? = null
        home(HomeViewModel.UIState(indexInProgress = false, recentGames = listOf(game)), onSelect = { selected = it })
        compose.onNodeWithText("Continue", substring = true).performClick()
        assertEquals(game, selected)
    }

    @Test
    @Config(qualifiers = "w720dp-h360dp-land")
    fun shortLandscapeKeepsImportAndResumeWithoutPermanentSearch() {
        var selected: Game? = null
        home(
            HomeViewModel.UIState(indexInProgress = true, recentGames = listOf(game)),
            onSelect = { selected = it },
        )
        compose.onNodeWithText("Add Cartridges").assertIsNotEnabled()
        compose.onNodeWithText("Search collection").assertDoesNotExist()
        compose.onNodeWithText("Continue", substring = true).performClick()
        assertEquals(game, selected)
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").assertExists()
    }

    @Test
    @Config(qualifiers = "w720dp-h360dp-land")
    fun shortLandscapeFilterSearchRemainsAvailable() {
        home(HomeViewModel.UIState(indexInProgress = false, recentGames = listOf(game)))
        compose.onNodeWithText("Search collection").assertDoesNotExist()
        compose.onNodeWithText("Filters").assertIsDisplayed().performClick()
        compose.onNodeWithText("Search collection").assertExists()
        compose.onNodeWithText("Done").assertIsDisplayed().performClick()
        compose.onNodeWithText("Search collection").assertDoesNotExist()
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").assertExists()
    }

    @Test
    @Config(qualifiers = "w720dp-h360dp-land")
    fun namedLandscapeSectionsUseTheSameCartridgeDensity() {
        val second = cartridge(2, "Orbit", "gba")
        val layout =
            ShelfLayout(
                sections =
                    listOf(
                        ShelfSection(
                            ShelfLayout.UNSORTED,
                            "First shelf",
                            listOf(ShelfCell(gameKey = shelfGameKey(game.systemId, game.fileUri))),
                        ),
                        ShelfSection(
                            "second",
                            "Second shelf",
                            listOf(ShelfCell(gameKey = shelfGameKey(second.systemId, second.fileUri))),
                        ),
                    ),
            )
        compose.setContent {
            CartridgeLibraryTheme {
                HomeScreen(
                    state = HomeViewModel.UIState(indexInProgress = false, discoveryGames = listOf(game, second)),
                    onGameClicked = {},
                    onGameLongClick = {},
                    onOpenCoreSelection = {},
                    onEnableNotificationsClicked = {},
                    onEnableMicrophoneClicked = {},
                    onSetDirectoryClicked = {},
                    shelfLayout = layout,
                )
            }
        }
        val first = compose.onNodeWithContentDescription("Atlas, GB, spine facing")
        first.assertIsDisplayed()
        val firstBounds = first.getUnclippedBoundsInRoot()
        val width = firstBounds.right - firstBounds.left
        compose.onNodeWithText("Second shelf").performClick()
        compose.mainClock.advanceTimeBy(1000)
        val next = compose.onNodeWithContentDescription("Orbit, GBA, spine facing")
        next.assertIsDisplayed()
        val nextBounds = next.getUnclippedBoundsInRoot()
        assertEquals(width, nextBounds.right - nextBounds.left)
    }

    @Test fun narrowLandscapeDetailStillScrollsToActions() {
        compose.setContent {
            CartridgeLibraryTheme {
                Box(Modifier.requiredSize(300.dp, 240.dp)) {
                    CartridgeDetail(game, {}, {}, { _, _ -> }, {})
                }
            }
        }
        compose.onNodeWithText("Open").assertIsDisplayed().assertIsEnabled()
        openArtworkSetup()
        compose.onNodeWithText("Upload / replace image").performScrollTo().assertIsEnabled()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun invalidLocalArtworkDoesNotDeletePreviousCover() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val manager = CustomCoverManager(context)
        val invalid = File(context.cacheDir, "invalid-local-art.png").apply { writeText("not an image") }
        val old = manager.stageReplacement(915301) { it.writeBytes(byteArrayOf(1, 2, 3)) }
        val previous = File(requireNotNull(Uri.parse(old).path))
        try {
            val before = manager.getCoversDirectory().listFiles().orEmpty().map { it.name }.toSet()
            assertTrue(runCatching { manager.stageLocalCover(915301, Uri.fromFile(invalid)) }.isFailure)
            val after = manager.getCoversDirectory().listFiles().orEmpty().map { it.name }.toSet()
            assertEquals(before, after)
            assertTrue(previous.exists())
            assertTrue(previous.readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        } finally {
            manager.discardOwnedCover(915301, old)
            invalid.delete()
        }
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun validLocalArtworkStagesWithoutRemovingPreviousCover() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val manager = CustomCoverManager(context)
        val source = File(context.cacheDir, "valid-local-art.png")
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        try {
            source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
        val old = manager.stageReplacement(915302) { it.writeBytes(byteArrayOf(1, 2, 3)) }
        var staged: String? = null
        try {
            staged = manager.stageLocalCover(915302, Uri.fromFile(source))
            val replacement = File(requireNotNull(Uri.parse(staged).path))
            assertTrue(replacement.length() > 0)
            assertTrue(File(requireNotNull(Uri.parse(old).path)).readBytes().contentEquals(byteArrayOf(1, 2, 3)))
            assertFalse(old == staged)
        } finally {
            manager.discardOwnedCover(915302, staged)
            manager.discardOwnedCover(915302, old)
            source.delete()
        }
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

    @Test fun onlineCoverCommitOnlyChangesMatchingCartridgeAndRetainsOldFileUntilCommit() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val db =
                Room.inMemoryDatabaseBuilder(context, RetrogradeDatabase::class.java)
                    .addCallback(GameSearchDao.CALLBACK)
                    .allowMainThreadQueries()
                    .build()
            val manager = CustomCoverManager(context)
            val old = manager.stageReplacement(game.id) { it.writeBytes(byteArrayOf(1, 2)) }
            val replacement = manager.stageReplacement(game.id) { it.writeBytes(byteArrayOf(3, 4)) }
            try {
                db.gameDao().insert(listOf(game.copy(customCoverUri = old, isFavorite = true)))
                assertTrue(File(Uri.parse(old).path!!).exists())
                assertEquals(0, db.gameDao().replaceCoverIfCurrent(game.id, "wrong-id", old, replacement))
                assertEquals(old, db.gameDao().selectById(game.id)?.customCoverUri)
                assertEquals(1, db.gameDao().replaceCoverIfCurrent(game.id, game.cartridgeId, old, replacement))
                val updated = db.gameDao().selectById(game.id)
                assertEquals(game.copy(customCoverUri = replacement, isFavorite = true), updated)
                manager.discardOwnedCover(game.id, old)
                assertFalse(File(Uri.parse(old).path!!).exists())
                assertTrue(File(Uri.parse(replacement).path!!).exists())
                assertEquals(0, db.gameDao().replaceCoverIfCurrent(game.id, game.cartridgeId, old, null))
                assertTrue(File(Uri.parse(replacement).path!!).exists())
                assertEquals(1, db.gameDao().replaceCoverIfCurrent(game.id, game.cartridgeId, replacement, null))
                assertEquals(game.copy(customCoverUri = null, isFavorite = true), db.gameDao().selectById(game.id))
                manager.discardOwnedCover(game.id, replacement)
                assertFalse(File(Uri.parse(replacement).path!!).exists())
            } finally {
                manager.discardOwnedCover(game.id, old)
                manager.discardOwnedCover(game.id, replacement)
                db.close()
            }
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
        val roomBounds = compose.onNodeWithTag("cartdeck_stationary_room").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("cartdeck_shelf_vertical_list")
            .performScrollToNode(hasContentDescription("Cartridge 16, GB, spine facing"))
        compose.onNodeWithContentDescription("Cartridge 16, GB, spine facing").assertExists()
        assertEquals(roomBounds, compose.onNodeWithTag("cartdeck_stationary_room").fetchSemanticsNode().boundsInRoot)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Cartridge 16, GB, spine facing").assertExists()
    }

    @Test
    fun largeTextKeepsTitleAndExplicitOpenAccessible() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.8f)) {
                CartridgeLibraryTheme { CartridgeDetail(game, {}, {}, { _, _ -> }, {}) }
            }
        }
        assertTrue(
            "Cartridge title must remain accessible",
            compose.onAllNodesWithText("Atlas").fetchSemanticsNodes().isNotEmpty(),
        )
        compose.onNodeWithText("Open").assertIsDisplayed().assertIsEnabled()
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
        openArtworkSetup()
        compose.onNodeWithText("Upload / replace image")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
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
                compose.onAllNodesWithContentDescription("Atlas, GB, spine facing").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Atlas, GB, spine facing").performClick()
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
        assertEquals("X360", cartridgeSystemLabel("xbox360"))
    }
}
