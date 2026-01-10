package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class QuizResultActivity : AppCompatActivity() {

    private lateinit var questionTextView: TextView
    private lateinit var questionProgressText: TextView
    private lateinit var optionsRadioGroup: RadioGroup
    private lateinit var nextButton: Button
    private lateinit var timerTextView: TextView

    private var questions: List<Question> = emptyList()
    private var totalQuestionsRequested = 0
    private var currentQuestionIndex = 0
    private val userAnswers = mutableMapOf<Int, Int>()
    private var countDownTimer: CountDownTimer? = null
    private var quizTopic: String = "General"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        questionTextView = findViewById(R.id.questionTextView)
        questionProgressText = findViewById(R.id.questionProgressText)
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup)
        nextButton = findViewById(R.id.nextButton)
        timerTextView = findViewById(R.id.timerTextView)

        val rawQuizData = intent.getStringExtra("QUIZ_DATA")
        val minutes = intent.getLongExtra("TIMER_MINUTES", 0)
        quizTopic = intent.getStringExtra("QUIZ_TOPIC") ?: "General Knowledge"
        totalQuestionsRequested = intent.getIntExtra("NUM_QUESTIONS", 10)

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
                showParsingError(rawQuizData)
            }
        }

        nextButton.setOnClickListener {
            handleNextButtonClick()
        }
    }

    private fun handleNextButtonClick() {
        // Save the users answer for the current question
        saveCurrentAnswer()

        // Move to the next question
        currentQuestionIndex++
        if (currentQuestionIndex < questions.size) {
            showQuestion(currentQuestionIndex)
        } else {
            // If it's the end of the quiz, finish and show the score
            finishQuizAndShowScore()
        }
    }

    private fun showQuestion(index: Int) {
        if (index >= questions.size) return
        val q = questions[index]

        questionTextView.text = q.text
        questionProgressText.text = "Question ${index + 1} of ${totalQuestionsRequested}"
        optionsRadioGroup.removeAllViews()
        optionsRadioGroup.clearCheck()

        for ((i, option) in q.options.withIndex()) {
            val rb = RadioButton(this)
            rb.text = option
            rb.id = i
            rb.setTextColor(Color.BLACK) // Ensure text is readable
            optionsRadioGroup.addView(rb)
        }

        // If the user has already answered this question, check the saved answer
        if (userAnswers.containsKey(index)) {
            optionsRadioGroup.check(userAnswers[index]!!)
        }

        // Update button text on the last question
        nextButton.text = if (index == questions.size - 1) "Finish" else "Next"
    }

    private fun saveCurrentAnswer() {
        val selectedId = optionsRadioGroup.checkedRadioButtonId
        if (selectedId != -1) {
            userAnswers[currentQuestionIndex] = selectedId
        }
    }

    private fun finishQuizAndShowScore() {
        countDownTimer?.cancel()
        var score = 0
        for (i in questions.indices) {
            if (userAnswers.getOrDefault(i, -1) == questions[i].correctAnswerIndex) {
                score++
            }
        }
        val scoreText = "$score/${totalQuestionsRequested}"

        // Save score to database
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val resultEntity = QuizResultEntity(
                    topic = quizTopic,
                    score = scoreText,
                    timestamp = System.currentTimeMillis()
                )
                db.quizDao().insertQuizResult(resultEntity)
            } catch (e: Exception) {
                Log.e("QuizResultActivity", "Failed to save score to database", e)
            }
        }

        // Show final score dialog
        AlertDialog.Builder(this)
            .setTitle("Quiz Completed!")
            .setMessage("You scored $scoreText.\nResult saved to history!")
            .setPositiveButton("Return to Home") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun startTimer(minutes: Long) {
        val timeInMillis = minutes * 60 * 1000
        countDownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = millisUntilFinished / 1000
                val min = remainingSeconds / 60
                val sec = remainingSeconds % 60
                timerTextView.text = String.format("Time: %02d:%02d", min, sec)
                if (remainingSeconds < 20) timerTextView.setTextColor(Color.RED)
            }

            override fun onFinish() {
                timerTextView.text = "Time's Up!"
                saveCurrentAnswer() // Save any selected answer before finishing
                finishQuizAndShowScore()
            }
        }.start()
    }

    private fun parseQuizData(data: String): List<Question> {
        val questionsList = mutableListOf<Question>()
        try {
            val questionBlocks = data.split(Regex("(?i)Question \\d+:"))
                .filter { it.isNotBlank() }

            for (block in questionBlocks) {
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty()) continue

                val questionText = lines[0]
                val options = lines.filter { it.matches(Regex("^[A-D][.)].*")) }
                    .map { it.substring(2).trim() }

                val answerLine = lines.firstOrNull { it.lowercase().startsWith("answer:") }
                if (options.size >= 4 && answerLine != null) {
                    val answerChar = answerLine.substringAfter(":").trim().firstOrNull()
                    val correctIndex = when (answerChar?.uppercaseChar()) {
                        'A' -> 0
                        'B' -> 1
                        'C' -> 2
                        'D' -> 3
                        else -> -1
                    }
                    if (correctIndex != -1) {
                        questionsList.add(Question(questionText, options.take(4), correctIndex))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ParseQuizData", "Exception during parsing", e)
            return emptyList()
        }
        return questionsList
    }

    private fun showParsingError(rawData: String) {
        Log.e("QuizResultActivity", "Parsing failed. Raw data: $rawData")
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Could not load questions from the AI's response. The format was unexpected.")
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
