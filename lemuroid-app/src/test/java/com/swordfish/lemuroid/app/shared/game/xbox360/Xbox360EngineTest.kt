package com.swordfish.lemuroid.app.shared.game.xbox360

import com.swordfish.lemuroid.app.shared.game.xbox.XboxEngineStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties

class Xbox360EngineTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun updatesAndRollbackKeepGuestDataAndOriginalXboxIndependent() {
        val files = temporary.newFolder()
        val original =
            File(files, "native/xbox/hdd/save").apply {
                parentFile!!.mkdirs()
                writeText("original")
            }
        val save =
            File(Xbox360Engine.data(files), "content/profile/progress").apply {
                parentFile!!.mkdirs()
                writeText("xbox360")
            }
        val store = Xbox360Engine.store(files)

        fun install(value: String): String {
            val native = temporary.newFolder()
            val core = File(native, Xbox360Engine.CORE).apply { writeText(value) }
            val hash = XboxEngineStore.hash(core)
            val props =
                Properties().apply {
                    setProperty("revision", Xbox360Engine.REVISION)
                    setProperty("core.sha256", hash)
                    setProperty("library.names", Xbox360Engine.CORE)
                    setProperty("library.${Xbox360Engine.CORE}.sha256", hash)
                }
            return store.install(native, props, byteArrayOf(1)).id
        }
        val first = install("first")
        val second = install("second")
        assertTrue(first != second)
        assertTrue(store.canRollback())
        assertTrue(store.rollback())
        assertEquals(first, store.selected()!!.id)
        assertEquals("xbox360", save.readText())
        assertEquals("original", original.readText())
        Xbox360Engine.data(files)
        assertEquals("xbox360", save.readText())
    }
}
