package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.auth.ktx.auth
// REMOVED: No longer need Firebase Database for this activity
// import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TopicQuizActivity : AppCompatActivity() {

    // --- Views ---
    private lateinit var topicEditText: EditText
    private lateinit var questionsCountEditText: EditText
    private lateinit var timerEditText: EditText
    private lateinit var generateTopicQuizButton: Button
    private lateinit var pdfQuizNavButton: Button
    private lateinit var quickQuizNavButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var profileNavButton: Button
    private lateinit var progressBar: ProgressBar

    // --- AI Service ---
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // --- Firebase Auth (Still needed for User ID) ---
    private val auth = Firebase.auth

    // --- 1. NEW: Get a reference to the Room Database DAO ---
    private val quizHistoryDao by lazy {
        QuizHistoryDatabase.getDatabase(applicationContext).quizHistoryDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_quiz)

        // Initialize views
        topicEditText = findViewById(R.id.topicEditText)
        questionsCountEditText = findViewById(R.id.questionsCountEditText)
        timerEditText = findViewById(R.id.timerEditText)
        generateTopicQuizButton = findViewById(R.id.generateTopicQuizButton)
        pdfQuizNavButton = findViewById(R.id.pdfQuizNavButton)
        quickQuizNavButton = findViewById(R.id.quickQuizNavButton)
        viewHistoryButton = findViewById(R.id.viewHistoryButton)
        profileNavButton = findViewById(R.id.ProfileNavButton)
        progressBar = findViewById(R.id.progressBar)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        generateTopicQuizButton.setOnClickListener {
            val topic = topicEditText.text.toString().trim()
            val numQuestionsStr = questionsCountEditText.text.toString().trim()
            val timerMinutesStr = timerEditText.text.toString().trim()

            if (topic.isEmpty()) {
                Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val numQuestions = numQuestionsStr.toIntOrNull() ?: 5 // Default to 5 questions
            val timerMinutes = timerMinutesStr.toLongOrNull() ?: 0 // Default to no timer

            generateQuizFromTopic(topic, numQuestions, timerMinutes)
        }

        // Navigation listeners
        pdfQuizNavButton.setOnClickListener { finish() }
        quickQuizNavButton.isEnabled = false
        viewHistoryButton.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        profileNavButton.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
    }

    // --- 2. REWRITTEN: The core quiz generation logic ---
    // In TopicQuizActivity.kt

    private fun generateQuizFromTopic(topic: String, numQuestions: Int, timerMinutes: Long) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to start a quiz.", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid

        showLoading(true)
        Log.d("TopicQuizActivity", "Starting quiz generation for topic: $topic")

        lifecycleScope.launch {
            try {
                // Step 1: Generate the quiz content using the AI.
                val prompt = createTopicPrompt(topic, numQuestions)
                val quizText = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(prompt).text
                }

                // Step 2: Check if AI generation was successful.
                if (quizText.isNullOrBlank()) {
                    showError("Failed to generate quiz. The AI response was empty.")
                    showLoading(false) // Make sure to stop loading
                    return@launch
                }
                Log.d("TopicQuizActivity", "AI content received.")

                // Step 3: Create the complete QuizHistory object.
                val newQuizHistoryEntry = QuizHistory(
                    quizTopic = topic,
                    correctAnswers = 0,
                    totalQuestions = numQuestions,
                    timestamp = System.currentTimeMillis(),
                    quizData = quizText, // <-- Save the real quiz data
                    timerMinutes = timerMinutes,
                    userId = userId
                )

                // Step 4: Insert the complete object into Room DB and get the new ID.
                val newQuizId = withContext(Dispatchers.IO) {
                    quizHistoryDao.insert(newQuizHistoryEntry)
                }
                Log.d("TopicQuizActivity", "Saved to Room with new ID: $newQuizId")

                // Step 5: Launch the quiz activity with the new ID.
                val intent = Intent(this@TopicQuizActivity, QuizResultActivity::class.java).apply {
                    putExtra("QUIZ_DATA", quizText)
                    putExtra("TIMER_MINUTES", timerMinutes)
                    putExtra("QUIZ_HISTORY_ID", newQuizId) // Pass the ID of the complete record
                }
                startActivity(intent)

            } catch (e: Exception) {
                Log.e("TopicQuizActivity", "Error during quiz generation", e)
                showError("An error occurred: ${e.message}")
            } finally {
                // Ensure loading is always turned off
                showLoading(false)
            }
        }
    }

// You can now DELETE the createQuizHistoryInRoom() helper function. It is no longer needed.
// You can also DELETE the deleteById() function from your QuizHistoryDao.


    // --- 3. REWRITTEN: Helper function to use Room instead of Firebase ---


    // --- (The rest of your functions: createTopicPrompt, showLoading, showError remain the same) ---
    // In TopicQuizActivity.kt

    private fun createTopicPrompt(topic: String, numQuestions: Int): String {
        return """
    Based on the topic "$topic", generate a multiple-choice quiz with exactly $numQuestions questions.

    STRICTLY follow this format for EACH question. There must be a blank line between each question block.

    Question 1: [The question text]
    A. [Option A]
    B. [Option B]
    C. [Option C]
    D. [Option D]
    Answer: [Correct letter, e.g., A]
    """.trimIndent()
    }


    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        generateTopicQuizButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
