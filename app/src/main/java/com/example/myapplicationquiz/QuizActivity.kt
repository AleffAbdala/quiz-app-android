package com.example.myapplicationquiz

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplicationquiz.databinding.ActivityQuizBinding
import com.example.myapplicationquiz.databinding.ScoreDialogBinding
import com.example.myapplicationquiz.local.AppDatabase
import com.example.myapplicationquiz.local.ResultEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        lateinit var perguntaModelList: List<PerguntaModel>
        lateinit var time: String
        lateinit var quizModel: QuizModel
    }

    private lateinit var binding: ActivityQuizBinding
    private lateinit var database: AppDatabase

    private var currentQuestionIndex = 0
    private var selectedOption = ""
    private var score = 0

    private var countDownTimer: CountDownTimer? = null
    private var totalTimeSeconds: Long = 0L
    private var remainingSeconds: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = AppDatabase.getDatabase(this)

        val quizId = intent.getStringExtra("QUIZ_ID")
        if (quizId != null) {

            val selectedQuiz = QuizDataHolder.quizList.find { it.id == quizId }
            if (selectedQuiz != null) {
                quizModel = selectedQuiz
                perguntaModelList = selectedQuiz.perguntas
                time = selectedQuiz.time
                setupListeners()
                loadQuestions()
                startTimer()
            } else {
                Toast.makeText(this, "Não foi possível encontrar o quiz.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.apply {
            bottom0.setOnClickListener(this@QuizActivity)
            bottom1.setOnClickListener(this@QuizActivity)
            bottom2.setOnClickListener(this@QuizActivity)
            bottom3.setOnClickListener(this@QuizActivity)
            bottomnext.setOnClickListener(this@QuizActivity)
        }
    }

    private fun loadQuestions() {
        selectedOption = ""

        if (currentQuestionIndex == perguntaModelList.size) {
            finishQuiz()
            return
        }

        binding.apply {
            questionIndicatorTextview.text =
                "Pergunta ${currentQuestionIndex + 1}/${perguntaModelList.size}"

            quizProgressIndicator.progress =
                ((currentQuestionIndex + 1).toFloat() / perguntaModelList.size * 100).toInt()

            questionTextview.text =
                perguntaModelList[currentQuestionIndex].pergunta

            val buttons = listOf(bottom0, bottom1, bottom2, bottom3)
            val options = perguntaModelList[currentQuestionIndex].options

            buttons.forEachIndexed { index, button ->
                if (index < options.size) {
                    button.text = options[index]
                    button.visibility = View.VISIBLE
                } else {
                    button.visibility = View.GONE
                }
            }
        }
    }

    private fun startTimer() {
        val totalTimeInMillis =
            time.toLongOrNull()?.times(60 * 1000L) ?: 0L

        totalTimeSeconds = totalTimeInMillis / 1000L
        remainingSeconds = totalTimeSeconds

        countDownTimer = object : CountDownTimer(totalTimeInMillis, 1000L) {
            override fun onFinish() {
                remainingSeconds = 0L
                finishQuiz()
            }

            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                remainingSeconds = seconds
                val min = seconds / 60
                val rseconds = seconds % 60
                binding.timerQuestionText.text =
                    String.format("%02d:%02d", min, rseconds)
            }
        }.start()
    }

    override fun onClick(view: View?) {

        binding.apply {
            bottom0.setBackgroundColor(getColor(R.color.gray))
            bottom1.setBackgroundColor(getColor(R.color.gray))
            bottom2.setBackgroundColor(getColor(R.color.gray))
            bottom3.setBackgroundColor(getColor(R.color.gray))
        }

        val clickedBtn = view as? Button ?: return

        if (clickedBtn.id == R.id.bottomnext) {

            if (selectedOption.isNotEmpty() &&
                selectedOption ==
                perguntaModelList[currentQuestionIndex].correctAnswer
            ) {
                score++
            }

            currentQuestionIndex++
            loadQuestions()

        } else {
            selectedOption = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.orange))
        }
    }

    private fun finishQuiz() {

        if (isFinishing) return
        countDownTimer?.cancel()

        val totalQuestions = perguntaModelList.size
        val percentage =
            if (totalQuestions > 0)
                ((score.toFloat() / totalQuestions.toFloat()) * 100).toInt()
            else 0

        val elapsedSeconds = (totalTimeSeconds - remainingSeconds).coerceAtLeast(0L)

        val resultId = saveResultToFirebase(elapsedSeconds)
        saveResultToRoom(resultId, elapsedSeconds)

        val dialogBinding =
            ScoreDialogBinding.inflate(layoutInflater)

        dialogBinding.apply {
            scoreProgressIndicator.progress = percentage
            scoreProgressText.text = "$percentage%"

            if (percentage > 60) {
                scoreTitle.text = "Parabéns!! Você passou"
                scoreTitle.setTextColor(getColor(R.color.blue))
            } else {
                scoreTitle.text = "Infelizmente você não passou"
                scoreTitle.setTextColor(getColor(R.color.red))
            }

            scoreSubtext.text =
                "Você acertou $score de $totalQuestions perguntas em ${elapsedSeconds}s"

            fimButton.setOnClickListener {
                val intent =
                    Intent(this@QuizActivity, MainActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .show()
    }

    private fun saveResultToFirebase(elapsedSeconds: Long): String {
        val currentUser =
            FirebaseAuth.getInstance().currentUser ?: return ""
        val resultsRef = FirebaseDatabase.getInstance().reference
            .child("Results")
            .child(currentUser.uid)
        val resultId = resultsRef.push().key ?: return ""

        val result = QuizResultModel(
            resultId = resultId,
            userId = currentUser.uid,
            quizId = quizModel.id,
            title = quizModel.title,
            score = score,
            totalQuestions = perguntaModelList.size,
            elapsedSeconds = elapsedSeconds,
            timestamp = System.currentTimeMillis()
        )

        resultsRef
            .child(resultId)
            .setValue(result)
        return resultId
    }

    private fun saveResultToRoom(resultId: String, elapsedSeconds: Long) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        if (resultId.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {

            val resultEntity = ResultEntity(
                id = resultId,
                userId = currentUser.uid,
                quizId = quizModel.id,
                title = quizModel.title,
                score = score,
                totalQuestions = perguntaModelList.size,
                elapsedSeconds = elapsedSeconds,
                timestamp = System.currentTimeMillis()
            )

            database.resultDao().insert(resultEntity)
        }
    }
}
