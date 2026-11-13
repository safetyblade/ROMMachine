package com.swordfish.lemuroid.app.mobile.feature.gamemenu

/** Presentation capabilities only; emulator ownership and actions stay with the host. */
data class GameMenuCapabilities(
    val controls: Boolean = false,
    val states: Boolean = false,
    val settings: Boolean = false,
    val advanced: Boolean = false,
) {
    fun homeActions(): List<GameMenuAction> =
        buildList {
            add(GameMenuAction.RESUME)
            if (controls) add(GameMenuAction.CONTROLS)
            if (states) add(GameMenuAction.STATES)
            if (settings) add(GameMenuAction.SETTINGS)
            if (advanced) add(GameMenuAction.ADVANCED)
            add(GameMenuAction.RETURN_TO_SHELF)
        }
}

enum class GameMenuAction(val label: String) {
    RESUME("Resume"),
    CONTROLS("Controls"),
    STATES("Save / Load"),
    SETTINGS("Game Settings"),
    ADVANCED("Advanced"),
    RETURN_TO_SHELF("Return to Shelf"),
}

/** Deduplicate requests from menu and Activity lifecycle without unpausing in the background. */
internal class GameMenuPauseGate {
    private var sent: Boolean? = null

    fun transition(
        foreground: Boolean,
        menuOpen: Boolean,
        recoveryOpen: Boolean,
        leaving: Boolean,
    ): Boolean? {
        val paused = !foreground || menuOpen || recoveryOpen || leaving
        if (paused == sent) return null
        sent = paused
        return paused
    }
}

internal data class XboxMenuCapabilities(val debug: Boolean, val compatibleProbe: Boolean, val rollback: Boolean) {
    val showProbe get() = debug && compatibleProbe
    val home get() = GameMenuCapabilities(controls = true, advanced = true)
}
