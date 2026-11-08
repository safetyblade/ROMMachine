package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/** Root coordinates are shared by shelf cells and the in-place pickup overlay. */
class CartridgeGeometry {
    val cells = mutableStateMapOf<Int, Rect>()
    var selectedId: Int? by mutableStateOf(null)
    private val owners = mutableMapOf<Int, Any>()

    fun update(
        id: Int,
        owner: Any,
        bounds: Rect,
    ) {
        owners[id] = owner
        cells[id] = bounds
    }

    fun remove(
        id: Int,
        owner: Any,
    ) {
        if (owners[id] === owner) {
            owners.remove(id)
            cells.remove(id)
        }
    }
}

val LocalCartridgeGeometry = staticCompositionLocalOf { CartridgeGeometry() }

/** Lift first, then travel/rotate; total timeline is 500ms. */
data class CartridgeFlight(val bounds: Rect, val rotationY: Float, val showFront: Boolean)

fun cartridgeFlight(
    source: Rect,
    target: Rect,
    progress: Float,
): CartridgeFlight {
    val p = progress.coerceIn(0f, 1f)
    val travel = ((p - 0.22f) / 0.68f).coerceIn(0f, 1f)
    val lift = if (p < 0.22f) p / 0.22f else 1f - travel
    val bounds =
        Rect(
            source.left + (target.left - source.left) * travel,
            source.top + (target.top - source.top) * travel - lift * source.height * 0.12f,
            source.right + (target.right - source.right) * travel,
            source.bottom + (target.bottom - source.bottom) * travel - lift * source.height * 0.12f,
        )
    val rotation = if (travel < 0.5f) travel * 180f else (travel - 1f) * 180f
    return CartridgeFlight(bounds, rotation, travel >= 0.5f)
}
