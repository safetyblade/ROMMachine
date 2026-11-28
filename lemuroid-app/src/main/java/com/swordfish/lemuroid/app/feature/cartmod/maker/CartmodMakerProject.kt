package com.swordfish.lemuroid.app.feature.cartmod.maker

/**
 * SCAFFOLD — NOT WIRED to any UI, storage, launch path or native build adapter.
 *
 * Start of the CARTMOD Maker engineering sequence in spikes/cartmod-maker/README.md
 * ("Engineering sequence after PC gate"), step 1: "Author a limited data-driven
 * native GBC runtime against GBDK/GB Studio references; compile once through a
 * verified desktop/CI toolchain. Boot and save it in an independent emulator."
 *
 * This file is the app-side foundation of that slice: the Kotlin domain model
 * mirroring the spike's versioned project contract (project.schema.json, v1)
 * and its bounded validation, so the future runtime, packer and editor all
 * consume one contract. The deterministic preview semantics live in
 * [CartmodMakerPreview] and mirror maker_reference.py.
 *
 * Gates #212 / #304 still stand: no merge to release, no UI, no ROM export.
 */

data class MakerPoint(val x: Int, val y: Int)

data class MakerGrid(
    val width: Int,
    val height: Int,
    /** Tiles the actor cannot enter. */
    val blocked: Set<MakerPoint> = emptySet(),
    /** Interactive tiles, mapped to the rule trigger id fired on Interact. */
    val interactive: Map<MakerPoint, String> = emptyMap(),
    /** Entering this tile wins, when set. */
    val goal: MakerPoint? = null,
) {
    fun inside(p: MakerPoint): Boolean = p.x in 0 until width && p.y in 0 until height
}

enum class MakerComparison { EQ, NOT_EQ, GT, GTE, LT, LTE }

data class MakerCondition(val variable: String, val comparison: MakerComparison, val value: Int)

sealed interface MakerEffect {
    data class SetVariable(val variable: String, val value: Int) : MakerEffect

    data class AddVariable(val variable: String, val delta: Int) : MakerEffect

    data object Win : MakerEffect
}

enum class MakerTrigger { ON_ENTER, ON_INTERACT }

/** A conditional rule: when [trigger] fires (optionally on tile [tileId]), and
 *  [condition] holds, apply [effects] in order. Mirrors the reference preview. */
data class MakerRule(
    val trigger: MakerTrigger,
    val tileId: String? = null,
    val condition: MakerCondition? = null,
    val effects: List<MakerEffect> = emptyList(),
)

data class MakerProject(
    val schemaVersion: Int,
    val title: String,
    /** Design intent only (e.g. "gbc", "gba"); no build adapter exists yet. */
    val targets: List<String> = emptyList(),
    val grid: MakerGrid,
    val start: MakerPoint,
    val variables: Map<String, Int> = emptyMap(),
    val rules: List<MakerRule> = emptyList(),
    val stepLimit: Int = DEFAULT_STEP_LIMIT,
) {
    companion object {
        const val CONTRACT_VERSION = 1
        const val DEFAULT_STEP_LIMIT = 500
        const val MAX_GRID_DIMENSION = 64
        const val MAX_STEP_LIMIT = 10_000
        const val MAX_TITLE_LENGTH = 64
        const val MAX_VARIABLES = 64
        const val MAX_RULES = 256
        const val MAX_TARGETS = 8
    }
}

/** Bounded contract validation, mirroring the spike validator's role: reject
 *  malformed and oversized projects with named violations, never throw. */
object MakerProjectValidator {
    fun validate(project: MakerProject): List<String> {
        val errors = mutableListOf<String>()
        if (project.schemaVersion != MakerProject.CONTRACT_VERSION) {
            errors += "unsupported schemaVersion ${project.schemaVersion}"
        }
        if (project.title.isBlank()) errors += "title must not be blank"
        if (project.title.length > MakerProject.MAX_TITLE_LENGTH) errors += "title too long"
        if (project.targets.size > MakerProject.MAX_TARGETS) errors += "too many targets"
        val g = project.grid
        if (g.width !in 1..MakerProject.MAX_GRID_DIMENSION ||
            g.height !in 1..MakerProject.MAX_GRID_DIMENSION
        ) {
            errors += "grid dimensions out of bounds"
        }
        if (project.stepLimit !in 1..MakerProject.MAX_STEP_LIMIT) errors += "stepLimit out of bounds"
        if (project.variables.size > MakerProject.MAX_VARIABLES) errors += "too many variables"
        if (project.rules.size > MakerProject.MAX_RULES) errors += "too many rules"
        if (!g.inside(project.start)) errors += "start outside grid"
        if (project.start in g.blocked) errors += "start on a blocked tile"
        g.goal?.let { if (!g.inside(it)) errors += "goal outside grid" }
        g.blocked.firstOrNull { !g.inside(it) }?.let { errors += "blocked tile outside grid" }
        g.interactive.keys.firstOrNull { !g.inside(it) }?.let { errors += "interactive tile outside grid" }
        return errors
    }
}
