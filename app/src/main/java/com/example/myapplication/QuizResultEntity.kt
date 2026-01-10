package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val topic: String,
    val score: String,
    val timestamp: Long = System.currentTimeMillis(),
)
