package com.swordfish.lemuroid.app.feature.cartmod.maker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic fixtures only, mirroring the spike's two original templates and
 *  their documented sample outcomes in spikes/cartmod-maker/README.md. */
class CartmodMakerPreviewTest {

    /** Pocket Puzzle shape: README sample — won=true after R,R,D. */
    private fun puzzle() = MakerProject(
        schemaVersion = 1,
        title = "Pocket Puzzle (synthetic)",
        targets = listOf("gbc"),
        grid = MakerGrid(
            width = 3, height = 2,
            blocked = setOf(MakerPoint(1, 1)),
            goal = MakerPoint(2, 1),
        ),
        start = MakerPoint(0, 0),
    )

    /** Pocket Shop shape: README sample — coins=6, stock=0, won=true after I,I,I. */
    private fun shop() = MakerProject(
        schemaVersion = 1,
        title = "Pocket Shop (synthetic)",
        targets = listOf("gbc"),
        grid = MakerGrid(
            width = 1, height = 1,
            interactive = mapOf(MakerPoint(0, 0) to "counter"),
        ),
        start = MakerPoint(0, 0),
        variables = mapOf("coins" to 0, "stock" to 3),
        rules = listOf(
            MakerRule(
                trigger = MakerTrigger.ON_INTERACT,
                tileId = "counter",
                condition = MakerCondition("stock", MakerComparison.GT, 0),
                effects = listOf(
                    MakerEffect.AddVariable("coins", 2),
                    MakerEffect.AddVariable("stock", -1),
                ),
            ),
            MakerRule(
                trigger = MakerTrigger.ON_INTERACT,
                tileId = "counter",
                condition = MakerCondition("stock", MakerComparison.EQ, 0),
                effects = listOf(MakerEffect.Win),
            ),
        ),
    )

    @Test
    fun puzzleSampleWinsAfterRightRightDown() {
        val result = CartmodMakerPreview.run(
            puzzle(),
            listOf(MakerAction.RIGHT, MakerAction.RIGHT, MakerAction.DOWN),
        )
        assertTrue(result.won)
        assertEquals(MakerPoint(2, 1), result.position)
        assertEquals(3, result.stepsUsed)
    }

    @Test
    fun blockedTileStopsMovementButConsumesStep() {
        val result = CartmodMakerPreview.run(
            puzzle(),
            listOf(MakerAction.RIGHT, MakerAction.DOWN),
        )
        assertFalse(result.won)
        assertEquals(MakerPoint(1, 0), result.position)
        assertEquals(2, result.stepsUsed)
    }

    @Test
    fun shopSampleMatchesDocumentedOutcome() {
        val result = CartmodMakerPreview.run(
            shop(),
            listOf(MakerAction.INTERACT, MakerAction.INTERACT, MakerAction.INTERACT),
        )
        assertEquals(6, result.variables["coins"])
        assertEquals(0, result.variables["stock"])
        assertTrue(result.won)
    }

    @Test
    fun stepLimitHaltsFurtherActions() {
        val limited = puzzle().copy(stepLimit = 1)
        val result = CartmodMakerPreview.run(
            limited,
            listOf(MakerAction.RIGHT, MakerAction.RIGHT, MakerAction.DOWN),
        )
        assertEquals(1, result.stepsUsed)
        assertFalse(result.won)
    }

    @Test
    fun validatorRejectsMalformedAndOversizedProjects() {
        assertTrue(MakerProjectValidator.validate(puzzle()).isEmpty())
        assertTrue(MakerProjectValidator.validate(shop()).isEmpty())
        val bad = puzzle().copy(
            schemaVersion = 99,
            grid = MakerGrid(width = 0, height = 2, goal = MakerPoint(9, 9)),
            stepLimit = 0,
        )
        val errors = MakerProjectValidator.validate(bad)
        assertTrue(errors.any { it.contains("schemaVersion") })
        assertTrue(errors.any { it.contains("grid") })
        assertTrue(errors.any { it.contains("stepLimit") })
        assertTrue(errors.any { it.contains("goal") })
    }
}
