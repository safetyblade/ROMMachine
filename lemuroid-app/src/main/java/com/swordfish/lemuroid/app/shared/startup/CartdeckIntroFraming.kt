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

        /**
         * On portrait phones the approved landscape clip should end with a large centred mark,
         * not a tiny letterbox. A measured 1.6x Fit zoom keeps the whole CARTDECK wordmark
         * within the phone viewport; avoid full-screen Fill, which cuts off its outer letters.
         * Tablets, TV and landscape retain their previous framing.
         */
        fun defaultFor(context: Context): CartdeckIntroFraming {
            val portraitPhone =
                orientation(context) == "portrait" &&
                    context.resources.configuration.smallestScreenWidthDp < 600
            return CartdeckIntroFraming(
                fit = orientation(context) == "portrait",
                zoom = if (portraitPhone) 1.6f else 1f,
            )
        }

        fun load(context: Context): CartdeckIntroFraming {
            val mode = orientation(context)
            val fallback = defaultFor(context)
            val settings = context.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
            return CartdeckIntroFraming(
                fit = settings.getBoolean("fit_$mode", fallback.fit),
                zoom = settings.getFloat("zoom_$mode", fallback.zoom),
                offsetX = settings.getFloat("x_$mode", fallback.offsetX),
                offsetY = settings.getFloat("y_$mode", fallback.offsetY),
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
