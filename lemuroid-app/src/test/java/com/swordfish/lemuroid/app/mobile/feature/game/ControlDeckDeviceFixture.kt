package com.swordfish.lemuroid.app.mobile.feature.game

/** Root-local dp. Mirrors the production portrait screen opening (22dp sides, 84dp top, 4:3). */
internal data class ControlDeckDeviceFixture(
    val width: Float,
    val height: Float,
    val leftInset: Float = 0f,
    val topInset: Float = 24f,
    val rightInset: Float = 0f,
    val bottomInset: Float = 24f,
    val cutoutTop: Float = 0f,
) {
    val screen = PivotArcBounds(0f, 0f, width, height)
    val safe =
        PivotArcBounds(
            leftInset + 8f,
            maxOf(topInset, cutoutTop) + 8f,
            width - rightInset - 8f,
            height - bottomInset - 8f,
        )

    // Reserve the full screen opening, conservatively including any top-only cutout padding.
    val viewport = PivotArcBounds(22f, 84f, width - 22f, 84f + (width - 44f) * 3f / 4f)

    companion object {
        // Different width AND height classes, plus asymmetric gesture/cutout exclusions.
        val representative =
            listOf(
                ControlDeckDeviceFixture(320f, 640f),
                ControlDeckDeviceFixture(360f, 740f),
                ControlDeckDeviceFixture(393f, 852f),
                ControlDeckDeviceFixture(
                    393f,
                    852f,
                    leftInset = 16f,
                    rightInset = 8f,
                    bottomInset = 32f,
                    cutoutTop = 32f,
                ),
            )
    }
}
