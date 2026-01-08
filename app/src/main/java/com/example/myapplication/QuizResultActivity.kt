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
    private var quizTopic: String = "General"
    private var isShowingAnswer = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        questionTextView = findViewById(R.id.questionTextView)
        questionProgressText = findViewById(R.id.questionProgressText)
        optionsRadioGroup = findViewById(R.id.optionsRadioGroup)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        timerTextView = findViewById(R.id.timerTextView)

        val rawQuizData = intent.getStringExtra("QUIZ_DATA")
        val minutes = intent.getLongExtra("TIMER_MINUTES", 0)
        quizTopic = intent.getStringExtra("QUIZ_TOPIC") ?: "General Knowledge"

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

        // --- ENTIRE NEXT BUTTON LOGIC IS REPLACED ---
        nextButton.setOnClickListener {
            // If we are currently showing the answer, then move to the next question.
            if (isShowingAnswer) {
                currentQuestionIndex++
                if (currentQuestionIndex < questions.size - 1) {
                    showQuestion(currentQuestionIndex)
                } else {
                    finishQuizAndShowScore()
                }
            } else {
                // If we are NOT showing an answer, then check the selected answer and show it.
                val selectedId = optionsRadioGroup.checkedRadioButtonId
                if (selectedId != -1) {
                    saveCurrentAnswer() // Save the answer first
                    showCorrectAnswer() // Then show the colors
                } else {
                }
            }
        }

        // Previous button is disabled because this new feature makes it confusing.
        prevButton.isEnabled = false
    }

    // --- THIS IS A NEW FUNCTION ---
    private fun showCorrectAnswer() {
        val correctAnswerId = questions[currentQuestionIndex].correctAnswerIndex
        val selectedAnswerId = userAnswers[currentQuestionIndex]

        // This checks if the user's answer was correct and updates the score.
        if (selectedAnswerId == correctAnswerId) {
            // We can add logic to update score here if we want instant score update
        }

        // Go through each radio button to color it
        for (i in 0 until optionsRadioGroup.childCount) {
            val button = optionsRadioGroup.getChildAt(i) as RadioButton
            button.isClickable = false // Stop user from changing their answer

            // If this button is the correct answer, color it GREEN
            if (button.id == correctAnswerId) {
                button.setBackgroundColor(Color.GREEN)
                button.setTextColor(Color.WHITE) // Make text easy to read on green background
            }
            // If this button was the user's choice AND it was WRONG, color it RED
            else if (button.id == selectedAnswerId && selectedAnswerId != correctAnswerId) {
                button.setBackgroundColor(Color.RED)
                button.setTextColor(Color.WHITE) // Make text easy to read on red background
            }
        }

        isShowingAnswer = true // We are now in "showing answer" mode.
        // Change the button text to prompt the user to continue.
        nextButton.text = if (currentQuestionIndex < questions.size - 1) "Continue" else "Finish"
    }

    private fun finishQuizAndShowScore() {
        countDownTimer?.cancel()
        var score = 0
        for (i in questions.indices) {
            // Use .getOrDefault because user might not have answered all questions if timer runs out.
            if (userAnswers.getOrDefault(i, -1) == questions[i].correctAnswerIndex) {
                score++
            }
        }

        val scoreText = "$score/${questions.size}"

        // This part saves the result for the History page. We will keep it simple.
        val resultEntity = QuizResultEntity(
            topic = quizTopic,
            score = scoreText,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            // You will need to replace AppDatabase and quizDao with your actual DB classes.
            val db = AppDatabase.getDatabase(applicationContext)
            db.quizDao().insertQuizResult(resultEntity)
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Quiz Completed!")
        builder.setMessage("You scored $scoreText.\nResult saved to history!")
        builder.setPositiveButton("Return to Home") { _, _ ->
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
        builder.setCancelable(false)
        builder.show()
    }

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
        // --- ADDITIONS TO THIS FUNCTION ---
        isShowingAnswer = false // Reset the state for the new question.
        prevButton.isEnabled = false // Keep the previous button disabled.

        val q = questions[index]
        questionTextView.text = "Q${index + 1}: ${q.text}"
        questionProgressText.text = "Question ${index + 1} of ${questions.size}"
        optionsRadioGroup.removeAllViews()
        optionsRadioGroup.clearCheck()

        for ((i, option) in q.options.withIndex()) {
            val rb = RadioButton(this)
            rb.text = option
            rb.id = i
            rb.setTextColor(Color.BLACK) // Reset text color
            rb.setBackgroundColor(Color.TRANSPARENT) // Reset background color
            rb.isClickable = true // Make sure button is clickable
            optionsRadioGroup.addView(rb)
        }

        // This part of your code was good, it re-selects an answer if the user already made one.
        if (userAnswers.containsKey(index)) {
            optionsRadioGroup.check(userAnswers[index]!!)
        }

        nextButton.text = if (index == questions.size - 1) "Finish" else "Next"
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
                    val options = optionLines.map { it.substringAfter(". ").trim() } // Cleaned up options
                    val answerChar = answerLine.substringAfter(":").trim().firstOrNull()
                    val correctIndex = when (answerChar?.uppercaseChar()) {
                        'A' -> 0; 'B' -> 1; 'C' -> 2; 'D' -> 3; else -> -1
                    }

                    if (correctIndex != -1) {
                        questionsList.add(Question(questionText, options, correctIndex))
                    }
                }
            } catch (e: Exception) {
                // Catching errors to prevent crashes from bad Gemini data.
            }
        }
        return questionsList
    }
}
