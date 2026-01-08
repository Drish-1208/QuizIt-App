package com.example.myapplication

data class Question(
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int //0 for A, 1 for B, etc.
)
