package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

// This data class can remain the same
data class QuizQuestion(
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String
)

class QuizResultActivity : AppCompatActivity() {

    // --- UI and State Variables (No changes here) ---
    private lateinit var questionNumberTextView: TextView
    private lateinit var questionTextView: TextView
    private lateinit var optionsRadioGroup: RadioGroup
    private lateinit var optionA: RadioButton
    private lateinit var optionB: RadioButton
    private lateinit var optionC: RadioButton
    private lateinit var optionD: RadioButton
    private lateinit var feedbackTextView: TextView
    private lateinit var nextButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var quizProgressBar: ProgressBar
    private var questions: List<QuizQuestion> = listOf()
    private var currentQuestionIndex = 0
    private var score = 0
    private var countDownTimer: CountDownTimer? = null

    // --- 1. NEW: ROOM DATABASE VARIABLES ---
    // The ID for the Room database entry. Default to -1 to indicate it's not set.
    private var quizHistoryId: Long = -1L
    private val quizHistoryDao by lazy {
        QuizHistoryDatabase.getDatabase(applicationContext).quizHistoryDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        // No more Firebase initialization
        initializeViews()

        // --- 2. GET DATA FROM INTENT (Updated for Room) ---
        val quizText = intent.getStringExtra("QUIZ_DATA")
        // Get the Long ID for Room. Provide a default value of -1.
        quizHistoryId = intent.getLongExtra("QUIZ_HISTORY_ID", -1L)
        val timerMinutes = intent.getLongExtra("TIMER_MINUTES", 0L)

        // Check if the ID is valid. A value of -1 means it wasn't passed correctly.
        if (quizText.isNullOrBlank() || quizHistoryId == -1L) {
            Toast.makeText(this, "Failed to load quiz data or history ID.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        questions = parseQuizData(quizText)
        if (questions.isEmpty()) {
            Toast.makeText(this, "Could not parse quiz questions.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (timerMinutes > 0) {
            startTimer(timerMinutes)
        } else {
            timerTextView.isVisible = false
        }

        displayQuestion()

        nextButton.setOnClickListener {
            loadNextQuestion()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    // --- 3. NEW: Function to update the score in the Room database ---
    private fun updateQuizScoreInRoom(finalScore: Int) {
        // Check if we have a valid ID.
        if (quizHistoryId == -1L) {
            Log.e("QuizResultActivity", "Cannot update score: History ID is invalid.")
            return
        }

        // Use a coroutine to perform the database operation on a background thread.
        lifecycleScope.launch {
            try {
                quizHistoryDao.updateScore(quizHistoryId, finalScore)
                Log.d("QuizResultActivity", "Quiz score updated successfully in Room DB!")
            } catch (e: Exception) {
                Log.e("QuizResultActivity", "Failed to update score in Room DB", e)
            }
        }
    }

    private fun finishQuiz() {
        countDownTimer?.cancel()

        // --- 4. CALL THE NEW ROOM UPDATE FUNCTION ---
        updateQuizScoreInRoom(score)

        Toast.makeText(this, "Quiz finished! Your score: $score/${questions.size}", Toast.LENGTH_LONG).show()
        finish() // Go back to the main screen
    }

    // --- All your other functions (initializeViews, displayQuestion, parseQuizData, etc.) remain exactly the same ---
    // --- No changes are needed in the code below this point. ---

    private fun initializeViews() {
        questionNumberTextView = findViewById(R.id.questionNumberTextView)
        questionTextView = findViewById(R.id.questionTextView)
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup)
        optionA = findViewById(R.id.optionA)
        optionB = findViewById(R.id.optionB)
        optionC = findViewById(R.id.optionC)
        optionD = findViewById(R.id.optionD)
        feedbackTextView = findViewById(R.id.feedbackTextView)
        nextButton = findViewById(R.id.nextButton)
        timerTextView = findViewById(R.id.timerTextView)
        quizProgressBar = findViewById(R.id.quizProgressBar)
    }

    private fun startTimer(minutes: Long) {
        val millisInFuture = minutes * 60 * 1000
        countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutesLeft = (millisUntilFinished / 1000) / 60
                val secondsLeft = (millisUntilFinished / 1000) % 60
                timerTextView.text = String.format("%02d:%02d", minutesLeft, secondsLeft)
            }

            override fun onFinish() {
                timerTextView.text = "00:00"
                Toast.makeText(this@QuizResultActivity, "Time's up!", Toast.LENGTH_LONG).show()
                finishQuiz()
            }
        }.start()
    }

    private fun displayQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            quizProgressBar.max = questions.size
            quizProgressBar.progress = currentQuestionIndex + 1
            quizProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))

