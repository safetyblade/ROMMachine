package com.swordfish.lemuroid.app.gemrating

import java.util.Locale

/** Pure projection: never infer a rating or owned release solely from a title. */
internal object GemDiscovery {
    enum class Scope { FOR_YOU, MY_SHELF, DISCOVER }

    data class ShelfEntry(val cartridgeId: String, val systemId: String, val title: String)

    data class Input(
        val catalogue: List<GemRatingRecommender.Game>,
        val ratings: List<GemRatingRecommender.Rating>,
        val ownedIds: Set<String>,
        val possibleOwnedIds: Set<String>,
        val unlinkedRatings: Int,
    ) {
        fun candidates(scope: Scope): Set<String> =
            when (scope) {
                Scope.FOR_YOU -> catalogue.map { it.releaseId }.toSet() - possibleOwnedIds + ownedIds
                Scope.MY_SHELF -> ownedIds
                Scope.DISCOVER -> catalogue.map { it.releaseId }.toSet() - ownedIds - possibleOwnedIds
            }
    }

    private val genres =
        setOf(
            "action", "adventure", "fighting", "platform", "puzzle", "racing", "role-playing",
            "shooter", "simulation", "sports", "strategy",
        )
    private val nonGames =
        Regex(
            "\\b(demo|prototype|bios|firmware|sample|trial|map pack|bonus disc|video disc|double pack|" +
                "triple pack|compilation|soundtrack|system update|test disc|expansion pack|dlc)\\b",
            RegexOption.IGNORE_CASE,
        )

    private fun normalized(s: String): String = s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    fun prepare(
        releases: List<GemCataloguePackage.Release>,
        records: List<GemRatingRepository.Rating>,
        links: Map<String, String>,
        shelf: List<ShelfEntry>,
    ): Input {
        val catalogue =
            releases.map { release ->
                val features = GemGameplayFeatures.find(release.releaseId, release.title)
                val eligible =
                    release.primaryGenre?.let(::normalized) in genres && !nonGames.containsMatchIn(release.title)
                GemRatingRecommender.Game(
                    releaseId = release.releaseId,
                    title = release.title,
                    platform = release.systemId,
                    primaryGenre = release.primaryGenre,
                    crossoverGenre = features?.crossover,
                    playstyle = features?.playstyle,
                    mechanics = features?.mechanics.orEmpty(),
                    eligibility =
                        if (eligible) {
                            GemRatingRecommender.Eligibility.GAME
                        } else {
                            GemRatingRecommender.Eligibility.RESEARCH_ONLY
                        },
                    evidence =
                        if (features == null) {
                            GemRatingRecommender.Evidence.WEAK_CONTEXT
                        } else {
                            GemRatingRecommender.Evidence.EDITORIAL_PROVISIONAL
                        },
                )
            }
        val byId = catalogue.associateBy { it.releaseId }
        require(byId.size == catalogue.size)
        val installed = shelf.associateBy { it.cartridgeId }
        val owned =
            shelf.mapNotNull { entry ->
                links[entry.cartridgeId]?.takeIf { byId[it]?.platform == entry.systemId }
            }.toSet()
        val possibleKeys =
            shelf.filter { entry ->
                val link = links[entry.cartridgeId]
                link == null || byId[link]?.platform != entry.systemId
            }.map { it.systemId to normalized(it.title) }.toSet()
        val possible =
            catalogue.filter { (it.platform to normalized(it.title)) in possibleKeys }.map { it.releaseId }.toSet()
        val active = records.filter { it.score != null }
        val linked =
            active.mapNotNull { rating ->
                val releaseId = rating.catalogueReleaseId ?: links[rating.cartridgeId]
                val release = byId[releaseId] ?: return@mapNotNull null
                val platform = installed[rating.cartridgeId]?.systemId ?: rating.platformId
                if (platform != release.platform || release.eligibility != GemRatingRecommender.Eligibility.GAME) {
                    return@mapNotNull null
                }
                release.releaseId to requireNotNull(rating.score).toDouble()
            }
        // Independent cartridge scores stay intact; project one mean per explicitly linked release only for ranking.
        val ratings =
            linked.groupBy({ it.first }, { it.second }).map { (id, scores) ->
                GemRatingRecommender.Rating(id, scores.average())
            }.sortedBy { it.releaseId }
        return Input(catalogue, ratings, owned, possible, active.size - linked.size)
    }
}
