package com.swordfish.lemuroid.app.mobile.feature.product

import java.net.URI
import java.net.URLEncoder

/**
 * Curated external discovery destinations, not a bundled ROM catalogue and not user My Sources.
 *
 * Review applies to the directory domain, never to the rights or safety of individual uploads.
 */
internal data class HomebrewDirectoryLink(
    val name: String,
    val consoles: String,
    val url: String,
)

internal object HomebrewDirectory {
    val links =
        listOf(
            HomebrewDirectoryLink(
                name = "itch.io Homebrew",
                consoles = "Independent games for many retro systems",
                url = "https://itch.io/games/tag-homebrew",
            ),
            HomebrewDirectoryLink(
                name = "Homebrew Hub",
                consoles = "Game Boy, Game Boy Color, GBA and NES",
                url = "https://hh.gbdev.io/",
            ),
            HomebrewDirectoryLink(
                name = "PDRoms",
                consoles = "Multisystem homebrew archive",
                url = "https://pdroms.de/files",
            ),
            HomebrewDirectoryLink(
                name = "GameBrew PSP",
                consoles = "PSP community-made games",
                url = "https://www.gamebrew.org/wiki/List_of_PSP_homebrew_games",
            ),
            HomebrewDirectoryLink(
                name = "GameBrew Nintendo DS",
                consoles = "Nintendo DS community-made games",
                url = "https://www.gamebrew.org/wiki/List_of_DS_homebrew_games",
            ),
            HomebrewDirectoryLink(
                name = "GameBrew Nintendo 3DS",
                consoles = "Nintendo 3DS community-made games",
                url = "https://www.gamebrew.org/wiki/List_of_3DS_homebrew_games",
            ),
        )

    /**
     * User-triggered, site-restricted EXTERNAL web search, not a provider API or local GEM query.
     * Google may show unrelated individual entries; only the listed origins are scoped.
     */
    fun searchUri(query: String): URI? {
        if (query.length > 120 || query.any { it.isISOControl() }) return null
        val text =
            query.trim()
                .filter { it.isLetterOrDigit() || it.isWhitespace() || it == '-' || it == '_' || it == '\'' }
                .trim()
        if (text.isBlank()) return null
        val terms =
            "homebrew game \"" + text + "\" " +
                "(site:itch.io OR site:hh.gbdev.io OR site:pdroms.de OR site:gamebrew.org)"
        return URI.create("https://www.google.com/search?q=" + URLEncoder.encode(terms, "UTF-8"))
    }
}
