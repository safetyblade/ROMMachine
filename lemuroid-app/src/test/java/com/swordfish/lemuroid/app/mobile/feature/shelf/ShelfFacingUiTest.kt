package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.feature.main.CartridgeDetail
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.CartridgeLibraryTheme
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ShelfFacingUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val atlas =
        Game(
            id = 1,
            title = "Atlas",
            systemId = "gb",
            fileName = "atlas.gb",
            fileUri = "content://test/atlas",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )
    private val orbit = atlas.copy(id = 2, title = "Orbit", fileUri = "content://test/orbit")
    private val games = listOf(atlas, orbit)
    private val atlasId = "game:${shelfGameKey(atlas.systemId, atlas.fileUri)}"

    private fun library() =
        ShelfLayout().reconcile(
            games.associate {
                shelfGameKey(it.systemId, it.fileUri) to it.displayName()
            },
        )

    @Test fun arrangeFlipIsReversibleAndReducedMotionIsImmediate() {
        var layout by mutableStateOf(library().copy(appearance = ShelfAppearance(reducedMotion = true)))
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(
                    layout,
                    games,
                    {},
                    {},
                    Modifier.requiredSize(393.dp, 700.dp),
                    arrange = true,
                    onEdit = { layout = it(layout) },
                )
            }
        }
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").performClick()
        compose.onNodeWithText("Show front").performClick()
        compose.onNodeWithContentDescription("Atlas, GB, front facing").assertIsDisplayed()
        compose.onNodeWithContentDescription("Orbit, GB, spine facing").assertIsDisplayed()
        compose.runOnIdle { assertEquals(CartridgeFacing.FRONT, layout.sections.single().cells.first().facing) }
        compose.onNodeWithContentDescription("Atlas, GB, front facing").performClick()
        compose.onNodeWithText("Show spine").performClick()
        compose.onNodeWithContentDescription("Atlas, GB, spine facing").assertIsDisplayed()
    }

    @Test fun frontSelectionOpensDetailAndRequiresExplicitOpen() {
        var selected by mutableStateOf(false)
        var launches = 0
        val layout = library().setFacing(atlasId, CartridgeFacing.FRONT)
        compose.setContent {
            CartridgeLibraryTheme {
                if (selected) {
                    CartridgeDetail(atlas, { selected = false }, { launches++ }, { _, _ -> }, {})
                } else {
                    ShelfBoard(layout, games, { selected = true }, {})
                }
            }
        }
        compose.onNodeWithContentDescription("Atlas, GB, front facing").performClick()
        compose.onNodeWithText("Open").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, launches) }
        compose.onNodeWithText("Back to shelf").performClick()
        compose.onNodeWithContentDescription("Atlas, GB, front facing").assertIsDisplayed()
        compose.onNodeWithContentDescription("Atlas, GB, front facing").performClick()
        compose.onNodeWithText("Open").performClick()
        compose.runOnIdle { assertEquals(1, launches) }
    }

    @Test fun frontAndSpineShareBaselineWithoutOverlapping() {
        val layout = library().setFacing(atlasId, CartridgeFacing.FRONT)
        compose.setContent {
            CartridgeLibraryTheme { ShelfBoard(layout, games, {}, {}, Modifier.requiredSize(393.dp, 700.dp)) }
        }
        val front = compose.onNodeWithContentDescription("Atlas, GB, front facing").getUnclippedBoundsInRoot()
        val spine = compose.onNodeWithContentDescription("Orbit, GB, spine facing").getUnclippedBoundsInRoot()
        assertTrue(front.right <= spine.left)
        assertEquals(spine.bottom.value, front.bottom.value, 1f)
        assertEquals(spine.height.value, front.height.value, 1f)
        assertEquals(320f / 390f, front.width.value / front.height.value, 0.01f)
    }

    @Test fun frontRetainsLongPressOutsideArrange() {
        var held: Game? = null
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(
                    library().setFacing(atlasId, CartridgeFacing.FRONT),
                    games,
                    {},
                    { held = it },
                )
            }
        }
        compose.onNodeWithContentDescription("Atlas, GB, front facing").performTouchInput { longClick() }
        compose.runOnIdle { assertEquals(atlas, held) }
    }

    @Test fun accessibleFlipAndMoveKeepCellIdentity() {
        var layout by mutableStateOf(library().copy(appearance = ShelfAppearance(reducedMotion = true)))
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(
                    layout,
                    games,
                    {},
                    {},
                    arrange = true,
                    onEdit = { layout = it(layout) },
                )
            }
        }
        compose.onNodeWithContentDescription(
            "Atlas, GB, spine facing",
        ).performSemanticsAction(SemanticsActions.CustomActions) {
            assertTrue(it.first { action -> action.label == "Show front" }.action())
        }
        compose.onNodeWithContentDescription(
            "Atlas, GB, front facing",
        ).performSemanticsAction(SemanticsActions.CustomActions) {
            assertTrue(it.first { action -> action.label == "Move later" }.action())
        }
        compose.runOnIdle {
            assertEquals(
                listOf(orbit, atlas).map { "game:${shelfGameKey(it.systemId, it.fileUri)}" },
                layout.sections.single().cells.map { it.id },
            )
            assertEquals(CartridgeFacing.FRONT, layout.sections.single().cells.last().facing)
        }
    }
}
