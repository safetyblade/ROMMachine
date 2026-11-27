package com.swordfish.lemuroid.app.mobile.feature.gamemenu

/** Presentation capabilities only; emulator ownership and actions stay with the host. */
data class GameMenuCapabilities(
    val controls: Boolean = false,
    val states: Boolean = false,
    val settings: Boolean = false,
    val advanced: Boolean = false,
    val resume: Boolean = true,
    val display: Boolean = true,
    val reporting: Boolean = false,
) {
    fun homeActions(): List<GameMenuAction> =
        buildList {
            if (resume) add(GameMenuAction.RESUME)
            if (states) add(GameMenuAction.STATES)
            if (controls) add(GameMenuAction.CONTROLS)
            if (display) add(GameMenuAction.DISPLAY)
            if (settings) add(GameMenuAction.SETTINGS)
            add(GameMenuAction.RETURN_TO_SHELF)
            if (reporting) add(GameMenuAction.REPORT_PROBLEM)
            if (advanced) add(GameMenuAction.ADVANCED)
        }
}

enum class GameMenuAction(val label: String) {
    RESUME("Resume"),
    STATES("Save & Load"),
    CONTROLS("Controls"),
    DISPLAY("Display"),
    SETTINGS("Game Settings"),
    RETURN_TO_SHELF("Return to Shelf"),
    REPORT_PROBLEM("Report a Problem"),
    ADVANCED("Advanced"),
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

    /** A failed resume leaves the engine potentially paused. Permit retry. */
    fun retryResumeAfterFailure() {
        if (sent == false) sent = true
    }
}

internal data class XboxMenuCapabilities(
    val debug: Boolean,
    val compatibleProbe: Boolean,
    val rollback: Boolean,
    val resume: Boolean = true,
) {
    val showProbe get() = debug && compatibleProbe
    val home get() = GameMenuCapabilities(controls = true, states = true, advanced = true, resume = resume)
}
