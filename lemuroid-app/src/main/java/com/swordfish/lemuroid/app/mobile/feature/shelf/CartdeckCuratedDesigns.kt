package com.swordfish.lemuroid.app.mobile.feature.shelf

/**
 * First-party appearance collections use original colours and bounded surface art.
 * No copyrighted game artwork or game pixels belong to a design.
 */
data class CartdeckCuratedDesign(
    val id: String,
    val title: String,
    val description: String,
    val baseEdition: String,
    val colours: Map<String, Long>,
    val patternId: String = CartdeckSurfaceDesign.PATTERN_NONE,
    val stampId: String = CartdeckSurfaceDesign.STAMP_NONE,
)

object CartdeckCuratedDesigns {
    const val SIGNATURE_ID = "signature-005"
    const val LIGHT_ID = "light-004"
    const val STEALTH_ACID_ID = "stealth-acid-001"
    const val ARCANA_GILDED_ID = "arcana-gilded-002"
    const val WILDBOUND_FORGE_ID = "wildbound-forge-003"

    val designs: List<CartdeckCuratedDesign> =
        listOf(
            CartdeckCuratedDesign(
                id = SIGNATURE_ID,
                title = "Signature",
                description = "Midnight enamel, neon pink hardware and electric cyan highlights",
                baseEdition = "original",
                patternId = CartdeckSurfaceDesign.PATTERN_NONE,
                stampId = CartdeckSurfaceDesign.STAMP_NONE,
                colours =
                    mapOf(
                        "chassis" to 0xFF13131AL,
                        "secondary" to 0xFF282132L,
                        "bezel" to 0xFF0A0A0FL,
                        "glass" to 0xFF07080CL,
                        "button" to 0xFF3B2448L,
                        "accent" to 0xFFFF4FA3L,
                        "onChassis" to 0xFFF8F1F7L,
                        "onButton" to 0xFFFFFFFFL,
                        "focus" to 0xFF19F5DAL,
                        "roomBackdrop" to 0xFF0C0C12L,
                        "shelfBackboard" to 0xFF17131FL,
                        "shelfDeck" to 0xFF282132L,
                        "shelfLip" to 0xFFFF4FA3L,
                        "shelfGroove" to 0xFF090A0FL,
                        "edgeHighlight" to 0xFF19F5DAL,
                        "shelfShadow" to 0xFF06060AL,
                        "sectionDivider" to 0xFF413049L,
                        "vacantCell" to 0xFF211B2AL,
                        "sectionText" to 0xFFF8F1F7L,
                        "focusOutline" to 0xFF19F5DAL,
                        "interactionAccent" to 0xFF19F5DAL,
                    ),
            ),
            CartdeckCuratedDesign(
                id = LIGHT_ID,
                title = "Light",
                description = "Warm ivory, slate lettering and teal accents",
                baseEdition = "original",
                colours =
                    mapOf(
                        "chassis" to 0xFFF4F2EDL,
                        "secondary" to 0xFFE8E5DFL,
                        "bezel" to 0xFF303B49L,
                        "glass" to 0xFF0D1721L,
                        "button" to 0xFF4D5965L,
                        "accent" to 0xFF146E66L,
                        "onChassis" to 0xFF202630L,
                        "onButton" to 0xFFFFFFFFL,
                        "focus" to 0xFF146E66L,
                        "roomBackdrop" to 0xFFF4F2EDL,
                        "shelfBackboard" to 0xFFFFFFFFL,
                        "shelfDeck" to 0xFFE8E5DFL,
                        "shelfLip" to 0xFF788189L,
                        "shelfGroove" to 0xFFD4D1CBL,
                        "edgeHighlight" to 0xFF788189L,
                        "shelfShadow" to 0xFFC6C6C0L,
                        "sectionDivider" to 0xFF788189L,
                        "vacantCell" to 0xFFE8E5DFL,
                        "sectionText" to 0xFF202630L,
                        "focusOutline" to 0xFF146E66L,
                        "interactionAccent" to 0xFF146E66L,
                    ),
            ),
            CartdeckCuratedDesign(
                id = STEALTH_ACID_ID,
                title = "Stealth / Acid",
                description = "Tactical graphite, olive hardware and acid-green accents",
                baseEdition = "original",
                patternId = CartdeckSurfaceDesign.PATTERN_TACTICAL_GRID,
                stampId = CartdeckSurfaceDesign.STAMP_CHEVRON,
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
            CartdeckCuratedDesign(
                id = ARCANA_GILDED_ID,
                title = "Arcana / Gilded",
                description = "Amethyst enamel, antique gold and engraved card geometry",
                baseEdition = "original",
                patternId = CartdeckSurfaceDesign.PATTERN_ARCANA,
                stampId = CartdeckSurfaceDesign.STAMP_ARCANA,
                colours =
                    mapOf(
                        "chassis" to 0xFF362347L,
                        "secondary" to 0xFF624477L,
                        "bezel" to 0xFF1B1228L,
                        "glass" to 0xFF110D19L,
                        "button" to 0xFF492E5CL,
                        "accent" to 0xFFD9B477L,
                        "onChassis" to 0xFFF0E4D1L,
                        "onButton" to 0xFFFFF1D7L,
                        "focus" to 0xFFFFE19DL,
                        "roomBackdrop" to 0xFF17101FL,
                        "shelfBackboard" to 0xFF2B1C38L,
                        "shelfDeck" to 0xFF4B345EL,
                        "shelfLip" to 0xFF907347L,
                        "shelfGroove" to 0xFF20152AL,
                        "edgeHighlight" to 0xFFBDA16FL,
                        "shelfShadow" to 0xFF0D0913L,
                        "sectionDivider" to 0xFF725788L,
                        "vacantCell" to 0xFF3C294BL,
                        "sectionText" to 0xFFF3E8D5L,
                        "focusOutline" to 0xFFF3D28AL,
                        "interactionAccent" to 0xFFE7C67FL,
                    ),
            ),
            CartdeckCuratedDesign(
                id = WILDBOUND_FORGE_ID,
                title = "Wildbound / Forge",
                description = "Midnight indigo, forged copper and expedition-map engraving",
                baseEdition = "original",
                patternId = CartdeckSurfaceDesign.PATTERN_CONTOUR,
                stampId = CartdeckSurfaceDesign.STAMP_SUMMIT,
                colours =
                    mapOf(
                        "chassis" to 0xFF263044L,
                        "secondary" to 0xFF465671L,
                        "bezel" to 0xFF111827L,
                        "glass" to 0xFF0C1220L,
                        "button" to 0xFF354159L,
                        "accent" to 0xFFD18C60L,
                        "onChassis" to 0xFFF0E7DEL,
                        "onButton" to 0xFFFCE8D8L,
                        "focus" to 0xFFFFC197L,
                        "roomBackdrop" to 0xFF121924L,
                        "shelfBackboard" to 0xFF222C3DL,
                        "shelfDeck" to 0xFF3C4B64L,
                        "shelfLip" to 0xFF93674FL,
                        "shelfGroove" to 0xFF172131L,
                        "edgeHighlight" to 0xFFBA9278L,
                        "shelfShadow" to 0xFF090E17L,
                        "sectionDivider" to 0xFF52637DL,
                        "vacantCell" to 0xFF303D53L,
                        "sectionText" to 0xFFEFE8DDL,
                        "focusOutline" to 0xFFEDAE80L,
                        "interactionAccent" to 0xFFE3A477L,
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
            patternId = design.patternId,
            stampId = design.stampId,
            decal = null,
            lighting =
                if (design.id == SIGNATURE_ID) {
                    CartdeckLighting(
                        mode = CartdeckLighting.MODE_STATIC,
                        finish = CartdeckLighting.FINISH_NEON,
                        intensity = 40,
                    )
                } else {
                    CartdeckLighting()
                },
        )
    }
}
