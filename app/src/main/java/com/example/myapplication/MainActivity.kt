package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // These need to be declared here
    private lateinit var topicEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var generateQuizButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SESSION CHECK LOGIC ---
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            // If the user is NOT logged in, redirect them IMMEDIATELY.
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return // IMPORTANT: Stop further execution of onCreate
        }

        // --- If the code reaches here, it means the user IS logged in ---

        // NOW it is safe to set the content view and initialize everything.
        setContentView(R.layout.activity_main)

        // Initialize views
        topicEditText = findViewById(R.id.topicEditText)
        timeEditText = findViewById(R.id.timerEditText)
        generateQuizButton = findViewById(R.id.generateQuizButton)
        viewHistoryButton = findViewById(R.id.viewHistoryButton)
        logoutButton = findViewById(R.id.logoutButton)
        progressBar = findViewById(R.id.progressBar)

        // Set up button listeners
        generateQuizButton.setOnClickListener {
            val topic = topicEditText.text.toString().trim()
            val timeStr = timeEditText.text.toString().trim()
            val timeInMinutes = timeStr.toLongOrNull() ?: 0

            if (topic.isNotEmpty()) {
                generateQuiz(topic, timeInMinutes)
            } else {
                Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
            }
        }

        viewHistoryButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    // --- ALL THE FUNCTIONS BELOW WERE MISSING ---

    private fun logoutUser() {
        // 1. Update SharedPreferences to set isLoggedIn to false
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()

        // 2. Redirect to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Close MainActivity
    }

    private fun generateQuiz(topic: String, timeInMinutes: Long) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val prompt = "Generate a 10-question multiple-choice quiz on the topic of '$topic'. For each question, provide 4 options (A, B, C, D) and clearly state the correct answer on a new line, like 'Answer: A'."
                val response = generativeModel.generateContent(prompt)
                val quizText = response.text

                if (quizText != null) {
                    val intent = Intent(this@MainActivity, QuizResultActivity::class.java)
                    intent.putExtra("QUIZ_DATA", quizText)
                    intent.putExtra("TIMER_MINUTES", timeInMinutes)
                    intent.putExtra("QUIZ_TOPIC", topic)
                    startActivity(intent)
                } else {
                    showError("Failed to generate quiz. Response was empty.")
                }
            } catch (e: Exception) {
                showError("Error: ${e.localizedMessage}")
                Log.e("MainActivity", "Gemini API Error", e)
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.isVisible = isLoading
        generateQuizButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
