package com.example.eat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface TedDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTalk(talk: TedTalkEntity)

    // Get the latest cached talk (assuming we only keep one "daily" talk)
    @Query("SELECT * FROM ted_talks_cache ORDER BY fetchTimestamp DESC LIMIT 1")
    suspend fun getLatestTalk(): TedTalkEntity?

    @Query("DELETE FROM ted_talks_cache")
    suspend fun deleteAllTalks()
}
