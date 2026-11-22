package com.swordfish.lemuroid.app.gemrating

import java.util.Locale

/**
 * Research-only, controller-free DeckFit projection for the offline GEM release catalogue.
 *
 * Each ordinary release receives a provisional 1–5 estimate from its system and optional broad
 * genre; special-input holds remain unscored pending evidence. Exact-ID touch assessments
 * take precedence. This is NOT a gameplay/boot pass.
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
        INPUT_REVIEW,
    }

    data class Estimate(
        val halfSteps: Int?,
        val basis: Basis,
    ) {
        init {
            require(halfSteps == null || halfSteps in 2..10)
        }

        val scoreText: String
            get() = if (halfSteps == null) "—" else if (halfSteps % 2 == 0) (halfSteps / 2).toString() else "${halfSteps / 2}.5"

        val displayText: String
            get() = if (halfSteps == null) "DeckFit · Input review" else "DeckFit $scoreText/5 · Provisional"

        val evidenceText: String
            get() =
                when (basis) {
                    Basis.REVIEWED_TOUCH -> "Research-reviewed touch estimate; game not device-tested"
                    Basis.PLATFORM_GENRE -> "Low-confidence system/genre estimate; game not device-tested"
                    Basis.PLATFORM_ONLY -> "Low-confidence system estimate; game not device-tested"
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
            "scd" to 6,
            "pce" to 7,
            "n64" to 6,
            "psx" to 6,
            "psp" to 7,
            "nds" to 7,
            "3ds" to 6,
            "fbneo" to 7,
            "mame2003plus" to 6,
            "dos" to 3,
            "dc" to 5,
            "gc" to 5,
            "ps2" to 5,
            "xbox" to 5,
            "wii" to 4,
            "xbox360" to 4,
            "wiiu" to 3,
            "ps3" to 2,
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
            "VDG:xb:522" to 4,
            "VDG:n64:202" to 5,
            "VDG:xb:85" to 5,
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
            "VDG:wii:784" to 5,
            "VDG:wii:864" to 6,
            "VDG:xb:519" to 4,
            "VDG:xb:452" to 6,
            "VDG:xb:79" to 3,
            "VDG:xb:455" to 7,
            "VDG:xb:572" to 4,
            "VDG:xb:336" to 5,
            "VDG:xb:210" to 5,
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
            "VDG:wii:1252",
            "VDG:wii:616",
        )

    fun estimate(release: GemCataloguePackage.Release): Estimate =
        estimate(release.releaseId, release.systemId, release.primaryGenre)

    fun estimate(
        releaseId: String,
        systemId: String,
        primaryGenre: String?,
    ): Estimate {
        reviewedTouchHalfSteps[releaseId]?.let { return Estimate(it, Basis.REVIEWED_TOUCH) }
        if (releaseId in inputReviewReleaseIds) return Estimate(null, Basis.INPUT_REVIEW)

        val base = platformHalfSteps[systemId] ?: return Estimate(null, Basis.INPUT_REVIEW)
        // Only use explicit, broad genre labels. Action/Adventure/Unknown make no control promises.
        val delta =
            when (primaryGenre?.trim()?.lowercase(Locale.ROOT)) {
                "strategy", "puzzle", "role-playing", "role playing" -> 1
                "fighting", "racing", "shooter", "sports" -> -1
                else -> 0
            }
        return Estimate(
            (base + delta).coerceIn(2, 10),
            if (delta == 0) Basis.PLATFORM_ONLY else Basis.PLATFORM_GENRE,
        )
    }
}
