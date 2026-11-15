package com.swordfish.lemuroid.app.shared.game.wiiu

/** Renderer readiness releases the opaque loader; only callbacks establish frame submission. */
data class WiiuLaunchStatus(val rendererReady: Boolean, val frames: Long, val error: String) {
    val showLoadingScreen: Boolean get() = !rendererReady && frames == 0L && error.isEmpty()

    companion object {
        fun parse(report: String): WiiuLaunchStatus {
            val fields =
                report.lineSequence().mapNotNull { line ->
                    val separator = line.indexOf('=')
                    if (separator < 1) null else line.substring(0, separator) to line.substring(separator + 1)
                }.toMap()
            return WiiuLaunchStatus(
                fields["renderer_ready"] == "1" || fields["host_stage"] == "3",
                fields["presented_image_callbacks"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                fields["native_error"].orEmpty().take(160),
            )
        }
    }
}
