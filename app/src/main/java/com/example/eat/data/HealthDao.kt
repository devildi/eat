package com.example.eat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Insert
    suspend fun insertHealthData(data: HealthDataEntity)

    @Query("SELECT * FROM health_data ORDER BY timestamp DESC")
    fun getAllHealthData(): Flow<List<HealthDataEntity>>

    @Query("SELECT * FROM health_data ORDER BY timestamp DESC")
    suspend fun getAllHealthDataSync(): List<HealthDataEntity>

    @Query("DELETE FROM health_data")
    suspend fun deleteAllHealthData()

    @Query("DELETE FROM health_data WHERE type = :type AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    suspend fun deleteHealthDataByTypeAndDate(type: String, startTimestamp: Long, endTimestamp: Long)
}
