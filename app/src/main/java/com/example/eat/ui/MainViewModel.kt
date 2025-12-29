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
import kotlinx.coroutines.flow.asStateFlow
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
    private val tedDao = database.tedDao()
    private val syncManager = com.example.eat.utils.SyncManager(application, articleDao, healthDao, eventDao)

    private val _serverIp = kotlinx.coroutines.flow.MutableStateFlow("")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _syncStatus = kotlinx.coroutines.flow.MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _backupSummary = kotlinx.coroutines.flow.MutableStateFlow<com.example.eat.utils.BackupSummary?>(null)
    val backupSummary: StateFlow<com.example.eat.utils.BackupSummary?> = _backupSummary.asStateFlow()

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun startLanServer() {
        try {
            val ip = syncManager.startServer()
            _serverIp.value = ip
            _syncStatus.value = "服务运行于 $ip"
            // Ensure data is exported when server starts
            viewModelScope.launch {
                val summary = syncManager.exportData()
                _backupSummary.value = summary
                _syncStatus.value = "数据已打包并准备就绪"
            }
        } catch (e: Exception) {
            _syncStatus.value = "启动服务失败: ${e.message}"
        }
    }

    fun stopLanServer() {
        syncManager.stopServer()
        syncManager.deleteBackup()
        _serverIp.value = ""
        _syncStatus.value = "服务已停止"
        _backupSummary.value = null
    }

    fun importLanData(ip: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "正在连接到 $ip..."
            try {
                val summary = syncManager.importData(ip)
                val sizeMb = String.format("%.2f", summary.totalSizeBytes / (1024.0 * 1024.0))
                _syncStatus.value = "同步成功！\n数据大小: ${sizeMb}MB\n文章: ${summary.articleCount}, 记录: ${summary.eventCount}, 健康数据: ${summary.healthDataCount}"
            } catch (e: Exception) {
                _syncStatus.value = "同步失败: ${e.message}"
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    val articleUiState: StateFlow<ArticleUiState> = articleDao.getAllArticles()
        .map { ArticleUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ArticleUiState.Loading)

    val todayArticleCount: StateFlow<Int> = run {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1
        articleDao.getArticleCountByDateRange(startOfDay, endOfDay)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }


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
            // Calculate start and end of today
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

            // Delete existing weight data for today (Replace logic)
            healthDao.deleteHealthDataByTypeAndDate("Weight", startOfDay, endOfDay)

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
            // Calculate start and end of today
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1

            // Delete existing blood pressure data for today (Replace logic)
            healthDao.deleteHealthDataByTypeAndDate("BloodPressure", startOfDay, endOfDay)

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

    suspend fun isArticleExists(title: String): Boolean {
        return articleDao.getArticleByTitle(title) != null
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

    fun updateEventCategory(timestamps: List<Long>, newType: String) {
        viewModelScope.launch {
            eventDao.updateEventType(timestamps, newType)
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


    private val tedRepository = com.example.eat.data.TedRepository()
    
    private val _currentTedTalk = kotlinx.coroutines.flow.MutableStateFlow<com.example.eat.data.TedTalkItem?>(null)
    val currentTedTalk: StateFlow<com.example.eat.data.TedTalkItem?> = _currentTedTalk.asStateFlow()
    
    // Loading State
    private val _isTedLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isTedLoading: StateFlow<Boolean> = _isTedLoading.asStateFlow()

    private val _hasTedFetchCompleted = kotlinx.coroutines.flow.MutableStateFlow(false)
    val hasTedFetchCompleted: StateFlow<Boolean> = _hasTedFetchCompleted.asStateFlow()

    private val _currentPlaybackPosition = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val currentPlaybackPosition: StateFlow<Long> = _currentPlaybackPosition.asStateFlow()

    fun updatePlaybackPosition(position: Long) {
        _currentPlaybackPosition.value = position
    }

    private val _isCalibrated = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated.asStateFlow()

    fun fetchRandomTedTalk() {
        viewModelScope.launch {
            _isTedLoading.value = true
            _hasTedFetchCompleted.value = false
            _isCalibrated.value = false

            // 1. Check Local Cache
            val cachedTalk = tedDao.getLatestTalk()
            val today = java.time.LocalDate.now().toEpochDay()
            val cachedDay = if (cachedTalk != null) java.time.Instant.ofEpochMilli(cachedTalk.fetchTimestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay() else -1L

            if (cachedTalk != null && cachedDay == today) {
                // Use cached talk
                _currentTedTalk.value = com.example.eat.data.TedTalkItem(
                    title = cachedTalk.title,
                    description = cachedTalk.description,
                    audioUrl = cachedTalk.audioUrl,
                    imageUrl = cachedTalk.imageUrl,
                    link = cachedTalk.link,
                    transcript = cachedTalk.transcript,
                    transcriptLines = cachedTalk.transcriptLines,
                    rawTranscriptLines = cachedTalk.transcriptLines, // Assuming stored lines are raw
                    duration = cachedTalk.duration,
                    isCalibrated = cachedTalk.isCalibrated
                )
                _isTedLoading.value = false
                _hasTedFetchCompleted.value = true
                _isCalibrated.value = cachedTalk.isCalibrated
                return@launch
            }

            // 2. Fetch from Network if no valid cache
            // Clean old cache first
            tedDao.deleteAllTalks()

            val talks = tedRepository.fetchTedTalks().toMutableList()
            var foundTalk: com.example.eat.data.TedTalkItem? = null
            var attempts = 0
            val maxAttempts = 5

            while (foundTalk == null && attempts < maxAttempts && talks.isNotEmpty()) {
                attempts++
                val candidateIndex = talks.indices.random()
                val candidate = talks[candidateIndex]
                
                // Try fetching transcript
                // We do this inside the loop (suspending)
                val (transcriptLinesSorted, videoDuration) = tedRepository.fetchTranscript(candidate.link)
                
                if (transcriptLinesSorted.isNotEmpty()) {
                    var finalLines = transcriptLinesSorted
                    
                    // Logic: RSS audio duration (e.g. 15:00) vs Video duration (e.g. 14:40)
                    // The difference is usually the intro.
                    // If RSS > Video, offset = RSS - Video.
                    // But we need to convert everything to milliseconds.
                    val rssDurationSec = candidate.duration 
                    if (rssDurationSec > 0 && videoDuration > 0) {
                        val offsetSec = rssDurationSec - videoDuration
                        
                        // Heuristic: If offset is positive and reasonable (e.g. < 300s), assume it's intro.
                        // Sometimes offset might be weird if metadata is bad.
                        // Common intro is ~5-15s.
                        if (offsetSec > 0 && offsetSec < 300) { 
                             val offsetMs = offsetSec * 1000
                             finalLines = transcriptLinesSorted.map { 
                                 it.copy(startTime = it.startTime + offsetMs) 
                             }
                        }
                    }
                    
                    // Create a single string for fallback compatibility if needed, 
                    // or just use the lines.
                    val fullText = finalLines.joinToString("\n\n") { it.text }
                    foundTalk = candidate.copy(
                        transcript = fullText,
                        transcriptLines = finalLines,
                        rawTranscriptLines = transcriptLinesSorted
                    )
                } else {
                    // Remove this candidate from potential list so we don't pick it again
                    talks.removeAt(candidateIndex)
                }
            }

            // Fallback: if loop finished without success, just pick a random one with description
            if (foundTalk == null && talks.isNotEmpty()) {
                 // Re-fetch original list if we emptied it, or just use what's left
                 // Simple fallback: just use the initial random one (even if no transcript)
                 // or show error? Better to show something.
                 val fallback = tedRepository.fetchTedTalks().randomOrNull()
                 foundTalk = fallback
            }

            if (foundTalk != null) {
                 // Save to Cache
                 val entity = com.example.eat.data.TedTalkEntity(
                     title = foundTalk.title,
                     description = foundTalk.description,
                     audioUrl = foundTalk.audioUrl,
                     imageUrl = foundTalk.imageUrl,
                     link = foundTalk.link,
                     transcript = foundTalk.transcript,
                     duration = foundTalk.duration,
                     fetchTimestamp = System.currentTimeMillis(),
                     transcriptLines = foundTalk.transcriptLines,
                     isCalibrated = false
                 )
                 tedDao.insertTalk(entity)
            }

            _currentTedTalk.value = foundTalk
            _isTedLoading.value = false
            _hasTedFetchCompleted.value = true
        }
    }
    
    fun syncTranscript(currentAudioPositionMs: Long) {
        viewModelScope.launch {
            val currentTalk = _currentTedTalk.value ?: return@launch
            if (currentTalk.rawTranscriptLines.isEmpty()) return@launch
            
            // Logic:
            // User clicks "Sync" when they hear the first sentence.
            // This means, the actual start of the first sentence in the AUDIO is 'currentAudioPositionMs'.
            // In raw transcript, the first sentence usually starts at 0 (or very close to it).
            // So offset = currentAudioPositionMs - firstLine.rawStartTime.
            // We apply this offset to ALL lines.
            
            val firstLineRawTime = currentTalk.rawTranscriptLines.firstOrNull()?.startTime ?: 0L
            val offset = currentAudioPositionMs - firstLineRawTime
            
            val newLines = currentTalk.rawTranscriptLines.map { 
                it.copy(startTime = it.startTime + offset) 
            }
            
            val updatedTalk = currentTalk.copy(
                transcriptLines = newLines,
                isCalibrated = true
            )
            _currentTedTalk.value = updatedTalk
            _isCalibrated.value = true

            // Persist the calibration
            // We interpret "Calibration" as fixing the data for this talk.
            // Since we only cache one talk, we overwrite the cache.
            try {
                tedDao.deleteAllTalks()
                val entity = com.example.eat.data.TedTalkEntity(
                    title = updatedTalk.title,
                    description = updatedTalk.description,
                    audioUrl = updatedTalk.audioUrl,
                    imageUrl = updatedTalk.imageUrl,
                    link = updatedTalk.link,
                    transcript = updatedTalk.transcript,
                    duration = updatedTalk.duration,
                    fetchTimestamp = System.currentTimeMillis(), // We could keep the original or update it. Updating keeps it "fresh".
                    transcriptLines = updatedTalk.transcriptLines,
                    isCalibrated = true
                )
                tedDao.insertTalk(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Configuration State (Persistent)
    private val sharedPreferences = application.getSharedPreferences("app_config", android.content.Context.MODE_PRIVATE)

    private val _isSyncEnabled = kotlinx.coroutines.flow.MutableStateFlow(sharedPreferences.getBoolean("is_sync_enabled", false))
    val isSyncEnabled: StateFlow<Boolean> = _isSyncEnabled.asStateFlow()

    private val _syncTarget = kotlinx.coroutines.flow.MutableStateFlow(sharedPreferences.getString("sync_target", "FIREBASE") ?: "FIREBASE")
    val syncTarget: StateFlow<String> = _syncTarget.asStateFlow()

    fun setSyncEnabled(enabled: Boolean) {
        _isSyncEnabled.value = enabled
        sharedPreferences.edit().putBoolean("is_sync_enabled", enabled).apply()
    }

    fun setSyncTarget(target: String) {
        _syncTarget.value = target
        sharedPreferences.edit().putString("sync_target", target).apply()
    }
}

sealed interface ArticleUiState {
    data object Loading : ArticleUiState
    data class Success(val articles: List<com.example.eat.data.ArticleEntity>) : ArticleUiState
}
