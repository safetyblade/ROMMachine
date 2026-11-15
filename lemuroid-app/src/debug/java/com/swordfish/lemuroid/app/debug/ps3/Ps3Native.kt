package com.swordfish.lemuroid.app.debug.ps3

import android.view.Surface

internal object Ps3Native {
    fun loadBridge() = System.loadLibrary("cartdeck_ps3")

    external fun load(path: String): Boolean

    external fun initialize(
        root: String,
        user: String,
    ): Boolean

    external fun boot(path: String): Int

    external fun state(): Int

    external fun framePeriod(): Long

    external fun process(compilation: Boolean)

    external fun surface(
        surface: Surface?,
        event: Int,
        width: Int,
        height: Int,
    ): Boolean

    external fun pause(paused: Boolean)

    external fun stop()

    external fun install(
        fd: Int,
        firmware: Boolean,
    ): Boolean

    external fun pad(
        port: Int,
        d1: Int,
        d2: Int,
        lx: Int,
        ly: Int,
        rx: Int,
        ry: Int,
    )
}
