package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val english: String,
    val ukrainian: String,
    val transcription: String,
    val partOfSpeech: String,
    val category: String,
    val exampleEn: String,
    val exampleUa: String,
    val level: String = "A1", // A1, A2, B1
    val language: String = "EN", // EN, DE
    val isBookmarked: Boolean = false,
    val masteryLevel: Int = 0, // 0 = New, 1 = Learning, 2 = Familiar, 3 = Mastered
    val correctAttempts: Int = 0,
    val wrongAttempts: Int = 0,
    val lastReviewedTimestamp: Long = 0L
)

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val id: Int = 1,
    val totalXp: Int = 0,
    val quizzesCompleted: Int = 0,
    val currentStreak: Int = 1,
    val bestStreak: Int = 1,
    val lastActiveDate: String = ""
)
