package com.swordfish.lemuroid.app.gemrating

import android.content.Context

/** Preferences share the atomic player ledger with ratings and backup restores. */
internal class GemDiscoveryPreferences(
    context: Context,
    private val session: GemRatingRepository.Session,
) {
    private val repository = GemRatingLocalStore.open(context).repository

    fun wildcardPercent(): Int = repository.snapshot(session).preferences?.wildcardPercent ?: 30

    fun saveWildcardPercent(percent: Int): Boolean =
        runCatching {
            repository.updatePreferences(session) { it.copy(wildcardPercent = percent) }
        }.isSuccess

    fun recentIds(): List<String> = repository.snapshot(session).preferences?.recentSurprises.orEmpty()

    fun rememberSurprise(releaseId: String): Boolean =
        runCatching {
            repository.updatePreferences(session) {
                it.copy(
                    recentSurprises = (it.recentSurprises.filterNot { id -> id == releaseId } + releaseId).takeLast(20),
                )
            }
        }.isSuccess
