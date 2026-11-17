package com.swordfish.lemuroid.app.shared

import android.app.Application
import android.view.View
import android.widget.PopupMenu
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.ControllerSettingsActivity
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.DisplaySettingsActivity
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeSetupEntryTest {
    @Test fun everyRegisteredConsoleKeepsItsCartridgeIdentityInBothPrelaunchSettingsRoutes() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        for (system in GameSystem.all() + NativeGameSystems.configurations()) {
            val game =
                Game(
                    fileName = "fixture.rom",
                    fileUri = "content://fixture/game",
                    title = "Fixture",
                    systemId = system.id.dbname,
                    developer = null,
                    coverFrontUrl = null,
                    lastIndexedAt = 0,
                    cartridgeId = "stable-cartridge",
                )
            val menu = PopupMenu(context, View(context)).menu
            addCartridgeSetupEntries(menu, context, game)
            val setup = menu.getItem(0).subMenu!!
            assertEquals("Setup", menu.getItem(0).title)
            assertEquals(DisplaySettingsActivity::class.java.name, setup.getItem(0).intent!!.component!!.className)
            assertEquals(ControllerSettingsActivity::class.java.name, setup.getItem(1).intent!!.component!!.className)
            for (index in 0..1) {
                @Suppress("DEPRECATION")
                val passed = setup.getItem(index).intent!!.getSerializableExtra("cartdeck.game") as Game
                assertEquals(game.cartridgeId, passed.cartridgeId)
                assertEquals(game.systemId, passed.systemId)
                assertEquals(game.fileUri, passed.fileUri)
            }
        }
    }
}
