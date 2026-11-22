package com.swordfish.lemuroid.app.shared.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DisplaySettingsStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun defaultsOverridesAndResetSurviveReopenWithoutTouchingOtherCartridges() {
        val root = temporary.newFolder()
        val store = DisplaySettingsStore(root) { DisplaySettingsStore.Choices(filter = "auto", hd = true) }
        assertEquals(DisplaySettingsStore.Border.OFF, store.read("one").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.STANDARD, store.read("one").effective.border(false))
        store.set(null, "landscapeBorder", "STANDARD")
        store.set("one", "landscapeBorder", "OFF")
        store.set("one", "filter", "crt")
        val reopened = DisplaySettingsStore(root)
        assertEquals(DisplaySettingsStore.Border.OFF, reopened.read("one").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.STANDARD, reopened.read("two").effective.border(true))
        assertEquals("crt", reopened.read("one").effective.filter)
        reopened.useGlobal("one")
        assertEquals(DisplaySettingsStore.Border.STANDARD, store.read("one").effective.border(true))
        assertEquals("auto", store.read("one").effective.filter)
        assertTrue(store.read("one").effective.hd == true)
    }

    @Test fun landscapeDefaultsOffAndExistingSelectionsRemainExplicit() {
        val root = temporary.newFolder()
        val store = DisplaySettingsStore(root)
        assertEquals(DisplaySettingsStore.Border.OFF, store.read(null).effective.border(true))
        assertEquals(DisplaySettingsStore.Border.STANDARD, store.read(null).effective.border(false))
        assertEquals(DisplaySettingsStore.Border.OFF, store.read("one").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.STANDARD, store.read("one").effective.border(false))
        // A previously selected global or cartridge-specific OFF must not be rewritten.
        store.set(null, "landscapeBorder", "OFF")
        store.set("one", "portraitBorder", "OFF")
        val reopened = DisplaySettingsStore(root)
        assertEquals(DisplaySettingsStore.Border.OFF, reopened.read("two").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.STANDARD, reopened.read("two").effective.border(false))
        assertEquals(DisplaySettingsStore.Border.OFF, reopened.read("one").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.OFF, reopened.read("one").effective.border(false))
        reopened.set("one", "landscapeBorder", "STANDARD")
        assertEquals(DisplaySettingsStore.Border.STANDARD, reopened.read("one").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.OFF, reopened.read("two").effective.border(true))
    }

    @Test fun globalMinimalAppliesUntilCartridgeExplicitOffAndResetReinheritsGlobal() {
        val store = DisplaySettingsStore(temporary.newFolder())
        store.set(null, "landscapeBorder", "MINIMAL")
        assertEquals(DisplaySettingsStore.Border.MINIMAL, store.read("xbox").effective.border(true))
        store.set("xbox", "landscapeBorder", "OFF")
        assertEquals(DisplaySettingsStore.Border.OFF, store.read("xbox").effective.border(true))
        store.useGlobal("xbox")
        assertEquals(DisplaySettingsStore.Border.MINIMAL, store.read("xbox").effective.border(true))
    }

    @Test fun minimalIsIndependentByOrientationAndPreservesExistingOverrides() {
        val root = temporary.newFolder()
        val store = DisplaySettingsStore(root)
        store.set(null, "landscapeBorder", "STANDARD")
        store.set("one", "portraitBorder", "OFF")
        store.set("one", "landscapeBorder", "MINIMAL")
        val reopened = DisplaySettingsStore(root)
        assertEquals(DisplaySettingsStore.Border.MINIMAL, reopened.read("one").effective.border(true))
        assertEquals(DisplaySettingsStore.Border.OFF, reopened.read("one").effective.border(false))
        assertEquals(DisplaySettingsStore.Border.STANDARD, reopened.read("two").effective.border(true))
        reopened.useGlobal("one")
        assertEquals(DisplaySettingsStore.Border.STANDARD, reopened.read("one").effective.border(true))
    }

    @Test fun reportedEngineOptionsRespectNamespaceOverrideAndGlobalReset() {
        val store = DisplaySettingsStore(temporary.newFolder())
        val values = setOf("top/bottom", "left/right")
        store.setEngineOption(null, "nds:DESMUME", "screens", "top/bottom", values)
        store.setEngineOption("one", "nds:DESMUME", "screens", "left/right", values)
        assertEquals("left/right", store.read("one").effective.engineOptions["nds:DESMUME:screens"])
        assertEquals("top/bottom", store.read("two").effective.engineOptions["nds:DESMUME:screens"])
        assertTrue(runCatching { store.setEngineOption("one", "nds:DESMUME", "screens", "stretch", values) }.isFailure)
        store.useGlobal("one")
        assertEquals("top/bottom", store.read("one").effective.engineOptions["nds:DESMUME:screens"])
    }

    @Test fun futureAndMalformedDocumentsArePreservedAndNeverOverwritten() {
        val root = temporary.newFolder()
        val global = root.resolve("global.json")
        val store = DisplaySettingsStore(root)
        for (content in listOf("{\"schema\":2,\"future\":true}", "broken")) {
            global.writeText(content)
            assertNotNull(store.readSafely(null).error)
            assertTrue(runCatching { store.set(null, "landscapeBorder", "OFF") }.isFailure)
            assertEquals(content, global.readText())
        }
    }

    @Test fun independentWritersPreserveUnknownFieldsAndRejectInvalidChoices() {
        val root = temporary.newFolder()
        root.resolve("global.json").writeText("{\"schema\":1,\"future\":\"retained\"}")
        val first = DisplaySettingsStore(root)
        val second = DisplaySettingsStore(root)
        first.set(null, "landscapeBorder", "STANDARD")
        second.set(null, "portraitBorder", "OFF")
        assertEquals(DisplaySettingsStore.Border.STANDARD, first.read(null).effective.border(true))
        assertEquals(DisplaySettingsStore.Border.OFF, first.read(null).effective.border(false))
        assertTrue(root.resolve("global.json").readText().contains("retained"))
        assertTrue(runCatching { first.set(null, "alignment", "stretch") }.isFailure)
        assertTrue(runCatching { first.set(null, "quality", "99") }.isFailure)
        assertTrue(runCatching { first.set("", "hd", "true") }.isFailure)
    }
}
