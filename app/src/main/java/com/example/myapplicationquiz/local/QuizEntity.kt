package com.example.myapplicationquiz.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val updatedAt: Long
)
