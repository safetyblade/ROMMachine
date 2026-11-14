package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min

/**
 * UI-only core-feedback signal, owned by ONE game viewmodel. A conflated channel
 * prevents rumble callbacks from allocating coroutines or blocking vibration.
 * Neither the game core, input mapper, nor persistent data is touched.
 */
internal class CartdeckReactiveLightingSession(
    scope: CoroutineScope,
    private val clockMs: () -> Long = SystemClock::elapsedRealtime,
) {
    private val events = Channel<Float>(Channel.CONFLATED)
    private val output = MutableStateFlow(0f)
    val strength: StateFlow<Float> = output

    @Volatile
    private var listening = false
    private var previousUpdateAt = Long.MIN_VALUE

    init {
        scope.launch {
            while (isActive) {
                val first = events.receive()
                if (!listening) continue
                apply(first)
                while (listening) {
                    val next = withTimeoutOrNull(IDLE_TIMEOUT_MS) { events.receive() }
                    if (next == null) {
                        output.value = 0f
                        previousUpdateAt = Long.MIN_VALUE
                        break
                    }
                    if (!listening) break
                    apply(next)
                }
            }
        }
    }

    /** This callback is safe from the existing rumble worker thread. */
    fun onCoreRumble(
        strong: Float,
        weak: Float,
    ) {
        if (listening) events.trySend(CartdeckFeedbackStrength.normalized(strong, weak))
    }

    fun configure(enabled: Boolean) {
        listening = enabled
        if (!enabled) {
            output.value = 0f
            previousUpdateAt = Long.MIN_VALUE
            // Drop an event received just before pause or profile change.
            while (events.tryReceive().isSuccess) Unit
        }
    }

    private fun apply(normalized: Float) {
        if (normalized <= 0f) {
            output.value = 0f
            previousUpdateAt = Long.MIN_VALUE
            return
        }
        val now = clockMs()
        if (previousUpdateAt == Long.MIN_VALUE || now - previousUpdateAt >= MIN_INTERVAL_MS) {
            output.value = normalized
            previousUpdateAt = now
        }
    }

    companion object {
        const val MIN_INTERVAL_MS = 125L // At most 8 effective feedback updates/second.
        const val IDLE_TIMEOUT_MS = 300L // No polling; restore static light on silence.
    }
}

internal object CartdeckFeedbackStrength {
    fun normalized(
        strong: Float,
        weak: Float,
    ): Float {
        val s = if (strong.isFinite()) strong.coerceIn(0f, 1f) else 0f
        val w = if (weak.isFinite()) weak.coerceIn(0f, 1f) else 0f
        return (s * 0.66f + w * 0.34f).coerceIn(0f, 1f)
    }

    /** Cap the light increase at 35% of baseline, or the remaining headroom. */
    fun intensity(
        baseline: Int,
        strength: Float,
    ): Float {
        val base = baseline.coerceIn(0, 100).toFloat()
        val safeStrength = if (strength.isFinite()) strength.coerceIn(0f, 1f) else 0f
        val additional = min(base * 0.35f, 100f - base) * safeStrength
        return (base + additional).coerceIn(0f, 100f)
    }
}
