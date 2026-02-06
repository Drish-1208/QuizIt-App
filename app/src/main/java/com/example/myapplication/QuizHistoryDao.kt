package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizHistoryDao {

    // Inserts a new quiz history record. If there's a conflict, it ignores it.
    // This returns the 'id' of the new row, which is crucial.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(quizHistory: QuizHistory): Long

    // This is the function QuizResultActivity will use to update the final score.
    @Query("UPDATE quiz_history_table SET correctAnswers = :correctAnswers WHERE id = :quizId")
    suspend fun updateScore(quizId: Long, correctAnswers: Int)

    // This function gets all history records for a specific user, ordered by the newest first.
    // It returns a Flow, so the UI can automatically update when the data changes.
    @Query("SELECT * FROM quiz_history_table WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllHistoryForUser(userId: String): Flow<List<QuizHistory>>

    // Optional: A function to clear all history for a specific user.
    @Query("DELETE FROM quiz_history_table WHERE userId = :userId")
    suspend fun clearHistoryForUser(userId: String,)
}
