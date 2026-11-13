package com.swordfish.lemuroid.app.shared.game.xbox

/** Joins existing asynchronous native and SurfaceView observations; never judges gameplay. */
class XboxPixelProbeCapture(private val budgetMillis: Long = 6000) {
    enum class Surface { PENDING, SAMPLED, UNAVAILABLE }

    enum class Status { COMPLETE_SAMPLES, INCOMPLETE_SAMPLES, CAPTURE_TIMEOUT }

    data class Result(val status: Status, val nativePairs: Int)

    private var surface = Surface.PENDING
    private var finished = false

    /** First terminal SurfaceView result wins; a late PixelCopy callback cannot replace a timeout. */
    fun surfaceResult(sampled: Boolean): Boolean {
        if (finished || surface != Surface.PENDING) return false
        surface = if (sampled) Surface.SAMPLED else Surface.UNAVAILABLE
        return true
    }

    fun observe(
        elapsedMillis: Long,
        nativeReport: String,
    ): Result? {
        if (finished) return null
        val pairs =
            nativeReport.take(4096).lineSequence().count {
                it.startsWith("serial=") && it.contains("guest=") && it.contains("swap=") && it.contains("fnv=")
            }.coerceAtMost(3)
        val nativeFailed =
            listOf(
                "pixel_probe=unsupported",
                "pixel_probe=buffer allocation failed",
                "pixel_probe=submit failed",
            ).any { nativeReport.contains(it) }
        val terminalNative = pairs == 3 || nativeFailed
        val status =
            when {
                terminalNative && surface != Surface.PENDING ->
                    if (!nativeFailed && surface == Surface.SAMPLED) {
                        Status.COMPLETE_SAMPLES
                    } else {
                        Status.INCOMPLETE_SAMPLES
                    }
                elapsedMillis >= budgetMillis -> Status.CAPTURE_TIMEOUT
                else -> return null
            }
        finished = true
        return Result(status, pairs)
    }

    fun cancel() {
        finished = true
    }
}
