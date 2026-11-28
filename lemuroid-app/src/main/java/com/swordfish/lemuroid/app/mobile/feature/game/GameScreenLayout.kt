package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager

internal data class LandscapeHardwareInsets(
    val padTopBottom: Dp,
    val sideInset: Dp,
    val screenHorizontal: Dp,
    val screenVertical: Dp,
)

/** Adapt decoration and control clearance to usable deck dimensions without moving individual buttons. */
internal fun landscapeHardwareInsets(
    availableWidth: Dp,
    availableHeight: Dp,
): LandscapeHardwareInsets =
    when {
        availableWidth < 700.dp || availableHeight < 360.dp ->
            LandscapeHardwareInsets(20.dp, 8.dp, 10.dp, 5.dp)
        availableWidth > 1080.dp && availableHeight > 520.dp ->
            LandscapeHardwareInsets(54.dp, 20.dp, 20.dp, 10.dp)
        else -> LandscapeHardwareInsets(42.dp, 16.dp, 16.dp, 8.dp)
    }

internal data class PortraitHardwareInsets(
    val top: Dp,
    val side: Dp,
    val controlGap: Dp,
)

/**
 * Adaptive manufactured portrait glass: the accepted 393×852 handheld is unchanged.
 * The emulator itself still owns true pixel aspect and screen/dual-screen coordinates.
 */
internal fun portraitHardwareInsets(
    availableWidth: Dp,
    availableHeight: Dp,
    expanded: Boolean,
): PortraitHardwareInsets =
    when {
        availableWidth < 360.dp || availableHeight < 700.dp ->
            PortraitHardwareInsets(
                if (expanded) 56.dp else 64.dp,
                if (expanded) 10.dp else 14.dp,
                12.dp,
            )
        availableWidth >= 600.dp && availableHeight >= 900.dp ->
            PortraitHardwareInsets(
                if (expanded) 92.dp else 104.dp,
                if (expanded) 22.dp else 30.dp,
                24.dp,
            )
        else ->
            PortraitHardwareInsets(
                if (expanded) 76.dp else 84.dp,
                if (expanded) 14.dp else 22.dp,
                16.dp,
            )
    }

object GameScreenLayout {
    const val CONSTRAINTS_BOTTOM_CONTAINER = "bottomContainer"
    const val CONSTRAINTS_LEFT_CONTAINER = "leftContainer"
    const val CONSTRAINTS_RIGHT_CONTAINER = "rightContainer"
    const val CONSTRAINTS_LEFT_PAD = "leftPad"
    const val CONSTRAINTS_RIGHT_PAD = "rightPad"
    const val CONSTRAINTS_GAME_VIEW = "gameView"
    const val CONSTRAINTS_GAME_CONTAINER = "gameContainer"

    /**
     * Dreamcast's single analog dial extends below its radial D-pad cluster.
     * Move only the standalone portrait controller deck inside the manufactured
     * shell, leaving enough bottom/gesture clearance on compact handsets.
     * Combined GripArc/PivotArc owns its own measured geometry and stays unchanged.
     */
    fun portraitBottomInsetFor(
        systemId: SystemID,
        standardMode: Boolean,
        isLandscape: Boolean,
        availableHeight: Dp,
    ): Dp {
        if (systemId != SystemID.DREAMCAST || !standardMode || isLandscape) return 0.dp
        return if (availableHeight >= 680.dp) 24.dp else 12.dp
    }

    /**
     * Preserve the saved radial constellation; cap the effective session scale
     * against the side-deck height rather than moving individual controls.
     */
    internal fun fitLandscapeHardwareSettings(
        settings: TouchControllerSettingsManager.Settings,
        height: Dp,
    ): TouchControllerSettingsManager.Settings {
        val available = (height.value - 84f).coerceAtLeast(0f)
        // BaseLayout uses 160dp × interpolated scale; allow the surrounding secondary dials.
        val maximumDialScale = available / (160f * 1.9f)
        val maximumSetting =
            (
                (maximumDialScale - TouchControllerSettingsManager.MIN_SCALE) /
                    (TouchControllerSettingsManager.MAX_SCALE - TouchControllerSettingsManager.MIN_SCALE)
            ).coerceIn(0f, 1f)
        return settings.copy(scale = minOf(settings.scale, maximumSetting))
    }

    internal fun gripArcLandscapeSideWidth(availableWidth: Dp): Dp = (availableWidth * 0.24f).coerceIn(132.dp, 260.dp)

