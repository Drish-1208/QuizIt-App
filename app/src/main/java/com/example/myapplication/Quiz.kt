package com.example.myapplication

import com.google.firebase.database.ServerValue

// This data class will hold the results of a single quiz.
data class QuizResult(
    val quizTopic: String ="",
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val timestamp: Any = ServerValue.TIMESTAMP // Automatically records the time of the quiz
)
