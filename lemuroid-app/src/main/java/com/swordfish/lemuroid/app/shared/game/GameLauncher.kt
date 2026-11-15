package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.game.engine.EngineSelectionPreferences
import com.swordfish.lemuroid.app.shared.game.engine.NativeEngineRegistry
import com.swordfish.lemuroid.app.shared.game.wiiu.WiiuEngine
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.core.NativeCoreProvisioner
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
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
    /** Reopen only after the isolated runtime has released the shared game lock. */
    suspend fun reopenNativeGame(
        activity: Activity,
        data: Intent?,
    ): Boolean {
        if (data?.getBooleanExtra("cartdeck.reopen", false) != true) return false
        val game = data.getSerializableExtra(BaseGameActivity.PLAY_GAME_RESULT_GAME) as? Game ?: return false
        // Android caches SharedPreferences per process. Apply the recovery choice
        // in the shelf process before asking it to select the next engine.
        if (!applyRecoveryChoice(activity.applicationContext, data, game)) {
            gameLaunchTaskHandler.handleGameFinish(
                false,
                activity,
                BaseGameActivity.RESULT_ERROR,
                Intent().putExtra(
                    BaseGameActivity.PLAY_GAME_RESULT_ERROR,
                    "Could not select the recovery engine. Saves were retained.",
                ),
            )
            return true
        }
        var attempts = 0
        while (GameProcessLock.isHeldByAnotherProcess(activity) && attempts++ < 100) {
            kotlinx.coroutines.delay(100)
        }
        if (!launchGameAsync(
                activity,
                game,
                false,
                data.getBooleanExtra(BaseGameActivity.PLAY_GAME_RESULT_LEANBACK, false),
            )
        ) {
            gameLaunchTaskHandler.handleGameFinish(
                false,
                activity,
                BaseGameActivity.RESULT_ERROR,
                Intent().putExtra(
                    BaseGameActivity.PLAY_GAME_RESULT_ERROR,
                    "The previous game process has not stopped. Return to Shelf and retry.",
                ),
            )
        }
        return true
    }

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
                // The Wii U catalogue is independently searchable before Cemu's
                // Vulkan host passes physical gameplay QA. Never try to launch
                // a catalogue-only cartridge via the existing GLES frontend.
                if (system.id == SystemID.WII_U &&
                    WiiuEngine.engineeringBuild
                ) {
                    val engine = NativeEngineRegistry.engines(system).single()
                    val eligibility = NativeEngineRegistry.eligibility(activity, engine)
                    check(eligibility.eligible) { eligibility.reason ?: "Cemu is unavailable" }
                    EngineSelectionPreferences(activity.applicationContext).beginLaunch(game, engine)
                    activity.startActivityForResult(
                        Intent().setClassName(
                            activity,
                            "com.swordfish.lemuroid.app.debug.WiiuVulkanRuntimeProbeActivity",
                        )
                            .putExtra("cartdeck.game", game)
                            .putExtra("cartdeck.leanback", leanback)
                            .putExtra("cartdeck.startTime", android.os.SystemClock.elapsedRealtime()),
                        BaseGameActivity.REQUEST_PLAY_GAME,
                    )
                    gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                    return@launch
                }
                if (system.id == SystemID.WII_U) {
                    Toast.makeText(
                        activity,
                        "Wii U cartridges are indexed. The in-app Vulkan engine is not yet approved for gameplay.",
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }
                val screenClass =
                    CartdeckScreenPolicy.screenClass(
                        isTvHardware =
                            activity.packageManager.hasSystemFeature("android.hardware.type.television") ||
                                activity.packageManager.hasSystemFeature("android.software.leanback"),
                        explicitTvMode = leanback,
                        smallestWidthDp = activity.resources.configuration.smallestScreenWidthDp,
                    )
                val landscape = activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                if (
                    CartdeckScreenPolicy.eligibility(system.id, screenClass, landscape) ==
                    CartdeckScreenEligibility.REQUIRES_PORTRAIT_DUAL_SCREEN
                ) {
                    Toast.makeText(
                        activity,
                        "DS dual-screen games require portrait handheld/tablet mode. " +
                            "They are unavailable in TV and landscape modes.",
                        Toast.LENGTH_LONG,
                    ).show()
                    return@launch
                }
                // Asset readiness must not depend on opening the optional Setup UI.
                if (system.id in setOf(SystemID.PSX, SystemID.PSP)) {
                    EmulatorModernAssetsProvisioner.installIfBundled(activity.applicationContext)
                }
                val enginePreferences = EngineSelectionPreferences(activity.applicationContext)
                val legacyCoreConfig =
                    if (system.nativeBackend == null) {
                        coresSelection.getCoreConfigForSystem(
                            system,
                        )
                    } else {
                        null
                    }
                val selection = withContext(Dispatchers.IO) { enginePreferences.select(game, legacyCoreConfig) }
                if (system.id == SystemID.PLAYSTATION_3) {
                    enginePreferences.beginLaunch(game, selection.engine)
                    activity.startActivityForResult(
                        Intent().setClassName(activity, com.swordfish.lemuroid.app.shared.game.ps3.Ps3Engine.ACTIVITY)
                            .putExtra("cartdeck.game", game).putExtra("cartdeck.engine", selection.engine.id)
                            .putExtra("cartdeck.leanback", leanback),
                        BaseGameActivity.REQUEST_PLAY_GAME,
                    )
                    gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                    return@launch
                }
                if (selection.engine.host == NativeEngineRegistry.Host.ARMSX2_ANDROID) {
                    enginePreferences.beginLaunch(game, selection.engine)
                    activity.startActivityForResult(
                        Intent().setClassName(activity, "com.swordfish.lemuroid.app.debug.Armsx2RuntimeProbeActivity")
                            .putExtra("cartdeck.game", game).putExtra("cartdeck.leanback", leanback)
                            .putExtra("cartdeck.startTime", android.os.SystemClock.elapsedRealtime()),
                        BaseGameActivity.REQUEST_PLAY_GAME,
                    )
                    gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                    return@launch
                }
                if (system.nativeBackend == "hakux") {
                    if (com.swordfish.lemuroid.app.shared.game.xbox.XboxGameLauncher.launch(activity, game, leanback)) {
                        runCatching { enginePreferences.beginLaunch(game, selection.engine) }
                        gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                    }
                    return@launch
                }
                if (system.nativeBackend == "xenia") {
                    com.swordfish.lemuroid.app.shared.game.xbox360.Xbox360GameLauncher.launch(activity, game, leanback)
                    enginePreferences.beginLaunch(game, selection.engine)
                    gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                    return@launch
                }
                val selectedCoreConfig =
                    PSPModernCoreConfig.configure(
                        PlayStationModernCoreConfig.configure(
                            requireNotNull(selection.engine.config),
                        ),
                    )
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
                            }.getOrThrow()
                        }
                    } else {
                        selectedCoreConfig
                    }
                enginePreferences.beginLaunch(game, selection.engine)
                gameLaunchTaskHandler.handleGameStart(activity.applicationContext)
                BaseGameActivity.launchGame(activity, coreConfig, game, loadSave && !selection.changedEngine, leanback)
            } catch (error: Exception) {
                // A failure before GameActivity starts was previously lost in an
                // unhandled launch coroutine. Show a safe diagnostic without
                // exposing game names, ROM paths or ContentResolver URIs.
                Timber.e(error, "CARTDECK game launch setup failed")
                if (!activity.isFinishing && !activity.isDestroyed) {
                    Toast.makeText(
                        activity,
                        if (error is EngineSelectionPreferences.SelectionException) {
                            error.message
                        } else {
                            "CARTDECK could not start this game (${error.javaClass.simpleName})."
                        },
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
        }

        return true
    }

    companion object {
        internal fun applyRecoveryChoice(
            context: Context,
            data: Intent,
            game: Game,
        ): Boolean {
            val engine = data.getStringExtra("cartdeck.reopen.engine") ?: return true
            return EngineSelectionPreferences(context).setPreferred(GameSystem.findById(game.systemId), engine, game)
        }
    }
}
