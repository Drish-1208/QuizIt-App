package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var db: AppDatabase // Reference to the database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // Initialize the database instance
        db = AppDatabase.getDatabase(applicationContext)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize adapter with an empty list. It will be updated shortly.
        adapter = HistoryAdapter(emptyList())
        recyclerView.adapter = adapter

        // Load the history from the database
        loadHistory()
    }

    private fun loadHistory() {
        // Use a coroutine to observe the database for changes
        lifecycleScope.launch {
            db.quizDao().getAllResults().collectLatest { resultsList ->
                // This block will run every time the data changes.
                // It automatically runs on the main thread.
                adapter.updateData(resultsList)
            }
        }
    }
}
