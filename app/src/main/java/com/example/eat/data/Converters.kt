package com.example.eat.data

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromTranscriptLines(value: List<TranscriptLine>?): String {
        if (value == null) return "[]"
        val jsonArray = JSONArray()
        value.forEach { line ->
            val jsonObject = JSONObject()
            jsonObject.put("text", line.text)
            jsonObject.put("startTime", line.startTime)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toTranscriptLines(value: String?): List<TranscriptLine> {
        if (value == null) return emptyList()
        val list = mutableListOf<TranscriptLine>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val text = jsonObject.getString("text")
                val startTime = jsonObject.getLong("startTime")
                list.add(TranscriptLine(text, startTime))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
