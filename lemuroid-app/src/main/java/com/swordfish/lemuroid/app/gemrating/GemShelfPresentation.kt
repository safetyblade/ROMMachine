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

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("gem_shelf_presentation_v1", Context.MODE_PRIVATE)

    private fun key(session: GemRatingRepository.Session): String = "${session.profileId}:showRatings"

    fun enabled(context: Context): Boolean {
        val session = GemRatingLocalStore.open(context).session
        return prefs(context).getBoolean(key(session), false)
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean,
    ): Boolean {
        val session = GemRatingLocalStore.open(context).session
        val saved = prefs(context).edit().putBoolean(key(session), enabled).commit()
        if (saved) changed()
        return saved
    }

    fun ratings(context: Context): Map<String, Int> {
        val store = GemRatingLocalStore.open(context)
        if (!prefs(context).getBoolean(key(store.session), false)) return emptyMap()
        return store.repository.snapshot(store.session).records.mapNotNull { rating ->
            rating.score?.let { rating.cartridgeId to it }
        }.toMap()
    }
}
