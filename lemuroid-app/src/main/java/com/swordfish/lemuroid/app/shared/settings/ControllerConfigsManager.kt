package com.swordfish.lemuroid.app.shared.settings

import android.content.SharedPreferences
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class ControllerConfigsManager(private val sharedPreferences: Lazy<SharedPreferences>) {
    suspend fun getControllerConfigs(
        systemId: SystemID,
        systemCoreConfig: SystemCoreConfig,
        game: Game? = null,
    ): Map<Int, ControllerConfig> =
        withContext(Dispatchers.IO) {
            systemCoreConfig.controllerConfigs.entries
                .associate { (port, controllers) ->
                    val preferences = sharedPreferences.get()
                    val cartridgeKey = cartridgeKey(systemId, systemCoreConfig.coreID, port, game)
                    val legacySource = game?.legacySourceUri ?: game?.fileUri
                    val names =
                        listOfNotNull(
                            cartridgeKey?.let { preferences.getString(it, null) },
                            legacySource?.let {
                                legacyCartridgeKey(systemId, systemCoreConfig.coreID, port, game, it)
                            }?.let { preferences.getString(it, null) },
                            game?.fileUri?.let {
                                legacyCartridgeKey(systemId, systemCoreConfig.coreID, port, game, it)
                            }?.let { preferences.getString(it, null) },
                            preferences.getString(
                                getSharedPreferencesId(systemId.dbname, systemCoreConfig.coreID, port),
                                null,
                            ),
                        )
                    val currentController =
                        names.firstNotNullOfOrNull { name ->
                            controllers.firstOrNull { it.name == name }
                        } ?: controllers.first()

                    port to currentController
                }
        }

    /**
     * Persist an explicitly selected controller; do not rewrite the other ports,
     * saves, engine preferences or any user's previous choice.
     */
    suspend fun selectController(
        systemId: SystemID,
        systemCoreConfig: SystemCoreConfig,
        port: Int,
        name: String,
        game: Game? = null,
    ): Boolean =
        withContext(Dispatchers.IO) {
            if ((systemId == SystemID.WII && game != null && game.systemId != systemId.dbname) ||
                systemCoreConfig.controllerConfigs[port]?.none { it.name == name } != false
            ) {
                return@withContext false
            }
            val key =
                cartridgeKey(systemId, systemCoreConfig.coreID, port, game)
                    ?: getSharedPreferencesId(systemId.dbname, systemCoreConfig.coreID, port)
            sharedPreferences.get().edit()
                .putString(key, name)
                .commit()
        }

    companion object {
        /** Stable cartridge UUID: verified source relocation keeps its controller choice. */
        fun cartridgeKey(
            systemId: SystemID,
            coreID: CoreID,
            port: Int,
            game: Game?,
        ): String? =
            game?.takeIf { systemId == SystemID.WII && it.systemId == systemId.dbname }
                ?.let { hashedCartridgeKey(systemId, coreID, port, "${it.systemId}\n${it.cartridgeId}") }

        /** Read older URI-keyed selections without mutating the player's preferences. */
        fun legacyCartridgeKey(
            systemId: SystemID,
            coreID: CoreID,
            port: Int,
            game: Game?,
            source: String,
        ): String? =
            game?.takeIf { systemId == SystemID.WII && it.systemId == systemId.dbname }
                ?.let { hashedCartridgeKey(systemId, coreID, port, "${it.systemId}\n$source") }

        private fun hashedCartridgeKey(
            systemId: SystemID,
            coreID: CoreID,
            port: Int,
            identity: String,
        ): String {
            val digest =
                MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it.toInt() and 255) }
            return getSharedPreferencesId(systemId.dbname, coreID, port) + "_cartridge_" + digest
        }

        fun getSharedPreferencesId(
            systemId: String,
            coreID: CoreID,
            port: Int,
        ) = "pref_key_controller_type_${systemId}_${coreID.coreName}_$port"
    }
}
