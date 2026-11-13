package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.dao.Migrations
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CartridgeMigrationTest {
    @Test fun installedDuplicateFilenamesKeepTheirRowsAndReceiveDurableDistinctIds() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val name = "cartridge-migration-fixture"
        context.deleteDatabase(name)
        val schema = JSONObject(javaClass.getResource("/cartdeck-schema11.json")!!.readText()).getJSONObject("database")
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(
                    object : SupportSQLiteOpenHelper.Callback(11) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            val entities = schema.getJSONArray("entities")
                            for (i in 0 until entities.length()) {
                                val entity = entities.getJSONObject(i)
                                val table = entity.getString("tableName")
                                db.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", table))
                                val indexes = entity.getJSONArray("indices")
                                for (j in 0 until indexes.length()) {
                                    val createIndex = indexes.getJSONObject(j).getString("createSql")
                                    db.execSQL(createIndex.replace("\u0024{TABLE_NAME}", table))
                                }
                            }
                            val setup = schema.getJSONArray("setupQueries")
                            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                            for (id in listOf(41, 42)) db.execSQL(
                                """INSERT INTO games(id,fileName,fileUri,title,systemId,developer,coverFrontUrl,
                                    lastIndexedAt,lastPlayedAt,isFavorite,customName,customCoverUri)
                                    VALUES(?,'same.gb',?,'Synthetic','gb',NULL,NULL,123,456,1,?,NULL)""",
                                arrayOf(id, "content://fixture/$id", "Custom $id"),
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
        assertEquals(11, helper.writableDatabase.version)
        helper.close()

        fun open() =
            Room.databaseBuilder(context, RetrogradeDatabase::class.java, name)
                .addMigrations(Migrations.VERSION_11_12).allowMainThreadQueries().build()

        val first = open()
        val identities =
            try {
                val sql = first.openHelper.writableDatabase
                assertEquals(12, sql.version)
                sql.query(
                    "SELECT id,cartridgeId,legacyFileName,legacySourceUri,customName,isFavorite FROM games ORDER BY id",
                ).use {
                    val ids = mutableListOf<String>()
                    while (it.moveToNext()) {
                        val id = it.getInt(0)
                        ids.add(UUID.fromString(it.getString(1)).toString())
                        assertEquals("same.gb", it.getString(2))
                        assertEquals("content://fixture/$id", it.getString(3))
                        assertEquals("Custom $id", it.getString(4))
                        assertEquals(1, it.getInt(5))
                    }
                    assertEquals(2, ids.size)
                    assertNotEquals(ids[0], ids[1])
                    ids
                }
            } finally {
                first.close()
            }
        val reopened = open()
        try {
            reopened.openHelper.writableDatabase.query("SELECT cartridgeId FROM games ORDER BY id").use {
                for (identity in identities) {
                    assertTrue(it.moveToNext())
                    assertEquals(identity, it.getString(0))
                }
            }
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }
}
