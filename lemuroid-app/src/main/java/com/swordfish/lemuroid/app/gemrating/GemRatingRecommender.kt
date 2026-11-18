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
        val novelty: Double = 0.0,
        val meaningfulBridge: Boolean = false,
    )

    private val ignored =
        setOf(
            "unknown", "unclassified", "misc", "miscellaneous", "other", "application",
            "compilation", "arcade", "vertical arcade", "none", "n/a",
        )

    private fun clean(value: String?): String? {
        val token = value?.trim()?.lowercase(java.util.Locale.ROOT)?.replace(Regex("\\s+"), " ")
        if (token.isNullOrEmpty() || token in ignored) return null
        return when (token) {
            "fps", "first person", "first-person shooter" -> "first-person"
            "rpg", "role playing", "role-playing" -> "role-playing"
            "card-based combat", "card battler", "card battles" -> "card combat"
            "deck building", "deck-building" -> "deckbuilding"
            "grid strategy", "grid-based strategy" -> "grid combat"
            else -> token
        }
    }

    /** The vector is deliberately sparse: absence of evidence is not a negative signal. */
    private fun features(game: Game): Map<String, Double> =
        buildMap {
            clean(game.primaryGenre)?.let { put("genre:$it", 0.25) }
            clean(game.crossoverGenre)?.let { put("crossover:$it", 0.25) }
            clean(game.playstyle)?.let { put("playstyle:$it", 0.30) }
            val mechanics = game.mechanics.mapNotNull(::clean).distinct()
            mechanics.forEach { put("mechanic:$it", 0.50 / mechanics.size) }
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
     * Wildcards are eligible positive matches with meaningful feature overlap and greater novelty.
     */
    fun recommend(
        catalogue: List<Game>,
        ratings: List<Rating>,
        limit: Int = 10,
        wildcardShare: Double = 0.30,
        candidateIds: Set<String>? = null,
    ): List<Recommendation> {
        require(limit >= 0)
        require(wildcardShare in 0.0..0.5)
        if (limit == 0) return emptyList()
        val candidates = rank(catalogue, ratings, candidateIds)
        val standardCount =
            candidates.filterNot { it.wildcard }.distinctBy { it.game.verifiedWorkId ?: it.game.releaseId }.size
        val wildcardSlots =
            minOf((limit * wildcardShare).toInt(), (standardCount * wildcardShare / (1.0 - wildcardShare)).toInt())
        val selected = mutableListOf<Recommendation>()
        val seenWorks = mutableSetOf<String>()

        fun add(candidate: Recommendation): Boolean {
            val work = candidate.game.verifiedWorkId
            if (work != null && !seenWorks.add(work)) return false
            selected.add(candidate)
            return true
        }
        candidates.filter { it.wildcard }.forEach {
            if (selected.size < wildcardSlots) add(it)
        }
        candidates.filterNot { it.wildcard }.forEach {
            if (selected.size < limit) add(it)
        }
        // A verified work shared by a wildcard and standard must not cause a quota spill.
        val standardSelected = selected.count { !it.wildcard }
        val allowedWildcards = (standardSelected * wildcardShare / (1.0 - wildcardShare)).toInt()
        val final =
            selected.filterNot { it.wildcard } + selected.filter { it.wildcard }.take(allowedWildcards)
        return final.sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.game.releaseId })
    }

    /** Stronger novelty than the mixed feed, but never random or based solely on a broad genre. */
    fun surprise(
        catalogue: List<Game>,
        ratings: List<Rating>,
        recentIds: Set<String> = emptySet(),
        candidateIds: Set<String>? = null,
    ): Recommendation? =
        rank(catalogue, ratings, candidateIds)
            .filter { it.meaningfulBridge && it.novelty >= 0.40 && it.game.releaseId !in recentIds }
            .sortedWith(
                compareByDescending<Recommendation> { it.score * (0.25 + 1.75 * it.novelty) }
                    .thenByDescending { it.novelty }.thenBy { it.game.releaseId },
            ).firstOrNull()

    private fun rank(
        catalogue: List<Game>,
        ratings: List<Rating>,
        candidateIds: Set<String>?,
    ): List<Recommendation> {
        require(ratings.all { it.score.isFinite() && it.score in 1.0..10.0 }) { "Ratings must be 1–10" }
        val indexed = catalogue.associateBy { it.releaseId }
        require(indexed.size == catalogue.size) { "Duplicate release ID" }
        require(ratings.map { it.releaseId }.distinct().size == ratings.size) { "Duplicate user rating" }
        if (ratings.isEmpty()) return emptyList() // explicit cold start

        val known =
            ratings.sortedBy { it.releaseId }.mapNotNull { rating ->
                indexed[rating.releaseId]?.takeIf { it.eligibility == Eligibility.GAME }?.let { it to rating.score }
            }
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
                .filter { candidateIds == null || it.releaseId in candidateIds }
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
                    val meaningfulBridge = common.any { it.startsWith("mechanic:") || it.startsWith("crossover:") }
                    val score = preference * best.second * confidence * (if (meaningfulBridge) 1.0 else 0.35)
                    val novelGenre = clean(game.primaryGenre)?.let { it !in likedGenres } == true
                    val sourceGenre = clean(best.first.primaryGenre)
                    val targetGenre = clean(game.primaryGenre)
                    val genreDifference = novelGenre && sourceGenre != null && targetGenre != null
                    val sourceStyle = clean(best.first.playstyle)
                    val targetStyle = clean(game.playstyle)
                    val styleDifference = sourceStyle != null && targetStyle != null && sourceStyle != targetStyle
                    val sourceCross = clean(best.first.crossoverGenre)
                    val targetCross = clean(game.crossoverGenre)
                    val crossoverDifference = sourceCross != null && targetCross != null && sourceCross != targetCross
                    val sourceMechanics = best.first.mechanics.mapNotNull(::clean).toSet()
                    val targetMechanics = game.mechanics.mapNotNull(::clean).toSet()
                    val mechanicNovelty =
                        if (sourceMechanics.isNotEmpty() && targetMechanics.isNotEmpty()) {
                            val overlap =
                                sourceMechanics.intersect(targetMechanics).size.toDouble() /
                                    sourceMechanics.union(targetMechanics).size
                            0.35 * (1.0 - overlap)
                        } else {
                            0.0
                        }
                    val genreNovelty = if (genreDifference) 0.45 else 0.0
                    val styleNovelty = if (styleDifference) 0.25 else 0.0
                    val crossoverNovelty = if (crossoverDifference) 0.30 else 0.0
                    val novelty = (genreNovelty + styleNovelty + crossoverNovelty + mechanicNovelty).coerceAtMost(1.0)
                    val differences =
                        listOfNotNull(
                            targetGenre?.takeIf { genreDifference },
                            targetStyle?.takeIf { styleDifference },
                            targetCross?.takeIf { crossoverDifference },
                        ).distinct()
                    val explanation =
                        "Similar to ${best.first.title}: " +
                            common.map { it.substringAfter(':') }.distinct().joinToString(", ") +
                            (if (differences.isEmpty()) "" else ". Explore: ${differences.joinToString(", ")}")
                    val wildcard = meaningfulBridge && novelty >= 0.25
                    Recommendation(
                        game = game,
                        score = score,
                        reason = explanation,
                        wildcard = wildcard,
                        novelty = novelty,
                        meaningfulBridge = meaningfulBridge,
                    )
                }
                .sortedWith(compareByDescending<Recommendation> { it.score }.thenBy { it.game.releaseId })
                .toList()

        return candidates
    }
}
