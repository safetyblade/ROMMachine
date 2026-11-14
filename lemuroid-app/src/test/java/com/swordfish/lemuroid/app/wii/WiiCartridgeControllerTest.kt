package com.swordfish.lemuroid.app.wii

import android.content.SharedPreferences
import com.swordfish.lemuroid.app.shared.settings.ControllerConfigsManager
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.NativeGameSystems
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class WiiCartridgeControllerTest {
    private val wii = NativeGameSystems.configurations().single { it.id == SystemID.WII }.systemCoreConfigs.single()

    private fun game(id: Int) =
        Game(
            id = id,
            title = "Wii fixture $id",
            fileName = "$id.iso",
            fileUri = "content://fixture/disc/$id",
            systemId = "wii",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )

    @Test fun verifiedReimportRetainsSavedControllerAndReadsLegacySourcePreference() =
        runBlocking {
            val original = game(12)
            val oldKey =
                ControllerConfigsManager.legacyCartridgeKey(
                    SystemID.WII,
                    CoreID.DOLPHIN_MODERN,
                    0,
                    original,
                    original.fileUri,
                )!!
            val data = mutableMapOf(oldKey to "wii_gamecube")
            val manager = ControllerConfigsManager(Lazy { preferences(data) })
            val returned =
                original.copy(
                    id = 33,
                    fileUri = "content://fixture/new-tree/disc/12",
                    legacySourceUri = original.fileUri,
                )
            assertEquals("wii_gamecube", manager.getControllerConfigs(SystemID.WII, wii, returned).getValue(0).name)
            assertTrue(manager.selectController(SystemID.WII, wii, 0, "wii_classic", returned))
            assertEquals(
                "wii_classic",
                manager.getControllerConfigs(
                    SystemID.WII,
                    wii,
                    returned.copy(fileUri = "content://fixture/third-tree/disc/12"),
                ).getValue(0).name,
            )
            assertEquals("wii_gamecube", data.getValue(oldKey))
            assertEquals(2, data.size)
        }

    @Test fun twoCartridgesKeepDifferentChoicesAcrossManagerReopenAndAllFourDevices() =
        runBlocking {
            val data = mutableMapOf<String, String>()

            fun manager() = ControllerConfigsManager(Lazy { preferences(data) })
            val first = game(1)
            val second = game(2)
            assertEquals(513, manager().getControllerConfigs(SystemID.WII, wii, first).getValue(0).libretroId)
            assertTrue(manager().selectController(SystemID.WII, wii, 0, "wii_gamecube", first))
            assertEquals(1, manager().getControllerConfigs(SystemID.WII, wii, first).getValue(0).libretroId)
            assertEquals(513, manager().getControllerConfigs(SystemID.WII, wii, second).getValue(0).libretroId)
            assertTrue(manager().selectController(SystemID.WII, wii, 0, "wii_nunchuk", second))
            assertEquals("wii_gamecube", manager().getControllerConfigs(SystemID.WII, wii, first).getValue(0).name)
            assertEquals("wii_nunchuk", manager().getControllerConfigs(SystemID.WII, wii, second).getValue(0).name)
            for ((name, device) in listOf(
                "wii_sideways" to 513,
                "wii_classic" to 1025,
                "wii_nunchuk" to 769,
                "wii_gamecube" to 1,
            )) {
                assertTrue(manager().selectController(SystemID.WII, wii, 0, name, first))
                assertEquals(device, manager().getControllerConfigs(SystemID.WII, wii, first).getValue(0).libretroId)
            }
            assertEquals("wii_nunchuk", manager().getControllerConfigs(SystemID.WII, wii, second).getValue(0).name)
            assertEquals(2, data.size)
        }

    @Test fun legacyConsoleFallbackIsReadWithoutWritingOrOverridingCartridgeChoice() =
        runBlocking {
            val key = ControllerConfigsManager.getSharedPreferencesId("wii", CoreID.DOLPHIN_MODERN, 0)
            val data = mutableMapOf(key to "wii_classic")
            val manager = ControllerConfigsManager(Lazy { preferences(data) })
            val first = game(1)
            val second = game(2)
            assertEquals("wii_classic", manager.getControllerConfigs(SystemID.WII, wii, first).getValue(0).name)
            assertEquals(mapOf(key to "wii_classic"), data)
            assertTrue(manager.selectController(SystemID.WII, wii, 0, "wii_gamecube", first))
            assertEquals("wii_gamecube", manager.getControllerConfigs(SystemID.WII, wii, first).getValue(0).name)
            assertEquals("wii_classic", manager.getControllerConfigs(SystemID.WII, wii, second).getValue(0).name)
            assertEquals("wii_classic", data[key])
            val before = data.toMap()
            assertFalse(manager.selectController(SystemID.WII, wii, 0, "unavailable", first))
            assertEquals(before, data)
        }

    @Test fun preferenceIdentitySurvivesRecordRenameAndDoesNotAffectOtherSystems() {
        val first = game(1)
        val key = ControllerConfigsManager.cartridgeKey(SystemID.WII, CoreID.DOLPHIN_MODERN, 0, first)
        assertEquals(
            key,
            ControllerConfigsManager.cartridgeKey(
                SystemID.WII,
                CoreID.DOLPHIN_MODERN,
                0,
                first.copy(id = 99, fileName = "renamed.iso", customName = "My cartridge"),
            ),
        )
        assertFalse(key!!.contains(first.fileUri))
        assertNull(ControllerConfigsManager.cartridgeKey(SystemID.GAMECUBE, CoreID.DOLPHIN_MODERN, 0, first))
        assertNull(ControllerConfigsManager.cartridgeKey(SystemID.WII, CoreID.DOLPHIN_MODERN, 0, null))
    }

    private fun preferences(data: MutableMap<String, String>): SharedPreferences {
        val editor =
            Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
            ) { proxy, method, args ->
                when (method.name) {
                    "putString" -> {
                        data[args[0] as String] = args[1] as String
                        proxy
                    }
                    "commit" -> true
                    else -> error("Unexpected editor operation: ${method.name}")
                }
            } as SharedPreferences.Editor
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getString" -> data[args[0]] ?: args[1]
                "edit" -> editor
                else -> error("Unexpected preference operation: ${method.name}")
            }
        } as SharedPreferences
    }
}
