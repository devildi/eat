package com.example.eat.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eat.data.AppDatabase
import com.example.eat.data.EventEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Event(
    val type: String,
    val timestamps: List<Long>, // All merged timestamps
    val colorIndices: List<Int>, // All colors (0=Red, 1=Yellow, 2=Blue)
    val imagePaths: List<String> = emptyList() // Associated image paths
) {
    // Helper to get the latest timestamp
    val latestTimestamp: Long get() = timestamps.last()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val eventDao = database.eventDao()
    private val healthDao = database.healthDao()

    val events: StateFlow<List<Event>?> = eventDao.getAllEvents()
        .map { entities ->
            // Group entities into UI Events (merged by 30 mins)
            val groupedEvents = mutableListOf<Event>()
            
            entities.forEach { entity ->
                val lastEvent = groupedEvents.lastOrNull { it.type == entity.type }
                val shouldMerge = if (lastEvent != null) {
                    val timeDiffMinutes = (entity.timestamp - lastEvent.latestTimestamp) / (60 * 1000)
                    timeDiffMinutes < 10
                } else {
                    false
                }

                if (shouldMerge && lastEvent != null) {
                    val index = groupedEvents.indexOf(lastEvent)
                    val newImagePaths = if (entity.imagePath != null) {
                        lastEvent.imagePaths + entity.imagePath
                    } else {
                        lastEvent.imagePaths
                    }
                    
                    // Assign a random color for the new dot (consistent with previous logic, though ideally stored)
                    // Since we don't store color in DB, we generate it deterministically or randomly here.
                    // For now, random is fine as per original logic, but it will change on reload.
                    // To fix this, we should store color in DB, but for now let's keep it simple.
                    val newColorIndex = (0..2).random() 

                    groupedEvents[index] = lastEvent.copy(
                        timestamps = lastEvent.timestamps + entity.timestamp,
                        colorIndices = lastEvent.colorIndices + newColorIndex,
                        imagePaths = newImagePaths
                    )
                } else {
                    val initialImagePaths = if (entity.imagePath != null) listOf(entity.imagePath) else emptyList()
                    val newColorIndex = (0..2).random()
                    groupedEvents.add(Event(entity.type, listOf(entity.timestamp), listOf(newColorIndex), initialImagePaths))
                }
            }
            groupedEvents
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weightData: StateFlow<List<com.example.eat.data.HealthDataEntity>> = healthDao.getAllHealthData()
        .map { list -> list.filter { it.type == "Weight" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bloodPressureData: StateFlow<List<com.example.eat.data.HealthDataEntity>> = healthDao.getAllHealthData()
        .map { list -> list.filter { it.type == "BloodPressure" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEvent(type: String, imagePath: String? = null) {
        viewModelScope.launch {
            val newTimestamp = System.currentTimeMillis()
            val eventEntity = EventEntity(
                type = type,
                timestamp = newTimestamp,
                imagePath = imagePath
            )
            eventDao.insertEvent(eventEntity)
        }
    }

    fun saveWeight(weight: Float) {
        viewModelScope.launch {
            val healthData = com.example.eat.data.HealthDataEntity(
                timestamp = System.currentTimeMillis(),
                type = "Weight",
                value1 = weight
            )
            healthDao.insertHealthData(healthData)
        }
    }

    fun saveBloodPressure(systolic: Float, diastolic: Float) {
        viewModelScope.launch {
            val healthData = com.example.eat.data.HealthDataEntity(
                timestamp = System.currentTimeMillis(),
                type = "BloodPressure",
                value1 = systolic,
                value2 = diastolic
            )
            healthDao.insertHealthData(healthData)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            eventDao.deleteAllEvents()
            healthDao.deleteAllHealthData()
        }
    }
}
