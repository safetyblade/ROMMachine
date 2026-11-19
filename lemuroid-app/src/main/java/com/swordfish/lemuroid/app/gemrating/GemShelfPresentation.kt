package com.swordfish.lemuroid.app.gemrating

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Presentation preferences and read-only rating projection; never part of shelf arrangement. */
internal object GemShelfPresentation {
    private val changes = MutableStateFlow(0L)
    val revision = changes.asStateFlow()

    fun changed() {
        synchronized(changes) { changes.value++ }
    }

    fun enabled(context: Context): Boolean {
        val store = GemRatingLocalStore.open(context)
        return store.repository.snapshot(store.session).preferences?.showShelfRatings == true
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ): Boolean {
        val store = GemRatingLocalStore.open(context)
        return runCatching {
            store.repository.updatePreferences(store.session) { it.copy(showShelfRatings = enabled) }
        }.isSuccess
    }

    fun ratings(context: Context): Map<String, Int> {
        val store = GemRatingLocalStore.open(context)
        val snapshot = store.repository.snapshot(store.session)
        if (snapshot.preferences?.showShelfRatings != true) return emptyMap()
        return snapshot.records.mapNotNull { rating ->
            rating.score?.let { rating.cartridgeId to it }
        }.toMap()
    }
}
