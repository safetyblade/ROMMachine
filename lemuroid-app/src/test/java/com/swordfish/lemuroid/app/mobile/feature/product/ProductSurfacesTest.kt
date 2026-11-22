package com.swordfish.lemuroid.app.mobile.feature.product

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ProductSurfacesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun saveCentreShowsApprovedBrandAndExposesRestoreWithAnEmptyInventory() {
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
        compose.onNodeWithContentDescription("SafeSave").assertExists()
        compose.onNodeWithText("Save Centre").assertExists()
        compose.onNodeWithText("Back Up Everything").assertIsNotEnabled()
        compose.onNodeWithText("Restore Backup").performClick()
        compose.onNodeWithText("Unassigned Backups").performClick()
        assertEquals(1, restores)
        assertEquals(1, unassigned)
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
        compose.onNodeWithContentDescription("SafeSave").assertIsDisplayed()
        compose.onNodeWithText("Back Up Everything").assertIsDisplayed()
        compose.onNodeWithText("Restore Backup").assertIsDisplayed()
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
        val list = compose.onNode(hasScrollAction())
        list.performScrollToNode(hasText("Games 1 · States 3 · Cards 1"))
        compose.onNodeWithText("Games 1 · States 3 · Cards 1").performClick()
        list.performScrollToNode(hasText(title))
        compose.onNodeWithText("Open").performClick()
        assertEquals("fixture", opened)
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
