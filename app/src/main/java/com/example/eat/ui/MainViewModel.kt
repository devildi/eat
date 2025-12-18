package com.example.eat.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eat.data.AppDatabase
import com.example.eat.data.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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
    private val articleDao = database.articleDao()
    
    val articleUiState: StateFlow<ArticleUiState> = articleDao.getAllArticles()
        .map { ArticleUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ArticleUiState.Loading)


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

    fun parseArticle(url: String, onSuccess: (String, String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .timeout(10000)
                    .get()

                var title = doc.title()
                if (title.isEmpty()) {
                    title = doc.select("h1").text()
                }
                if (title.isEmpty()) {
                    title = "未命名文章"
                }
                
                // Content extraction strategy
                // 1. Try generic "article" tag
                // 2. Try common class names
                // 3. Fallback to all paragraphs
                var content = doc.select("div#js_content").text() // WeChat specific
                if (content.isEmpty()) {
                     content = doc.select("article").text()
                }
                if (content.isEmpty()) {
                    content = doc.select("div.content").text()
                }
                if (content.isEmpty()) {
                    content = doc.select("p").eachText().joinToString("\n\n")
                }
                if (content.isEmpty()) {
                    content = doc.body().text() // Ultimate fallback
                }

                withContext(Dispatchers.Main) {
                    onSuccess(title, content)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // On error, let user edit manually (empty fields)
                    onSuccess("获取失败", "")
                }
            }
        }
    }

    fun saveArticle(title: String, content: String, url: String) {
        viewModelScope.launch {
            val article = com.example.eat.data.ArticleEntity(
                title = title,
                content = content,
                url = url,
                timestamp = System.currentTimeMillis()
            )
            articleDao.insertArticle(article)
        }
    }

    fun deleteArticle(article: com.example.eat.data.ArticleEntity) {
        viewModelScope.launch {
            articleDao.deleteArticle(article)
        }
    }


    fun deleteAllData() {
        viewModelScope.launch {
            eventDao.deleteAllEvents()
            healthDao.deleteAllHealthData()
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            // 1. Delete from Database
            eventDao.deleteEvents(event.timestamps)

            // 2. Delete associated images from storage
            event.imagePaths.forEach { path ->
                try {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun deleteEventByImagePath(imagePath: String) {
        viewModelScope.launch {
            // 1. Delete from Database
            eventDao.deleteEventByImagePath(imagePath)

            // 2. Delete image from storage
            try {
                val file = java.io.File(imagePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getEventsByDate(dateMillis: Long): Flow<List<Event>> {
        // Calculate start and end of the selected day
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return eventDao.getEventsByDateRange(startOfDay, endOfDay)
            .map { entities ->
                // Group entities into UI Events (merged by 10 mins)
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
    }


}

sealed interface ArticleUiState {
    data object Loading : ArticleUiState
    data class Success(val articles: List<com.example.eat.data.ArticleEntity>) : ArticleUiState
}
