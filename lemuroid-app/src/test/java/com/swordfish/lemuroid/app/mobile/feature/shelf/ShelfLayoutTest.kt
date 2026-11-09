package com.swordfish.lemuroid.app.mobile.feature.shelf

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ShelfLayoutTest {
    @Test fun missingAndReindexedGamesKeepIdentityAndEmptyRows() {
        val a = shelfGameKey("gb", "content://rom/a")
        val b = shelfGameKey("gba", "content://rom/b")
        var layout = ShelfLayout().reconcile(mapOf(a to "A", b to "B"))
        layout = layout.insertSpace(ShelfLayout.UNSORTED, "game:$b").insertSpace(ShelfLayout.UNSORTED, row = true)
        val cells = layout.sections.single().cells
        assertEquals(cells, layout.reconcile(emptyMap()).sections.single().cells)
        val restored = layout.reconcile(mapOf(a to "Custom A", b to "B"))
        assertEquals(cells.map { it.id }, restored.sections.single().cells.map { it.id })
        assertEquals("Custom A", restored.sections.single().cells.first().titleHint)
        for (columns in listOf(1, 3, 5, 12)) assertEquals(
            1,
            restored.sections.single().rows(columns).count {
                it.intentionalEmpty
            },
        )
        assertNotEquals(a, shelfGameKey("gba", "content://rom/a"))
    }

    @Test fun completeRefreshRemovesGhostCartridgesAndPreservesExplicitSpaces() {
        val games = mapOf("a" to "A", "b" to "B", "c" to "C", "d" to "D")
        var layout = ShelfLayout().reconcile(games)
        layout = layout.insertSpace(ShelfLayout.UNSORTED, "game:b")
        layout = layout.insertSpace(ShelfLayout.UNSORTED, row = true)
        layout = layout.createSection("Fighting").moveCell("game:d", layoutSectionId(layout))
        val sectionId = layout.sections.last().id
        val reserved = layout.sections.first().cells.filter { it.gameKey == null }
        val pruned = layout.reconcile(mapOf("a" to "A", "c" to "C", "d" to "D"), removeMissing = true)
        assertEquals(listOf("a", null, "c", null), pruned.sections.first().cells.map { it.gameKey })
        assertEquals(reserved.map { it.id }, pruned.sections.first().cells.filter { it.gameKey == null }.map { it.id })
        assertEquals(listOf("d"), pruned.sections.last().cells.mapNotNull { it.gameKey })
        assertEquals(sectionId, pruned.sections.last().id)
        assertEquals(1, pruned.sections.first().rows(5).count { it.intentionalEmpty })
        assertEquals(pruned, pruned.reconcile(mapOf("a" to "A", "c" to "C", "d" to "D"), removeMissing = true))
        val readded = pruned.reconcile(games)
        assertEquals("b", readded.sections.first().cells.last().gameKey)
    }

    private fun layoutSectionId(layout: ShelfLayout): String = layout.sections.last().id

    @Test fun incompleteOrFailedRefreshDoesNotDeleteMissingCartridgeCells() {
        val previous = ShelfLayout().reconcile(mapOf("a" to "A", "b" to "B"))
        val incompleteIndex = previous.reconcile(mapOf("a" to "A"))
        assertEquals(previous, incompleteIndex)
        assertEquals(listOf("a"), previous.reconcile(mapOf("a" to "A"), removeMissing = true)
            .sections.first().cells.mapNotNull { it.gameKey })
    }

    @Test fun movingAcrossSectionsSwapsVacancyAndDeleteRehomesEverything() {
        var layout = ShelfLayout().reconcile(mapOf("a" to "A", "b" to "B")).createSection("Favourites")
        val target = layout.sections.last().id
        layout = layout.insertSpace(target)
        val gap = layout.sections.last().cells.single()
        layout = layout.moveCell("game:a", target, gap.id)
        assertEquals(gap, layout.sections.first().cells.first())
        assertEquals("a", layout.sections.last().cells.single().gameKey)
        layout = layout.insertSpace(target, row = true).deleteSection(target)
        assertEquals(setOf("a", "b"), layout.sections.single().cells.mapNotNull { it.gameKey }.toSet())
        assertEquals(2, layout.sections.single().cells.count { it.gameKey == null })
        assertThrows(IllegalArgumentException::class.java) { layout.deleteSection(ShelfLayout.UNSORTED) }
    }

    @Test fun atomicStoreSerializesConcurrentWritersAndSurvivesReload() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val path = File(context.filesDir, "cartdeck/shelf-layout-v1.json")
            path.parentFile?.deleteRecursively()
            val stores = List(12) { ShelfStore(context) }
            stores.mapIndexed {
                    index,
                    store,
                ->
                async { store.update { it.createSection("Section $index") } }
            }.awaitAll()
            val restored = ShelfStore(context).read()
            assertEquals(13, restored.sections.size)
            assertEquals(12L, restored.revision)
            assertEquals(restored, decode(encode(restored)))
            val invalid = "{\"version\":999}"
            path.writeText(invalid)
            assertThrows(
                IllegalArgumentException::class.java,
            ) { runBlocking { stores.first().update { ShelfLayout() } } }
            assertEquals(invalid, path.readText())
        }

    @Test fun canonicalAndCustomFinishesHaveReadableTextAndFocus() {
        for (pink in listOf(false, true)) {
            val colors = shelfMaterials(ShelfAppearance(edition = if (pink) "pink" else "original"))
            assertTrue(colorContrast(colors.sectionText, colors.shelfBackboard) >= 4.5f)
            assertTrue(colorContrast(colors.focusOutline, colors.shelfBackboard) >= 3f)
            assertTrue(colorContrast(colors.focusOutline, colors.vacantCell) >= 3f)
        }
        val custom =
            shelfMaterials(
                ShelfAppearance(
                    custom =
                        mapOf(
                            "shelfBackboard" to 0xFFFFFFFF,
                            "vacantCell" to 0xFFEEEEEE,
                            "sectionText" to 0xFFFFFFFF,
                            "focusOutline" to 0xFFFFFFFF,
                        ),
                ),
            )
        assertTrue(colorContrast(custom.sectionText, custom.shelfBackboard) >= 4.5f)
        assertTrue(colorContrast(custom.focusOutline, custom.vacantCell) >= 3f)
    }
}
