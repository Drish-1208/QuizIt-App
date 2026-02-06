package com.example.myapplication
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

// Note: We might need to change the Adapter's data type later, but for now, this works.

class HistoryActivity : AppCompatActivity() {

    // --- 1. DEFINE UI ELEMENTS AND DATABASE REFERENCES ---
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyProgressBar: ProgressBar
    private lateinit var noHistoryTextView: TextView
    private lateinit var historyAdapter: HistoryAdapter // We will use your existing adapter

    // Navigation buttons
    private lateinit var quickQuizNavButton: Button
    private lateinit var pdfQuizNavButton: Button
    private lateinit var viewHistoryNavButton: Button
    private lateinit var profileNavButton: Button

    // Keep Firebase Auth for getting the user ID
    private val auth = Firebase.auth

    // --- NEW: Get a reference to the Room Database DAO ---
    private val quizHistoryDao by lazy {
        QuizHistoryDatabase.getDatabase(applicationContext).quizHistoryDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // --- 2. INITIALIZE THE VIEWS FROM THE LAYOUT ---
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        historyProgressBar = findViewById(R.id.historyProgressBar)
        noHistoryTextView = findViewById(R.id.noHistoryTextView)

        // Initialize navigation buttons
        quickQuizNavButton = findViewById(R.id.quickQuizNavButton)
        pdfQuizNavButton = findViewById(R.id.pdfQuizNavButton)
        viewHistoryNavButton = findViewById(R.id.viewHistoryNavButton)
        profileNavButton = findViewById(R.id.profileNavButton)

        historyRecyclerView.layoutManager = LinearLayoutManager(this)

        // --- 3. START THE PROCESS OF FETCHING DATA AND SETUP LISTENERS ---
        // The rest of your onCreate is good, no changes needed here
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // --- NEW: Fetch data every time the screen is shown ---
        // This ensures the list is up-to-date if you just finished a quiz.
        fetchQuizHistoryFromRoom()
    }

    private fun setupBottomNavigation() {
        // This function is perfectly fine, no changes needed.
        quickQuizNavButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        pdfQuizNavButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        viewHistoryNavButton.setOnClickListener { /* Do nothing */ }

        profileNavButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // --- 4. REWRITTEN: Function to fetch history from the Room database ---
    // --- 4. REWRITTEN: Function to fetch history from the Room database ---
    private fun fetchQuizHistoryFromRoom() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            historyProgressBar.isVisible = false
            noHistoryTextView.text = "You must be logged in to view history."
            noHistoryTextView.isVisible = true
            return
        }

        historyProgressBar.isVisible = true
        noHistoryTextView.isVisible = false
        historyRecyclerView.isVisible = false // Hide recycler view while loading

        val userId = currentUser.uid

        // Use a coroutine to launch the data collection
        lifecycleScope.launch {
            try {
                // *** FIX APPLIED HERE ***
                // We call .collect on the Flow to receive the list it emits.
                quizHistoryDao.getAllHistoryForUser(userId).collect { historyList ->

                    // The 'historyList' variable inside this block is the actual List<QuizHistory>.
                    // All UI logic must be handled inside this block.

                    historyProgressBar.isVisible = false // Hide progress bar once data is received

                    if (historyList.isEmpty()) { // This check is now on the List, which is correct.
                        noHistoryTextView.isVisible = true
                        historyRecyclerView.isVisible = false
                    } else {
                        noHistoryTextView.isVisible = false
                        historyRecyclerView.isVisible = true

                        // --- 5. SETUP THE ADAPTER WITH THE NEW DATA ---
                        historyAdapter = HistoryAdapter(historyList)
                        historyRecyclerView.adapter = historyAdapter
                    }
                }
            } catch (e: Exception) {
                // Handle any potential database or flow errors
                historyProgressBar.isVisible = false
                noHistoryTextView.text = "Failed to load history from local database."
                noHistoryTextView.isVisible = true
                Log.e("HistoryActivity", "Error collecting history from Room DB", e)
            }
        }
    }
}
