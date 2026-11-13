package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.lib.library.SystemID
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension

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

    fun buildConstraintSet(
        isLandscape: Boolean,
        allowTouchOverlay: Boolean,
        portraitMode: CartdeckViewportPolicy.PortraitMode,
        portraitBottomInset: Dp = 0.dp,
    ): ConstraintSet {
        return when {
            !isLandscape -> buildConstraintSetPortrait(portraitMode, portraitBottomInset)
            allowTouchOverlay -> buildConstraintSetLandscape()
            else -> buildConstraintSetLandscapeNoOverlay()
        }
    }

    private fun buildConstraintSetPortrait(
        portraitMode: CartdeckViewportPolicy.PortraitMode,
        bottomInset: Dp,
    ): ConstraintSet {
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
                    // One 4:3 physical screen opening. LibretroDroid preserves
                    // GB, GBA, PSP and dynamic source aspect ratios within it.
                    height = Dimension.ratio(CartdeckViewportPolicy.STANDARD_RATIO)
                    top.linkTo(parent.top, margin = 84.dp)
                    bottom.linkTo(leftPad.top, margin = 16.dp)
                    verticalBias = 0f
                    absoluteLeft.linkTo(parent.absoluteLeft, margin = 22.dp)
                    absoluteRight.linkTo(parent.absoluteRight, margin = 22.dp)
                } else {
                    // The tested v2101 DS layout stays flexible, including
                    // its original stylus/touch coordinate mapping.
                    height = Dimension.fillToConstraints
                    top.linkTo(parent.top, margin = 18.dp)
                    bottom.linkTo(leftPad.top, margin = 18.dp)
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

    private fun buildConstraintSetLandscapeNoOverlay(): ConstraintSet {
        return ConstraintSet {
            val gameView = createRefFor(CONSTRAINTS_GAME_VIEW)
            val leftPad = createRefFor(CONSTRAINTS_LEFT_PAD)
            val rightPad = createRefFor(CONSTRAINTS_RIGHT_PAD)
            val gameContainer = createRefFor(CONSTRAINTS_GAME_CONTAINER)
            val leftContainer = createRefFor(CONSTRAINTS_LEFT_CONTAINER)
            val rightContainer = createRefFor(CONSTRAINTS_RIGHT_CONTAINER)

            constrain(leftPad) {
                absoluteLeft.linkTo(parent.absoluteLeft)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                width = Dimension.wrapContent // Takes as much space as needed
            }

            constrain(gameView) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                absoluteLeft.linkTo(leftPad.absoluteRight)
                absoluteRight.linkTo(rightPad.absoluteLeft)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                width = Dimension.fillToConstraints // Expands between leftPad and rightPad
            }

            constrain(rightPad) {
                absoluteRight.linkTo(parent.absoluteRight)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                width = Dimension.wrapContent // Takes as much space as needed
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
