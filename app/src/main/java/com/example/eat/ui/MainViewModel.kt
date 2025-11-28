package com.example.eat.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

data class Event(
    val type: String,
    val timestamp: Long
)

class MainViewModel : ViewModel() {
    private val _events = mutableStateListOf<Event>()
    val events: List<Event> = _events

    fun addEvent(type: String) {
        _events.add(Event(type, System.currentTimeMillis()))
    }
}
