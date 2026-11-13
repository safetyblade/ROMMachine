package com.swordfish.lemuroid.app.mobile.feature.shelf

import java.util.UUID

/** Bounded local-only artwork reference. Never store an external content URI. */
data class CartdeckPersonalDecal(
    val assetId: String,
    val anchor: String = ANCHOR_LOWER_RIGHT,
    val scale: Float = 0.65f,
    val rotation: Float = 0f,
    val opacity: Float = 0.85f,
) {
    companion object {
        const val ANCHOR_UPPER_RAIL = "upper-rail"
        const val ANCHOR_LOWER_LEFT = "lower-left"
        const val ANCHOR_LOWER_RIGHT = "lower-right"
        const val ANCHOR_SHELF_RAIL = "shelf-rail"

        val anchors =
            linkedMapOf(
                ANCHOR_UPPER_RAIL to "Upper rail",
                ANCHOR_LOWER_LEFT to "Lower left",
                ANCHOR_LOWER_RIGHT to "Lower right",
                ANCHOR_SHELF_RAIL to "Shelf rail",
            )
    }

    fun validate(): CartdeckPersonalDecal {
        require(runCatching { UUID.fromString(assetId).toString() == assetId }.getOrDefault(false)) {
            "Invalid local decal ID"
        }
        require(anchor in anchors) { "Invalid decal anchor" }
        require(scale.isFinite() && scale in 0.25f..1.0f) { "Invalid decal size" }
        require(rotation.isFinite() && rotation in -90f..90f) { "Invalid decal rotation" }
        require(opacity.isFinite() && opacity in 0.10f..1f) { "Invalid decal opacity" }
        return this
    }
}
