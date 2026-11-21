package com.swordfish.lemuroid.app.shared.game.profile

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360PipelineAB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class Xbox360PipelineABTest {
    @Test fun globalSettingIsTemporaryAndRestoresDefaultAfterEveryTestLaunch() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.NORMAL)
        try {
            assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.consume(context))
            assertNull(Xbox360PipelineAB.Mode.NORMAL.launchOption)

            Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.SKIP_OFF)
            assertEquals(Xbox360PipelineAB.Mode.SKIP_OFF, Xbox360PipelineAB.queued(context))
            assertEquals("--vulkan_async_skip_draws=false", Xbox360PipelineAB.consume(context).launchOption)
            assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.queued(context))
            assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.consume(context))

            Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.SKIP_ON)
            assertEquals("--vulkan_async_skip_draws=true", Xbox360PipelineAB.consume(context).launchOption)
            assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.queued(context))

            Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.SKIP_OFF)
            Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.NORMAL)
            assertEquals(Xbox360PipelineAB.Mode.NORMAL, Xbox360PipelineAB.consume(context))
        } finally {
            Xbox360PipelineAB.queue(context, Xbox360PipelineAB.Mode.NORMAL)
        }
    }
}
