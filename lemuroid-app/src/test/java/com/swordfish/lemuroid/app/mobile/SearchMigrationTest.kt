package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.dao.GameSearchDao
import com.swordfish.lemuroid.lib.library.db.dao.Migrations
import com.swordfish.lemuroid.lib.library.db.dao.TitleSearch
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SearchMigrationTest {
    @Test fun realVersionTenSchemaMigratesWithoutChangingRecordsOrPrivateFiles() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val name = "search-migration-fixture"
        context.deleteDatabase(name)
        val schema = JSONObject(javaClass.getResource("/cartdeck-schema10.json")!!.readText()).getJSONObject("database")
        val old =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(10) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                val entities = schema.getJSONArray("entities")
                                for (i in 0 until entities.length()) {
                                    val entity = entities.getJSONObject(i)
                                    val table = entity.getString("tableName")
                                    db.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
                                    val indexes = entity.getJSONArray("indices")
                                    for (j in 0 until indexes.length()) db.execSQL(
                                        indexes.getJSONObject(
                                            j,
                                        ).getString("createSql").replace("\u0024{TABLE_NAME}", table),
                                    )
                                }
                                val setup = schema.getJSONArray("setupQueries")
                                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                                GameSearchDao.MIGRATION.migrate(db)
                                db.execSQL(
                                    """INSERT INTO games(id,fileName,fileUri,title,systemId,developer,coverFrontUrl,
                            lastIndexedAt,lastPlayedAt,isFavorite,customName,customCoverUri)
                            VALUES(41,'orbit.gb','content://persisted/rom','Orbit Adventure','gb','Developer',
                            'https://indexed.invalid/cover.png',123,456,1,'My Island','file:///private/custom.png')""",
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )
        val before =
            old.writableDatabase.query("SELECT * FROM games").use { cursor ->
                cursor.moveToFirst()
                (0 until cursor.columnCount).map { cursor.getString(it) }
            }
        old.close()
        val cover = File(context.filesDir, "migration-custom.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val save = File(context.filesDir, "migration-save.srm").apply { writeText("sentinel-existing-save") }
        val db =
            Room.databaseBuilder(context, RetrogradeDatabase::class.java, name)
                .addMigrations(Migrations.VERSION_10_11, Migrations.VERSION_11_12, Migrations.VERSION_12_13)
                .allowMainThreadQueries().build()
        try {
            val sql = db.openHelper.writableDatabase
            assertEquals(13, sql.version)
            val after =
                sql.query("SELECT * FROM games").use { cursor ->
                    cursor.moveToFirst()
                    (0 until cursor.columnCount).map { cursor.getString(it) }
                }
            assertEquals(before, after.take(before.size))
            sql.query("SELECT cartridgeId, legacyFileName, legacySourceUri FROM games WHERE id = 41").use {
                assertTrue(it.moveToFirst())
                java.util.UUID.fromString(it.getString(0))
                assertEquals("orbit.gb", it.getString(1))
                assertEquals("content://persisted/rom", it.getString(2))
            }
            // Changing the source display name must leave the compatibility alias intact.
            sql.execSQL("UPDATE games SET fileName = 'renamed.gb' WHERE id = 41")
            sql.query("SELECT legacyFileName FROM games WHERE id = 41").use {
                assertTrue(it.moveToFirst())
                assertEquals("orbit.gb", it.getString(0))
            }
            for (query in listOf("orb adv", "my isl")) sql.query(TitleSearch.query(query)).use {
                assertEquals(1, it.count)
                assertTrue(it.moveToFirst())
                assertEquals(41, it.getInt(it.getColumnIndexOrThrow("id")))
            }
            assertTrue(cover.readBytes().contentEquals(byteArrayOf(1, 2, 3, 4)))
            assertEquals("sentinel-existing-save", save.readText())
            sql.query("SELECT COUNT(*) FROM fts_games WHERE fts_games MATCH 'Island'").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
