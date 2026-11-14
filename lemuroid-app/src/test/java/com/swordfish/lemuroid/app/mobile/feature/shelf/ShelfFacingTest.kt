package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ShelfFacingTest {
    private fun library() = ShelfLayout().reconcile(linkedMapOf("a" to "Same", "b" to "Same", "c" to "C"))

    @Test fun facingIsIndependentAndReversible() {
        val original = library()
        assertTrue(original.sections.single().cells.all { it.facing == CartridgeFacing.SPINE })
        val flipped = original.setFacing("game:a", CartridgeFacing.FRONT)
        assertEquals(
            listOf(CartridgeFacing.FRONT, CartridgeFacing.SPINE, CartridgeFacing.SPINE),
            flipped.sections.single().cells.map { it.facing },
        )
        assertEquals(original, flipped.setFacing("game:a", CartridgeFacing.SPINE))
        assertNotEquals(
            shelfGameKey("gb", "content://folder1/same.gb"),
            shelfGameKey("gb", "content://folder2/same.gb"),
        )
    }

    @Test fun presentationSurvivesAllShelfOperationsAndAppearanceChanges() {
        val flipped = library().setFacing("game:b", CartridgeFacing.FRONT)
        val reconciled = flipped.reconcile(mapOf("a" to "New", "b" to "Same", "c" to "C", "d" to "D"))
        val sectioned = reconciled.createSection("Fronts")
        val moved = sectioned.moveCell("game:b", sectioned.sections.last().id)
        val renamed = moved.renameSection(moved.sections.last().id, "Renamed")
        val layouts =
            listOf(
                reconciled,
                moved,
                renamed,
                renamed.deleteSection(renamed.sections.last().id),
                flipped.autoArrange(ShelfArrangeMode.TITLE_A_TO_Z),
                flipped.autoArrange(ShelfArrangeMode.CONSOLE_THEN_TITLE),
                flipped.insertSpace(ShelfLayout.UNSORTED).removeAllSpaces(),
                flipped.copy(appearance = flipped.appearance.choosePreset("pink")),
            )
        layouts.forEach { layout ->
            val cells = layout.sections.flatMap { it.cells }
            assertEquals(CartridgeFacing.FRONT, cells.single { it.id == "game:b" }.facing)
            assertEquals(cells.mapNotNull { it.gameKey }.size, cells.mapNotNull { it.gameKey }.distinct().size)
            assertTrue(cells.mapNotNull { it.gameKey }.containsAll(listOf("a", "b", "c")))
            assertEquals(layout, decode(encode(layout)))
        }
    }

    @Test fun legacyAndUnexpectedFacingPreserveTheShelf() {
        val original =
            library().insertSpace(
                ShelfLayout.UNSORTED,
                "game:b",
            ).insertSpace(ShelfLayout.UNSORTED, row = true)
        val json = JSONObject(encode(original))
        val cells = json.getJSONArray("sections").getJSONObject(0).getJSONArray("cells")
        for (i in 0 until cells.length()) cells.getJSONObject(i).remove("facing")
        assertEquals(original, decode(json.toString()))
        cells.getJSONObject(0).put("facing", "FUTURE")
        assertEquals(original, decode(json.toString()))
        cells.getJSONObject(0).put("facing", JSONObject.NULL)
        assertEquals(original, decode(json.toString()))
    }

    @Test fun atomicRestartAndUndoRestoreFacing() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            File(context.filesDir, "cartdeck/shelf-layout-v1.json").parentFile!!.deleteRecursively()
            val store = ShelfStore(context)
            store.update { library() }
            val (before, after) = store.update { it.setFacing("game:a", CartridgeFacing.FRONT) }
            assertEquals(after, ShelfStore(context).read())
            store.update { before }
            assertEquals(before.sections, ShelfStore(context).read().sections)
            assertEquals(CartridgeFacing.SPINE, ShelfStore(context).read().sections.single().cells.first().facing)
        }

    @Test fun packingPreservesOrderGapsBoundariesAndNeverOverlaps() {
        val widths = listOf(1, 2, 3, 4, 5, 8, 12)
        for (fronts in listOf(emptySet(), (0..11).toSet(), setOf(1, 3, 5, 7, 9, 11), setOf(4))) {
            val cells =
                (0..11).map { index ->
                    ShelfCell(
                        id = "cell:$index",
                        gameKey = "key:$index",
                        facing =
                            if (index in fronts) CartridgeFacing.FRONT else CartridgeFacing.SPINE,
                    )
                }.toMutableList()
            cells.add(3, ShelfCell(id = "gap"))
            cells.add(7, ShelfCell(id = "row", emptyRow = true))
            val section = ShelfSection("test", "Test", cells)
            widths.forEach { columns ->
                val rows = section.rows(columns)
                assertEquals(cells, rows.flatMap { it.cells })
                assertEquals(cells, section.cells)
                assertEquals(1, rows.count { it.intentionalEmpty })
                rows.filterNot { it.intentionalEmpty }.forEach { row ->
                    var end = 0
                    row.cells.forEach { cell ->
                        val start = end
                        end += cell.span(columns)
                        assertTrue(end > start)
                        assertTrue(end <= columns)
                    }
                }
                assertEquals(1, rows.flatMap { it.cells }.count { it.id == "gap" })
                assertEquals(rows, section.rows(columns))
            }
        }
    }

    @Test fun frontWrapDoesNotFillReservedSpaceOrCrossEmptyRow() {
        val a = ShelfCell("a", "a")
        val b = ShelfCell("b", "b", facing = CartridgeFacing.FRONT)
        val gap = ShelfCell("gap")
        val boundary = ShelfCell("boundary", emptyRow = true)
        val section =
            ShelfSection(
                "s",
                "S",
                listOf(a, gap, a.copy(id = "c", gameKey = "c"), b, boundary, b.copy(id = "d", gameKey = "d")),
            )
        val rows = section.rows(5)
        assertEquals(
            listOf(listOf("a", "gap", "c"), listOf("b"), listOf("boundary"), listOf("d")),
            rows.map {
                it.cells.map { it.id }
            },
        )
        assertEquals(listOf(a, gap, section.cells[2], b), section.rows(8).first().cells)
    }
}
