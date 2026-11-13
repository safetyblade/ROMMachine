package com.swordfish.lemuroid.app.shared.game.xbox360

import com.swordfish.lemuroid.app.shared.game.xbox.XboxPadState

/** Exact pinned AndroidInputDriver 24-entry ABI. Opposing stick directions keep signed values. */
internal object Xbox360Pad {
    fun values(pad: XboxPadState): IntArray {
        val result = IntArray(24)
        listOf(4, 1, 8, 2).forEachIndexed { i, mask -> result[i] = if (pad.buttons and mask != 0) 1 else 0 }

        fun analog(index: Int) = ((pad.analog ushr (index * 8)) and 255).toInt()
        for (i in 0..3) result[4 + i] = analog(i)
        listOf(32, 16, 0, 0, 64, 128).forEachIndexed {
                i,
                mask,
            ->
            result[8 + i] = if (pad.buttons and mask != 0) 1 else 0
        }
        result[10] = analog(5)
        result[11] = analog(4)
        result[14] = analog(6)
        result[15] = analog(7)

        fun stick(
            offset: Int,
            x: Int,
            y: Int,
        ) {
            result[offset] = minOf(x, 0)
            result[offset + 1] = maxOf(y, 0)
            result[offset + 2] = maxOf(x, 0)
            result[offset + 3] = minOf(y, 0)
        }
        stick(16, pad.lx, pad.ly)
        stick(20, pad.rx, pad.ry)
        return result
    }

    fun merged(
        first: XboxPadState,
        second: XboxPadState,
    ): IntArray {
        val a = values(first)
        val b = values(second)
        return IntArray(24) { i -> if (kotlin.math.abs(a[i]) >= kotlin.math.abs(b[i])) a[i] else b[i] }
    }
}
