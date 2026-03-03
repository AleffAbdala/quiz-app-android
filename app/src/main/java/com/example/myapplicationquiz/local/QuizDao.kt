package com.example.myapplicationquiz.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<QuizEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM quizzes")
    suspend fun clearQuizzes()

    @Query("DELETE FROM questions")
    suspend fun clearQuestions()

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsByQuizId(quizId: String)

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuizById(quizId: String)

    @Transaction
    @Query("SELECT * FROM quizzes ORDER BY title ASC")
    suspend fun getQuizzesWithQuestions(): List<QuizWithQuestions>
}