            feedbackTextView.isVisible = false
            nextButton.isVisible = false
            optionsRadioGroup.clearCheck()
            setOptionsEnabled(true)
            resetOptionColors()

            questionNumberTextView.text = "Question ${currentQuestionIndex + 1}/${questions.size}"
            questionTextView.text = question.question
            optionA.text = question.optionA
            optionB.text = question.optionB
            optionC.text = question.optionC
            optionD.text = question.optionD

            optionsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                if (checkedId != -1) {
                    checkAnswer()
                    optionsRadioGroup.setOnCheckedChangeListener(null)
                }
            }
        }
    }

    private fun checkAnswer() {
        val selectedRadioButtonId = optionsRadioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) return

        val selectedOption = when (selectedRadioButtonId) {
            R.id.optionA -> "A"
            R.id.optionB -> "B"
            R.id.optionC -> "C"
            R.id.optionD -> "D"
            else -> ""
        }

        val correctAnswer = questions[currentQuestionIndex].correctAnswer.first().toString().uppercase()
        setOptionsEnabled(false)
        feedbackTextView.isVisible = true

        if (selectedOption == correctAnswer) {
            score++
            feedbackTextView.text = "Correct!"
            feedbackTextView.setTextColor(Color.parseColor("#008000"))
        } else {
            feedbackTextView.text = "Wrong! The correct answer is $correctAnswer."
            feedbackTextView.setTextColor(Color.RED)
        }
        highlightCorrectAnswer()

        nextButton.text = if (currentQuestionIndex < questions.size - 1) "Next Question" else "Finish Quiz"
        nextButton.isVisible = true
    }

    private fun highlightCorrectAnswer() {
        val correctAnswer = questions[currentQuestionIndex].correctAnswer.first().toString().uppercase()
        val correctRadioButton = when (correctAnswer) {
            "A" -> optionA
            "B" -> optionB
            "C" -> optionC
            "D" -> optionD
            else -> null
        }
        correctRadioButton?.setTextColor(Color.parseColor("#008000"))
    }

    private fun resetOptionColors() {
        optionA.setTextColor(Color.BLACK)
        optionB.setTextColor(Color.BLACK)
        optionC.setTextColor(Color.BLACK)
        optionD.setTextColor(Color.BLACK)
    }

    private fun setOptionsEnabled(enabled: Boolean) {
        for (i in 0 until optionsRadioGroup.childCount) {
            optionsRadioGroup.getChildAt(i).isEnabled = enabled
        }
    }

    private fun loadNextQuestion() {
        currentQuestionIndex++
        if (currentQuestionIndex < questions.size) {
            displayQuestion()
        } else {
            finishQuiz()
        }
    }

    private fun parseQuizData(quizText: String): List<QuizQuestion> {
        val questionsList = mutableListOf<QuizQuestion>()
        try {
            val questionBlocks = quizText.trim().split(Regex("\n\n+"))
            for (block in questionBlocks) {
                val lines = block.lines().filter { it.isNotBlank() }
                if (lines.size >= 6) {
                    val question = lines[0].substringAfter(":")
                    val optionA = lines[1]
                    val optionB = lines[2]
                    val optionC = lines[3]
                    val optionD = lines[4]
                    val answer = lines[5].substringAfter(":").trim()
                    questionsList.add(QuizQuestion(question, optionA, optionB, optionC, optionD, answer))
                }
            }
        } catch (e: Exception) {
            Log.e("QuizResultActivity", "Failed to parse quiz data", e)
        }
        return questionsList
    }
}
