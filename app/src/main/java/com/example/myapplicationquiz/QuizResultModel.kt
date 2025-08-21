package com.example.myapplicationquiz

data class QuizResultModel(
    val quizId: String = "",
    val title: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val timestamp: Long = 0L
)

