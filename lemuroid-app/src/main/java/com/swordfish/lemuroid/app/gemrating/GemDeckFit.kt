package com.swordfish.lemuroid.app.gemrating

import java.util.Locale

/**
 * Research-only, controller-free DeckFit projection for the offline GEM release catalogue.
 *
 * QuickScore = per-system baseline + bounded, evidence-backed gameplay adjustments.
 * Required-but-unavailable inputs remain unscored. Reviewed exact-ID touch scores take precedence.
 * This research projection is NOT a claim that a game boots, saves or works in the installed APK.
 * User-edited My DeckFit, My GEMRating and private RoxyRating remain independent.
 *
 * The reviewed exact-ID seed is taken from "GEM-03 Assessment Audit — 2026-11-14".
 * Historical controller scores are deliberately not imported. Neither title strings nor
 * unverified cross-platform work matches are used to transfer ratings.
 */
object GemDeckFit {
    enum class Basis {
        REVIEWED_TOUCH,
        PLATFORM_GENRE,
        PLATFORM_ONLY,
        GAME_TRAITS,
        INPUT_REVIEW,
    }

    /** Curated traits are opt-in and must be supported by source evidence, never title guessing. */
    enum class Trait(val deltaHalfSteps: Int) {
        SIMPLE_2D(1),
        TURN_BASED(1),
        PRECISION_ACTION(-1),
        COMPLEX_CHORDS(-1),
        DUAL_ANALOG_AIM(-1),
        SMALL_TEXT(-1),
        MOTION_FALLBACK(-2),
        REQUIRED_INPUT_UNAVAILABLE(0),
    }

    data class Estimate(
        val halfSteps: Int?,
        val basis: Basis,
    ) {
        init {
            require(halfSteps == null || halfSteps in 2..10)
        }

        val scoreText: String
            get() =
                when {
                    halfSteps == null -> "—"
                    halfSteps % 2 == 0 -> (halfSteps / 2).toString()
                    else -> "${halfSteps / 2}.5"
                }

        val displayText: String
            get() = if (halfSteps == null) "DeckFit · Input review" else "DeckFit $scoreText/5 · Provisional"

        val evidenceText: String
            get() =
                when (basis) {
                    Basis.REVIEWED_TOUCH -> "Research-reviewed touch estimate; game not device-tested"
                    Basis.PLATFORM_GENRE -> "Low-confidence system/genre estimate; game not device-tested"
                    Basis.PLATFORM_ONLY -> "Low-confidence system estimate; game not device-tested"
                    Basis.GAME_TRAITS -> "Research-tagged gameplay adjustment; game not device-tested"
                    Basis.INPUT_REVIEW -> "Special input needs review; game not device-tested"
                }
    }

    /**
     * Conservative half-step baselines for normal phone play, with CARTDECK on-screen controls.
     * Never increase these for Bluetooth pads, external peripherals or hypothetical fixes.
     */
    private val platformHalfSteps =
        mapOf(
            "atari2600" to 8,
            "atari7800" to 8,
            "lynx" to 7,
            "gb" to 9,
            "gbc" to 9,
            "gba" to 8,
            "gg" to 8,
            "ngp" to 9,
            "ngc" to 9,
            "ws" to 8,
            "wsc" to 8,
            "nes" to 8,
            "snes" to 7,
            "sms" to 8,
            "md" to 7,
            "scd" to 7,
            "pce" to 7,
            "n64" to 6,
            "psx" to 6,
            "psp" to 7,
            "nds" to 8,
            "3ds" to 8,
            "fbneo" to 7,
            "mame2003plus" to 6,
            "dos" to 4,
            "dc" to 6,
            "gc" to 6,
            "ps2" to 6,
            "xbox" to 5,
            "wii" to 5,
            "xbox360" to 5,
            "wiiu" to 5,
            "ps3" to 4,
        )