    fun buildConstraintSet(
        isLandscape: Boolean,
        allowTouchOverlay: Boolean,
        portraitMode: CartdeckViewportPolicy.PortraitMode,
        portraitBottomInset: Dp = 0.dp,
        portraitApertureRatio: String = CartdeckViewportPolicy.STANDARD_RATIO,
        hardwareLandscape: Boolean = false,
        gripArcLandscape: Boolean = false,
        availableWidth: Dp = 852.dp,
        availableHeight: Dp = 393.dp,
        dualScreenSoloPivot: Boolean = false,
        independentPortraitAperture: Boolean = false,
        standaloneGripRailInset: Dp = 0.dp,
        standalonePortraitBottomReserve: Dp = 0.dp,
    ): ConstraintSet {
        return when {
            !isLandscape ->
                buildConstraintSetPortrait(
                    portraitMode,
                    portraitBottomInset,
                    portraitApertureRatio,
                    availableWidth,
                    availableHeight,
                    dualScreenSoloPivot,
                    independentPortraitAperture,
                    standalonePortraitBottomReserve,
                )
            allowTouchOverlay -> buildConstraintSetLandscape()
            else ->
                buildConstraintSetLandscapeNoOverlay(
                    hardwareLandscape,
                    gripArcLandscape,
                    availableWidth,
                    availableHeight,
                    standaloneGripRailInset,
                )
        }
    }

