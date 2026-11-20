package com.swordfish.lemuroid.app.shared.startup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Source-level regression check for startup and adaptive-icon compatibility. */
class CartdeckStartupSourceTest {
    private fun source(path: String): String {
        val relative = "src/main/$path"
        return listOf(File(relative), File("lemuroid-app", relative))
            .firstOrNull { it.isFile }
            ?.readText()
            ?: error("Missing source: $relative")
    }

    @Test
    fun videoOnlyRemovesBrandAfterFirstRenderedFrameAndFailsOpen() {
        val implementation = source("java/com/swordfish/lemuroid/app/shared/startup/CartdeckStartupVideo.kt")
        assertTrue(implementation.contains("TextureView(context)"))
        assertTrue(implementation.contains("MEDIA_INFO_VIDEO_RENDERING_START"))
        assertTrue(implementation.contains("placeholder.visibility = View.GONE"))
        assertTrue(implementation.contains("firstFrameTimeoutMs"))
        assertTrue(implementation.contains("setTransform("))
        assertTrue(implementation.contains("finishOnce()"))
        assertFalse(implementation.contains("VideoView(context)"))
    }

    @Test
    fun startupFramingIsAdjustableAndPersistedPerOrientation() {
        val settings = source("java/com/swordfish/lemuroid/app/shared/startup/CartdeckIntroFraming.kt")
        val player = source("java/com/swordfish/lemuroid/app/shared/startup/CartdeckStartupVideo.kt")
        val ui = source("java/com/swordfish/lemuroid/app/mobile/feature/main/CartdeckLaunchSignature.kt")
        assertTrue(settings.contains("fun constrained()"))
        assertTrue(settings.contains("cartdeck_signature_framing_v1"))
        assertTrue(settings.contains("context.resources.configuration.smallestScreenWidthDp < 600"))
        assertTrue(settings.contains("zoom = if (portraitPhone) 1.6f else 1f"))
        assertTrue(ui.contains("CartdeckIntroFraming.defaultFor(context)"))
        assertTrue(settings.contains("putFloat("))
        assertTrue(player.contains("postTranslate(safe.offsetX * viewWidth, safe.offsetY * viewHeight)"))
        assertTrue(player.contains("fun pauseForFraming()"))
        assertTrue(ui.contains("Fit whole video"))
        assertTrue(ui.contains("Fill screen"))
        assertTrue(ui.contains("Save & play"))
    }

    @Test
    fun launcherMasksShareOneOpticallyCentredMark() {
        val foreground = source("res/drawable/cartdeck_mark_foreground.xml")
        val monochrome = source("res/drawable/cartdeck_mark_monochrome.xml")
        assertTrue(foreground.contains("android:translateX=\"7\""))
        assertTrue(monochrome.contains("android:translateX=\"7\""))
        for (name in listOf("cartdeck_launcher.xml", "cartdeck_launcher_round.xml")) {
            assertTrue(source("res/mipmap-anydpi/$name").contains("android:translateX=\"11\""))
            assertTrue(source("res/mipmap-anydpi-v26/$name").contains("@drawable/cartdeck_mark_foreground"))
        }
    }
}
