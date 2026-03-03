package com.example.myapplicationquiz.local


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val quizId: String,
    val title: String,
    val score: Int,
    val totalQuestions: Int,
    val elapsedSeconds: Long,
    val timestamp: Long
)
