package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sentences")
data class Sentence(
    @PrimaryKey
    val id: Int,
    val english: String,
    val ukrainian: String,
    val category: String,
    val level: String = "A1", // A1, A2, B1
    val language: String = "EN", // EN, DE
    val isCompleted: Boolean = false,
    val attempts: Int = 0
)
