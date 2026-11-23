package com.swordfish.lemuroid.app.gemrating

import java.util.Locale

/**
 * Controlled, reversible projection of licensed source genres. Original source text stays in
 * immutable GEM shards. No title-to-game identity, ROM compatibility, or user score is inferred.
 */
internal object GemGenreTaxonomy {
    data class Classification(
        val primary: String?,
        val crossover: String? = null,
        val playstyle: String? = null,
        val mechanics: Set<String> = emptySet(),
        val contentType: String = "Unknown",
        val reviewFlag: Boolean = false,
    ) {
        val gameEligible: Boolean get() = contentType == "Game" && primary != null
    }

    private val whitespace = Regex("\\s+")

    private fun key(raw: String?): String? =
        raw?.trim()?.lowercase(Locale.ROOT)?.replace(whitespace, " ")?.takeIf { it.isNotEmpty() }

    private val nonGame =
        setOf(
            "application",
            "firmware",
            "bios",
            "system utility",
            "operating system",
            "video",
        )

    private val ambiguous =
        setOf(
            "miscellaneous",
            "misc",
            "unknown",
            "other",
            "none",
            "n/a",
        )

    // The keys preserve source vocabulary. The values are supported broad game styles, not
    // claims about specific controls or compatibility on a handset.
    private val genres =
        mapOf(
            "action" to "Action",
            "action-adventure" to "Action",
            "action adventure" to "Action",
            "action role-playing" to "Role-Playing",
            "action rpg" to "Role-Playing",
            "adventure" to "Adventure",
            "visual novel" to "Adventure",
            "text adventure" to "Adventure",
            "interactive fiction" to "Adventure",
            "point-and-click" to "Adventure",
            "dating simulation" to "Simulation",
            "fighting" to "Fighting",
            "2d fighting" to "Fighting",
            "3d fighting" to "Fighting",
            "arena fighter" to "Fighting",
            "beat 'em up" to "Fighting",
            "hack and slash" to "Action",
            "platform" to "Platform",
            "platformer" to "Platform",
            "2d platformer" to "Platform",
            "3d platformer" to "Platform",
            "puzzle platformer" to "Platform",
            "run and gun" to "Platform",
            "puzzle" to "Puzzle",
            "quiz and trivia" to "Puzzle",
            "trivia" to "Puzzle",
            "pinball" to "Puzzle",
            "board, card and tabletop" to "Strategy",
            "board game" to "Strategy",
            "card game" to "Strategy",
            "collectible card game" to "Strategy",
            "poker" to "Strategy",
            "gambling" to "Strategy",
            "racing" to "Racing",
            "horse racing" to "Racing",
            "vehicular combat" to "Action",
            "role-playing" to "Role-Playing",
            "role playing" to "Role-Playing",
            "role-playing game" to "Role-Playing",
            "tactical role-playing" to "Role-Playing",
            "massively multiplayer role-playing" to "Role-Playing",
            "shooter" to "Shooter",
            "first-person shooter" to "Shooter",
            "third-person shooter" to "Shooter",
            "shoot 'em up" to "Shooter",
            "shooting gallery and light gun" to "Shooter",
            "rail shooter" to "Shooter",
            "simulation" to "Simulation",
            "life simulation" to "Simulation",
            "flight simulation" to "Simulation",
            "vehicle simulation" to "Simulation",
            "management and construction" to "Simulation",
            "construction and management" to "Simulation",
            "god game" to "Simulation",
            "sandbox" to "Simulation",
            "sports" to "Sports",
            "association football" to "Sports",
            "american football" to "Sports",
            "basketball" to "Sports",
            "baseball" to "Sports",
            "volleyball" to "Sports",
            "tennis" to "Sports",
            "wrestling" to "Sports",
            "boxing" to "Sports",
            "fishing and hunting" to "Sports",
            "track and field" to "Sports",
            "fitness" to "Sports",
            "dance" to "Music",
            "music" to "Music",
            "rhythm" to "Music",
            "rhythm game" to "Music",
            "party" to "Party",
            "educational" to "Educational",
            "virtual pet" to "Simulation",
            "karaoke" to "Music",
            "boxing and martial arts" to "Sports",
            "hockey" to "Sports",
            "mahjong" to "Strategy",
            "golf" to "Sports",
            "skateboarding and extreme sports" to "Sports",
            "kart racing" to "Racing",
            "interactive movie" to "Adventure",
            "hidden object" to "Puzzle",
            "winter sports" to "Sports",
            "sim racing" to "Racing",
            "chess" to "Strategy",
            "shogi and go" to "Strategy",
            "metroidvania" to "Platform",
            "billiards" to "Sports",
            "ball and paddle" to "Action",
            "off-road racing" to "Racing",
            "logic puzzle" to "Puzzle",
            "motorcycle racing" to "Racing",
            "japanese role-playing" to "Role-Playing",
            "maze" to "Puzzle",
            "artillery" to "Strategy",
            "boat and watercraft racing" to "Racing",
            "cricket" to "Sports",
            "bowling" to "Sports",
            "roguelike" to "Roguelike",
            "sports management" to "Simulation",
            "futuristic racing" to "Racing",
            "dungeon crawler" to "Role-Playing",
            "tile-matching" to "Puzzle",
            "falling-block puzzle" to "Puzzle",
            "endless runner" to "Platform",
            "multiplayer online battle arena" to "Strategy",
            "twin-stick shooter" to "Shooter",
            "compilation" to "Collection",
            "anthology" to "Collection",
            "edutainment" to "Educational",
            "strategy" to "Strategy",
            "turn-based strategy" to "Strategy",
            "real-time strategy" to "Strategy",
            "4x" to "Strategy",
            "tower defense" to "Strategy",
            "stealth" to "Action",
            "survival horror" to "Adventure",
            "open world" to "Action",
        )
    private val reviewTitle =
        Regex(
            "\\b(bios|firmware|system update|demo disc|test disc|bonus disc|soundtrack|map pack)\\b",
            RegexOption.IGNORE_CASE,
        )

