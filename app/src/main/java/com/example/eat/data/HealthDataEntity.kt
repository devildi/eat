package com.example.eat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_data")
data class HealthDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val type: String, // "Weight" or "BloodPressure"
    val value1: Float, // Weight or Systolic
    val value2: Float? = null // Diastolic (null for Weight)
)
