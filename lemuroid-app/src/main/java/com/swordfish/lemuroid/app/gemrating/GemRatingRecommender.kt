package com.swordfish.lemuroid.app.gemrating

import kotlin.math.sqrt

/** Pure, offline recommendation calculation. No ROM, save, network or database access. */
object GemRatingRecommender {
    enum class Eligibility { GAME, RESEARCH_ONLY, EXCLUDED }

    enum class Evidence { SOURCE_BACKED, EDITORIAL_PROVISIONAL, WEAK_CONTEXT }

    data class Game(
        val releaseId: String,
        val title: String,
        val platform: String,
        val verifiedWorkId: String? = null,
        val primaryGenre: String? = null,
        val crossoverGenre: String? = null,
        val playstyle: String? = null,
        val mechanics: Set<String> = emptySet(),
        val eligibility: Eligibility = Eligibility.RESEARCH_ONLY,
        val evidence: Evidence = Evidence.WEAK_CONTEXT,
    )

    data class Rating(val releaseId: String, val score: Double)

    data class Recommendation(
        val game: Game,
        val score: Double,
        val reason: String,
        val wildcard: Boolean,
    )

    private val ignored =
        setOf(
            "unknown", "unclassified", "misc", "miscellaneous", "other", "application",
            "compilation", "arcade", "vertical arcade", "none", "n/a",
        )

    private fun clean(value: String?): String? =
        value?.trim()?.lowercase()
            ?.replace(Regex("\\s+"), " ")?.takeIf { it.isNotEmpty() && it !in ignored }

    /** The vector is deliberately sparse: absence of evidence is not a negative signal. */
    private fun features(game: Game): Map<String, Double> =
        buildMap {
            clean(game.primaryGenre)?.let { put("genre:$it", 0.25) }
            clean(game.crossoverGenre)?.let { put("crossover:$it", 0.25) }
            clean(game.playstyle)?.let { put("playstyle:$it", 0.30) }
            game.mechanics.mapNotNull(::clean).distinct().forEach { put("mechanic:$it", 0.20) }
        }

    private fun similarity(
        a: Map<String, Double>,
        b: Map<String, Double>,
    ): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val overlap = a.keys.intersect(b.keys).sumOf { key -> a.getValue(key) * b.getValue(key) }
        val magnitudeA = sqrt(a.values.sumOf { it * it })
        val magnitudeB = sqrt(b.values.sumOf { it * it })
        return if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0 else overlap / (magnitudeA * magnitudeB)
    }

    /**
     * Only explicit player ratings are training signals. Roxy/critic/DeckFit do not enter this API.
     * A verified work identity is used only for deduplication, never inferred from a shared title.
     * Wildcards are eligible positive matches with a genre not present among positively rated games.
     */
    fun recommend(
        catalogue: List<Game>,
        ratings: List<Rating>,
        limit: Int = 10,
        wildcardShare: Double = 0.30,
    ): List<Recommendation> {
        require(limit >= 0) { "limit must be non-negative" }
        require(wildcardShare in 0.0..0.5) { "wildcardShare must be 0–0.5" }
        require(ratings.all { it.score.isFinite() && it.score in 1.0..10.0 }) { "Ratings must be 1–10" }
        val indexed = catalogue.associateBy { it.releaseId }
        require(indexed.size == catalogue.size) { "Duplicate release ID" }
        require(ratings.map { it.releaseId }.distinct().size == ratings.size) { "Duplicate user rating" }
        if (limit == 0 || ratings.isEmpty()) return emptyList() // explicit cold start

        val known = ratings.mapNotNull { rating -> indexed[rating.releaseId]?.let { it to rating.score } }
        if (known.isEmpty()) return emptyList()
        val ratedIds = ratings.map { it.releaseId }.toSet()
        val ratedWorks = known.mapNotNull { it.first.verifiedWorkId }.toSet()
        val likedGenres = known.filter { it.second > 5.5 }.mapNotNull { clean(it.first.primaryGenre) }.toSet()
        val training =
            known.mapNotNull { (game, rating) ->
                features(game).takeIf { it.isNotEmpty() }?.let { Triple(game, it, (rating - 5.5) / 4.5) }
            }
        if (training.isEmpty()) return emptyList()

        val candidates =
            catalogue.asSequence()
                .filter { it.releaseId !in ratedIds && it.eligibility == Eligibility.GAME }
                .filter { it.verifiedWorkId == null || it.verifiedWorkId !in ratedWorks }
                .mapNotNull { game ->
                    val vector = features(game)
                    if (vector.isEmpty()) return@mapNotNull null
                    val pairs =
                        training.mapNotNull { (ratedGame, ratedFeatures, preference) ->
                            val match = similarity(vector, ratedFeatures)
                            if (match <= 0.0) null else Triple(ratedGame, match, preference)
                        }
                    if (pairs.isEmpty()) return@mapNotNull null
                    // Dislikes contribute negative evidence without banning an entire genre.
                    val preference = pairs.sumOf { it.second * it.third } / pairs.sumOf { it.second }
                    if (preference <= 0.0) return@mapNotNull null
                    val best =
                        pairs.filter { it.third > 0.0 }.maxByOrNull { it.second * it.third }
                            ?: return@mapNotNull null
                    val common = vector.keys.intersect(features(best.first).keys).sorted()
                    val confidence =
                        when (game.evidence) {
                            Evidence.SOURCE_BACKED -> 1.0
                            Evidence.EDITORIAL_PROVISIONAL -> 0.75
                            Evidence.WEAK_CONTEXT -> 0.40
                        }
                    val score = preference * best.second * confidence
                    val novelGenre = clean(game.primaryGenre)?.let { it !in likedGenres } == true
                    Recommendation(
                        game = game,
                        score = score,
                        reason = "Similar to ${best.first.title}: ${common.joinToString(
                            ", ",
                        ) { it.substringAfter(':') }}",
                        wildcard = novelGenre,
                    )
                }
                .sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.game.releaseId })
                .toList()

        val wildcardSlots = (limit * wildcardShare).toInt()
        val wildcards = candidates.filter { it.wildcard }.take(wildcardSlots)
        val standards = candidates.filterNot { it in wildcards }.take(limit - wildcards.size)
        // Final order stable. No auto-collapse for same-title releases lacking verified work IDs.
        return (standards + wildcards).sortedWith(
            compareByDescending<Recommendation> { it.score }.thenBy { it.game.releaseId },
        ).take(limit)
    }
}
