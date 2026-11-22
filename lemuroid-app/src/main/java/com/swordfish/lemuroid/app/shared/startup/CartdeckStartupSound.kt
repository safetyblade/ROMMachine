package com.swordfish.lemuroid.app.shared.startup

import android.content.Context
import android.content.SharedPreferences

internal object CartdeckStartupSound {
    const val VOLUME = "volume"
    const val MUTED = "muted"
    const val DEFAULT_VOLUME = 50

    fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences("cartdeck_startup_sound", Context.MODE_PRIVATE)

    fun volume(preferences: SharedPreferences): Int = preferences.getInt(VOLUME, DEFAULT_VOLUME).coerceIn(0, 100)

    fun muted(preferences: SharedPreferences): Boolean = preferences.getBoolean(MUTED, false)

    fun gain(preferences: SharedPreferences): Float = if (muted(preferences)) 0f else volume(preferences) / 100f
}
