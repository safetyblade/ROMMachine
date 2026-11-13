package com.swordfish.lemuroid.app.mobile.feature.main

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartdeckChromeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun arrangeDockAppearsOnlyOnHome() {
        assertTrue(showShelfEditDock(MainRoute.HOME, true))
        assertFalse(showShelfEditDock(MainRoute.HOME, false))
        assertFalse(showShelfEditDock(MainRoute.SEARCH, true))
        assertFalse(showShelfEditDock(MainRoute.FAVORITES, true))
        assertFalse(showShelfEditDock(MainRoute.SETTINGS, true))
    }

    @Test fun homeHasOneBrandTitleAndHiddenShelfOptions() {
        var arrangeRequested = 0
        var appearanceRequested = 0
        compose.setContent {
            CartridgeLibraryTheme {
                MainTopBar(
                    currentRoute = MainRoute.HOME,
                    navController = rememberNavController(),
                    onHelpPressed = {},
                    onUpdateQueryString = {},
                    mainUIState = MainViewModel.UiState(),
                    shelfAvailable = true,
                    onToggleArrangeShelf = { arrangeRequested++ },
                    onShelfAppearance = { appearanceRequested++ },
                )
            }
        }
        compose.onNodeWithText("CARTDECK").assertExists()
        compose.onNodeWithText("LIBRARY").assertExists()
        compose.onNodeWithText("Home").assertDoesNotExist()
        compose.onNodeWithText("Arrange shelf").assertDoesNotExist()
        compose.onNodeWithContentDescription("Library options").performClick()
        compose.onNodeWithText("Arrange shelf").performClick()
        assertTrue(arrangeRequested == 1)
        compose.onNodeWithContentDescription("Library options").performClick()
        compose.onNodeWithText("Shelf appearance").performClick()
        assertTrue(appearanceRequested == 1)
    }

    @Test fun contextualDockExitsToExistingNavigationWithoutDiscardingChanges() {
        var arranging by mutableStateOf(true)
        var editToolsCalls = 0
        var undoCalls = 0
        compose.setContent {
            CartridgeLibraryTheme {
                MainNavigationBar(
                    currentRoute = MainRoute.HOME,
                    navController = rememberNavController(),
                    arrangingShelf = arranging,
                    canUndoShelf = false,
                    onDoneArrange = { arranging = false },
                    onEditTools = { editToolsCalls++ },
                    onUndoShelf = { undoCalls++ },
                )
            }
        }
        compose.onNodeWithText("Tools").performClick()
        assertTrue(editToolsCalls == 1)
        compose.onNodeWithText("Undo").assertIsNotEnabled()
        assertTrue(undoCalls == 0)
        compose.onNodeWithText("Home").assertDoesNotExist()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Home").assertExists()
        compose.onNodeWithText("Favorites").assertExists()
        compose.onNodeWithText("Systems").assertExists()
        compose.onNodeWithText("Search").assertExists()
    }
}
