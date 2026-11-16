package com.swordfish.lemuroid.app.shared.input

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.Lazy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class InputDeviceManagerEmptyDevicesTest {
    @Test fun noControllersStillEmitsBindingsShortcutsAndPortConfiguration() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Application>()
            val preferences = context.getSharedPreferences("empty-controller-test", Context.MODE_PRIVATE)
            val manager = InputDeviceManager(context, Lazy { preferences })
            // An empty combine previously emitted nothing, freezing menu/configuration updates.
            withTimeout(1000) {
                assertTrue(manager.getInputBindingsObservable().first()(null).isEmpty())
                assertTrue(manager.getGameShortcutsObservable().first().isEmpty())
                assertNull(manager.getGamePadsPortMapperObservable().first()(null))
            }
        }
}
