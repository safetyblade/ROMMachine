package com.swordfish.lemuroid.app.shared.startup

import android.content.Context
import android.content.res.Configuration

/** Saved independently per orientation. Offsets are fractions of the available video viewport. */
internal data class CartdeckIntroFraming(
    val fit: Boolean = true,
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    fun constrained(): CartdeckIntroFraming =
        copy(
            zoom = zoom.takeIf { it.isFinite() }?.coerceIn(0.8f, 2.5f) ?: 1f,
            offsetX = offsetX.takeIf { it.isFinite() }?.coerceIn(-0.45f, 0.45f) ?: 0f,
            offsetY = offsetY.takeIf { it.isFinite() }?.coerceIn(-0.45f, 0.45f) ?: 0f,
        )

    companion object {
        private const val SETTINGS = "cartdeck_signature_framing_v1"

        private fun orientation(context: Context): String =
            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                "landscape"
            } else {
                "portrait"
            }

        fun load(context: Context): CartdeckIntroFraming {
            val mode = orientation(context)
            val settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            return CartdeckIntroFraming(
                fit = settings.getBoolean("fit_$mode", mode == "portrait"),
                zoom = settings.getFloat("zoom_$mode", 1f),
                offsetX = settings.getFloat("x_$mode", 0f),
                offsetY = settings.getFloat("y_$mode", 0f),
            ).constrained()
        }

        fun save(
            context: Context,
            framing: CartdeckIntroFraming,
        ) {
            val mode = orientation(context)
            val safe = framing.constrained()
            context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("fit_$mode", safe.fit)
                .putFloat("zoom_$mode", safe.zoom)
                .putFloat("x_$mode", safe.offsetX)
                .putFloat("y_$mode", safe.offsetY)
                .apply()
        }
    }
}
