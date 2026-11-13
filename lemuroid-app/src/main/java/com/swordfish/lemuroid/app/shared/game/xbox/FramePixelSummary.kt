package com.swordfish.lemuroid.app.shared.game.xbox

/** Numerical debug evidence only. Never writes or retains captured pixel arrays. */
object FramePixelSummary {
    fun argb(pixels: IntArray): String {
        require(pixels.isNotEmpty() && pixels.size <= 4096)
        var hash = 0xcbf29ce484222325UL
        var low = 255
        var high = 0
        var alphaLow = 255
        var alphaHigh = 0
        var white = 0
        var black = 0
        for (pixel in pixels) {
            val values = intArrayOf(pixel ushr 16 and 255, pixel ushr 8 and 255, pixel and 255, pixel ushr 24)
            for (value in values) hash = (hash xor value.toULong()) * 0x100000001b3UL
            for (value in values.take(3)) {
                low = minOf(low, value)
                high = maxOf(high, value)
            }
            alphaLow = minOf(alphaLow, values[3])
            alphaHigh = maxOf(alphaHigh, values[3])
            if (values.take(3).all { it >= 250 }) white++
            if (values.take(3).all { it <= 5 }) black++
        }
        return "n=${pixels.size} rgb=$low..$high white=$white black=$black " +
            "alpha=$alphaLow..$alphaHigh fnv=${hash.toString(16).padStart(16, '0')}"
    }
}
