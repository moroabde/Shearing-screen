package com.example.model

data class ReplySuggestion(
    val id: String,
    val text: String,
    val tone: String,
    val emoji: String = "✨",
    val highlightTag: String = "Suggested"
)

data class SocialAnalysisResult(
    val originalPostText: String,
    val author: String = "",
    val platform: String = "Social Media",
    val explanation: String,
    val sentiment: String = "Neutral",
    val keyTopics: List<String> = emptyList(),
    val suggestions: List<ReplySuggestion> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
