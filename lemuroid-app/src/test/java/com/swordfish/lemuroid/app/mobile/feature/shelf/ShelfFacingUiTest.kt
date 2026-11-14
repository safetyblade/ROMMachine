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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
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
@Config(sdk = [28], application = Application::class, qualifiers = "w393dp-h1200dp-mdpi")
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
        assertEquals((spine.bottom - spine.top).value, (front.bottom - front.top).value, 1f)
        assertEquals(
            320f / 390f,
            (front.right - front.left).value / (front.bottom - front.top).value,
            0.01f,
        )
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
        val flipActions =
            compose.onNodeWithContentDescription("Atlas, GB, spine facing")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(flipActions.first { it.label == "Show front" }.action()) }
        val moveActions =
            compose.onNodeWithContentDescription("Atlas, GB, front facing")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(moveActions.first { it.label == "Move later" }.action()) }
        compose.runOnIdle {
            assertEquals(
                listOf(orbit, atlas).map { "game:${shelfGameKey(it.systemId, it.fileUri)}" },
                layout.sections.single().cells.map { it.id },
            )
            assertEquals(CartridgeFacing.FRONT, layout.sections.single().cells.last().facing)
        }
    }

    @Test fun dragUnderstandsBothFacesAndPreservesSelectionAndInventory() {
        var layout by mutableStateOf(library())
        var selections = 0
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(
                    layout,
                    games,
                    { selections++ },
                    {},
                    Modifier.requiredSize(393.dp, 800.dp),
                    arrange = true,
                    onEdit = { layout = it(layout) },
                )
            }
        }
        for (source in CartridgeFacing.entries) {
            for (destination in CartridgeFacing.entries) {
                compose.runOnIdle {
                    layout =
                        library().setFacing(atlasId, source)
                            .setFacing("game:${shelfGameKey(orbit.systemId, orbit.fileUri)}", destination)
                }
                val from = compose.onNodeWithContentDescription("Atlas, GB, ${source.name.lowercase()} facing")
                val to = compose.onNodeWithContentDescription("Orbit, GB, ${destination.name.lowercase()} facing")
                val destinationBounds = to.fetchSemanticsNode().boundsInRoot
                val sourceBounds = from.fetchSemanticsNode().boundsInRoot
                val delta = destinationBounds.center.copy(x = destinationBounds.right - 4f) - sourceBounds.center
                from.performTouchInput { swipe(center, center + delta, 500) }
                compose.runOnIdle {
                    assertEquals(
                        listOf(orbit.fileUri, atlas.fileUri).map { shelfGameKey("gb", it) },
                        layout.sections.single().cells.map { it.gameKey },
                    )
                    assertEquals(source, layout.sections.single().cells.last().facing)
                    assertEquals(0, selections)
                }
            }
        }
    }

    @Test fun frontPickupStartsWithFrontAndKeepsItsAspectRatio() {
        val source = androidx.compose.ui.geometry.Rect(0f, 0f, 160f, 195f)
        val target = androidx.compose.ui.geometry.Rect(10f, 10f, 330f, 400f)
        for (step in 0..20) {
            val flight =
                com.swordfish.lemuroid.app.mobile.shared.compose.ui.cartridgeFlight(
                    source,
                    target,
                    step / 20f,
                    sourceFront = true,
                )
            assertTrue(flight.showFront)
            assertEquals(0f, flight.rotationY)
            assertEquals(320f / 390f, flight.bounds.width / flight.bounds.height, 0.001f)
        }
    }

    @Test fun frontDragPreservesSectionsVacanciesAndIntentionalRowBoundaries() {
        var layout by mutableStateOf(library())
        var selections = 0
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(layout, games, { selections++ }, {}, arrange = true, onEdit = { layout = it(layout) })
            }
        }
        val orbitId = "game:${shelfGameKey(orbit.systemId, orbit.fileUri)}"
        for (scenario in listOf("section", "vacancy", "before-row", "after-row")) {
            compose.runOnIdle {
                val base = library().setFacing(atlasId, CartridgeFacing.FRONT)
                layout =
                    when (scenario) {
                        "section" ->
                            base.createSection("Destination").let {
                                it.moveCell(orbitId, it.sections.last().id)
                            }
                        "vacancy" -> base.insertSpace(ShelfLayout.UNSORTED, orbitId)
                        "before-row" -> base.insertSpace(ShelfLayout.UNSORTED, atlasId, row = true)
                        else -> base.insertSpace(ShelfLayout.UNSORTED, orbitId, row = true)
                    }
            }
            val before = layout
            val from = compose.onNodeWithContentDescription("Atlas, GB, front facing")
            val targetBounds =
                if (scenario == "vacancy" || scenario == "before-row") {
                    compose.onAllNodesWithContentDescription(
                        "Reserved empty shelf space",
                    )[0].fetchSemanticsNode().boundsInRoot
                } else {
                    compose.onNodeWithContentDescription("Orbit, GB, spine facing").fetchSemanticsNode().boundsInRoot
                }
            val target =
                if (scenario == "section" || scenario == "after-row") {
                    targetBounds.center.copy(x = targetBounds.right - 4f)
                } else {
                    targetBounds.center
                }
            val delta = target - from.fetchSemanticsNode().boundsInRoot.center
            from.performTouchInput { swipe(center, center + delta, 500) }
            compose.runOnIdle {
                val cells = layout.sections.flatMap { it.cells }
                assertEquals(before.sections.flatMap { it.cells }.map { it.id }.toSet(), cells.map { it.id }.toSet())
                assertEquals(2, cells.count { it.gameKey != null })
                assertEquals(CartridgeFacing.FRONT, cells.single { it.id == atlasId }.facing)
                when (scenario) {
                    "section" -> assertTrue(layout.sections.last().cells.any { it.id == atlasId })
                    "vacancy" -> assertEquals(null, cells.first().gameKey)
                    "before-row" ->
                        assertTrue(
                            cells.indexOfFirst { it.id == atlasId } < cells.indexOfFirst { it.emptyRow },
                        )
                    else -> assertTrue(cells.indexOfFirst { it.id == atlasId } > cells.indexOfFirst { it.emptyRow })
                }
                assertEquals(0, selections)
            }
        }
    }

    @Test fun animatedTurnCommitsAtMidpointAndRestoresFocusAfterWrapping() {
        // Four preceding spines fill most of the first row. Atlas must wrap
        // to the next row when its footprint expands to three slots.
        val preceding = (3..6).map { atlas.copy(id = it, title = "Earlier $it", fileUri = "content://test/$it") }
        val records = preceding + atlas
        var layout by mutableStateOf(
            ShelfLayout().reconcile(
                records.associate {
                    shelfGameKey(it.systemId, it.fileUri) to it.displayName()
                },
            ),
        )
        compose.setContent {
            CartridgeLibraryTheme {
                ShelfBoard(layout, records, {}, {}, arrange = true, onEdit = {
                    layout = it(layout).copy(revision = layout.revision + 1)
                })
            }
        }
        val initial = compose.onNodeWithContentDescription("Atlas, GB, spine facing").getUnclippedBoundsInRoot()
        val neighbor = compose.onNodeWithContentDescription("Earlier 6, GB, spine facing").getUnclippedBoundsInRoot()
        assertEquals(neighbor.bottom.value, initial.bottom.value, 1f)
        // Use the same accessible action as the arrangement menu. Wrapped
        // cartridges need not expose a stable popup during frame-clock testing.
        val flip =
            compose.onNodeWithContentDescription("Atlas, GB, spine facing")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .first { it.label == "Show front" }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { assertTrue(flip.action()) }
        compose.mainClock.advanceTimeBy(60)
        compose.runOnIdle {
            assertEquals(CartridgeFacing.SPINE, layout.sections.single().cells.last().facing)
        }
        compose.mainClock.advanceTimeBy(400)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithContentDescription("Atlas, GB, front facing").assertIsDisplayed().assertIsFocused()
        compose.runOnIdle {
            assertEquals(
                records.map { shelfGameKey(it.systemId, it.fileUri) },
                layout.sections.single().cells.map { it.gameKey },
            )
        }
    }
}
