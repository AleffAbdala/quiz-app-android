package com.example.myapplicationquiz.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ResultEntity)

    @Query("SELECT * FROM results WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllResultsByUser(userId: String): List<ResultEntity>

    @Query("SELECT COUNT(*) FROM results WHERE userId = :userId")
    suspend fun getQuizCountByUser(userId: String): Int

    @Query("SELECT COALESCE(SUM(score), 0) FROM results WHERE userId = :userId")
    suspend fun getTotalCorrectByUser(userId: String): Int

    @Query("SELECT COALESCE(SUM(totalQuestions), 0) FROM results WHERE userId = :userId")
    suspend fun getTotalQuestionsByUser(userId: String): Int

    @Query("SELECT COALESCE(AVG((score * 100.0) / totalQuestions), 0) FROM results WHERE userId = :userId")
    suspend fun getAveragePercentByUser(userId: String): Double

    @Query("DELETE FROM results")
    suspend fun deleteAll()
}
