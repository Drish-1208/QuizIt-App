package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QuizHistory::class], version = 1, exportSchema = false)
abstract class QuizHistoryDatabase : RoomDatabase() {

    abstract fun quizHistoryDao(): QuizHistoryDao

    companion object {
        // The @Volatile annotation ensures that the INSTANCE variable is always up-to-date
        // and visible to all execution threads.
        @Volatile
        private var INSTANCE: QuizHistoryDatabase? = null

        fun getDatabase(context: Context): QuizHistoryDatabase {
            // Return the existing instance if it's not null.
            // If it is null, create the database in a thread-safe way.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizHistoryDatabase::class.java,
                    "quiz_history_database" // This will be the name of the database file
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
