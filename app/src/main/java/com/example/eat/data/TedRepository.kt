package com.example.eat.data

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class TedRepository {
    private val client = OkHttpClient()
    private val rssUrl = "http://feeds.feedburner.com/tedtalks_audio"

    suspend fun fetchTedTalks(): List<TedTalkItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(rssUrl)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext emptyList()
            // Check if response is successful
            if (!response.isSuccessful) return@withContext emptyList()
            
            return@withContext parseRss(responseBody)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun fetchTranscript(url: String): Pair<List<TranscriptLine>, Long> = withContext(Dispatchers.IO) {
        if (url.isEmpty()) return@withContext Pair(emptyList(), 0L)
        try {
            // Check if it's a Go TED URL or direct TED URL
            // If it's Go TED, Jsoup handles redirects.
            // We want to append /transcript if it's a TED talk page.
            // But we can't easily append until we know the final URL.
            // So logic:
            // 1. Connect to original URL to resolve redirect and get final URL.
            val connection = org.jsoup.Jsoup.connect(url).followRedirects(true)
            val initialResponse = connection.execute()
            val finalUrl = initialResponse.url().toString()
            val host = initialResponse.url().host
            
            // Only proceed if it's strictly ted.com (audiocollective often doesn't have transcripts)
            if (!host.contains("ted.com")) {
                 return@withContext Pair(emptyList(), 0L)
            }
            
            // 2. If final URL contains "ted.com/talks", append /transcript
            val transcriptUrl = if (finalUrl.contains("ted.com/talks") && !finalUrl.endsWith("/transcript")) {
                "$finalUrl/transcript"
            } else {
                return@withContext Pair(emptyList(), 0L) // Not a standard talk page
            }
            
            val doc = org.jsoup.Jsoup.connect(transcriptUrl).get()
            
            // 3. Extract transcript text.
            // The text is NOT in static HTML paragraphs, it's hydration data in __NEXT_DATA__.
            val scriptTag = doc.select("script[id=__NEXT_DATA__]").firstOrNull() ?: return@withContext Pair(emptyList(), 0L)
            val jsonText = scriptTag.data()
            
            // We need to parse this JSON. 
            // Since we don't have Gson/Moshi set up as a dependency for general use (only Retrofit scaler),
            // and adding a full JSON library might be overkill if we just need one field,
            // we can try a regex approach or adding a JSON library.
            // But wait, we DO have Retrofit, which often implies Gson or similar.
            // Actually, let's just use a simple Regex to extract the "transcript" section if possible, 
            // OR finding the "transcript" text inside the JSON is risky with regex due to nesting.
            //
            // Better approach: We passed `serialization-json` or simply `org.json` (part of Android SDK).
            // Android has `org.json.JSONObject`.
            
            val jsonObject = org.json.JSONObject(jsonText)
            val props = jsonObject.optJSONObject("props")
            val pageProps = props?.optJSONObject("pageProps")
            var videoDuration = 0L
            val videoData = pageProps?.optJSONObject("videoData")
            if (videoData != null) {
                videoDuration = videoData.optLong("duration")
            }
            
            val transcriptData = pageProps?.optJSONObject("transcriptData")
            val translation = transcriptData?.optJSONObject("translation")
            val paragraphs = translation?.optJSONArray("paragraphs")

            
            if (paragraphs != null) {
                val lines = mutableListOf<TranscriptLine>()
                for (i in 0 until paragraphs.length()) {
                    val paragraph = paragraphs.optJSONObject(i)
                    val cues = paragraph?.optJSONArray("cues")
                    if (cues != null) {
                        for (j in 0 until cues.length()) {
                            val cue = cues.optJSONObject(j)
                            val text = cue?.optString("text")
                            val time = cue?.optLong("time") ?: 0L
                            if (!text.isNullOrEmpty()) {
                                lines.add(TranscriptLine(text, time))
                            }
                        }
                    }
                }
                return@withContext Pair(lines, videoDuration)
            }
            
            return@withContext Pair(emptyList(), 0L)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Pair(emptyList(), 0L)
        }
    }

    private fun parseRss(xml: String): List<TedTalkItem> {
        val items = mutableListOf<TedTalkItem>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var currentTitle = ""
        var currentDescription = ""
        var currentAudioUrl = ""
        var currentImageUrl = ""
        var currentLink = ""
        var currentDuration = 0L
        var insideItem = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = true
                        currentTitle = ""
                        currentDescription = ""
                        currentAudioUrl = ""
                        currentImageUrl = ""
                        currentLink = ""
                        currentDuration = 0L
                    }
                    if (insideItem) {
                        when {
                            tagName.equals("title", ignoreCase = true) -> currentTitle = parser.nextText()
                            tagName.equals("link", ignoreCase = true) -> currentLink = parser.nextText()
                            tagName.equals("description", ignoreCase = true) -> {
                                // Only use description if we haven't found richer content
                                if (currentDescription.isEmpty()) {
                                    currentDescription = parser.nextText()
                                }
                            }
                            tagName.equals("encoded", ignoreCase = true) && parser.prefix == "content" -> {
                                // Prefer content:encoded for full text/transcript
                                currentDescription = parser.nextText()
                            }
                            tagName.equals("enclosure", ignoreCase = true) -> {
                                currentAudioUrl = parser.getAttributeValue(null, "url") ?: ""
                            }
                            tagName.equals("itunes:duration", ignoreCase = true) || (tagName.equals("duration", ignoreCase = true) && parser.prefix == "itunes") -> {
                                val durationStr = parser.nextText()
                                // format usually HH:MM:SS or MM:SS
                                val parts = durationStr.split(":")
                                var seconds = 0L
                                if (parts.size == 3) {
                                    seconds = parts[0].toLong() * 3600 + parts[1].toLong() * 60 + parts[2].toLong()
                                } else if (parts.size == 2) {
                                    seconds = parts[0].toLong() * 60 + parts[1].toLong()
                                } else if (parts.size == 1) {
                                    seconds = parts[0].toLong()
                                }
                                currentDuration = seconds
                            }
                            tagName.equals("image", ignoreCase = true) && parser.prefix == "itunes" -> {
                                currentImageUrl = parser.getAttributeValue(null, "href") ?: ""
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = false
                        if (currentTitle.isNotEmpty() && currentAudioUrl.isNotEmpty()) {
                            // Clean up description (remove HTML tags if any, FeedBurner often has them)
                            // A simple regex to remove tags is minimal but effective for display
                            val cleanDescription = try {
                                org.jsoup.Jsoup.parse(currentDescription).text()
                            } catch (e: Exception) {
                                currentDescription.replace(Regex("<.*?>"), "").trim()
                            }
                            
                            items.add(
                                TedTalkItem(
                                    title = currentTitle,
                                    description = cleanDescription,
                                    audioUrl = currentAudioUrl,
                                    imageUrl = currentImageUrl,
                                    link = currentLink,
                                    transcript = cleanDescription, // Initial fallback
                                    transcriptLines = emptyList(),
                                    duration = currentDuration
                                )
                            )
                        }
                    }
                }
            }
            try {
                eventType = parser.next()
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }
        return items
    }
}
