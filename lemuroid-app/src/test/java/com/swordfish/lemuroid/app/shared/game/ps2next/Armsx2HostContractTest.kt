package com.swordfish.lemuroid.app.shared.game.ps2next

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class Armsx2HostContractTest {
    @Test
    fun cartridgeRootsAreStableButSeparateAndNeverOverlapPlay() {
        val temp = Files.createTempDirectory("cartdeck-armsx2-test").toFile()
        try {
            val a = Armsx2Storage.paths(temp, "78bb531a-8ef3-4390-9713-3e6dfb7852e1")
            val same = Armsx2Storage.paths(temp, "78bb531a-8ef3-4390-9713-3e6dfb7852e1")
            val b = Armsx2Storage.paths(temp, "8a12b7ab-989a-45ec-af54-700ac15d6cd7")
            assertEquals(a.gameRoot, same.gameRoot)
            assertTrue(a.memcards.path.contains("armsx2"))
            assertFalse(a.gameRoot == b.gameRoot)
            assertFalse(a.memcards.path.contains("Play Data Files"))
            assertFalse(a.gameRoot.path.contains("libretro"))
            assertEquals(a.bios, b.bios)
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun identitiesCannotEscapeAppPrivateStorage() {
        val filesDir = File("/private/files")
        for (id in listOf("../saves", "", "123", "78bb531a/../../..")) {
            val error = runCatching { Armsx2Storage.paths(filesDir, id) }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
        }
    }

    @Test
    fun absentDiscBiosAndResourcesBlockNativeStartWithoutCreatingFakeFiles() {
        val temp = Files.createTempDirectory("cartdeck-armsx2-gate").toFile()
        try {
            val p = Armsx2Storage.paths(temp, "b74a3f84-20cc-4b67-90f1-61b2e014e524")
            val image = File(temp, "test.iso")
            assertEquals(listOf("DISC", "BIOS", "RESOURCES"), Armsx2Storage.missingRequirements(p, image))
            Armsx2Storage.createPrivateDirectories(p)
            assertFalse(p.resources.exists())
            assertFalse(File(temp, "Play Data Files").exists())
            image.writeBytes(byteArrayOf(1, 2, 3))
            File(p.bios, "user-bios.bin").writeBytes(byteArrayOf(1))
            p.resources.mkdirs()
            File(p.resources, "index.dat").writeBytes(byteArrayOf(2))
            assertTrue(Armsx2Storage.missingRequirements(p, image).isEmpty())
        } finally {
            temp.deleteRecursively()
        }
    }

    @Test
    fun directButtonValuesAreMappedFromVerifiedNativeAbi() {
        assertEquals(96, Armsx2PadMapping.button(KeyEvent.KEYCODE_BUTTON_A, true)?.nativeCode)
        assertEquals(100, Armsx2PadMapping.button(KeyEvent.KEYCODE_BUTTON_Y, true)?.nativeCode)
        assertEquals(104, Armsx2PadMapping.button(KeyEvent.KEYCODE_BUTTON_L2, true)?.nativeCode)
        assertNull(Armsx2PadMapping.button(KeyEvent.KEYCODE_UNKNOWN, true))
        assertEquals(0, Armsx2PadMapping.button(KeyEvent.KEYCODE_BUTTON_A, true)?.range)
    }

    @Test
    fun bothSticksTransmitMagnitudeDirectionAndRelease() {
        val west = Armsx2PadMapping.stick(-0.5f, 0.25f, right = false)
        assertEquals(listOf(110, 111, 112, 113), west.map { it.nativeCode })
        assertTrue(west[3].pressed)
        assertTrue(west[2].pressed)
        assertFalse(west[1].pressed)
        assertTrue(west[3].range in 16382..16385)
        val reset = Armsx2PadMapping.stick(Float.NaN, 0f, right = true)
        assertEquals(listOf(120, 121, 122, 123), reset.map { it.nativeCode })
        assertTrue(reset.all { !it.pressed && it.range == 0 })
    }

    @Test
    fun triggerPressureAndReleaseMatchNativeDualShockAbi() {
        val half = Armsx2PadMapping.trigger(0.5f, right = false)
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, half.nativeCode)
        assertEquals(16384, half.range)
        assertTrue(half.pressed)
        val full = Armsx2PadMapping.trigger(2f, right = true)
        assertEquals(KeyEvent.KEYCODE_BUTTON_R2, full.nativeCode)
        assertEquals(32767, full.range)
        for (value in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            val released = Armsx2PadMapping.trigger(value, right = true)
            assertEquals(0, released.range)
            assertFalse(released.pressed)
        }
    }
}
