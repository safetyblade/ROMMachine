package com.swordfish.lemuroid.app.shared.game

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.libretrodroid.Variable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DisplayEngineOptionsTest {
    @Test fun catalogueContainsOnlyActuallyReportedAndExposedDisplayChoices() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val config = GameSystem.findById("nds").systemCoreConfigs.first()
        val catalogue = DisplayEngineOptions(context)
        val namespace = DisplayEngineOptions.namespace("nds", config)
        val entries =
            catalogue.record(
                namespace,
                config,
                arrayOf(
                    Variable("desmume_screens_layout", "top/bottom", "Layout; top/bottom|left/right"),
                    Variable("desmume_frameskip", "1", "Frameskip; 0|1|2"),
                    Variable("unexposed_widescreen", "disabled", "Wide; enabled|disabled"),
                ),
            )
        assertEquals(listOf("desmume_screens_layout"), entries.map { it.key })
        assertEquals(listOf("top/bottom", "left/right"), entries.single().values)
        assertEquals(entries, catalogue.read(namespace))
        assertEquals(emptyList<DisplayEngineOptions.Option>(), catalogue.read("another:core"))
        assertFalse(DisplayEngineOptions.isDisplayKey("melonds_jit_enable"))
    }
}
