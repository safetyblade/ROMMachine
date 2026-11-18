package com.swordfish.lemuroid.app.shared.game

import android.content.Context
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Cartridge-local, launch-only comparisons. No writes to core state or save data. */
enum class CitraCompatibilityMode(
    val label: String,
    val optionKey: String? = null,
    val optionValue: String? = null,
) {
    ORIGINAL("Original rendering"),
    REDRAW("Redraw repeated frames"),
    GEOMETRY_OFF("Disable accurate geometry shaders", "citra_use_acc_geo_shaders", "disabled"),
    MULTIPLY_ON("Enable accurate shader multiplication", "citra_use_acc_mul", "enabled"),
    SOFTWARE_SHADERS("Disable hardware shaders (slow)", "citra_use_hw_shaders", "disabled"),
    ;

    fun applyOptions(options: List<CoreVariable>): List<CoreVariable> {
        val key = optionKey ?: return options
        val value = optionValue ?: return options
        return options.filterNot { it.key == key } + CoreVariable(key, value)
    }

    fun skipDuplicates(original: Boolean): Boolean = original && this != REDRAW

    companion object {
        private const val PREFS = "cartdeck_citra_qa"

        private fun key(game: Game) = "mode_${game.cartridgeId}"

        fun read(
            context: Context,
            game: Game,
        ): CitraCompatibilityMode {
            val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(game), null)
            return entries.firstOrNull { it.name == value } ?: ORIGINAL
        }

        fun write(
            context: Context,
            game: Game,
            mode: CitraCompatibilityMode,
        ) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(key(game), mode.name)
                .apply()
        }
    }
}
