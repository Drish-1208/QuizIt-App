package com.example.myapplication

import androidx.room.Entity
import androidx.room.PrimaryKey@Entity(tableName = "quiz_history_table")
data class QuizHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Use Long for the ID, as insert() returns a Long.

    val quizTopic: String,
    val correctAnswers: Int,
    val totalQuestions: Int,
    val timestamp: Long,
    val quizData: String,      // The raw AI-generated text for the quiz
    val timerMinutes: Long,
    val userId: String         // To associate the history with a Firebase User
)