    fun classify(
        sourceGenre: String?,
        title: String,
    ): Classification {
        val token = key(sourceGenre)
        if (token == null || token in ambiguous) {
            return Classification(null, contentType = "Needs review", reviewFlag = true)
        }
        if (token in nonGame) {
            return Classification(null, contentType = "Non-game", reviewFlag = false)
        }
        val genre = genres[token]
        if (genre == null) {
            return Classification(null, contentType = "Needs review", reviewFlag = true)
        }
        val crossover =
            when (token) {
                "action-adventure", "action adventure" -> "Adventure"
                "action role-playing", "action rpg" -> "Action"
                "tactical role-playing" -> "Tactics"
                "puzzle platformer" -> "Puzzle"
                "board, card and tabletop", "board game" -> "Board game"
                "card game", "collectible card game" -> "Card game"
                "survival horror" -> "Horror"
                "music", "rhythm", "rhythm game", "dance" -> "Rhythm"
                "open world" -> "Exploration"
                else -> null
            }
        val playstyle =
            when (token) {
                "first-person shooter" -> "First-person"
                "third-person shooter" -> "Third-person"
                "turn-based strategy" -> "Turn-based"
                "real-time strategy" -> "Real-time"
                "point-and-click" -> "Point-and-click"
                else -> null
            }
        val mechanics =
            when (token) {
                "platformer", "platform", "2d platformer", "3d platformer" -> setOf("Platforming")
                "puzzle platformer" -> setOf("Platforming", "Puzzle solving")
                "collectible card game" -> setOf("Card collecting", "Card play")
                "card game" -> setOf("Card play")
                "turn-based strategy" -> setOf("Strategy")
                else -> emptySet()
            }
        val flagged = reviewTitle.containsMatchIn(title)
        return Classification(
            primary = genre,
            crossover = crossover,
            playstyle = playstyle,
            mechanics = mechanics,
            contentType = if (flagged) "Needs review" else "Game",
            reviewFlag = flagged,
        )
    }

    fun knownSourceGenres(): Set<String> = genres.keys + nonGame + ambiguous
}
