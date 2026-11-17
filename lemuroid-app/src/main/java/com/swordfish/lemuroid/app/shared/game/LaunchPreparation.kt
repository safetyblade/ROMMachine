package com.swordfish.lemuroid.app.shared.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One launch owner. Cancellation waits for blocking/atomic staging to leave its safe boundary. */
class LaunchPreparation {
    private var job: Job? = null
    private val cancellation = MutableStateFlow(false)
    val cancelling: StateFlow<Boolean> = cancellation

    fun start(
        scope: CoroutineScope,
        prepare: suspend CoroutineScope.() -> Unit,
    ): Job? {
        if (cancellation.value) return null
        return job ?: scope.launch(block = prepare).also { job = it }
    }

    fun requestCancellation() {
        cancellation.value = true
        job?.cancel()
    }

    suspend fun awaitCleanup() {
        requestCancellation()
        job?.cancelAndJoin()
    }
}
