package com.swordfish.lemuroid.app.shared.game.xbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Properties

class XboxEngineStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun packageFor(
        directory: File,
        revision: Char,
    ): Properties {
        File(directory, "libhakuxcore.so").writeText("engine-$revision")
        File(directory, "libSDL3.so").writeText("sdl-$revision")
        return Properties().apply {
            setProperty("revision", revision.toString().repeat(40))
            setProperty("core.sha256", XboxEngineStore.hash(File(directory, "libhakuxcore.so")))
            setProperty("library.names", "libhakuxcore.so,libSDL3.so")
            listOf("libhakuxcore.so", "libSDL3.so").forEach {
                setProperty("library.$it.sha256", XboxEngineStore.hash(File(directory, it)))
            }
        }
    }

    @Test fun rollbackSelectsTheCompleteOldPackageAndStaysPinned() {
        val store = XboxEngineStore(temporary.newFolder())
        val binaries = temporary.newFolder()
        val a = store.install(binaries, packageFor(binaries, 'a'), byteArrayOf(1))
        val bProperties = packageFor(binaries, 'b')
        val b = store.install(binaries, bProperties, byteArrayOf(2))
        assertNotEquals(a.id, b.id)
        assertTrue(store.rollback())
        val selected = store.install(binaries, bProperties, byteArrayOf(2))
        assertEquals(a.id, selected.id)
        assertEquals("engine-a", selected.core.readText())
        assertEquals("sdl-a", selected.sdl.readText())
        assertTrue(File(selected.directory, "hakux-licenses.zip").isFile)
    }

    @Test fun corruptActiveDependencyFallsBackToVerifiedPreviousPackage() {
        val root = temporary.newFolder()
        val store = XboxEngineStore(root)
        val binaries = temporary.newFolder()
        val a = store.install(binaries, packageFor(binaries, 'a'), byteArrayOf(1))
        val bProperties = packageFor(binaries, 'b')
        val b = store.install(binaries, bProperties, byteArrayOf(2))
        b.sdl.setWritable(true)
        b.sdl.writeText("corrupted dependency")
        assertEquals(a.id, store.selected()?.id)
        // The real launcher calls install again on every Open, not selected alone.
        assertEquals(a.id, store.install(binaries, bProperties, byteArrayOf(2)).id)
        assertFalse(store.canRollback())
        assertFalse(store.rollback())
    }

    @Test(expected = IllegalStateException::class)
    fun changedNativeDependencyCannotBeInstalled() {
        val binaries = temporary.newFolder()
        val props = packageFor(binaries, 'a')
        File(binaries, "libSDL3.so").writeText("changed")
        XboxEngineStore(temporary.newFolder()).install(binaries, props, byteArrayOf(1))
    }
}
