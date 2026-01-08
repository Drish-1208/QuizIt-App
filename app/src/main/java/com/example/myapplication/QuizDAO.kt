package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity)

    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<QuizResultEntity>>
}
