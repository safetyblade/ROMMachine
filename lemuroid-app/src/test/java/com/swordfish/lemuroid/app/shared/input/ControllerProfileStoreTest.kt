package com.swordfish.lemuroid.app.shared.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ControllerProfileStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun cartridgeOverrideDeviceDefaultAndCalibrationRoundTripAcrossReaders() {
        val root = temporary.newFolder()
        val first = ControllerProfileStore(root)
        first.save(
            "device-one",
            null,
            ControllerProfileStore.Profile(
                buttons = mapOf(96 to 97),
                deadzone = 0.1f,
                calibrated = true,
            ),
        )
        first.save("device-one", "cartridge-one", ControllerProfileStore.Profile(buttons = mapOf(96 to 99)))
        val reopened = ControllerProfileStore(root)
        assertEquals(mapOf(96 to 99), reopened.read("device-one", "cartridge-one").buttons)
        assertEquals(0.1f, reopened.read("device-one", "cartridge-one").deadzone, 0f)
        assertEquals(mapOf(96 to 97), reopened.read("device-one", "cartridge-two").buttons)
        assertEquals(null, reopened.read("device-two", "cartridge-one").buttons)
        reopened.useDevice("device-one", "cartridge-one")
        assertFalse(first.hasOverride("device-one", "cartridge-one"))
        assertEquals(mapOf(96 to 97), first.read("device-one", "cartridge-one").buttons)
    }

    @Test fun overwriteRollbackAndResetRetainCalibrationAndUnknownFields() {
        val root = temporary.newFolder()
        val store = ControllerProfileStore(root)
        store.save("device", null, ControllerProfileStore.Profile(buttons = mapOf(96 to 97), deadzone = 0.1f))
        val document = root.listFiles()!!.single { it.extension == "json" }
        document.writeText(document.readText().dropLast(1) + ",\"future\":true}")
        store.save("device", null, store.read("device").copy(buttons = mapOf(96 to 99)))
        assertEquals(mapOf(96 to 99), store.read("device").buttons)
        assertTrue(document.readText().contains("future"))
        store.restorePrevious("device", null)
        assertEquals(mapOf(96 to 97), store.read("device").buttons)
        store.resetMappings()
        assertEquals(null, store.read("device").buttons)
        assertEquals(0.1f, store.read("device").deadzone, 0f)
        assertTrue(document.readText().contains("future"))
    }

    @Test fun malformedOrFutureProfileCannotBeOverwritten() {
        val root = temporary.newFolder()
        val store = ControllerProfileStore(root)
        store.save("device", null, ControllerProfileStore.Profile())
        val document = root.listFiles()!!.single { it.extension == "json" }
        for (content in listOf("broken", "{\"schema\":2}")) {
            document.writeText(content)
            assertTrue(runCatching { store.save("device", null, ControllerProfileStore.Profile()) }.isFailure)
            assertEquals(content, document.readText())
        }
        assertTrue(runCatching { store.read("device", "") }.isFailure)
    }
}
