package com.swordfish.lemuroid.app.mobile.feature.product

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.app.gemrating.GemCataloguePackage
import com.swordfish.lemuroid.app.mobile.feature.main.MainNavigationBar
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.MainTopBar
import com.swordfish.lemuroid.app.mobile.feature.main.MainViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.CartridgeSaveBinding
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ProductSurfacesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun saveCentreShowsCompactContentAndExposesRestoreWithAnEmptyInventory() {
        var restores = 0
        var unassigned = 0
        compose.setContent {
            CartridgeLibraryTheme {
                SafeSaveSurface(
                    playerName = "Manager",
                    inventory = emptyList(),
                    onBackUp = null,
                    onRestore = { restores++ },
                    availableActions = emptyList(),
                    onDetails = {},
                    onUnassigned = { unassigned++ },
                )
            }
        }
        compose.onNodeWithText("Save Centre").assertExists()
        compose.onNodeWithText("Back Up Everything").assertIsNotEnabled()
        compose.onNodeWithText("Restore Backup").performClick()
        compose.onNodeWithText("Unassigned Backups").performClick()
        assertEquals(1, restores)
        assertEquals(1, unassigned)
    }

    @Test fun bulkBackupShowsCoverageBeforeOpeningTheFilePicker() {
        val game =
            Game(
                fileName = "backup-scope.gb",
                fileUri = "content://fixture/backup-scope",
                title = "Backup scope fixture",
                systemId = "gb",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 1,
            )
        val directories = DirectoriesManager(compose.activity.application)
        val binding = CartridgeSaveBinding(directories) { listOf(game) }
        runBlocking { SavesManager(directories, binding).setSaveRAM(game, byteArrayOf(1, 2), CoreID.GAMBATTE) }
        compose.setContent {
            CartridgeLibraryTheme { SafeSaveScreen(listOf(game), binding) }
        }
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithText("Back Up Everything").assertIsEnabled() }.isSuccess
        }
        compose.onNodeWithText("Back Up Everything").performClick()
        compose.onNodeWithText("Choose Backup File").assertIsDisplayed()
        compose.onNodeWithText("unassigned backups are excluded", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Choose Backup File").assertDoesNotExist()
    }

    @Test fun gameIntelligenceUsesTheInstalledCartridgeTitleAndKeepsCatalogueFacts() {
        val release = GemCataloguePackage.Release("fixture:01", "gb", "Catalogue title", "fixture", "01")
        val game =
            Game(
                fileName = "fixture.gb",
                fileUri = "content://fixture/installed",
                title = "My cartridge title",
                systemId = "gb",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 1,
            )
        compose.setContent {
            CartridgeLibraryTheme { GemIntelligenceRecord(release, null, null, installedGame = game) }
        }
        compose.onAllNodes(hasText("My cartridge title")).fetchSemanticsNodes().also { assertTrue(it.isNotEmpty()) }
        compose.onNodeWithText("Catalogue title").assertDoesNotExist()
        compose.onNodeWithText("More details").performClick()
        compose.onNodeWithText("Catalogue title: Catalogue title").assertExists()
    }

    @Test fun curatedHomebrewSearchOpensTheBrowserWithoutSavingSources() {
        val term = "PSP puzzle"
        compose.setContent {
            CartridgeLibraryTheme { GemWebsiteSearchScreen() }
        }
        val action = compose.onNodeWithText("Search curated sites")
        action.assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput(term)
        action.assertIsEnabled().performClick()
        compose.runOnIdle {
            val launch = shadowOf(compose.activity).nextStartedActivity
            assertEquals(Intent.ACTION_VIEW, launch.action)
            assertEquals(HomebrewDirectory.searchUri(term)?.toASCIIString(), launch.dataString)
            assertTrue(GemWebsiteStore(compose.activity).read().isEmpty())
        }
    }

    @Test fun juniorEmptyStateOffersOnlyParentRequest() {
        var requests = 0
        compose.setContent {
            CartridgeLibraryTheme {
                JuniorPlayroomSurface(
                    playerName = "Player",
                    selected = null,
                    canContinue = false,
                    canNavigate = false,
                    onPrevious = {},
                    onNext = {},
                    onPlay = {},
                    onRequestGrownUps = { requests++ },
                )
            }
        }
        compose.onNodeWithText("Ask a grown-up to add your games").assertExists()
        compose.onNodeWithText("Play").assertDoesNotExist()
        compose.onNodeWithText("Grown-Ups").performClick()
        assertEquals(1, requests)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    @OptIn(ExperimentalTestApi::class)
    fun tvSaveCentreStartsOnRestoreAndAcceptsDpadConfirm() {
        var restores = 0
        compose.setContent {
            CartridgeLibraryTheme {
                SafeSaveSurface(
                    playerName = "Manager",
                    inventory = emptyList(),
                    onBackUp = null,
                    onRestore = { restores++ },
                    availableActions = emptyList(),
                    onDetails = {},
                )
            }
        }
        compose.onNodeWithText("Restore Backup").assertIsFocused().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, restores)
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-port")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun saveCentreRendersPortraitWithConsoleDetails() = captureSaveCentre("portrait")

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun saveCentreRendersCompactLandscapeWithBothPrimaryActions() = captureSaveCentre("landscape")

    @Test
    @Config(qualifiers = "w393dp-h852dp-port")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun saveCentreRendersInSharedProductChrome() {
        compose.setContent {
            CartridgeLibraryTheme {
                val navigation = rememberNavController()
                val route = MainRoute.SAFESAVE
                Scaffold(
                    topBar = { MainTopBar(route, navigation, {}, MainViewModel.UiState()) },
                    bottomBar = { MainNavigationBar(route, navigation) },
                ) { padding ->
                    SafeSaveSurface(
                        playerName = "Manager",
                        inventory = listOf(SaveInventoryRow("fixture", "A cartridge", "psx", 0, 2, 1, 1, "3 states")),
                        onBackUp = {},
                        onRestore = {},
                        availableActions = emptyList(),
                        onDetails = {},
                        onUnassigned = {},
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
        compose.onNodeWithText("Save Centre").assertIsDisplayed()
        compose.onNodeWithText("SafeSave").assertIsDisplayed()
        compose.onNodeWithContentDescription("SafeSave").assertIsDisplayed()
        saveCentreScreenshot("portrait-with-chrome")
    }

    private fun captureSaveCentre(orientation: String) {
        val title = "A Long Cartridge Title With Save Slots and a Shared Card"
        var opened: String? = null
        compose.setContent {
            CartridgeLibraryTheme {
                Surface {
                    SafeSaveSurface(
                        playerName = "Manager",
                        inventory = listOf(SaveInventoryRow("fixture", title, "psx", 0, 2, 1, 1, "3 states · 1 card")),
                        onBackUp = {},
                        onRestore = {},
                        availableActions = emptyList(),
                        onDetails = { opened = it },
                        unassignedCount = 2,
                        onUnassigned = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Save Centre").assertIsDisplayed()
        compose.onNodeWithText("Back Up Everything").assertIsDisplayed()
        compose.onNodeWithText("Restore Backup").assertIsDisplayed()
        saveCentreScreenshot(orientation)
        val list = compose.onNode(hasScrollAction())
        list.performScrollToNode(hasText("Games 1 · States 3 · Cards 1"))
        compose.onNodeWithText("Games 1 · States 3 · Cards 1").performClick()
        list.performScrollToNode(hasText(title))
        compose.onNodeWithText("Open").performClick()
        assertEquals("fixture", opened)
    }

    private fun saveCentreScreenshot(orientation: String) {
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val image = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(image))
                val destination = File("build/reports/gc01-menu-screens/safesave", "$orientation.png")
                check(destination.parentFile?.let { it.isDirectory || it.mkdirs() } == true)
                destination.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                image.recycle()
            }
        }
    }

    @Test fun unresolvedSaveConflictCannotConfirm() {
        compose.setContent {
            CartridgeLibraryTheme {
                SaveOperationReview(
                    preview =
                        SaveOperationPreview(
                            operation = "Import",
                            scope = "Player",
                            verifiedItems = 1,
                            exclusions = emptyList(),
                            conflicts = listOf("Destination exists"),
                            canProceed = true,
                        ),
                    onConfirm = {},
                    onCancel = {},
                )
            }
        }
        compose.onNodeWithText("Destination exists").assertExists()
        compose.onNodeWithText("Confirm").assertIsNotEnabled()
    }
}
