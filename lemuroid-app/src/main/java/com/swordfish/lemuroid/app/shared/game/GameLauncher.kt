package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.widget.Toast
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.core.NativeCoreProvisioner
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ): Boolean {
        if (GameProcessLock.isHeldByAnotherProcess(activity.applicationContext)) {
            activity.displayToast(R.string.game_process_another_game_running)
            return false
        }

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val system = GameSystem.findById(game.systemId)
                if (system.nativeBackend == "hakux") {
                    if (com.swordfish.lemuroid.app.shared.game.xbox.XboxGameLauncher.launch(activity, game, leanback)) {
                        gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                    }
                    return@launch
                }
                val legacyCoreConfig = coresSelection.getCoreConfigForSystem(system)
                val compatibility = EmulatorCompatibilityPreferences(activity.applicationContext)
                val candidate = compatibility.candidateCore(system.id)
                val selectedCoreConfig =
                    if (candidate != null && compatibility.isUpdatedRequested(system.id, game.id)) {
                        // A distinct CoreID keeps save-states isolated. Other emulators are untouched.
                        PSPModernCoreConfig.configure(
                            PlayStationModernCoreConfig.configure(
                                legacyCoreConfig.copy(
                                    coreID = candidate,
                                    statesVersion = legacyCoreConfig.statesVersion + 1,
                                ),
                            ),
                        )
                    } else {
                        legacyCoreConfig
                    }
                val coreConfig =
                    if (
                        selectedCoreConfig.coreID in
                        setOf(
                            CoreID.FLYCAST_MODERN,
                            CoreID.DOLPHIN_MODERN,
                            CoreID.PLAY_MODERN,
                        )
                    ) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                selectedCoreConfig.copy(
                                    statesVersion =
                                        NativeCoreProvisioner.stateVersion(
                                            activity.applicationContext,
                                            selectedCoreConfig.coreID,
                                        ),
                                )
                            }.getOrElse { selectedCoreConfig.copy(statesSupported = false) }
                        }
                    } else {
                        selectedCoreConfig
                    }
                gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                BaseGameActivity.launchGame(activity, coreConfig, game, loadSave, leanback)
            } catch (error: Exception) {
                // A failure before GameActivity starts was previously lost in an
                // unhandled launch coroutine. Show a safe diagnostic without
                // exposing game names, ROM paths or ContentResolver URIs.
                Timber.e(error, "CARTDECK game launch setup failed")
                if (!activity.isFinishing && !activity.isDestroyed) {
                    Toast.makeText(
                        activity,
                        "CARTDECK could not start this game (${error.javaClass.simpleName}).",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

        return true
    }
}
