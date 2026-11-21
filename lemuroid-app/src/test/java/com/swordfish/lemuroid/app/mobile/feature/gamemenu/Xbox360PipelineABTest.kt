package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360PipelineAB

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Xbox360PipelineABTest {
    private lateinit var context: Context

    @Before fun reset() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("cartdeck_xbox360_pipeline_ab", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test fun orderedUploadTestRunsOnceThenRestoresDefaultWithoutChangingGameData() {
        assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.queued(context))
        val mode = Xbox360PipelineAB.Mode.ORDERED_UPLOADS
        assertEquals("--vulkan_hoist_shmem_uploads=false", mode.launchOption)
        Xbox360PipelineAB.queue(context, mode)
        assertEquals(mode, Xbox360PipelineAB.queued(context))
        assertEquals(mode, Xbox360PipelineAB.consume(context))
        assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.queued(context))
        assertNull(Xbox360PipelineAB.consume(context).launchOption)
    }

    @Test fun restoringDefaultClearsQueuedMemoryTestAndOtherModesRemainAvailable() {
        Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.ORDERED_UPLOADS)
        Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.NORMAL)
        assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.consume(context))
        Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.SKIP_ON)
        assertEquals(Xbox360PipelineAB.Mode.SKIP_ON, Xbox360PipelineAB.consume(context))
        assertTrue(Xbox360PipelineAB.Mode.entries.any { it.launchOption == "--vulkan_async_skip_draws=false" })
    }
}
