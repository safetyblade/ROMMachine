package com.swordfish.lemuroid.app.mobile.feature.gamemenu

/**
 * Host-specific capabilities. A callable pause API is not evidence of an
 * acknowledged paused guest. Save-state support is never inferred from console.
 */
internal enum class GameplayMenuHostKind {
    LIBRETRO,
    XBOX_SDL,
    XENIA,
    ARMSX2,
    CEMU,
}

internal object GameplayMenuHostProfiles {
    fun capabilities(
        host: GameplayMenuHostKind,
        libretroStatesSupported: Boolean = false,
    ): GameplayMenuHostCapabilities =
        when (host) {
            GameplayMenuHostKind.LIBRETRO ->
                GameplayMenuHostCapabilities(
                    canSaveState = libretroStatesSupported,
                    canLoadState = libretroStatesSupported,
                    saveMethodDescription =
                        "Use the game's own saves. Emulator snapshots are separate when this core supports them.",
                )
            GameplayMenuHostKind.XBOX_SDL ->
                GameplayMenuHostCapabilities(
                    canPause = true,
                    saveMethodDescription = "Save inside the Xbox game to its persistent virtual HDD.",
                )
            GameplayMenuHostKind.XENIA ->
                GameplayMenuHostCapabilities(
                    canPause = true,
                    saveMethodDescription = "Save inside the Xbox 360 game using its offline profile.",
                )
            GameplayMenuHostKind.ARMSX2 ->
                GameplayMenuHostCapabilities(
                    canPause = true,
                    saveMethodDescription = "Save inside the PS2 game to its existing memory card.",
                )
            GameplayMenuHostKind.CEMU ->
                GameplayMenuHostCapabilities(
                    canPause = true,
                    saveMethodDescription = "Save inside the Wii U game to its existing NAND.",
                )
        }
}

/**
 * Calls ONLY the existing host's synchronous lifecycle entry points from the
 * same caller/thread as before. The host owns JNI, SDL and Vulkan lifetimes.
 * A returned Unit has NO guest pause/resume acknowledgement, so return Submitted.
 */
internal class GameplayMenuHostAdapter(
    override val capabilities: GameplayMenuHostCapabilities,
    private val pause: () -> Unit,
    private val resume: () -> Unit,
) : GameplayMenuCommandHost {
    fun dispatch(command: GameplayMenuCommand): GameplayMenuCommandResult {
        if (command != GameplayMenuCommand.PAUSE && command != GameplayMenuCommand.RESUME) {
            return GameplayMenuCommandResult.Unsupported(
                "This action remains owned by the active game's save/exit pathway.",
            )
        }
        if (!capabilities.canPause) {
            return GameplayMenuCommandResult.Unsupported("The host does not expose a pause command.")
        }
        return try {
            if (command == GameplayMenuCommand.PAUSE) pause() else resume()
            GameplayMenuCommandResult.Submitted(
                "Command delivered to the active engine; completion has not been confirmed.",
            )
        } catch (error: Exception) {
            GameplayMenuCommandResult.Failed(error.message ?: "Engine lifecycle command failed.")
        }
    }

    override suspend fun execute(command: GameplayMenuCommand): GameplayMenuCommandResult =
        dispatch(command)
}

internal fun GameplayMenuCommandResult.menuFeedback(): String =
    when (this) {
        GameplayMenuCommandResult.Completed -> "Game paused."
        is GameplayMenuCommandResult.Submitted ->
            "Menu open. Engine pause is requested but not confirmed."
        is GameplayMenuCommandResult.Unsupported ->
            "Menu open. This engine may continue running."
        is GameplayMenuCommandResult.Failed ->
            "Menu open. Engine pause failed; gameplay may continue: $reason"
    }

/** Failed native resume never silently dismisses the menu. */
internal fun GameplayMenuCommandResult.resumeFailureFeedback(): String? =
    when (this) {
        is GameplayMenuCommandResult.Failed ->
            "Resume failed. Retry or return to the shelf; the current game was not restarted."
        is GameplayMenuCommandResult.Unsupported ->
            "This engine cannot resume the session. Return to the shelf or choose recovery."
        else -> null
    }
