package com.example.eat.utils

import android.content.Context
import android.util.Log
import com.example.eat.data.ArticleDao
import com.example.eat.data.EventDao
import com.example.eat.data.HealthDao
import com.example.eat.data.ArticleEntity
import com.example.eat.data.EventEntity
import com.example.eat.data.HealthDataEntity
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import fi.iki.elonen.NanoHTTPD

data class BackupData(
    val articles: List<ArticleEntity>,
    val healthData: List<HealthDataEntity>,
    val events: List<EventEntity>,
    val timestamp: Long = System.currentTimeMillis()
)

data class BackupSummary(
    val file: File,
    val articleCount: Int,
    val imageCount: Int,
    val healthDataCount: Int,
    val eventCount: Int
)

data class ImportSummary(
    val totalSizeBytes: Long,
    val articleCount: Int,
    val eventCount: Int,
    val healthDataCount: Int
)



class SyncManager(
    private val context: Context,
    private val articleDao: ArticleDao,
    private val healthDao: HealthDao,
    private val eventDao: EventDao
) {
    private val gson = com.google.gson.GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(Int::class.java, SafeIntTypeAdapter())
        .registerTypeAdapter(Integer::class.java, SafeIntTypeAdapter())
        .create()
    private var server: SimpleWebServer? = null
    
    // File paths
    private val backupDir = File(context.cacheDir, "sync_backup")
    private val backupJsonFile = File(backupDir, "data.json")
    private val zipFile = File(backupDir, "backup.zip")

    suspend fun exportData(): BackupSummary = withContext(Dispatchers.IO) {
        if (!backupDir.exists()) backupDir.mkdirs()

        // 1. Fetch all data
        val articles = articleDao.getAllArticlesSync()
        val healthData = healthDao.getAllHealthDataSync()
        val events = eventDao.getAllEventsSync()

        val backup = BackupData(
            articles = articles,
            healthData = healthData.map { 
                if (it.type == "Weight") {
                    // Convert Kg to Jin for standardized transfer
                    it.copy(value1 = it.value1 * 2f) 
                } else it 
            },
            events = events
        )
        val json = gson.toJson(backup)
        
        backupJsonFile.writeText(json)
        
        // 2. Zip logic
        val imageFiles = events.mapNotNull { it.imagePath }.map { File(it) }.filter { it.exists() }
        
        zipFiles(listOf(backupJsonFile) + imageFiles, zipFile)
        
        return@withContext BackupSummary(
            file = zipFile,
            articleCount = articles.size,
            imageCount = imageFiles.size,
            healthDataCount = healthData.size,
            eventCount = events.size
        )
    }

    private fun zipFiles(files: List<File>, outputZip: File) {
        ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->
            files.forEach { file ->
                val entryName = if (file.name == "data.json") "data.json" else "images/${file.name}"
                zipOut.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { input ->
                    input.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }

    suspend fun importData(serverIp: String): ImportSummary = withContext(Dispatchers.IO) {
        val client = okhttp3.OkHttpClient()
        val finalIp = if (serverIp.contains(":")) serverIp else "$serverIp:8080"
        val request = okhttp3.Request.Builder()
            .url("http://$finalIp/backup.zip")
            .build()
            
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("Failed to connect to server")
        
        val tempZip = File(context.cacheDir, "import_temp.zip")
        val destDir = File(context.cacheDir, "import_temp_extracted")
        if (destDir.exists()) destDir.deleteRecursively()
        destDir.mkdirs()
        
        FileOutputStream(tempZip).use { output ->
            response.body?.byteStream()?.copyTo(output)
        }
        
        // Unzip
        java.util.zip.ZipInputStream(FileInputStream(tempZip)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val filePath = File(destDir, entry.name)
                if (!entry.isDirectory) {
                    filePath.parentFile?.mkdirs()
                    FileOutputStream(filePath).use { output ->
                        zipIn.copyTo(output)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        
        // Import JSON
        val jsonFile = File(destDir, "data.json")
        if (jsonFile.exists()) {
            val json = jsonFile.readText()
            val backup = gson.fromJson(json, BackupData::class.java)
            
            // Clear existing data (Overwrite mode)
            articleDao.clearAll()
            healthDao.deleteAllHealthData()
            eventDao.deleteAllEvents()

            // Insert Data
            backup.articles.forEach { articleDao.insertArticle(it) }
            backup.healthData.forEach { 
                val finalEntity = if (it.type == "Weight") {
                    // Convert Jin to Kg for local storage
                    it.copy(value1 = it.value1 * 0.5f)
                } else it
                healthDao.insertHealthData(finalEntity) 
            }
            
            // Handle Events & Images
            val finalImagesDir = File(context.filesDir, "photos") // Assumed standard
            if (!finalImagesDir.exists()) finalImagesDir.mkdirs()
            
            val importedImagesDir = File(destDir, "images")
            
            backup.events.forEach { event ->
                var finalEvent = event
                if (!event.imagePath.isNullOrEmpty()) {
                    val oldFile = File(event.imagePath)
                    val fileName = oldFile.name
                    val importedFile = File(importedImagesDir, fileName)
                    
                    if (importedFile.exists()) {
                        val finalFile = File(finalImagesDir, fileName)
                        importedFile.copyTo(finalFile, overwrite = true)
                        finalEvent = event.copy(imagePath = finalFile.absolutePath)
                    }
                }
                eventDao.insertEvent(finalEvent)
            }
            
            return@withContext ImportSummary(
                totalSizeBytes = zipFile.length(),
                articleCount = backup.articles.size,
                healthDataCount = backup.healthData.size,
                eventCount = backup.events.size
            )
        }
        throw Exception("No data.json found in backup")
    }

    fun startServer(startPort: Int = 8080): String {
        stopServer()
        
        var port = startPort
        val maxPort = startPort + 10
        var lastException: Exception? = null
        
        while (port <= maxPort) {
            try {
                val myServer = SimpleWebServer(port, backupDir)
                myServer.start()
                server = myServer
                return getDeviceIpAddress() + ":$port"
            } catch (e: Exception) { // NanoHTTPD might throw IOException or possibly others
                lastException = e
                Log.w("SyncManager", "Port $port occupied, trying next...")
                port++
            }
        }
        
        throw lastException ?: Exception("Could not find available port between $startPort and $maxPort")
    }

    fun stopServer() {
        server?.stop()
        server = null
    }

    fun deleteBackup() {
        if (backupDir.exists()) {
            backupDir.deleteRecursively()
        }
    }

    private fun getDeviceIpAddress(): String {
        // Simple implementation to get IP
         try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unknown"
    }
    
    private class SimpleWebServer(port: Int, private val rootDir: File) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            if (uri == "/backup.zip") {
                 val file = File(rootDir, "backup.zip")
                 if (file.exists()) {
                     return newChunkedResponse(Response.Status.OK, "application/zip", FileInputStream(file))
                 }
            }
            return newFixedLengthResponse("Hello from Android Sync Server!")
        }
    }

    private class SafeIntTypeAdapter : com.google.gson.TypeAdapter<Number>() {
        override fun write(out: com.google.gson.stream.JsonWriter, value: Number?) {
            out.value(value)
        }

        override fun read(input: com.google.gson.stream.JsonReader): Number {
            if (input.peek() == com.google.gson.stream.JsonToken.NULL) {
                input.nextNull()
                return 0
            }
            return try {
                input.nextInt()
            } catch (e: Exception) {
                input.nextString() // Consume the invalid value
                0 // Return default ID to trigger auto-gen
            }
        }
    }
}
