package com.swordfish.lemuroid.app.feature.cartmod.maker

/**
 * SCAFFOLD — NOT WIRED. Deterministic preview stepper for [MakerProject],
 * mirroring the semantics of the spike's maker_reference.py: tile grid, actor
 * movement, blocked tiles, interactive tiles, state variables, conditional
 * rules, actions and step limits. Pure and side-effect free: the same project
 * and action list always produce the same result, which is what the future
 * native runtime's behaviour will be compared against (sequence step 2).
 */

enum class MakerAction { UP, DOWN, LEFT, RIGHT, INTERACT }

data class MakerPreviewState(
    val position: MakerPoint,
    val variables: Map<String, Int>,
    val stepsUsed: Int,
    val won: Boolean,
)

object CartmodMakerPreview {
    fun run(
        project: MakerProject,
        actions: List<MakerAction>,
    ): MakerPreviewState {
        var state = MakerPreviewState(project.start, project.variables, 0, won = false)
        for (action in actions) {
            if (state.won || state.stepsUsed >= project.stepLimit) break
            state = step(project, state, action)
        }
        return state
    }

    private fun step(
        project: MakerProject,
        state: MakerPreviewState,
        action: MakerAction,
    ): MakerPreviewState {
        var next = state.copy(stepsUsed = state.stepsUsed + 1)
        next =
            when (action) {
                MakerAction.INTERACT -> next
                else -> {
                    val moved =
                        when (action) {
                            MakerAction.UP -> MakerPoint(next.position.x, next.position.y - 1)
                            MakerAction.DOWN -> MakerPoint(next.position.x, next.position.y + 1)
                            MakerAction.LEFT -> MakerPoint(next.position.x - 1, next.position.y)
                            MakerAction.RIGHT -> MakerPoint(next.position.x + 1, next.position.y)
                            MakerAction.INTERACT -> next.position
                        }
                    if (project.grid.inside(moved) && moved !in project.grid.blocked) {
                        next.copy(position = moved)
                    } else {
                        next
                    }
                }
            }
        if (next.position == project.grid.goal) next = next.copy(won = true)
        val trigger = if (action == MakerAction.INTERACT) MakerTrigger.ON_INTERACT else MakerTrigger.ON_ENTER
        val tileId = project.grid.interactive[next.position]
        for (rule in project.rules) {
            if (rule.trigger != trigger) continue
            if (trigger == MakerTrigger.ON_INTERACT && rule.tileId != tileId) continue
            if (!conditionHolds(rule.condition, next.variables)) continue
            for (effect in rule.effects) {
                next =
                    when (effect) {
                        is MakerEffect.SetVariable ->
                            next.copy(variables = next.variables + (effect.variable to effect.value))
                        is MakerEffect.AddVariable -> {
                            val value = (next.variables[effect.variable] ?: 0) + effect.delta
                            next.copy(variables = next.variables + (effect.variable to value))
                        }
                        MakerEffect.Win -> next.copy(won = true)
                    }
            }
        }
        return next
    }

    private fun conditionHolds(
        condition: MakerCondition?,
        variables: Map<String, Int>,
    ): Boolean {
        if (condition == null) return true
        val actual = variables[condition.variable] ?: 0
        return when (condition.comparison) {
            MakerComparison.EQ -> actual == condition.value
            MakerComparison.NOT_EQ -> actual != condition.value
            MakerComparison.GT -> actual > condition.value
            MakerComparison.GTE -> actual >= condition.value
            MakerComparison.LT -> actual < condition.value
            MakerComparison.LTE -> actual <= condition.value
        }
    }
}
