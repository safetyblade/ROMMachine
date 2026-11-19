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

    private val whitespace = Regex("\\s+")

    private fun clean(value: String?): String? {
        val token = value?.trim()?.lowercase(java.util.Locale.ROOT)?.replace(whitespace, " ")
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

    private data class Training(
        val game: Game,
        val vector: Map<String, Double>,
        val magnitude: Double,
        val preference: Double,
    )

    private data class FeaturePosting(val index: Int, val weight: Double)

    private fun magnitude(vector: Map<String, Double>): Double = sqrt(vector.values.sumOf { it * it })

    /** Exact sparse cosine: compare only training vectors sharing at least one feature. */
    private fun matchingDots(
        vector: Map<String, Double>,
        postings: Map<String, List<FeaturePosting>>,
    ): Map<Int, Double> {
        val dots = mutableMapOf<Int, Double>()
        for ((key, weight) in vector) {
            for (posting in postings[key].orEmpty()) {
                dots[posting.index] = (dots[posting.index] ?: 0.0) + weight * posting.weight
            }
        }
        return dots
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
        if (ratings.isEmpty()) return emptyList() // explicit cold start
        val indexed = catalogue.associateBy { it.releaseId }
        require(indexed.size == catalogue.size) { "Duplicate release ID" }
        require(ratings.map { it.releaseId }.distinct().size == ratings.size) { "Duplicate user rating" }

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
                val vector = features(game)
                if (vector.isEmpty()) null else Training(game, vector, magnitude(vector), (rating - 5.5) / 4.5)
            }
        if (training.isEmpty()) return emptyList()
        val postings = mutableMapOf<String, MutableList<FeaturePosting>>()
        training.forEachIndexed { index, item ->
            item.vector.forEach { (feature, weight) ->
                postings.getOrPut(feature) { mutableListOf() }.add(FeaturePosting(index, weight))
            }
        }

        val candidates =
            catalogue.asSequence()
                .filter { it.releaseId !in ratedIds && it.eligibility == Eligibility.GAME }
                .filter { candidateIds == null || it.releaseId in candidateIds }
                .filter { it.verifiedWorkId == null || it.verifiedWorkId !in ratedWorks }
                .mapNotNull { game ->
                    val vector = features(game)
                    if (vector.isEmpty()) return@mapNotNull null
                    val dots = matchingDots(vector, postings)
                    if (dots.isEmpty()) return@mapNotNull null
                    val norm = magnitude(vector)
                    var numerator = 0.0
                    var denominator = 0.0
                    var bestIndex = -1
                    var bestMatch = 0.0
                    var bestWeight = 0.0
                    // Sorted indices preserve the original rating order and deterministic ties.
                    for (index in dots.keys.sorted()) {
                        val item = training[index]
                        val match = dots.getValue(index) / (norm * item.magnitude)
                        val weighted = match * item.preference
                        numerator += weighted
                        denominator += match
                        if (weighted > bestWeight) {
                            bestIndex = index
                            bestMatch = match
                            bestWeight = weighted
                        }
                    }
                    if (numerator <= 0.0 || bestIndex < 0) return@mapNotNull null
                    val preference = numerator / denominator
                    val best = training[bestIndex]
                    val common = vector.keys.intersect(best.vector.keys).sorted()
                    val confidence =
                        when (game.evidence) {
                            Evidence.SOURCE_BACKED -> 1.0
                            Evidence.EDITORIAL_PROVISIONAL -> 0.75
                            Evidence.WEAK_CONTEXT -> 0.40
                        }
                    val meaningfulBridge = common.any { it.startsWith("mechanic:") || it.startsWith("crossover:") }
                    val score = preference * bestMatch * confidence * (if (meaningfulBridge) 1.0 else 0.35)
                    val novelGenre = clean(game.primaryGenre)?.let { it !in likedGenres } == true
                    val sourceGenre = clean(best.game.primaryGenre)
                    val targetGenre = clean(game.primaryGenre)
                    val genreDifference = novelGenre && sourceGenre != null && targetGenre != null
                    val sourceStyle = clean(best.game.playstyle)
                    val targetStyle = clean(game.playstyle)
                    val styleDifference = sourceStyle != null && targetStyle != null && sourceStyle != targetStyle
                    val sourceCross = clean(best.game.crossoverGenre)
                    val targetCross = clean(game.crossoverGenre)
                    val crossoverDifference = sourceCross != null && targetCross != null && sourceCross != targetCross
                    val sourceMechanics = best.game.mechanics.mapNotNull(::clean).toSet()
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
                        "Similar to ${best.game.title}: " +
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
