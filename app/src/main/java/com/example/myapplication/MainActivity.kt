package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
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
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var topicEditText: TextInputEditText
    private lateinit var questionsCountEditText: TextInputEditText
    private lateinit var timerEditText: TextInputEditText
    private lateinit var generateQuizButton: Button
    private lateinit var selectPdfButton: Button
    private lateinit var viewHistoryButton: Button
    private lateinit var logoutButton: Button
    private lateinit var selectedPdfTextView: TextView
    private lateinit var progressBar: ProgressBar
    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(it)
            selectedPdfTextView.text = fileName
            selectedPdfTextView.visibility = View.VISIBLE

            Toast.makeText(this, "Selected: $fileName", Toast.LENGTH_SHORT).show()


        }
    }

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check if user is logged in
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (!isLoggedIn) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // initialize all the views
        topicEditText = findViewById(R.id.topicEditText)
        questionsCountEditText = findViewById(R.id.questionsCountEditText)
        timerEditText = findViewById(R.id.timerEditText)
        generateQuizButton = findViewById(R.id.generateQuizButton)
        selectPdfButton = findViewById(R.id.selectPdfButton)
        viewHistoryButton = findViewById(R.id.viewHistoryButton)
        logoutButton = findViewById(R.id.logoutButton)
        selectedPdfTextView = findViewById(R.id.selectedPdfTextView)
        progressBar = findViewById(R.id.progressBar)

        generateQuizButton.setOnClickListener {
            val topic = topicEditText.text.toString().trim()
            val numQuestionsStr = questionsCountEditText.text.toString().trim()
            val timeStr = timerEditText.text.toString().trim()

            val numQuestions = numQuestionsStr.toIntOrNull() ?: 10
            val timeInMinutes = timeStr.toLongOrNull() ?: 0

            if (topic.isNotEmpty()) {
                generateQuiz(topic, numQuestions, timeInMinutes)
            } else {
                Toast.makeText(this, "Please enter a topic", Toast.LENGTH_SHORT).show()
            }
        }

        selectPdfButton.setOnClickListener {
            pdfPickerLauncher.launch("application/pdf")
        }

        viewHistoryButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun generateQuiz(topic: String, numQuestions: Int, timeInMinutes: Long) {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val prompt = "Generate a $numQuestions-question multiple-choice quiz on '$topic'. For each question, provide 4 options (A, B, C, D) and the correct answer on a new line like 'Answer: A'."
                val response = generativeModel.generateContent(prompt)
                val quizText = response.text

                if (quizText != null) {
                    val intent = Intent(this@MainActivity,QuizResultActivity::class.java)
                    intent.putExtra("QUIZ_DATA", quizText)
                    intent.putExtra("TIMER_MINUTES", timeInMinutes)
                    intent.putExtra("QUIZ_TOPIC", topic)
                    intent.putExtra("NUM_QUESTIONS", numQuestions)
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
        selectPdfButton.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getFileName(uri: Uri): String {
        var name = "Selected File"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}
