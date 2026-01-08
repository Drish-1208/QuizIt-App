package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
// Import lifecycleScope and coroutines for database operations
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class QuizResultActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var questionProgressText: TextView
    private lateinit var optionsRadioGroup: RadioGroup
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var timerTextView: TextView

    private var questions: List<Question> = emptyList()
    private var currentQuestionIndex = 0
    private val userAnswers = mutableMapOf<Int, Int>()
    private var countDownTimer: CountDownTimer? = null

    // Variable to hold the quiz topic
    private var quizTopic: String = "General"

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        // Initialize Views
        questionTextView = findViewById(R.id.questionTextView)
        questionProgressText = findViewById(R.id.questionProgressText)
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        timerTextView = findViewById(R.id.timerTextView)

        val rawQuizData = intent.getStringExtra("QUIZ_DATA")
        val minutes = intent.getLongExtra("TIMER_MINUTES", 0)
        // Get the topic from the Intent
        quizTopic = intent.getStringExtra("QUIZ_TOPIC") ?: "General Knowledge"


        // Timer Logic
        if (minutes > 0) {
            startTimer(minutes)
        } else {
            timerTextView.text = "Time: Unlimited"
        }

        if (rawQuizData != null) {
            questions = parseQuizData(rawQuizData)
            if (questions.isNotEmpty()) {
                showQuestion(0)
            } else {
                questionTextView.text = "Error: Could not load questions."
            }
        }

        // --- BUTTON LISTENERS ---
        nextButton.setOnClickListener {
            saveCurrentAnswer()
            if (currentQuestionIndex < questions.size - 1) {
                currentQuestionIndex++
                showQuestion(currentQuestionIndex)
            } else {
                finishQuizAndShowScore()
            }
        }

        prevButton.setOnClickListener {
            saveCurrentAnswer()
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                showQuestion(currentQuestionIndex)
            }
        }
    }

    private fun finishQuizAndShowScore() {
        countDownTimer?.cancel()
        var score = 0
        for (i in questions.indices) {
            if (userAnswers[i] == questions[i].correctAnswerIndex) {
                score++
            }
        }

        // --- THIS IS THE NEW CODE TO SAVE TO THE DATABASE ---
        val scoreText = "$score/${questions.size}"
        val resultEntity = QuizResultEntity(
            topic = quizTopic,
            score = scoreText,
            timestamp = System.currentTimeMillis()
        )

        // Use a coroutine to insert data in the background
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.quizDao().insertQuizResult(resultEntity)
        }
        // --- END OF NEW CODE ---


        // Show Result Dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quiz Completed!")
        // Update the message to confirm saving
        builder.setMessage("You scored $scoreText.\nResult saved to history!")
        builder.setPositiveButton("Return to Home") { _, _ ->
            // Use an Intent to go to MainActivity cleanly
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish() // Close this activity
        }
        builder.setCancelable(false)
        builder.show()
    }

    // (The rest of your file: onDestroy, showQuestion, saveCurrentAnswer, parseQuizData, etc. remains the same)
    // Make sure to copy the rest of your functions below this line if you are copy-pasting selectively.
    // For simplicity, just replace the whole file content.

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun startTimer(minutes: Long) {
        val timeInMillis = minutes * 60 * 1000
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                timerTextView.text = String.format("Time: %02d:%02d", min, sec)
                if (remainingSeconds < 10) timerTextView.setTextColor(Color.RED)
            }

            override fun onFinish() {
                timerTextView.text = "Time's Up!"
                finishQuizAndShowScore()
            }
        }.start()
    }

    private fun showQuestion(index: Int) {
        val q = questions[index]
        questionTextView.text = "Q${index + 1}: ${q.text}"
        questionProgressText.text = "Question ${index + 1} of ${questions.size}"
        optionsRadioGroup.removeAllViews()
        optionsRadioGroup.clearCheck()

        for ((i, option) in q.options.withIndex()) {
            val rb = RadioButton(this)
            rb.text = option
            rb.id = i
            optionsRadioGroup.addView(rb)
        }

        if (userAnswers.containsKey(index)) {
            optionsRadioGroup.check(userAnswers[index]!!)
        }

        prevButton.isEnabled = index > 0
        nextButton.text = if (index == questions.size - 1) "Finish Quiz" else "Next"
    }

    private fun saveCurrentAnswer() {
        val selectedId = optionsRadioGroup.checkedRadioButtonId
        if (selectedId != -1) {
            userAnswers[currentQuestionIndex] = selectedId
        }
    }

    private fun parseQuizData(data: String): List<Question> {
        val questionsList = mutableListOf<Question>()
        val rawQuestions = data.split(Regex("(?=Q\\d+:)"))
            .filter { it.trim().startsWith("Q") }

        for (rawQ in rawQuestions) {
            try {
                val lines = rawQ.lines().filter { it.isNotBlank() }
                val questionTextWithPrefix = lines.firstOrNull()?.trim() ?: continue
                val questionText = questionTextWithPrefix.substringAfter(":").trim()
                val optionLines = lines.filter { it.matches(Regex("^[A-D]\\..*")) }
                val answerLine = lines.firstOrNull { it.startsWith("Answer:") }

                if (optionLines.size == 4 && answerLine != null) {
                    val options = optionLines.map { it.trim() }
                    val answerChar = answerLine.substringAfter(":").trim().firstOrNull()
                    val correctIndex = when (answerChar?.uppercaseChar()) {
                        'A' -> 0; 'B' -> 1; 'C' -> 2; 'D' -> 3; else -> -1
                    }

                    if (correctIndex != -1) {
                        questionsList.add(Question(questionText, options, correctIndex))
                    }
                }
            } catch (e: Exception) {
                // Ignore malformed questions
            }
        }
        return questionsList
    }

}
