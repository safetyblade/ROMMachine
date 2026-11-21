package com.swordfish.lemuroid.app.shared.game.xbox360

import android.content.Context

/**
 * Debug-only, one-launch global Xbox 360 shader pipeline A/B test.
 *
 * Does not alter Xenia's config, per-title overrides, shader cache, guest HDD
 * or save files. A pending test is consumed before launching any Xbox 360 title.
 * Shipping/default behaviour passes no override and remains unchanged.
 */
internal object Xbox360PipelineAB {
    private const val PREFS = "cartdeck_xbox360_pipeline_ab"
    private const val NEXT = "next_launch_mode"

    enum class Mode(
        val description: String,
        val launchOption: String?,
    ) {
        NORMAL("Engine default", null),
        SKIP_ON("Draw skipping ON", "--vulkan_async_skip_draws=true"),
        SKIP_OFF("Draw skipping OFF", "--vulkan_async_skip_draws=false"),
    }

    fun queued(context: Context): Mode {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NEXT, null)
        return Mode.values().firstOrNull { it.name == stored } ?: Mode.NORMAL
    }

    fun queue(
        context: Context,
        mode: Mode,
    ) {
        val edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (mode == Mode.NORMAL) edit.remove(NEXT) else edit.putString(NEXT, mode.name)
        check(edit.commit()) { "Could not save the temporary Xbox 360 diagnostic setting" }
    }

    fun consume(context: Context): Mode {
        val selected = queued(context)
        if (selected != Mode.NORMAL) {
            // Never persist the override beyond one guest launch, even on crash.
            check(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(NEXT).commit()) {
                "Could not reset the temporary Xbox 360 diagnostic setting"
            }
        }
        return selected
    }
}
