package com.example.myapplication.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class Quiz(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val timestamp: Long,

    // --- NEW COLUMNS TO ADD FOR THE REVIEW FEATURE ---
    val fullQuizData: String,
    val userAnswers: String,
    val correctAnswers: String,
)
