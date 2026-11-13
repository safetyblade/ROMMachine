package com.swordfish.lemuroid.app.mobile.feature.shelf

/**
 * First-party appearance collections are colour-only in Phase 1.
 * Future original shell patterns and lighting may extend this catalogue,
 * but no copyrighted game artwork or game pixels belong to a design.
 */
data class CartdeckCuratedDesign(
    val id: String,
    val title: String,
    val description: String,
    val baseEdition: String,
    val colours: Map<String, Long>,
)

object CartdeckCuratedDesigns {
    const val STEALTH_ACID_ID = "stealth-acid-001"

    val designs: List<CartdeckCuratedDesign> =
        listOf(
            CartdeckCuratedDesign(
                id = STEALTH_ACID_ID,
                title = "Stealth / Acid",
                description = "Tactical graphite, olive hardware and acid-green accents",
                baseEdition = "original",
                colours =
                    mapOf(
                        "chassis" to 0xFF151C20L,
                        "secondary" to 0xFF465840L,
                        "bezel" to 0xFF0B1112L,
                        "glass" to 0xFF070D0DL,
                        "button" to 0xFF253028L,
                        "accent" to 0xFF9BC74AL,
                        "onChassis" to 0xFFD6DED2L,
                        "onButton" to 0xFFF0F5E9L,
                        "focus" to 0xFFE5A13AL,
                        "roomBackdrop" to 0xFF0A100DL,
                        "shelfBackboard" to 0xFF17211AL,
                        "shelfDeck" to 0xFF364337L,
                        "shelfLip" to 0xFF67795DL,
                        "shelfGroove" to 0xFF0D160FL,
                        "edgeHighlight" to 0xFF8DA276L,
                        "shelfShadow" to 0xFF050805L,
                        "sectionDivider" to 0xFF526148L,
                        "vacantCell" to 0xFF243127L,
                        "sectionText" to 0xFFE7F0DFL,
                        "focusOutline" to 0xFFC6E468L,
                        "interactionAccent" to 0xFFB5DA53L,
                    ),
            ),
        )

    fun byId(id: String?): CartdeckCuratedDesign? = designs.firstOrNull { it.id == id }

    fun apply(
        appearance: ShelfAppearance,
        designId: String,
    ): ShelfAppearance {
        val design = requireNotNull(byId(designId)) { "Unknown CARTDECK curated design" }
        return appearance.copy(
            edition = design.baseEdition,
            custom = design.colours,
            activeProfileId = null,
            curatedDesignId = design.id,
        )
    }
}
