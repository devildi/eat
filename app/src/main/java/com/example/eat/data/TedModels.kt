package com.example.eat.data

data class TedTalkItem(
    val title: String,
    val description: String,
    val audioUrl: String,
    val imageUrl: String,
    val link: String,
    val transcript: String = "",
    val transcriptLines: List<TranscriptLine> = emptyList(),
    val rawTranscriptLines: List<TranscriptLine> = emptyList(),
    val duration: Long = 0L
)

data class TranscriptLine(
    val text: String,
    val startTime: Long
)
