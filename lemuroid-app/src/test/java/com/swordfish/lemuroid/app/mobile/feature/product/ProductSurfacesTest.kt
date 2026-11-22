package com.swordfish.lemuroid.app.mobile.feature.product

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ProductSurfacesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun saveCentreShowsApprovedBrandAndExposesRestoreWithAnEmptyInventory() {
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
        compose.onNodeWithContentDescription("SafeSave").assertExists()
        compose.onNodeWithText("Save Centre").assertExists()
        compose.onNodeWithText("Back Up Everything").assertIsNotEnabled()
        compose.onNodeWithText("Restore Backup").performClick()
        assertEquals(1, restores)
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
