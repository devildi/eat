package com.example.eat.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class Event(
    val type: String,
    val timestamps: List<Long>, // All merged timestamps
    val colorIndices: List<Int> // All colors (0=Red, 1=Yellow, 2=Blue)
) {
    // Helper to get the latest timestamp
    val latestTimestamp: Long get() = timestamps.last()
}

class MainViewModel : ViewModel() {
    private val _events = mutableStateListOf<Event>()
    val events: List<Event> = _events

    fun addEvent(type: String) {
        val newTimestamp = System.currentTimeMillis()
        val newColorIndex = (0..2).random()
        
        // Find the last event of the same type
        val lastSameTypeEvent = _events.lastOrNull { it.type == type }
        
        if (lastSameTypeEvent != null) {
            val timeDiffMinutes = (newTimestamp - lastSameTypeEvent.latestTimestamp) / (60 * 1000)
            
            // If within 40 minutes, merge with existing event
            if (timeDiffMinutes < 40) {
                val index = _events.indexOf(lastSameTypeEvent)
                _events[index] = lastSameTypeEvent.copy(
                    timestamps = lastSameTypeEvent.timestamps + newTimestamp,
                    colorIndices = lastSameTypeEvent.colorIndices + newColorIndex
                )
                return
            }
        }
        
        // Otherwise, create a new event
        _events.add(Event(type, listOf(newTimestamp), listOf(newColorIndex)))
    }
}
