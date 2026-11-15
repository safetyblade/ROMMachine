package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CARTDECK owns presentation; the active emulator host owns lifecycle commands.
 * Host implementations must acknowledge a real engine result, not merely dispatch.
 * This contract never assumes that opening a menu pauses a native engine.
 */
internal enum class GameplayMenuCommand {
    PAUSE,
    RESUME,
    SAVE_STATE,
    LOAD_STATE,
    EXIT,
}

internal data class GameplayMenuHostCapabilities(
    val canPause: Boolean = false,
    val canSaveState: Boolean = false,
    val canLoadState: Boolean = false,
    val canExit: Boolean = true,
    val saveMethodDescription: String = "Save through the game itself.",
)

internal sealed interface GameplayMenuCommandResult {
    data object Completed : GameplayMenuCommandResult

    data class Unsupported(val reason: String) : GameplayMenuCommandResult

    data class Failed(val reason: String) : GameplayMenuCommandResult
}

/** Execute on the host's own engine/SDL/Vulkan thread where required. */
internal interface GameplayMenuCommandHost {
    val capabilities: GameplayMenuHostCapabilities

    suspend fun execute(command: GameplayMenuCommand): GameplayMenuCommandResult
}

internal enum class GameplayMenuSessionStatus {
    CLOSED,
    OPENING,
    PAUSED,
    OPEN_RUNNING,
    RESUMING,
    ERROR,
}

internal data class GameplayMenuSessionSnapshot(
    val status: GameplayMenuSessionStatus = GameplayMenuSessionStatus.CLOSED,
    val message: String? = null,
    val lastCommand: GameplayMenuCommand? = null,
)

/**
 * Serialises menu lifecycle requests and exposes only host-acknowledged states.
 * Do not use this as another input dispatcher or an emulator lifecycle owner.
 */
internal class GameplayMenuSession(private val host: GameplayMenuCommandHost) {
    private val mutex = Mutex()

    var snapshot: GameplayMenuSessionSnapshot = GameplayMenuSessionSnapshot()
        private set

    suspend fun open(): GameplayMenuSessionSnapshot =
        mutex.withLock {
            if (snapshot.status != GameplayMenuSessionStatus.CLOSED) return@withLock snapshot
            snapshot = GameplayMenuSessionSnapshot(GameplayMenuSessionStatus.OPENING)
            if (!host.capabilities.canPause) {
                snapshot =
                    GameplayMenuSessionSnapshot(
                        GameplayMenuSessionStatus.OPEN_RUNNING,
                        "Menu open. This engine may continue running.",
                        GameplayMenuCommand.PAUSE,
                    )
            } else {
                snapshot =
                    when (val result = executeSafely(GameplayMenuCommand.PAUSE)) {
                        GameplayMenuCommandResult.Completed ->
                            GameplayMenuSessionSnapshot(
                                GameplayMenuSessionStatus.PAUSED,
                                lastCommand = GameplayMenuCommand.PAUSE,
                            )
                        is GameplayMenuCommandResult.Unsupported ->
                            GameplayMenuSessionSnapshot(
                                GameplayMenuSessionStatus.OPEN_RUNNING,
                                result.reason,
                                GameplayMenuCommand.PAUSE,
                            )
                        is GameplayMenuCommandResult.Failed ->
                            GameplayMenuSessionSnapshot(
                                GameplayMenuSessionStatus.OPEN_RUNNING,
                                "Pause failed: ${result.reason}. Gameplay may continue.",
                                GameplayMenuCommand.PAUSE,
                            )
                    }
            }
            snapshot
        }

    suspend fun resume(): GameplayMenuSessionSnapshot =
        mutex.withLock {
            when (snapshot.status) {
                GameplayMenuSessionStatus.CLOSED -> return@withLock snapshot
                GameplayMenuSessionStatus.OPEN_RUNNING -> {
                    snapshot = GameplayMenuSessionSnapshot()
                    return@withLock snapshot
                }
                GameplayMenuSessionStatus.PAUSED -> Unit
                else -> return@withLock snapshot
            }
            snapshot = snapshot.copy(status = GameplayMenuSessionStatus.RESUMING)
            snapshot =
                when (val result = executeSafely(GameplayMenuCommand.RESUME)) {
                    GameplayMenuCommandResult.Completed -> GameplayMenuSessionSnapshot()
                    is GameplayMenuCommandResult.Unsupported ->
                        GameplayMenuSessionSnapshot(
                            GameplayMenuSessionStatus.ERROR,
                            "Resume unsupported: ${result.reason}",
                            GameplayMenuCommand.RESUME,
                        )
                    is GameplayMenuCommandResult.Failed ->
                        GameplayMenuSessionSnapshot(
                            GameplayMenuSessionStatus.ERROR,
                            "Resume failed: ${result.reason}",
                            GameplayMenuCommand.RESUME,
                        )
                }
            snapshot
        }

    suspend fun request(command: GameplayMenuCommand): GameplayMenuCommandResult =
        mutex.withLock {
            if (command == GameplayMenuCommand.PAUSE || command == GameplayMenuCommand.RESUME) {
                return@withLock GameplayMenuCommandResult.Unsupported("Use menu open/resume lifecycle.")
            }
            if (snapshot.status != GameplayMenuSessionStatus.PAUSED &&
                snapshot.status != GameplayMenuSessionStatus.OPEN_RUNNING
            ) {
                return@withLock GameplayMenuCommandResult.Unsupported("No active menu session.")
            }
            val supported =
                when (command) {
                    GameplayMenuCommand.SAVE_STATE -> host.capabilities.canSaveState
                    GameplayMenuCommand.LOAD_STATE -> host.capabilities.canLoadState
                    GameplayMenuCommand.EXIT -> host.capabilities.canExit
                    else -> false
                }
            if (!supported) {
                return@withLock GameplayMenuCommandResult.Unsupported(
                    if (command == GameplayMenuCommand.SAVE_STATE ||
                        command == GameplayMenuCommand.LOAD_STATE
                    ) {
                        host.capabilities.saveMethodDescription
                    } else {
                        "This action is unavailable for this engine."
                    },
                )
            }
            val result = executeSafely(command)
            if (command == GameplayMenuCommand.EXIT && result == GameplayMenuCommandResult.Completed) {
                snapshot = GameplayMenuSessionSnapshot()
            }
            result
        }

    private suspend fun executeSafely(command: GameplayMenuCommand): GameplayMenuCommandResult =
        try {
            host.execute(command)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            GameplayMenuCommandResult.Failed(error.message ?: "Engine command failed.")
        }
}
