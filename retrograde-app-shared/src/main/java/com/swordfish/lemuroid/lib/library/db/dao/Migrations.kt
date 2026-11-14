package com.swordfish.lemuroid.lib.library.db.dao

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val VERSION_12_13: Migration =
        object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Additive ledger: existing game rows and user saves remain untouched.
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS \`retired_cartridges\` (
                        \`cartridgeId\` TEXT NOT NULL PRIMARY KEY,
                        \`formerGameId\` INTEGER NOT NULL,
                        \`fileName\` TEXT NOT NULL,
                        \`fileUri\` TEXT NOT NULL,
                        \`title\` TEXT NOT NULL,
                        \`systemId\` TEXT NOT NULL,
                        \`developer\` TEXT,
                        \`coverFrontUrl\` TEXT,
                        \`lastPlayedAt\` INTEGER,
                        \`isFavorite\` INTEGER NOT NULL,
                        \`customName\` TEXT,
                        \`customCoverUri\` TEXT,
                        \`legacyFileName\` TEXT,
                        \`legacySourceUri\` TEXT,
                        \`retiredAt\` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS \`index_retired_cartridges_systemId\` ON \`retired_cartridges\` (\`systemId\`)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS \`index_retired_cartridges_fileUri\` ON \`retired_cartridges\` (\`fileUri\`)",
                )
            }
        }

    val VERSION_11_12: Migration =
        object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE games ADD COLUMN cartridgeId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE games ADD COLUMN legacyFileName TEXT")
                database.execSQL("ALTER TABLE games ADD COLUMN legacySourceUri TEXT")
                // Room executes the whole migration in its upgrade transaction. Existing IDs,
                // foreign keys, titles, artwork and favourites remain untouched.
                database.query("SELECT id FROM games").use { cursor ->
                    while (cursor.moveToNext()) {
                        database.execSQL(
                            """UPDATE games SET cartridgeId = ?, legacyFileName = fileName,
                                legacySourceUri = fileUri WHERE id = ?""",
                            arrayOf(java.util.UUID.randomUUID().toString(), cursor.getInt(0)),
                        )
                    }
                }
                database.execSQL("CREATE UNIQUE INDEX index_games_cartridgeId ON games(cartridgeId)")
            }
        }

    val VERSION_10_11: Migration =
        object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createTitleSearchIndex(database)
            }
        }

    val VERSION_8_9: Migration =
        object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `datafiles`(
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileUri` TEXT NOT NULL,
                        `lastIndexedAt` INTEGER NOT NULL,
                        `path` TEXT, FOREIGN KEY(`gameId`
                    ) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_datafiles_id` ON `datafiles` (`id`)
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_datafiles_fileUri` ON `datafiles` (`fileUri`)
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_datafiles_gameId` ON `datafiles` (`gameId`)
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_datafiles_lastIndexedAt` ON `datafiles` (`lastIndexedAt`)
                    """.trimIndent(),
                )
            }
        }

    val VERSION_9_10: Migration =
        object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateGamesTableToVersion10(database)
                GameSearchDao.migrateFtsForVersion10(database)
            }
        }

    private fun migrateGamesTableToVersion10(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE games ADD COLUMN customName TEXT DEFAULT NULL",
        )
        database.execSQL(
            "ALTER TABLE games ADD COLUMN customCoverUri TEXT DEFAULT NULL",
        )
    }
}
