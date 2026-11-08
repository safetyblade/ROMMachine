package com.swordfish.lemuroid.lib.library.db.dao

import androidx.sqlite.db.SimpleSQLiteQuery

/** Only generated Unicode word tokens reach MATCH; user operators are never executable. */
object TitleSearch {
    fun tokens(input: String): List<String> =
        Regex("[\\p{L}\\p{N}]+")
            .findAll(input.take(1024)).take(16).map { it.value.take(64) }.toList()

    fun query(input: String): SimpleSQLiteQuery {
        val words = tokens(input)
        if (words.isEmpty()) return SimpleSQLiteQuery("SELECT * FROM games WHERE 0")
        // FTS4 prefix syntax places the star INSIDE the quoted token.
        // Adjacent literals provide AND even on Android builds without enhanced FTS syntax.
        val match = words.joinToString(" ") { "\"$it*\"" }
        val title = input.trim().take(1024)
        val prefix = title.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        return SimpleSQLiteQuery(
            """
            SELECT games.* FROM fts_game_titles
            JOIN games ON games.id = fts_game_titles.docid
            WHERE fts_game_titles MATCH ?
            ORDER BY CASE
                WHEN games.title = ? COLLATE NOCASE OR games.customName = ? COLLATE NOCASE THEN 0
                WHEN games.title LIKE ? ESCAPE '\' OR games.customName LIKE ? ESCAPE '\' THEN 1
                ELSE 2 END,
                COALESCE(games.customName, games.title) COLLATE NOCASE, games.id
            """.trimIndent(),
            arrayOf(match, title, title, prefix, prefix),
        )
    }
}
