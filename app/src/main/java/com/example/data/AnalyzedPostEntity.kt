package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analyzed_posts")
data class AnalyzedPostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postText: String,
    val author: String,
    val platform: String,
    val explanation: String,
    val sentiment: String,
    val repliesJson: String, // serialized json of replies
    val selectedTone: String,
    val isFavorite: Boolean = false,
    val copyCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
