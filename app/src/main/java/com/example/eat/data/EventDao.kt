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

    @Query("DELETE FROM events WHERE imagePath = :imagePath")
    suspend fun deleteEventByImagePath(imagePath: String)

    @Query("SELECT * FROM events WHERE timestamp >= :startTime AND timestamp < :endTime ORDER BY timestamp ASC")
    fun getEventsByDateRange(startTime: Long, endTime: Long): Flow<List<EventEntity>>

    @Query("UPDATE events SET type = :newType WHERE timestamp IN (:timestamps)")
    suspend fun updateEventType(timestamps: List<Long>, newType: String)
}
