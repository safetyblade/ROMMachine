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
    @Test fun removeAllSpacesKeepsOrderSectionsAndAppearance() {
        var layout = ShelfLayout().reconcile(mapOf("a" to "Zelda", "b" to "Animal", "c" to "Mario"))
            .createSection("My Games")
        val section = layout.sections.last().id
        layout = layout.insertSpace(ShelfLayout.UNSORTED, "game:b")
            .insertSpace(ShelfLayout.UNSORTED, row = true)
            .moveCell("game:c", section).insertSpace(section)
        val expected = layout.sections.map { it.id to it.cells.mapNotNull { cell -> cell.gameKey } }
        val compact = layout.removeAllSpaces()
        assertEquals(expected, compact.sections.map { it.id to it.cells.mapNotNull { cell -> cell.gameKey } })
        assertTrue(compact.sections.all { it.cells.all { cell -> cell.gameKey != null } })
        assertEquals(layout.appearance, compact.appearance)
        assertEquals(compact, compact.removeAllSpaces())
        assertEquals(compact, decode(encode(compact)))
    }

    @Test fun autoArrangeSortsWithinSectionsDeterministically() {
        var layout = ShelfLayout().reconcile(
            linkedMapOf("z" to "Zelda", "c" to "Chrono", "a" to "Animal", "m" to "Mario"),
        ).createSection("Manual")
        val section = layout.sections.last().id
        layout = layout.moveCell("game:m", section).insertSpace(ShelfLayout.UNSORTED, "game:a")
        val title = layout.autoArrange(ShelfArrangeMode.TITLE_A_TO_Z)
        assertEquals(listOf("a", "c", "z"), title.sections.first().cells.map { it.gameKey })
        assertEquals(listOf("m"), title.sections.last().cells.map { it.gameKey })
        assertEquals(layout.sections.map { it.id }, title.sections.map { it.id })
        val systems = mapOf("z" to "snes", "c" to "gba", "a" to "ps2", "m" to "gba")
        val console = layout.autoArrange(ShelfArrangeMode.CONSOLE_THEN_TITLE, systems)
        assertEquals(listOf("c", "a", "z"), console.sections.first().cells.map { it.gameKey })
        assertEquals(console, console.autoArrange(ShelfArrangeMode.CONSOLE_THEN_TITLE, systems))
        assertEquals(setOf("a", "z", "c", "m"), console.sections.flatMap { it.cells }.mapNotNull { it.gameKey }.toSet())
    }

    @Test fun movingIntoVisualPaddingDoesNotPersistExtraSlots() {
        val layout = ShelfLayout().reconcile(mapOf("a" to "A", "b" to "B", "c" to "C"))
        val moved = layout.moveCell("game:a", ShelfLayout.UNSORTED)
        assertEquals(listOf("b", "c", "a"), moved.sections.first().cells.map { it.gameKey })
        assertEquals(0, moved.sections.first().cells.count { it.gameKey == null })
        assertEquals(3, moved.sections.first().rows(5).single().cells.size)
        assertEquals(3, decode(encode(moved)).sections.first().cells.size)
    }

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
        layout = layout.createSection("Fighting")
        layout = layout.moveCell("game:d", layout.sections.last().id)
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

    @Test fun incompleteOrFailedRefreshDoesNotDeleteMissingCartridgeCells() {
        val previous = ShelfLayout().reconcile(mapOf("a" to "A", "b" to "B"))
        val incompleteIndex = previous.reconcile(mapOf("a" to "A"))
        assertEquals(previous, incompleteIndex)
        val confirmed = previous.reconcile(mapOf("a" to "A"), removeMissing = true)
        assertEquals(listOf("a"), confirmed.sections.first().cells.mapNotNull { it.gameKey })
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

    @Test fun v1AppearanceLoadsWithoutLosingSectionsCellsOrCustomColours() {
        var old = ShelfLayout().reconcile(mapOf("a" to "A")).createSection("Racing")
        old = old.insertSpace(ShelfLayout.UNSORTED, row = true)
        val legacyAppearance =
            ShelfAppearance(
                edition = "pink",
                custom = mapOf("chassis" to 0xFF1299AAL),
                highContrast = true,
                reducedMotion = true,
            )
        old = old.copy(appearance = legacyAppearance)
        val json = org.json.JSONObject(encode(old))
        val a = json.getJSONObject("appearance")
        a.remove("appearanceVersion")
        a.remove("profiles")
        a.remove("activeProfileId")
        val restored = decode(json.toString())
        assertEquals(old, restored)
        assertEquals(1, restored.sections.first().cells.count { it.emptyRow })
        assertEquals(0xFF1299AAL, restored.appearance.custom["chassis"])
        assertEquals(true, restored.appearance.highContrast)
        assertEquals(true, restored.appearance.reducedMotion)
    }

    @Test fun threePresetsAndNamedProfilesRoundTripAtomicallyWithoutReorderingGames() {
        val base =
            ShelfLayout()
                .reconcile(mapOf("gba-a" to "Atlas", "gb-b" to "Archive"))
                .insertSpace(ShelfLayout.UNSORTED)
                .createSection("My favourites")
        val named =
            base.appearance
                .choosePreset("green")
                .copy(custom = mapOf("accent" to 0xFFD4F47DL))
                .saveNewProfile("Moss", now = 100L)
        val second =
            named.choosePreset("pink")
                .copy(custom = mapOf("button" to 0xFF793653L))
                .saveNewProfile("Berry", now = 200L)
        assertEquals(2, second.profiles.size)
        assertEquals("pink", second.edition)
        assertEquals("Moss", second.selectProfile(named.profiles.single().id).profiles.first().name)
        val persisted = decode(encode(base.copy(appearance = second)))
        assertEquals(base.sections, persisted.sections)
        assertEquals(second, persisted.appearance)
        val first = persisted.appearance.selectProfile(named.profiles.first().id)
        assertEquals("green", first.edition)
        assertEquals(0xFFD4F47DL, first.custom["accent"])
        val removed = first.removeProfile(named.profiles.first().id)
        assertEquals(null, removed.activeProfileId)
        assertEquals(1, removed.profiles.size)
        assertEquals("green", removed.edition)
        assertEquals(emptyMap<String, Long>(), removed.choosePreset("original").custom)
    }

    @Test fun unsupportedAppearanceSchemaNeverSilentlyOverwritesUserData() {
        val json = org.json.JSONObject(encode(ShelfLayout()))
        json.getJSONObject("appearance").put("appearanceVersion", 999)
        assertThrows(IllegalArgumentException::class.java) { decode(json.toString()) }
    }

    @Test fun canonicalAndCustomFinishesHaveReadableTextAndFocus() {
        for (edition in listOf("original", "pink", "green")) {
            val colors = shelfMaterials(ShelfAppearance(edition = edition))
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
