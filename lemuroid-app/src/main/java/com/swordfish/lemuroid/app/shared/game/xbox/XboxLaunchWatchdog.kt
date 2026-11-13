package com.swordfish.lemuroid.app.shared.game.xbox

/** Counts foreground startup time only; a Surface or guest-generated frame alone is not presentation. */
class XboxLaunchWatchdog(private val timeoutMs: Long = 90000L) {
    var presented = false
        private set
    var stage = "NATIVE_INITIALIZATION"
        private set
    private var elapsedMs = 0L

    fun poll(
        activeElapsedMs: Long,
        bootState: Int,
        guestFramePresented: Boolean,
        legacyFrameAcquired: Boolean = false,
    ): String? {
        presented = presented || guestFramePresented
        stage =
            when {
                bootState < 0 -> "CORE_STOPPED"
                presented -> "FIRST_GUEST_FRAME_PRESENTED"
                legacyFrameAcquired -> "LEGACY_PRESENTATION_UNVERIFIED"
                bootState > 0 -> "CORE_RUNNING_AWAITING_PRESENT"
                else -> "NATIVE_INITIALIZATION"
            }
        if (bootState < 0) return "The Xbox core stopped. Copy the engine/GPU report before retrying."
        if (presented || legacyFrameAcquired) return null
        elapsedMs += activeElapsedMs.coerceAtLeast(0L)
        return if (elapsedMs >= timeoutMs) {
            "No guest frame reached the display within ${timeoutMs / 1000} foreground seconds ($stage). " +
                "Copy the engine/GPU report or select a retained engine before retrying."
        } else {
            null
        }
    }
}
