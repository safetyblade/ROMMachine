package com.swordfish.lemuroid.lib.library.db.dao

import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive derived index. Legacy fts_games and every source record remain untouched. */
internal fun createTitleSearchIndex(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        CREATE VIRTUAL TABLE fts_game_titles USING FTS4(
            title, customName, content="games", tokenize=unicode61 "remove_diacritics=1")
        """.trimIndent(),
    )
    for ((suffix, event) in listOf("bu" to "UPDATE", "bd" to "DELETE")) {
        db.execSQL(
            """
            CREATE TRIGGER title_search_$suffix BEFORE $event ON games BEGIN
                DELETE FROM fts_game_titles WHERE docid=old.id;
            END
            """.trimIndent(),
        )
    }
    val insert =
        """
        INSERT INTO fts_game_titles(docid,title,customName)
        VALUES(new.id,new.title,COALESCE(new.customName,''));
        """.trimIndent()
    db.execSQL("CREATE TRIGGER title_search_au AFTER UPDATE ON games BEGIN $insert END")
    db.execSQL("CREATE TRIGGER title_search_ai AFTER INSERT ON games BEGIN $insert END")
    db.execSQL("INSERT INTO fts_game_titles(docid,title,customName) SELECT id,title,COALESCE(customName,'') FROM games")
}
