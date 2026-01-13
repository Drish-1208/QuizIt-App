package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
class MainActivity : AppCompatActivity() {

    private lateinit var topicEditText: TextInputEditText
    private lateinit var questionsCountEditText: TextInputEditText
    private lateinit var timerEditText: TextInputEditText
    private lateinit var selectPdfButton: Button
    private lateinit var selectedPdfTextView: TextView
    private lateinit var generateQuizButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var logoutButton: Button
    private lateinit var progressBar: ProgressBar

    private var selectedPdfUri: Uri? = null

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        if (!sharedPreferences.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        topicEditText = findViewById(R.id.topicEditText)
        questionsCountEditText = findViewById(R.id.questionsCountEditText)
        timerEditText = findViewById(R.id.timerEditText)
        selectPdfButton = findViewById(R.id.selectPdfButton)
        selectedPdfTextView = findViewById(R.id.selectedPdfTextView)
        generateQuizButton = findViewById(R.id.generateQuizButton)
        viewHistoryButton = findViewById(R.id.viewHistoryButton)
        logoutButton = findViewById(R.id.logoutButton)

        progressBar = findViewById(R.id.progressBar)

        selectPdfButton.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }

        generateQuizButton.setOnClickListener {
            val topic = topicEditText.text.toString().trim()
            val numQuestionsStr = questionsCountEditText.text.toString().trim()
            val numQuestions = numQuestionsStr.toIntOrNull() ?: 5 // Default to 5

            val timerMinutesStr = timerEditText.text.toString().trim()
            val timerMinutes = timerMinutesStr.toLongOrNull() ?: 0 // Default to 0 (no timer)

            if (topic.isNotEmpty() || selectedPdfUri != null) {
                generateQuiz(topic, numQuestions, timerMinutes)
            } else {
                Toast.makeText(
                    this,
                    "Please enter a topic or select a PDF file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewHistoryButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        logoutButton.setOnClickListener {
            logoutUser()

        }
    }

    private fun generateQuiz(topic: String, numQuestions: Int, timerMinutes: Long) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val promptText = if (selectedPdfUri != null) {
                    val pdfText = readPdfText(selectedPdfUri!!)
                    """
                    Based on the following text from a PDF, generate a multiple-choice quiz with exactly $numQuestions questions.
                    PDF Content: "$pdfText"
                    
                    STRICTLY follow this format for EACH question. There must be a blank line between each question block.

                    Question 1: [The question text]
                    A. [Option A]
                    B. [Option B]
                    C. [Option C]
                    D. [Option D]
                    Answer: [Correct letter, e.g., A]
                    """.trimIndent()
                } else {
                    """
                    Generate a multiple-choice quiz with exactly $numQuestions questions about the topic: "$topic".
                    STRICTLY follow this format for EACH question. There must be a blank line between each question block.

                    Question 1: [The question text]
                    A. [Option A]
                    B. [Option B]
                    C. [Option C]
                    D. [Option D]
                    Answer: [Correct letter, e.g., A]
                    """.trimIndent()
                }

                val response = generativeModel.generateContent(promptText)
                val quizText = response.text

                if (!quizText.isNullOrBlank()) {
                    val intent = Intent(this@MainActivity, QuizResultActivity::class.java).apply {
                        putExtra("QUIZ_DATA", quizText)
                        putExtra("TIMER_MINUTES", timerMinutes)
                        putExtra("QUIZ_TOPIC", topic)
                        putExtra("NUM_QUESTIONS", numQuestions)
                    }
                    startActivity(intent)
                } else {
                    showError("Failed to generate quiz. The response was empty.")
                }

            } catch (e: Exception) {
                showError("Error: ${e.localizedMessage}")
                Log.e("MainActivity", "Gemini API Error", e)
            } finally {
                setLoading(false)
            }
        }
    }

    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            val fileName = getFileName(it)
            selectedPdfTextView.text = fileName ?: "PDF Selected"
            Toast.makeText(this, "PDF selected. Topic will be based on the file.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }
    private suspend fun readPdfText(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            try {
                PDFBoxResourceLoader.init(applicationContext)

                val inputStream = contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                inputStream?.close()

                // Limit the text to avoid sending too much data to the AI
                text.take(20000)
            } catch (e: Exception) {
                Log.e("PdfReader", "Error reading PDF text", e)
                "Error reading PDF file."
            }
        }
    }




    private fun logoutUser() {
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.isVisible = isLoading
        generateQuizButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
