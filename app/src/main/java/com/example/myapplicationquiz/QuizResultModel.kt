package com.example.myapplicationquiz

data class QuizResultModel(
    val resultId: String = "",
    val userId: String = "",
    val quizId: String = "",
    val title: String = "",
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val elapsedSeconds: Long = 0L,
    val timestamp: Long = 0L
)
