package com.example.eat.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "ted_talks_cache")
data class TedTalkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val link: String,
    val transcript: String,
    val duration: Long,
    val fetchTimestamp: Long,
    val transcriptLines: List<TranscriptLine>,
    val isCalibrated: Boolean = false
)