    /** Exact release IDs only, not title aliases or controller-based assessments. */
    private val reviewedTouchHalfSteps =
        mapOf(
            "VDG:gb:94" to 10,
            "VDG:gc:382" to 6,
            "VDG:gba:794" to 10,
            "VDG:n64:153" to 6,
            "VDG:nes:58" to 9,
            "VDG:gb:46" to 10,
            "VDG:ps:540" to 7,
            "VDG:ds:1410" to 8,
            "VDG:snes:109" to 9,
            "VDG:gba:617" to 10,
            "VDG:gba:508" to 8,
            "VDG:gba:593" to 9,
            "VDG:ps:678" to 7,
            "VDG:psp:803" to 8,
            "VDG:nes:7" to 7,
            "VDG:gba:90" to 7,
            "VDG:snes:9" to 6,
            "VDG:ps:415" to 7,
            "VDG:xb:row-522" to 4,
            "VDG:n64:202" to 5,
            "VDG:xb:row-85" to 5,
            "VDG:gb:26" to 9,
            "VDG:gb:27" to 9,
            "VDG:gb:5" to 9,
            "VDG:gb:10" to 9,
            "VDG:gb:68" to 10,
            "VDG:gb:50" to 10,
            "VDG:gb:6" to 9,
            "VDG:gb:11" to 9,
            "VDG:gb:43" to 10,
            "VDG:gb:2" to 9,
            "VDG:gb:85" to 8,
            "VDG:gb:3" to 8,
            "VDG:gb:17" to 9,
            "VDG:gb:35" to 9,
            "VDG:gb:36" to 9,
            "VDG:gb:39" to 10,
            "VDG:gba:597" to 10,
            "VDG:gba:598" to 10,
            "VDG:gba:605" to 9,
            "VDG:gba:144" to 9,
            "VDG:gba:341" to 8,
            "VDG:gba:544" to 6,
            "VDG:gba:294" to 7,
            "VDG:gba:798" to 10,
            "VDG:gba:186" to 8,
            "VDG:ds:1362" to 6,
            "VDG:ds:1633" to 9,
            "VDG:ds:1046" to 8,
            "VDG:snes:121" to 10,
            "VDG:snes:12" to 8,
            "VDG:snes:82" to 8,
            "VDG:n64:113" to 6,
            "VDG:n64:212" to 5,
            "VDG:n64:90" to 6,
            "VDG:n64:193" to 8,
            "VDG:ps:300" to 5,
            "VDG:ps:75" to 6,
            "VDG:ps:719" to 9,
            "VDG:ps:416" to 7,
            "VDG:ps:593" to 7,
            "VDG:psp:566" to 8,
            "VDG:psp:624" to 8,
            "VDG:psp:823" to 9,
            "VDG:psp:614" to 6,
            "VDG:psp:691" to 5,
            "VDG:psp:63" to 4,
            "VDG:psp:938" to 6,
            "VDG:psp:894" to 9,
            "VDG:gen:16" to 8,
            "VDG:gen:23" to 7,
            "VDG:gen:27" to 10,
            "VDG:dc:29" to 4,
            "VDG:dc:22" to 6,
            "VDG:dc:38" to 9,
            "VDG:dc:23" to 5,
            "VDG:gc:343" to 9,
            "VDG:gc:78" to 6,
            "VDG:gc:298" to 4,
            "VDG:gc:308" to 5,
            "VDG:gc:149" to 4,
            "VDG:gc:52" to 4,
            "VDG:wii:row-784" to 5,
            "VDG:wii:row-864" to 6,
            "VDG:xb:row-519" to 4,
            "VDG:xb:row-452" to 6,
            "VDG:xb:row-79" to 3,
            "VDG:xb:row-455" to 7,
            "VDG:xb:row-572" to 4,
            "VDG:xb:row-336" to 5,
            "VDG:xb:row-210" to 5,
        )

    /** Research records with nonstandard input controls; do not score these as ordinary games. */
    private val inputReviewReleaseIds =
        setOf(
            "VDG:gb:40",
            "VDG:ds:1246",
            "VDG:ds:502",
            "VDG:ds:1806",
            "VDG:ds:1891",
            "VDG:ds:698",
            "VDG:ds:1583",
            "VDG:3ds:15", // Beyblade Evolution: native 3DS motion path awaiting physical verification
            "VDG:wii:row-1252",
            "VDG:wii:row-616",
            "VDG:wii:row-736", // WarioWare: Smooth Moves requires distinct Wii Remote motion poses
            "VDG:wii:row-1253", // Wii Sports Resort needs verified MotionPlus-equivalent guest motion
        )

    fun estimate(release: GemCataloguePackage.Release): Estimate =
        estimate(release.releaseId, release.systemId, release.primaryGenre, release.title)

    /** Optional explicit traits allow a versioned catalogue-sidecar to add exact-ID evidence. */
    fun estimate(
        releaseId: String,
        systemId: String,
        primaryGenre: String?,
        title: String? = null,
        traits: Set<Trait> = emptySet(),
    ): Estimate {
        if (Trait.REQUIRED_INPUT_UNAVAILABLE in traits || releaseId in inputReviewReleaseIds) {
            return Estimate(null, Basis.INPUT_REVIEW)
        }
        reviewedTouchHalfSteps[releaseId]?.let { return Estimate(it, Basis.REVIEWED_TOUCH) }

        val base = platformHalfSteps[systemId] ?: return Estimate(null, Basis.INPUT_REVIEW)
        // Avoid assuming every RPG is turn-based or every action game has a 3D camera.
        val genreDelta =
            when (primaryGenre?.trim()?.lowercase(Locale.ROOT)) {
                "strategy", "puzzle" -> 1
                "fighting", "racing", "shooter" -> -1
                else -> 0
            }
        val researchedTraits = title?.let { curatedTraits(releaseId, it) }.orEmpty()
        val allTraits = researchedTraits + traits
        val traitDelta = allTraits.sumOf { it.deltaHalfSteps }
        val boundedDelta = (genreDelta + traitDelta).coerceIn(-2, 2)
        val basis =
            when {
                allTraits.isNotEmpty() -> Basis.GAME_TRAITS
                genreDelta != 0 -> Basis.PLATFORM_GENRE
                else -> Basis.PLATFORM_ONLY
            }
        return Estimate((base + boundedDelta).coerceIn(2, 10), basis)
    }

    private fun curatedTraits(releaseId: String, title: String): Set<Trait> {
        val feature = GemGameplayFeatures.find(releaseId, title) ?: return emptySet()
        return buildSet {
            if (feature.playstyle.equals("Turn-based", ignoreCase = true)) add(Trait.TURN_BASED)
            if (feature.playstyle.equals("First-person", ignoreCase = true)) add(Trait.DUAL_ANALOG_AIM)
            if ("High speed" in feature.mechanics) add(Trait.PRECISION_ACTION)
            if ("2D tactical" in feature.mechanics) add(Trait.SIMPLE_2D)
        }
    }
}
