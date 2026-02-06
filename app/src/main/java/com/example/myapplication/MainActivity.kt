package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // --- UI & State Variables ---
    private lateinit var questionsCountEditText: TextInputEditText
    private lateinit var timerEditText: TextInputEditText
    private lateinit var selectPdfButton: Button
    private lateinit var selectedPdfTextView: TextView
    private lateinit var generateQuizButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var ProfileButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var quickQuizButton: Button
    private lateinit var quizNameEditText: TextInputEditText

    private var selectedPdfUri: Uri? = null

    // --- Firebase, AI & NEW: Room DB ---
    private lateinit var auth: FirebaseAuth
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }
    private val quizHistoryDao by lazy {
        QuizHistoryDatabase.getDatabase(applicationContext).quizHistoryDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. INITIALIZE LIBRARIES AND FIREBASE ---
        PDFBoxResourceLoader.init(applicationContext)
        auth = Firebase.auth

        // --- 2. AUTHENTICATION CHECK ---
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // --- 3. SET UP THE UI ---
        setContentView(R.layout.activity_main)

        questionsCountEditText = findViewById(R.id.questionsCountEditText)
        timerEditText = findViewById(R.id.timerEditText)
        selectPdfButton = findViewById(R.id.selectPdfButton)
        selectedPdfTextView = findViewById(R.id.selectedPdfTextView)
        generateQuizButton = findViewById(R.id.generateQuizButton)
        viewHistoryButton = findViewById(R.id.viewHistoryButton)
        ProfileButton = findViewById(R.id.ProfileButton)
        progressBar = findViewById(R.id.progressBar)
        quickQuizButton = findViewById(R.id.quickQuizButton)
        quizNameEditText = findViewById(R.id.quizNameEditText)

        // --- 4. SET UP BUTTON LISTENERS ---
        setupListeners()
    }

    private fun setupListeners() {
        selectPdfButton.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }

        generateQuizButton.setOnClickListener {
            val numQuestionsStr = questionsCountEditText.text.toString().trim()
            val numQuestions = numQuestionsStr.toIntOrNull() ?: 5 // Default to 5

            val timerMinutesStr = timerEditText.text.toString().trim()
            val timerMinutes = timerMinutesStr.toLongOrNull() ?: 0 // Default to 0

            val currentPdfUri = selectedPdfUri
            if (currentPdfUri != null) {
                // Pass the URI directly to generateQuiz
                generateQuiz(currentPdfUri, numQuestions, timerMinutes)
            } else {
                showError("Please select a PDF file to generate a quiz")
            }
        }

        // --- NAVIGATION LISTENERS ---
        quickQuizButton.setOnClickListener {
            startActivity(Intent(this, TopicQuizActivity::class.java))
        }

        viewHistoryButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        ProfileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    // --- REFACTORED generateQuiz to use Room DB ---
    private fun generateQuiz(pdfUri: Uri, numQuestions: Int, timerMinutes: Long) {
        setLoading(true)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            showError("You must be logged in to generate a quiz.")
            setLoading(false)
            return
        }
        val userId = currentUser.uid

        lifecycleScope.launch {
            try {
                // Step 1: Read PDF content in a background thread.
                Log.d("MainActivity", "Reading PDF text...")
                val pdfText = readPdfText(pdfUri)
                if (pdfText.startsWith("Error")) {
                    showError(pdfText) // Show the specific PDF reading error
                    setLoading(false)
                    return@launch
                }
                Log.d("MainActivity", "PDF reading complete.")

                // Step 2: Generate AI content using the PDF text.
                Log.d("MainActivity", "Generating quiz content with AI...")
                val promptText = createPrompt(pdfText, numQuestions)
                val quizText = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(promptText).text
                }

                if (quizText.isNullOrBlank()) {
                    showError("Failed to generate quiz. The AI response was empty.")
                    setLoading(false)
                    return@launch
                }
                Log.d("MainActivity", "AI content received.")

                // Step 3: Determine the quiz name and create the Room entity.
                val customQuizName = quizNameEditText.text.toString().trim()
                val finalTopic = if (customQuizName.isNotEmpty()) {
                    customQuizName
                }
                else {
                    getFileName(pdfUri) ?: "Unnamed PDF Quiz"
                }

                val newQuizHistoryEntry = QuizHistory(
                    quizTopic = finalTopic,
                    correctAnswers = 0,
                    totalQuestions = numQuestions,
                    timestamp = System.currentTimeMillis(),
                    quizData = quizText,
                    timerMinutes = timerMinutes,
                    userId = userId
                )

                // Step 4: Insert into Room DB and get the new ID.
                val newQuizId = withContext(Dispatchers.IO) {
                    quizHistoryDao.insert(newQuizHistoryEntry)
                }
                Log.d("MainActivity", "Saved to Room with new ID: $newQuizId")

                // Step 5: Launch the quiz activity, passing the Room ID.
                val intent = Intent(this@MainActivity, QuizResultActivity::class.java).apply {
                    putExtra("QUIZ_DATA", quizText)
                    putExtra("TIMER_MINUTES", timerMinutes)
                    putExtra("QUIZ_HISTORY_ID", newQuizId)
                }
                startActivity(intent)

            } catch (e: Exception) {
                showError("An error occurred during quiz generation: ${e.localizedMessage}")
                Log.e("MainActivity", "Error in quiz generation coroutine", e)
            } finally {
                setLoading(false)
                Log.d("MainActivity", "Coroutine finished. Hiding loading indicator.")
            }
        }
    }


    // --- REUSABLE PROMPT CREATION FUNCTION ---
    private fun createPrompt(pdfContent: String, numQuestions: Int): String {
        // This function now simply creates the prompt string, it does not read the file.
        return """
        Based on the following text from a PDF, generate a multiple-choice quiz with exactly $numQuestions questions.
        PDF Content: "$pdfContent"
        STRICTLY follow this format for EACH question. There must be a blank line between each question block.

        Question 1: [The question text]
        A. [Option A]
        B. [Option B]
        C. [Option C]
        D. [Option D]
        Answer: [Correct letter, e.g., A]
        """.trimIndent()
    }

    // --- Helper Functions and Activity Result Launchers ---

    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            val fileName = getFileName(it)
            selectedPdfTextView.text = fileName ?: "PDF Selected"
            quizNameEditText.setText(fileName?.substringBeforeLast('.'))
            Toast.makeText(this, "PDF selected. A quiz name has been suggested.", Toast.LENGTH_LONG).show()
        }
    }

    // FIX: The getColumnIndex method can return -1 if the column doesn't exist.
    // Added a check to prevent a crash.
    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    private suspend fun readPdfText(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    PDDocument.load(inputStream).use { document ->
                        PDFTextStripper().getText(document)
                    }
                } ?: "Error: Could not open PDF file."
            } catch (e: Exception) {
                Log.e("PdfReader", "Error reading PDF text", e)
                "Error reading PDF file: ${e.message}"
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.isVisible = isLoading
        generateQuizButton.isEnabled = !isLoading
        selectPdfButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
