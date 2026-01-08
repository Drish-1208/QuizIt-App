package com.example.myapplication

import android.annotation.SuppressLint
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
    private lateinit var prevButton: Button
    private lateinit var timerTextView: TextView

    private var questions: List<Question> = emptyList()
    private var totalQuestionsRequested = 0
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
                questionTextView.text = "Error: Could not load questions from the response."
                Log.e("QuizResultActivity", "Parsing failed. Raw data: $rawQuizData")
            }
        }

        nextButton.setOnClickListener {
            handleNextButtonClick()
        }

        prevButton.setOnClickListener {
        }
        prevButton.isEnabled = false
    }

    private fun handleNextButtonClick() {
        if (isShowingAnswer) {
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                showQuestion(currentQuestionIndex)
            } else {
                finishQuizAndShowScore()
            }
        } else {
            val selectedId = optionsRadioGroup.checkedRadioButtonId
            if (selectedId != -1) {
                saveCurrentAnswer()
                showCorrectAnswer()
            }
        }
    }

    private fun showQuestion(index: Int) {
        if (index >= questions.size) return

        isShowingAnswer = false
        val q = questions[index]

        questionTextView.text = q.text
        questionProgressText.text = "Question ${index + 1} of ${totalQuestionsRequested}"
        optionsRadioGroup.removeAllViews()
        optionsRadioGroup.clearCheck()

        for ((i, option) in q.options.withIndex()) {
            val rb = RadioButton(this)
            rb.text = option
            rb.id = i
            rb.setTextColor(Color.BLACK)
            rb.setBackgroundColor(Color.TRANSPARENT)
            rb.isClickable = true
            optionsRadioGroup.addView(rb)
        }

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

    private fun showCorrectAnswer() {
        isShowingAnswer = true
        val correctAnswerId = questions[currentQuestionIndex].correctAnswerIndex
        val selectedAnswerId = userAnswers[currentQuestionIndex]

        for (i in 0 until optionsRadioGroup.childCount) {
            val button = optionsRadioGroup.getChildAt(i) as RadioButton
            button.isClickable = false

            if (button.id == correctAnswerId) {
                button.setBackgroundColor(Color.GREEN)
                button.setTextColor(Color.WHITE)
            } else if (button.id == selectedAnswerId && selectedAnswerId != correctAnswerId) {
                button.setBackgroundColor(Color.RED)
                button.setTextColor(Color.WHITE)
            }
        }

        nextButton.text = if (currentQuestionIndex < questions.size - 1) "Continue" else "Finish"
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

        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val resultEntity = QuizResultEntity(topic = quizTopic, score = scoreText, timestamp = System.currentTimeMillis())
                db.quizDao().insertQuizResult(resultEntity)
            } catch (e: Exception) {
                Log.e("QuizResultActivity", "Failed to save score to database", e)
            }
        }

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
                finishQuizAndShowScore()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun parseQuizData(data: String): List<Question> {
        val questionsList = mutableListOf<Question>()
        val questionBlockRegex = Regex("(?ism)(Question \\d{1,2}:.*?)(?=Question \\d{1,2}:|$)")

        val matches = questionBlockRegex.findAll(data)

        for (match in matches) {
            val block = match.value
            try {
                val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty()) continue

                val questionText = lines.first().replace(Regex("(?i)Question \\d{1,2}:"), "").trim()
                val optionLines = lines.filter { it.matches(Regex("^[a-dA-D][.)].*")) }
                val answerLine = lines.firstOrNull { it.matches(Regex("(?i)^Answer:?\\s*[a-dA-D].*")) }

                if (optionLines.size == 4 && answerLine != null) {
                    val options = optionLines.map { it.substring(2).trim() }
                    val answerChar = answerLine.find { it.isLetter() }
                    val correctIndex = when (answerChar?.uppercaseChar()) {
                        'A' -> 0; 'B' -> 1; 'C' -> 2; 'D' -> 3
                        else -> -1
                    }

                    if (correctIndex != -1) {
                        questionsList.add(Question(questionText, options, correctIndex))
                    }
                }
            } catch (e: Exception) {
                Log.e("ParseQuizData", "Failed to parse a question block: $block", e)
            }
        }
        return questionsList
    }
}
