package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.io.File

/**
 * User-controlled, opt-in compatibility engine selection. The legacy core stays
 * selected unless the player explicitly requests a packaged, verified alternative.
 *
 * The engine selection is independent of the cartridge's library/database identity.
 */
class EmulatorCompatibilityPreferences(private val context: Context) {
    private val preferences = context.getSharedPreferences("cartdeck_engine_compatibility_v1", Context.MODE_PRIVATE)

    fun candidateCore(systemId: SystemID): CoreID? =
        when (systemId) {
            SystemID.PSX -> CoreID.PCSX_REARMED_MODERN
            SystemID.PSP -> CoreID.PPSSPP_MODERN
            else -> null
        }

    fun available(systemId: SystemID): Boolean {
        val candidate = candidateCore(systemId) ?: return false
        if (!Build.SUPPORTED_ABIS.contains("arm64-v8a")) return false
        val binary = File(context.applicationInfo.nativeLibraryDir, candidate.libretroFileName)
        if (!binary.isFile || binary.length() <= 0) return false
        // The updated PSP core must never run against the inherited 1.15 support assets.
        if (systemId == SystemID.PSP) {
            if (!EmulatorModernAssetsProvisioner.isInstalled(context)) return false
        }
        return true
    }

    fun isUpdatedRequested(systemId: SystemID, gameId: Int): Boolean {
        val perGameKey = gamePreferenceKey(systemId, gameId)
        val requested =
            if (preferences.contains(perGameKey)) {
                preferences.getBoolean(perGameKey, false)
            } else {
                preferences.getBoolean(systemPreferenceKey(systemId), false)
            }
        return requested && available(systemId)
    }

    fun isUpdatedSelectedForGame(systemId: SystemID, gameId: Int): Boolean =
        isUpdatedRequested(systemId, gameId)

    fun isSystemUpdatedSelected(systemId: SystemID): Boolean =
        preferences.getBoolean(systemPreferenceKey(systemId), false) && available(systemId)

    fun isGameOverrideSet(systemId: SystemID, gameId: Int): Boolean =
        preferences.contains(gamePreferenceKey(systemId, gameId))

    fun setSystemEngine(systemId: SystemID, updated: Boolean): Boolean {
        if (candidateCore(systemId) == null || (updated && !available(systemId))) return false
        return preferences.edit().putBoolean(systemPreferenceKey(systemId), updated).commit()
    }

    fun setGameEngine(systemId: SystemID, gameId: Int, updated: Boolean): Boolean {
        if (candidateCore(systemId) == null || (updated && !available(systemId))) return false
        return preferences.edit().putBoolean(gamePreferenceKey(systemId, gameId), updated).commit()
    }

    fun restoreSystemDefault(systemId: SystemID): Boolean =
        preferences.edit().remove(systemPreferenceKey(systemId)).commit()

    fun restoreGameDefault(systemId: SystemID, gameId: Int): Boolean =
        preferences.edit().remove(gamePreferenceKey(systemId, gameId)).commit()

    /**
     * Called only for an explicit native core startup/load failure, not for
     * slow gameplay or titles which take time to load.
     *
     * Does not retry automatically. The next manual launch uses the old core.
     */
    fun recordConfirmedStartupFailure(game: Game): Boolean {
        if (!isUpdatedRequested(game.systemId, game.id)) return false
        return preferences.edit()
            .putBoolean(gamePreferenceKey(game.systemId, game.id), false)
            .putString(failureKey(game.systemId, game.id), candidateCore(game.systemId)?.coreName)
            .commit()
    }

    fun lastFailedCandidate(systemId: SystemID, gameId: Int): String? =
        preferences.getString(failureKey(systemId, gameId), null)

    private fun systemPreferenceKey(systemId: SystemID) = "system_${systemId.dbname}"
    private fun gamePreferenceKey(systemId: SystemID, gameId: Int) = "game_${systemId.dbname}_$gameId"
    private fun failureKey(systemId: SystemID, gameId: Int) = "failure_${systemId.dbname}_$gameId"
}
