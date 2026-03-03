package com.example.myapplicationquiz

data class RankingItem(
    val userId: String,
    val email: String,
    val averagePercent: Double,
    val attempts: Int
)
