package com.swordfish.lemuroid.lib.core

import android.content.SharedPreferences
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import dagger.Lazy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.InvalidParameterException

class CoreVariablesManager(private val sharedPreferences: Lazy<SharedPreferences>) {
    suspend fun getOptionsForCore(
        systemID: SystemID,
        systemCoreConfig: SystemCoreConfig,
        game: Game? = null,
    ): List<CoreVariable> {
        val defaultMap = convertCoreVariablesToMap(systemCoreConfig.defaultSettings)
        val coreVariables = retrieveCustomCoreVariables(systemID, systemCoreConfig)
        val coreVariablesMap = defaultMap + convertCoreVariablesToMap(coreVariables)
        val pointer =
            if (systemID == SystemID.WII && game != null && WII_POINTER in coreVariablesMap) {
                wiiPointerMode(game)
            } else {
                null
            }
        return convertMapToCoreVariables(
            if (pointer == null) coreVariablesMap else coreVariablesMap + (WII_POINTER to pointer),
        )
    }

    fun wiiPointerMode(game: Game): String? =
        sharedPreferences.get().getString(wiiPointerPreferenceKey(game), null)?.takeIf { it in WII_POINTER_MODES }

    fun defaultWiiPointerMode(): String =
        sharedPreferences.get()
            .getString(computeSharedPreferenceKey(WII_POINTER, SystemID.WII.dbname), null)
            ?.takeIf { it in WII_POINTER_MODES } ?: "2"

    fun setWiiPointerMode(
        game: Game,
        mode: String,
    ) {
        require(game.systemId == SystemID.WII.dbname && mode in WII_POINTER_MODES)
        sharedPreferences.get().edit().putString(wiiPointerPreferenceKey(game), mode).apply()
    }

    private fun wiiPointerPreferenceKey(game: Game): String {
        val identity = game.cartridgeId.ifEmpty { game.fileUri }
        return "${computeSharedPreferenceKey(WII_POINTER, SystemID.WII.dbname)}_$identity"
    }

    private fun convertMapToCoreVariables(variablesMap: Map<String, String>): List<CoreVariable> {
        return variablesMap.entries.map { CoreVariable(it.key, it.value) }
    }

    private fun convertCoreVariablesToMap(coreVariables: List<CoreVariable>): Map<String, String> {
        return coreVariables.associate { it.key to it.value }
    }

    private suspend fun retrieveCustomCoreVariables(
        systemID: SystemID,
        systemCoreConfig: SystemCoreConfig,
    ): List<CoreVariable> =
        withContext(Dispatchers.IO) {
            val exposedKeys = systemCoreConfig.exposedSettings
            val exposedAdvancedKeys = systemCoreConfig.exposedAdvancedSettings

            val requestedKeys =
                (exposedKeys + exposedAdvancedKeys).map { it.key }
                    .map { computeSharedPreferenceKey(it, systemID.dbname) }

            sharedPreferences.get().all.filter { it.key in requestedKeys }
                .map { (key, value) ->
                    val result =
                        when (value!!) {
                            is Boolean -> if (value as Boolean) "enabled" else "disabled"
                            is String -> value as String
                            else -> throw InvalidParameterException("Invalid setting in SharedPreferences")
                        }
                    CoreVariable(computeOriginalKey(key, systemID.dbname), result)
                }
        }

    companion object {
        const val WII_POINTER = "dolphin_ir_mode"
        private val WII_POINTER_MODES = setOf("2", "1", "0")
        private const val RETRO_OPTION_PREFIX = "cv"

        fun computeSharedPreferenceKey(
            retroVariableName: String,
            systemID: String,
        ): String {
            return "${computeSharedPreferencesPrefix(systemID)}$retroVariableName"
        }

        fun computeOriginalKey(
            sharedPreferencesKey: String,
            systemID: String,
        ): String {
            return sharedPreferencesKey.replace(computeSharedPreferencesPrefix(systemID), "")
        }

        private fun computeSharedPreferencesPrefix(systemID: String): String {
            return "${RETRO_OPTION_PREFIX}_${systemID}_"
        }
    }
}
