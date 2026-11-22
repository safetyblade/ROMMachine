package com.swordfish.lemuroid.app.shared.input

/** A menu-local transaction; no preference write until the host explicitly saves. */
class ControllerMappingDraft(initial: Map<Int, Int>) {
    private var saved = initial.toMap()
    var bindings: Map<Int, Int> = saved
        private set
    private val history = mutableListOf<Map<Int, Int>>()
    var conflict: Pair<Int, Int>? = null
        private set
    val savedBindings: Map<Int, Int> get() = saved.toMap()
    val dirty get() = bindings != saved
    val canUndo get() = history.isNotEmpty()

    fun assign(
        source: Int,
        target: Int,
    ): Boolean {
        val oldTarget = bindings[source]
        if (oldTarget != null && oldTarget != target) {
            conflict = source to target
            return false
        }
        applyBinding(source, target)
        return true
    }

    /** Preview a reciprocal move using physical input order. Only confirmation changes the draft. */
    fun requestSwap(
        first: Int,
        second: Int,
    ): Boolean {
        val action = bindings[first] ?: return false
        if (first == second || bindings[second] == action) return false
        conflict = second to action
        return true
    }

    fun cancelConflict() {
        conflict = null
    }

    /** Explicit conflict correction swaps the displaced target rather than silently losing it. */
    fun resolveConflict() {
        val (source, target) = conflict ?: return
        val displaced = bindings[source]
        val oldSource = bindings.entries.firstOrNull { it.value == target }?.key
        val next = bindings.filterValues { it != target }.toMutableMap()
        next[source] = target
        if (oldSource != null && oldSource != source && displaced != null) next[oldSource] = displaced
        change(next)
    }

    private fun applyBinding(
        source: Int,
        target: Int,
    ) {
        change(bindings.filterValues { it != target } + (source to target))
    }

    private fun change(next: Map<Int, Int>) {
        if (next != bindings) history.add(bindings)
        bindings = next.toMap()
        conflict = null
    }

    fun reset(defaults: Map<Int, Int>) = change(defaults)

    fun undo() {
        if (history.isNotEmpty()) bindings = history.removeAt(history.lastIndex)
        conflict = null
    }

    fun cancel() {
        bindings = saved
        history.clear()
        conflict = null
    }

    fun saved() {
        saved = bindings.toMap()
        history.clear()
        conflict = null
    }
}
