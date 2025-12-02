package com.example.eat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Insert
    suspend fun insertHealthData(data: HealthDataEntity)

    @Query("SELECT * FROM health_data ORDER BY timestamp ASC")
    fun getAllHealthData(): Flow<List<HealthDataEntity>>

    @Query("DELETE FROM health_data")
    suspend fun deleteAllHealthData()
}
