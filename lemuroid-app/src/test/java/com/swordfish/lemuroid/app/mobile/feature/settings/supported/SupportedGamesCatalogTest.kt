package com.swordfish.lemuroid.app.mobile.feature.settings.supported

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SupportedGamesCatalogTest {
    private val all by lazy {
        SupportedGamesCatalog.entries(ApplicationProvider.getApplicationContext<Application>())
    }

    @Test
    fun includesEveryConfiguredSystemEvenWithNoImportedGames() {
        assertEquals(GameSystem.all().size, all.size)
        assertEquals(GameSystem.all().map { it.id }.toSet(), all.map { it.system.id }.toSet())
        assertTrue(all.size >= 25)
    }

    @Test
    fun extensionsAreTakenFromTheActualScannerConfiguration() {
        all.forEach { entry ->
            assertEquals(entry.system.supportedExtensions.distinct().sorted(), entry.extensions)
            assertFalse(entry.extensions.isEmpty())
        }
        assertEquals(
            listOf("chd", "cue", "iso", "m3u", "pbp"),
            all.first { it.system.id == SystemID.PSX }.extensions,
        )
        assertEquals(listOf("zip"), all.first { it.system.id == SystemID.FBNEO }.extensions)
    }

    @Test
    fun searchMatchesCommonNameAndFileSuffix() {
        assertTrue(SupportedGamesCatalog.search(all, "ps1").any { it.system.id == SystemID.PSX })
        assertTrue(SupportedGamesCatalog.search(all, "GBA").any { it.system.id == SystemID.GBA })
        assertTrue(SupportedGamesCatalog.search(all, ".chd").any { it.system.id == SystemID.PSX })
        assertTrue(SupportedGamesCatalog.search(all, "Game Boy").any { it.system.id == SystemID.GBC })
        assertTrue(SupportedGamesCatalog.search(all, "not-a-console").isEmpty())
    }

    @Test
    fun runtimeStatusSeparatesArchitectureCorePresenceAndFirmware() {
        val threeDs = all.first { it.system.id == SystemID.NINTENDO_3DS }
        val lynx = all.first { it.system.id == SystemID.LYNX }
        val psx = all.first { it.system.id == SystemID.PSX }
        val emptyArm64 = GameGuideRuntime(emptySet(), emptySet(), setOf("arm64-v8a"))
        assertEquals(GameGuideSetup.CHECKING, SupportedGamesCatalog.status(psx, null))
        assertEquals(
            GameGuideSetup.DEVICE_UNSUPPORTED,
            SupportedGamesCatalog.status(threeDs, emptyArm64.copy(deviceAbis = setOf("x86_64"))),
        )
        assertEquals(GameGuideSetup.ENGINE_MISSING, SupportedGamesCatalog.status(psx, emptyArm64))

        val lynxEngine = lynx.system.systemCoreConfigs.first().coreID.libretroFileName
        assertEquals(
            GameGuideSetup.FIRMWARE_MISSING,
            SupportedGamesCatalog.status(lynx, emptyArm64.copy(installedCoreFiles = setOf(lynxEngine))),
        )
        assertEquals(
            GameGuideSetup.ENGINE_PRESENT,
            SupportedGamesCatalog.status(
                lynx,
                emptyArm64.copy(
                    installedCoreFiles = setOf(lynxEngine),
                    installedFirmwareFiles = setOf("lynxboot.img"),
                ),
            ),
        )
    }

    @Test
    fun settingsGuideIsNestedBelowSettings() {
        assertEquals(MainRoute.SETTINGS, MainRoute.SETTINGS_SUPPORTED_GAMES.parent)
        assertEquals("settings/supported-games", MainRoute.SETTINGS_SUPPORTED_GAMES.route)
    }
}
