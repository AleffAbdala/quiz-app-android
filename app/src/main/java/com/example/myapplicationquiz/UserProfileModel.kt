package com.example.myapplicationquiz

data class UserProfileModel(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val lastLoginAt: Long = 0L
)
