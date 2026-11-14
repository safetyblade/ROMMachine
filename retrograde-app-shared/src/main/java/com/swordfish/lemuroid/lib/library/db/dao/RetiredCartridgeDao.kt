package com.swordfish.lemuroid.lib.library.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.swordfish.lemuroid.lib.library.db.entity.RetiredCartridge

/** Internal recovery records. No destructive user-facing operation in C2A. */
@Dao
interface RetiredCartridgeDao {
    @Query("SELECT * FROM retired_cartridges ORDER BY retiredAt, cartridgeId")
    fun selectAll(): List<RetiredCartridge>

    @Insert
    fun insert(records: List<RetiredCartridge>)

    @Delete
    fun delete(records: List<RetiredCartridge>)
}
