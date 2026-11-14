package com.swordfish.lemuroid.app.shared.game.ps2next

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Properties

class Armsx2PageSelectionTest {
    private fun universal() =
        Properties().apply {
            setProperty("host.page_size", "universal")
            setProperty("core.sha256", "a".repeat(64))
            setProperty("library.names", "libemucore_4k.so,libemucore_16k.so,libc++_shared.so,liblibrashader_capi.so")
            setProperty("library.libemucore_4k.so.sha256", "a".repeat(64))
            setProperty("library.libemucore_16k.so.sha256", "b".repeat(64))
        }

    @Test fun universalApkSelectsMatchingCoreAndRetainsDependencies() {
        for ((page, core, digest, other) in listOf(
            listOf("4096", "libemucore_4k.so", "a", "libemucore_16k.so"),
            listOf("16384", "libemucore_16k.so", "b", "libemucore_4k.so"),
        )) {
            val bundled = universal()
            val selected = Armsx2Engine.selectPage(bundled, page.toInt())
            assertEquals(page, selected.getProperty("host.page_size"))
            assertEquals(digest.repeat(64), selected.getProperty("core.sha256"))
            assertEquals("$core,libc++_shared.so,liblibrashader_capi.so", selected.getProperty("library.names"))
            assertFalse(selected.getProperty("library.names").contains(other))
            assertEquals("universal", bundled.getProperty("host.page_size"))
        }
    }

    @Test(expected = IllegalStateException::class)
    fun incompatibleSinglePageApkIsRejected() {
        Armsx2Engine.selectPage(universal().apply { setProperty("host.page_size", "4096") }, 16384)
    }
}
