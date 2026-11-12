package com.swordfish.lemuroid.app.mobile.feature.game

/** In-memory choices only. Never written to cartridge or controller preferences. */
internal data class ArcControlSessionChoice(
    val standardForSession: Boolean = false,
    val sideForSession: GripArcSide? = null,
) {
    fun modeFor(saved: ArcControlMode): ArcControlMode = if (standardForSession) ArcControlMode.STANDARD else saved

    fun sideFor(saved: GripArcSide): GripArcSide = sideForSession ?: saved

    fun useStandard(): ArcControlSessionChoice = copy(standardForSession = true)

    fun tryOppositeSide(saved: GripArcSide): ArcControlSessionChoice =
        copy(
            standardForSession = false,
            sideForSession = if (sideFor(saved) == GripArcSide.RIGHT) GripArcSide.LEFT else GripArcSide.RIGHT,
        )
}
