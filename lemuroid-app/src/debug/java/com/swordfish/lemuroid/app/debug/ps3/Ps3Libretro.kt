package com.swordfish.lemuroid.app.debug.ps3

import android.view.Surface

internal object Ps3Libretro {
    fun load() = System.loadLibrary("cartdeckps3vulkanhost")
    external fun nativePrepare()
    external fun nativeRun(surface: Surface, root: String, disc: String, maxFrames: Int, coreLibrary: String, mediaDirectory: String): String
    external fun nativeSurface(surface: Surface?)
    external fun nativeStop()
    external fun nativePause(paused: Boolean)
    external fun nativeReport(): String
    external fun nativeButton(id: Int, down: Boolean)
    external fun nativeAxis(axis: Int, value: Int)
}
