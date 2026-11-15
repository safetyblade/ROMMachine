package com.swordfish.lemuroid.app.wiiu

import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuLaunchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WiiuLaunchStatusTest {
    @Test fun rendererReadinessReleasesLoaderBeforeFirstGameFrame() {
        assertTrue(WiiuLaunchStatus.parse("host_stage=2\nrenderer_ready=0").showLoadingScreen)
        val ready = WiiuLaunchStatus.parse("host_stage=2\nrenderer_ready=1\npresented_image_callbacks=0")
        assertFalse(ready.showLoadingScreen)
        assertEquals(0L, ready.frames)
    }

    @Test fun gameFrameEvidenceRemainsIndependentFromLoaderAndReadiness() {
        val playing = WiiuLaunchStatus.parse("presented_image_callbacks=8071\nnative_error=")
        assertFalse(playing.showLoadingScreen)
        assertEquals(8071L, playing.frames)
    }

    @Test fun nativeErrorCannotRemainAnEndlessLoadingScreen() {
        val failed = WiiuLaunchStatus.parse("host_stage=4\nnative_error=VULKAN_SWAPCHAIN_CREATE_FAILED_RESULT_-1")
        assertFalse(failed.showLoadingScreen)
        assertEquals("VULKAN_SWAPCHAIN_CREATE_FAILED_RESULT_-1", failed.error)
        assertTrue(WiiuLaunchStatus.parse("unavailable").showLoadingScreen)
    }
}
