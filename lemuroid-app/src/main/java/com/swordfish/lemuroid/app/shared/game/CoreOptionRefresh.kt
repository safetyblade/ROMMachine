package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.core.CoreVariable

/** Avoid re-sending unchanged frontend options on every resume; never caches across sessions. */
internal object CoreOptionRefresh {
    fun changed(
        desired: List<CoreVariable>,
        current: Map<String, String>,
    ): List<CoreVariable> = desired.filter { current[it.key] != it.value }
}
