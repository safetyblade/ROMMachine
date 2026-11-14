package com.swordfish.lemuroid.app.wiiu

import android.view.KeyEvent
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuEngine
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuPad
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuStorage
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuViewport
import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties

class WiiuRuntimeTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun savesSurviveMediaRestagingAndCartridgesRemainIndependent() {
        val files = temporary.newFolder("files")
        val cache = temporary.newFolder("cache")
        val a = "11111111-1111-4111-8111-111111111111"
        val b = "22222222-2222-4222-8222-222222222222"
        val nand = WiiuStorage.cartridgeRoot(files, a).apply { mkdirs() }
        File(nand, "save.bin").writeText("progress")
        val first = WiiuStorage.stagingRoot(cache, a, "content://games/first").apply { mkdirs() }
        File(first, "game.wua").writeText("media")
        first.deleteRecursively()
        assertEquals("progress", File(WiiuStorage.cartridgeRoot(files, a), "save.bin").readText())
        assertNotEquals(nand, WiiuStorage.cartridgeRoot(files, b))
        assertNotEquals(first, WiiuStorage.stagingRoot(cache, a, "content://games/relinked"))
        assertThrows(IllegalArgumentException::class.java) { WiiuStorage.cartridgeRoot(files, "../wii") }
        listOf("..", "a/b", "a\\b", "\u0000").forEach { bad ->
            assertThrows(IllegalArgumentException::class.java) { WiiuStorage.component(bad) }
        }
    }

    @Test fun pointerAccountsForBarsAndRejectsInvalidSurfaces() {
        val centre = WiiuViewport.pointer(500f, 500f, 1000, 1000, 16f / 9f)
        assertTrue(centre.inside)
        assertEquals(0, centre.x)
        assertEquals(0, centre.y)
        assertFalse(WiiuViewport.pointer(500f, 10f, 1000, 1000, 16f / 9f).inside)
        assertFalse(WiiuViewport.pointer(Float.NaN, 10f, 1000, 1000, 1f).inside)
        assertFalse(WiiuViewport.pointer(0f, 0f, 0, 0, 1f).inside)
        assertEquals(32767, WiiuViewport.pointer(1600f, 900f, 1600, 900, 16f / 9f).x)
    }

    @Test fun labelledNintendoButtonsAndBothSticksReachRetroPadAbi() {
        assertEquals(KeyEvent.KEYCODE_BUTTON_B, WiiuPad.touchKey(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(KeyEvent.KEYCODE_BUTTON_A, WiiuPad.touchKey(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, WiiuPad.touchKey(KeyEvent.KEYCODE_BUTTON_L1))
        assertEquals((0..15).toSet(), WiiuPad.retroToXenia.toSet())
        val pad = IntArray(24)
        pad[16] = -1234
        pad[19] = 2345
        pad[20] = 3456
        pad[23] = -4567
        assertArrayEquals(intArrayOf(-1234, -2345, 3456, 4567), WiiuPad.analog(pad))
        assertEquals(14, WiiuPad.retroToXenia[12]) // L2
        assertEquals(12, WiiuPad.retroToXenia[14]) // left stick click
    }

    @Test fun coreAndHostRollbackTogetherWithoutTouchingNand() {
        val files = temporary.newFolder("engine-files")
        val natives = temporary.newFolder("native")
        val store = WiiuEngine.store(files)

        fun install(
            revision: String,
            host: String,
        ): XboxEngineStore.Selection {
            File(natives, WiiuEngine.CORE).writeText("same core")
            File(natives, WiiuEngine.HOST).writeText(host)
            val props =
                Properties().apply {
                    setProperty("revision", revision)
                    setProperty("core.sha256", XboxEngineStore.hash(File(natives, WiiuEngine.CORE)))
                    setProperty("library.names", "${WiiuEngine.CORE},${WiiuEngine.HOST}")
                    listOf(WiiuEngine.CORE, WiiuEngine.HOST).forEach {
                        setProperty("library.$it.sha256", XboxEngineStore.hash(File(natives, it)))
                    }
                }
            return store.install(natives, props, byteArrayOf(1))
        }
        val old = install("a".repeat(40), "first host")
        val next = install("b".repeat(40), "second host")
        assertNotEquals(old.id, next.id)
        assertTrue(store.rollback())
        assertEquals(old.id, store.selected()!!.id)
        assertEquals("first host", File(store.selected()!!.directory, WiiuEngine.HOST).readText())
        assertEquals(old.id, install("b".repeat(40), "second host").id)
        File(old.directory, WiiuEngine.HOST).apply {
            setWritable(true)
            writeText("corrupt")
        }
        assertNotEquals(old.id, store.selected()?.id)
    }
}
