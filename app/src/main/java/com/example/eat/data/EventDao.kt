package com.example.eat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY timestamp ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM events WHERE timestamp IN (:timestamps)")
    suspend fun deleteEvents(timestamps: List<Long>)
}