    private fun buildConstraintSetPortrait(
        portraitMode: CartdeckViewportPolicy.PortraitMode,
        bottomInset: Dp,
        apertureRatio: String,
        availableWidth: Dp,
        availableHeight: Dp,
        dualScreenSoloPivot: Boolean,
        independentPortraitAperture: Boolean,
        standalonePortraitBottomReserve: Dp,
    ): ConstraintSet {
        val expanded = apertureRatio != CartdeckViewportPolicy.STANDARD_RATIO
        val insets = portraitHardwareInsets(availableWidth, availableHeight, expanded)
        val maxApertureWidth =
            if (standalonePortraitBottomReserve > 0.dp) {
                val maxHeight = (availableHeight - insets.top - standalonePortraitBottomReserve).coerceAtLeast(0.dp)
                val ratio = apertureRatio.split(':').mapNotNull(String::toFloatOrNull)
                val aspect = if (ratio.size == 2 && ratio[1] > 0f) ratio[0] / ratio[1] else 4f / 3f
                (maxHeight * aspect).coerceIn(1.dp, (availableWidth - insets.side * 2).coerceAtLeast(1.dp))
            } else {
                0.dp
            }
        return ConstraintSet {
            val gameView = createRefFor(CONSTRAINTS_GAME_VIEW)
            val leftPad = createRefFor(CONSTRAINTS_LEFT_PAD)
            val rightPad = createRefFor(CONSTRAINTS_RIGHT_PAD)
            val gameContainer = createRefFor(CONSTRAINTS_GAME_CONTAINER)
            val bottomContainer = createRefFor(CONSTRAINTS_BOTTOM_CONTAINER)

            val gamePadChain = createHorizontalChain(leftPad, rightPad, chainStyle = ChainStyle.SpreadInside)

            constrain(gameView) {
                width = Dimension.fillToConstraints
                if (portraitMode == CartdeckViewportPolicy.PortraitMode.STANDARD) {
                    // Portrait glass follows a known system's native frame
                    // when measured vertical space allows. The core retains
                    // its own true pixel aspect and letterboxing.
                    if (maxApertureWidth > 0.dp) width = Dimension.value(maxApertureWidth)
                    height = Dimension.ratio(apertureRatio)
                    top.linkTo(parent.top, margin = insets.top)
                    if (!independentPortraitAperture) bottom.linkTo(leftPad.top, margin = insets.controlGap)
                    verticalBias = 0f
                    absoluteLeft.linkTo(parent.absoluteLeft, margin = insets.side)
                    absoluteRight.linkTo(parent.absoluteRight, margin = insets.side)
                } else {
                    // The tested v2101 DS layout stays flexible, including
                    // its original stylus/touch coordinate mapping.
                    height =
                        if (dualScreenSoloPivot) {
                            Dimension.value(availableHeight * 0.46f)
                        } else {
                            Dimension.fillToConstraints
                        }
                    top.linkTo(parent.top, margin = 18.dp)
                    if (!dualScreenSoloPivot) bottom.linkTo(leftPad.top, margin = 18.dp)
                    absoluteLeft.linkTo(parent.absoluteLeft, margin = 18.dp)
                    absoluteRight.linkTo(parent.absoluteRight, margin = 18.dp)
                }
            }

            constrain(bottomContainer) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(parent.absoluteRight)
                top.linkTo(leftPad.top)
                bottom.linkTo(parent.bottom)
            }

            constrain(gamePadChain) {
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(parent.absoluteRight)
            }

            constrain(rightPad) {
                width = Dimension.fillToConstraints
                bottom.linkTo(parent.bottom, margin = bottomInset)
            }

            constrain(leftPad) {
                width = Dimension.fillToConstraints
                bottom.linkTo(parent.bottom, margin = bottomInset)
            }

            constrain(gameContainer) {
                absoluteLeft.linkTo(gameView.absoluteLeft)
                absoluteRight.linkTo(gameView.absoluteRight)
                top.linkTo(gameView.top)
                bottom.linkTo(gameView.bottom)
            }
        }
    }

    private fun buildConstraintSetLandscape(): ConstraintSet {
        return ConstraintSet {
            val gameView = createRefFor(CONSTRAINTS_GAME_VIEW)
            val leftPad = createRefFor(CONSTRAINTS_LEFT_PAD)
            val rightPad = createRefFor(CONSTRAINTS_RIGHT_PAD)
            val gameContainer = createRefFor(CONSTRAINTS_GAME_CONTAINER)

            val gamePadChain = createHorizontalChain(leftPad, rightPad, chainStyle = ChainStyle.SpreadInside)

            constrain(gameView) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                top.linkTo(parent.top)
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(parent.absoluteRight)
                bottom.linkTo(parent.bottom)
            }

            constrain(gamePadChain) {
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(parent.absoluteRight)
            }

            constrain(rightPad) {
                verticalBias = 1.0f
                bottom.linkTo(parent.bottom)
                top.linkTo(parent.top)
            }

            constrain(leftPad) {
                verticalBias = 1.0f
                bottom.linkTo(parent.bottom)
                top.linkTo(parent.top)
            }

            constrain(gameContainer) {
                absoluteLeft.linkTo(gameView.absoluteLeft)
                absoluteRight.linkTo(gameView.absoluteRight)
                top.linkTo(gameView.top)
                bottom.linkTo(gameView.bottom)
            }
        }
    }

    private fun buildConstraintSetLandscapeNoOverlay(
        hardwareLandscape: Boolean,
        gripArcLandscape: Boolean,
        availableWidth: Dp,
        availableHeight: Dp,
        standaloneGripRailInset: Dp,
    ): ConstraintSet {
        val insets = landscapeHardwareInsets(availableWidth, availableHeight)
        return ConstraintSet {
            val gameView = createRefFor(CONSTRAINTS_GAME_VIEW)
            val leftPad = createRefFor(CONSTRAINTS_LEFT_PAD)
            val rightPad = createRefFor(CONSTRAINTS_RIGHT_PAD)
            val gameContainer = createRefFor(CONSTRAINTS_GAME_CONTAINER)
            val leftContainer = createRefFor(CONSTRAINTS_LEFT_CONTAINER)
            val rightContainer = createRefFor(CONSTRAINTS_RIGHT_CONTAINER)

            val padInset = if (hardwareLandscape) insets.padTopBottom else 0.dp
            val sideInset = if (hardwareLandscape) insets.sideInset else 0.dp
            val gripSideWidth = gripArcLandscapeSideWidth(availableWidth)
            constrain(leftPad) {
                absoluteLeft.linkTo(parent.absoluteLeft, margin = sideInset)
                top.linkTo(parent.top, margin = padInset)
                bottom.linkTo(parent.bottom, margin = padInset)
                verticalBias = 0.5f
                width = if (gripArcLandscape) Dimension.value(gripSideWidth) else Dimension.wrapContent
                if (hardwareLandscape) height = Dimension.preferredWrapContent
            }

            constrain(gameView) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                val clearance = if (hardwareLandscape) insets.screenVertical else 0.dp
                val horizontalClearance = if (hardwareLandscape) insets.screenHorizontal else 0.dp
                absoluteLeft.linkTo(leftPad.absoluteRight, margin = horizontalClearance)
                absoluteRight.linkTo(rightPad.absoluteLeft, margin = horizontalClearance)
                val topClearance = if (standaloneGripRailInset > 0.dp) maxOf(clearance, 25.dp) else clearance
                top.linkTo(parent.top, margin = topClearance)
                bottom.linkTo(parent.bottom, margin = maxOf(clearance, standaloneGripRailInset))
                width = Dimension.fillToConstraints // Expands between leftPad and rightPad
            }

            constrain(rightPad) {
                absoluteRight.linkTo(parent.absoluteRight, margin = sideInset)
                top.linkTo(parent.top, margin = padInset)
                bottom.linkTo(parent.bottom, margin = padInset)
                verticalBias = 0.5f
                width = if (gripArcLandscape) Dimension.value(gripSideWidth) else Dimension.wrapContent
                if (hardwareLandscape) height = Dimension.preferredWrapContent
            }

            constrain(gameContainer) {
                absoluteLeft.linkTo(gameView.absoluteLeft)
                absoluteRight.linkTo(gameView.absoluteRight)
                top.linkTo(gameView.top)
                bottom.linkTo(gameView.bottom)
            }

            constrain(leftContainer) {
                absoluteLeft.linkTo(parent.absoluteLeft)
                absoluteRight.linkTo(leftPad.absoluteRight)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }

            constrain(rightContainer) {
                absoluteRight.linkTo(parent.absoluteRight)
                absoluteLeft.linkTo(rightPad.absoluteLeft)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
        }
    }
}
