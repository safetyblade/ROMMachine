package com.swordfish.lemuroid.app.gemrating

/**
 * Small, separately authored editorial feature layer. Exact snapshot IDs and titles must
 * both match before enrichment. These classifications are provisional, not device QA.
 */
internal object GemGameplayFeatures {
    data class Entry(
        val title: String,
        val crossover: String? = null,
        val playstyle: String? = null,
        val mechanics: Set<String> = emptySet(),
    )

    val entries =
        mapOf(
            "VDG:gba:794" to Entry(
                "Advance Wars",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Resource management"),
            ),
            "VDG:gba:795" to Entry(
                "Advance Wars 2: Black Hole Rising",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Resource management"),
            ),
            "VDG:gba:798" to Entry(
                "Fire Emblem",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Unit progression"),
            ),
            "VDG:gba:799" to Entry(
                "Fire Emblem: The Sacred Stones",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Unit progression"),
            ),
            "VDG:ds:2056" to Entry(
                "Advance Wars: Dual Strike",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Resource management"),
            ),
            "VDG:ds:1457" to Entry(
                "Fire Emblem: Shadow Dragon",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Unit progression"),
            ),
            "VDG:3ds:365" to Entry(
                "Fire Emblem: Awakening",
                "Tactics",
                "Turn-based",
                setOf("Grid combat", "Unit progression"),
            ),
            "VDG:gb:94" to Entry(
                "Pokémon Trading Card Game",
                "Card game",
                "Turn-based",
                setOf("Deckbuilding", "Card combat", "Collecting"),
            ),
            "VDG:xb:row-85" to Entry(
                "Phantom Dust",
                "Card game",
                "Real-time",
                setOf("Deckbuilding", "Arena combat"),
            ),
            "VDG:ds:2065" to Entry(
                "Culdcept DS",
                "Card game",
                "Turn-based",
                setOf("Deckbuilding", "Resource management"),
            ),
            "VDG:3ds:489" to Entry(
                "Culdcept Revolt",
                "Card game",
                "Turn-based",
                setOf("Deckbuilding", "Resource management"),
            ),
            "VDG:xb:row-522" to Entry(
                "Halo: Combat Evolved",
                playstyle = "First-person",
                mechanics = setOf("Campaign", "First-person combat"),
            ),
            "VDG:n64:202" to Entry(
                "GoldenEye 007",
                playstyle = "First-person",
                mechanics = setOf("Campaign", "First-person combat"),
            ),
            "VDG:ds:85" to Entry(
                "GoldenEye 007 (2010)",
                playstyle = "First-person",
                mechanics = setOf("Campaign", "First-person combat"),
            ),
            "VDG:n64:153" to Entry(
                "Mario Kart 64",
                "Kart racing",
                "Arcade-style",
                setOf("Items", "Lap racing"),
            ),
            "VDG:gba:544" to Entry(
                "Mario Kart: Super Circuit",
                "Kart racing",
                "Arcade-style",
                setOf("Items", "Lap racing"),
            ),
            "VDG:ds:1362" to Entry(
                "Mario Kart DS",
                "Kart racing",
                "Arcade-style",
                setOf("Items", "Lap racing"),
            ),
            "VDG:3ds:337" to Entry(
                "Mario Kart 7",
                "Kart racing",
                "Arcade-style",
                setOf("Items", "Lap racing"),
            ),
            "VDG:wiiu:row-113" to Entry(
                "Mario Kart 8",
                "Kart racing",
                "Arcade-style",
                setOf("Items", "Lap racing"),
            ),
            "VDG:n64:144" to Entry(
                "F-Zero X",
                playstyle = "Arcade-style",
                mechanics = setOf("Lap racing", "High speed"),
            ),
            "VDG:gba:527" to Entry(
                "F-Zero: GP Legend",
                playstyle = "Arcade-style",
                mechanics = setOf("Lap racing", "High speed"),
            ),
            "VDG:gba:528" to Entry(
                "F-Zero: Maximum Velocity",
                playstyle = "Arcade-style",
                mechanics = setOf("Lap racing", "High speed"),
            ),
            "VDG:ds:1434" to Entry(
                "Dragon Quest Monsters: Joker",
                playstyle = "Turn-based",
                mechanics = setOf("Collecting", "Team building"),
            ),
            "VDG:ds:1435" to Entry(
                "Dragon Quest Monsters: Joker 2",
                playstyle = "Turn-based",
                mechanics = setOf("Collecting", "Team building"),
            ),
            "VDG:n64:193" to Entry(
                "Paper Mario",
                playstyle = "Turn-based",
                mechanics = setOf("Timing", "Unit progression"),
            ),
            "VDG:gb:46" to Entry(
                "Tetris",
                playstyle = "Fixed-screen",
                mechanics = setOf("Falling blocks", "Score attack"),
            ),
            "VDG:ds:1295" to Entry(
                "Tetris DS",
                playstyle = "Fixed-screen",
                mechanics = setOf("Falling blocks", "Score attack"),
            ),
            "VDG:3ds:325" to Entry(
                "Puyo Puyo Tetris",
                playstyle = "Fixed-screen",
                mechanics = setOf("Falling blocks", "Score attack"),
            ),
        )

    fun find(
        id: String,
        title: String,
    ): Entry? = entries[id]?.takeIf { it.title == title }
}
