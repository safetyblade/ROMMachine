package com.swordfish.lemuroid.app.mobile

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.dao.GameSearchDao
import com.swordfish.lemuroid.lib.library.db.dao.TitleSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class TitleSearchTest {
    @Test fun literalPrefixesMatchBothNamesAndMaintainTriggers() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db =
            Room.inMemoryDatabaseBuilder(context, RetrogradeDatabase::class.java)
                .addCallback(GameSearchDao.CALLBACK).allowMainThreadQueries().build()
        try {
            val sql = db.openHelper.writableDatabase

            fun insert(
                id: Int,
                title: String,
                custom: String?,
            ) {
                sql.execSQL(
                    """INSERT INTO games(id,fileName,fileUri,title,systemId,lastIndexedAt,isFavorite,customName)
                    VALUES(?,? ,? ,? ,'gb',0,0,?)""",
                    arrayOf(id, "$id.gb", "content://fixture/$id", title, custom),
                )
            }

            fun ids(query: String): List<Int> =
                sql.query(TitleSearch.query(query)).use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getInt(cursor.getColumnIndexOrThrow("id"))) }
                }
            insert(1, "Super Mario World", "My Island Adventure")
            insert(2, "Super Mario Land", null)
            insert(3, "Café Orbit", "NEAR OR NOT")
            assertEquals(listOf(1, 2), ids("sup mar"))
            assertEquals(listOf(1), ids("mar wor"))
            assertEquals(listOf(1), ids("my adv"))
            assertEquals(listOf(1), ids("super adventure"))
            assertEquals(listOf(3), ids("cafe orb"))
            assertEquals(listOf(3), ids("OR"))
            for (query in listOf("", "\"*():-", "zzzz", "NEAR/5 ))", "x' OR 1=1 --")) assertTrue(ids(query).isEmpty())
            sql.execSQL("UPDATE games SET customName='Changed Shelf' WHERE id=1")
            assertEquals(listOf(1), ids("chang shel"))
            assertEquals(listOf(1), ids("mar wor"))
            assertTrue(ids("island").isEmpty())
            sql.execSQL("UPDATE games SET customName=NULL WHERE id=1")
            assertTrue(ids("changed").isEmpty())
            assertEquals(listOf(1), ids("mar wor"))
            sql.execSQL("DELETE FROM games WHERE id=1")
            assertTrue(ids("mar wor").isEmpty())
            assertTrue(TitleSearch.tokens("a ".repeat(5000)).size <= 16)
        } finally {
            db.close()
        }
    }

    @Test fun fiveThousandGamesUseIndexedPartialSearch() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val db =
            Room.inMemoryDatabaseBuilder(context, RetrogradeDatabase::class.java)
                .addCallback(GameSearchDao.CALLBACK).allowMainThreadQueries().build()
        try {
            val sql = db.openHelper.writableDatabase
            sql.beginTransaction()
            try {
                for (id in 1..5000) sql.execSQL(
                    """INSERT INTO games(id,fileName,fileUri,title,systemId,lastIndexedAt,isFavorite,customName)
                    VALUES(?,?,?,?,'gb',0,0,?)""",
                    arrayOf(id, "$id.gb", "content://fixture/$id", "Orbit Adventure $id", "My Shelf $id"),
                )
                sql.setTransactionSuccessful()
            } finally {
                sql.endTransaction()
            }
            val start = System.nanoTime()
            sql.query(TitleSearch.query("orb adv 4999")).use { assertEquals(1, it.count) }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            println("CARTDECK 5000-game indexed query: ${elapsed}ms (JVM SQLite; device measurement separate)")
            assertTrue("Indexed query exceeded 3s: $elapsed", elapsed < 3000)
        } finally {
            db.close()
        }
    }
}
