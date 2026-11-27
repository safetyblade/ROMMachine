package com.swordfish.lemuroid.app.gemrating

import java.util.Locale

/** Offline source observations. Title matches are advisory until the serial has been verified. */
internal object GemPs2Compatibility {
    enum class Classification(val label: String) {
        COMPATIBLE("Compatible"),
        KNOWN_ISSUES("Known issues"),
        INTRO_ONLY("Intro only"),
        UNSUPPORTED("Unsupported"),
        UNTESTED("Untested"),
    }

    data class Observation(
        val title: String,
        val serial: String,
        val region: String,
        val engine: String,
        val classification: Classification,
        val limitation: String,
        val source: String,
        val sourceVersion: String,
    )

    private const val PLAY_TRACKER = "https://github.com/jpd002/Play-Compatibility/issues"
    private const val PCSX2_REVISION = "51e3036bdb7cad7ae7e25ad8caa438b06f538c4e"
    private const val PCSX2_INDEX =
        "https://github.com/PCSX2/pcsx2/blob/$PCSX2_REVISION/bin/resources/GameIndex.yaml"

    private val observations =
        listOf(
            Observation(
                "Soulcalibur III",
                "SLUS-21216",
                "NTSC-U",
                "Upstream Play!",
                Classification.KNOWN_ISSUES,
                "Slow gameplay and visible lines.",
                "$PLAY_TRACKER/374",
                "2019-01-31 · 98045dc26faa9b394b77ebb4d0af5bcb330770b3",
            ),
            Observation(
                "Soulcalibur III",
                "SCES-53312",
                "PAL",
                "Upstream Play!",
                Classification.KNOWN_ISSUES,
                "Videos were missing in 50 Hz mode.",
                "$PLAY_TRACKER/418",
                "2019-01-31 · 98045dc26faa9b394b77ebb4d0af5bcb330770b3",
            ),
            Observation(
                "Gran Turismo 4",
                "SCUS-97328",
                "NTSC-U",
                "Upstream Play!",
                Classification.KNOWN_ISSUES,
                "Very slow and choppy on a Windows test system.",
                "$PLAY_TRACKER/2876",
                "2026-07-18 · engine revision unknown",
            ),
            Observation(
                "Hidden Invasion",
                "SLUS-20301",
                "NTSC-U",
                "Upstream Play!",
                Classification.COMPATIBLE,
                "Reported playable with good performance.",
                "$PLAY_TRACKER/2889",
                "2026-09-02 · e9aa5ace0a5a54a86f6911d24ca82651f5853f71",
            ),
            Observation(
                "Conflict: Desert Storm",
                "SLUS-20549",
                "NTSC-U",
                "Desktop PCSX2",
                Classification.COMPATIBLE,
                "Listed as playable in the desktop game index.",
                PCSX2_INDEX,
                PCSX2_REVISION,
            ),
        )

    fun forVerifiedSerial(serial: String): List<Observation> = observations.filter { it.serial.equals(serial, true) }

    fun forTitle(title: String): List<Observation> = observations.filter { titleKey(it.title) == titleKey(title) }

    private fun titleKey(title: String): String = title.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
}
