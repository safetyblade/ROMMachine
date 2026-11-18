package com.swordfish.lemuroid.app.gemrating

import android.content.Context

/** Local discovery preferences only. This is not an account or cloud store. */
internal class GemDiscoveryPreferences(
    context: Context,
    private val session: GemRatingRepository.Session,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences("gem_discovery_v1", Context.MODE_PRIVATE)

    init {
        GemRatingRepository.requireUuid(session.profileId)
        require(session.role != GemRatingRepository.Role.JUNIOR)
    }

    private val prefix = session.profileId + ":"

    fun wildcardPercent(): Int = prefs.getInt(prefix + "wildcard", 30).coerceIn(20, 50)

    fun saveWildcardPercent(percent: Int): Boolean {
        require(percent in 20..50)
        return prefs.edit().putInt(prefix + "wildcard", percent).commit()
    }

    fun recentIds(): List<String> =
        prefs.getString(prefix + "recent", "").orEmpty().split('\n').filter { it.isNotBlank() }.takeLast(20)

    fun rememberSurprise(releaseId: String): Boolean {
        require(releaseId.isNotBlank() && '\n' !in releaseId)
        val recent = (recentIds().filterNot { it == releaseId } + releaseId).takeLast(20)
        return prefs.edit().putString(prefix + "recent", recent.joinToString("\n")).commit()
    }
}
