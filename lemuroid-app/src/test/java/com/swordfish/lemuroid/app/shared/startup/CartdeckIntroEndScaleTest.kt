package com.swordfish.lemuroid.app.shared.startup

import android.app.Application
import android.graphics.SurfaceTexture
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartdeckIntroEndScaleTest {
    @Test fun endCardKeepsItsAspectAndSafeMarginsAcrossViewports() {
        val video = 1280f / 720f
        for (viewport in listOf(9f / 20, 3f / 4, 4f / 3, video, 21f / 9, 32f / 9)) {
            val (x, y) = cartdeckIntroEndScale(video, viewport)
            assertEquals(video, viewport * x / y, 0.0001f)
            assertTrue(x * 0.50f <= 0.9001f)
            assertTrue(y * 0.66f <= 0.9001f)
        }
        assertEquals(1f to 1f, cartdeckIntroEndScale(video, video))
    }

    @Test fun losingTheSurfaceFinishesOnceAndExplicitDisposalDoesNotCallback() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val surface = SurfaceTexture(0)
        try {
            var finished = 0
            val player = CartdeckStartupVideo.create(context) { finished++ }
            assertTrue(player.onSurfaceTextureDestroyed(surface))
            assertTrue(player.onSurfaceTextureDestroyed(surface))
            val disposed = CartdeckStartupVideo.create(context) { finished++ }
            disposed.stopPlayback()
            assertTrue(disposed.onSurfaceTextureDestroyed(surface))
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, finished)
        } finally {
            surface.release()
        }
    }
}
