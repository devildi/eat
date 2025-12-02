package com.example.eat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Main Meal" or "Snack"
    val timestamp: Long,
    val imagePath: String? = null
)
