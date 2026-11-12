package com.swordfish.lemuroid.app.mobile.feature.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GripArcPositionSolverTest {
    private val source =
        GripArcManifest.parse(
            File("src/main/assets/griparc/griparc-v1-manifest.json").readText(),
        )
    private val safe = PivotArcBounds(8f, 8f, 385f, 844f)
    private val video = PivotArcBounds(22f, 84f, 371f, 344f)

    private fun placement(
        side: GripArcSide = GripArcSide.RIGHT,
        blockers: Collection<PivotArcBounds> = emptyList(),
    ) = (GripArcPositionSolver.resolve(source, safe, video, blockers, side) as GripArcFit.Fits)
        .placement

    @Test fun approvedManifestUsesPermanentLAndExactlyFourStableSlots() {
        assertEquals("cartdeck.griparc.assets/1.0.0", source.schema)
        assertEquals(272f, source.artboard.width, 0.001f)
        assertEquals(308f, source.artboard.height, 0.001f)
        assertEquals(listOf("G1", "G2", "G3", "G4"), source.slots.map { it.id })
        val right = placement()
        assertEquals(4, right.targets.size)
        assertEquals(4, right.visuals.size)
        for (count in 1..4) {
            assertEquals((1..count).toList(), right.targets.keys.take(count).toList())
            assertEquals(right.housing, placement().housing)
        }
    }

    @Test fun everySlotRemainsAtLeast48dpAndDoesNotOverlap() {
        for (side in GripArcSide.values()) {
            val p = placement(side)
            val rects = p.targets.values.toList()
            rects.forEach { box ->
                assertTrue(box.width >= 48f)
                assertTrue(box.height >= 48f)
                assertTrue(box.inside(safe))
                assertFalse(box.intersects(video))
            }
            for (i in rects.indices) {
                for (j in i + 1 until rects.size) {
                    assertFalse(rects[i].intersects(rects[j]))
                }
            }
        }
    }

    @Test fun mirroringNeverSwapsSlotIdentitiesOrMovesTheirYCoordinates() {
        val right = placement()
        val left = placement(GripArcSide.LEFT)
        assertEquals(right.targets.keys, left.targets.keys)
        assertEquals(right.housing.height, left.housing.height, 0.001f)
        assertEquals(right.housing.width, left.housing.width, 0.001f)
        assertEquals(right.scale, left.scale, 0.001f)
        for (i in 1..4) {
            val r = right.targets.getValue(i)
            val l = left.targets.getValue(i)
            assertEquals(r.centerY, l.centerY, 0.001f)
            assertEquals(safe.left + safe.right - r.centerX, l.centerX, 0.001f)
        }
    }

    @Test fun occupiedZonesForceSmallerButUnchangedHousing() {
        val blocker = PivotArcBounds(80f, 590f, 285f, 760f)
        val fit = placement(blockers = listOf(blocker))
        val arms = GripArcPositionSolver.housingFootprint(fit.housing, fit.scale, fit.side)
        assertTrue(arms.none { it.intersects(blocker) })
        assertTrue(fit.scale <= 1f)
    }

    @Test fun impossibleFitNeverCropsHousingOrButtons() {
        val unavailable =
            GripArcPositionSolver.resolve(
                source,
                PivotArcBounds(8f, 8f, 180f, 360f),
                video,
                emptyList(),
                GripArcSide.RIGHT,
            )
        assertTrue(unavailable is GripArcFit.Unavailable)
        assertTrue((unavailable as GripArcFit.Unavailable).reason.isNotBlank())
    }

    @Test fun insideCornerOfLIsFreeButTheEntirePhysicalLStillCollides() {
        val innerControls = listOf(PivotArcBounds(140f, 558f, 255f, 700f))
        val right = placement(blockers = innerControls)
        val arms = GripArcPositionSolver.housingFootprint(right.housing, right.scale, right.side)
        assertTrue(right.housing.intersects(innerControls.first()))
        assertTrue(arms.none { it.intersects(innerControls.first()) })
        val blocked =
            GripArcPositionSolver.resolve(
                source,
                safe,
                video,
                listOf(PivotArcBounds(8f, 344f, 385f, 844f)),
                GripArcSide.RIGHT,
            )
        assertTrue(blocked is GripArcFit.Unavailable)
    }

    @Test fun realisticTwoHandControlEnvelopesFitTheOpenInterior() {
        val rightPadReflowed =
            listOf(
                PivotArcBounds(12f, 555f, 155f, 700f),
                PivotArcBounds(145f, 460f, 260f, 605f),
            )
        val right =
            GripArcPositionSolver.resolve(
                source,
                safe,
                video,
                rightPadReflowed,
                GripArcSide.RIGHT,
            )
        assertTrue(right is GripArcFit.Fits)
        val leftPadReflowed =
            listOf(
                PivotArcBounds(80f, 395f, 205f, 550f),
                PivotArcBounds(240f, 580f, 380f, 740f),
            )
        val left =
            GripArcPositionSolver.resolve(
                source,
                safe,
                video,
                leftPadReflowed,
                GripArcSide.LEFT,
            )
        assertTrue(left is GripArcFit.Fits)
    }

    @Test fun standardReflowOnlyMovesDockSideAndHonoursCompactDeck() {
        val right = GripArcStandardReflowPolicy.resolve(393f, 490f, GripArcSide.RIGHT)
        assertEquals(0f, right.leftX, 0.01f)
        assertTrue(right.rightX < -25f)
        assertTrue(right.rightY < -100f)
        val left = GripArcStandardReflowPolicy.resolve(393f, 490f, GripArcSide.LEFT)
        assertTrue(left.leftX > 25f)
        assertTrue(left.leftY < -100f)
        assertEquals(0f, left.rightX, 0.01f)
        // Realistic handset geometry: moving either composite dial into the
        // OTHER hand's original controls is not a viable dock layout.
        val dpad = PivotArcBounds(22f, 554f, 178f, 706f)
        val faces = PivotArcBounds(217f, 514f, 379f, 720f)
        val movedFaces =
            PivotArcBounds(
                faces.left + right.rightX,
                faces.top + right.rightY,
                faces.right + right.rightX,
                faces.bottom + right.rightY,
            )
        val movedDpad =
            PivotArcBounds(
                dpad.left + left.leftX,
                dpad.top + left.leftY,
                dpad.right + left.leftX,
                dpad.bottom + left.leftY,
            )
        assertFalse(movedFaces.intersects(dpad.inset(4f)))
        assertFalse(movedDpad.intersects(faces.inset(4f)))
        val small = GripArcStandardReflowPolicy.resolve(320f, 260f, GripArcSide.RIGHT)
        assertTrue(small.rightY > -10f)
    }

    @Test fun userOffsetsNeverMoveOneSlotWithoutTheOthers() {
        val original = placement()
        val adjusted =
            GripArcPositionSolver.resolve(
                source,
                safe,
                video,
                emptyList(),
                GripArcSide.RIGHT,
                dx = -24f,
                dy = 12f,
            ) as GripArcFit.Fits
        assertEquals(original.targets.keys, adjusted.placement.targets.keys)
        assertEquals(
            original.targets.getValue(1).centerX - original.targets.getValue(4).centerX,
            adjusted.placement.targets.getValue(1).centerX -
                adjusted.placement.targets.getValue(4).centerX,
            0.001f,
        )
    }
}
