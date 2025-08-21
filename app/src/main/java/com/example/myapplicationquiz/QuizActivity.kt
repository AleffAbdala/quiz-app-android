package com.example.myapplicationquiz

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplicationquiz.databinding.ActivityQuizBinding
import com.example.myapplicationquiz.databinding.ScoreDialogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class QuizActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        lateinit var perguntaModelList: List<PerguntaModel>
        lateinit var time: String
        lateinit var quizModel: QuizModel
    }

    lateinit var binding: ActivityQuizBinding

    var currentQuestionIndex = 0
    var selectedOption = ""
    var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val quizId = intent.getStringExtra("QUIZ_ID")
        if (quizId != null) {

            val selectedQuiz = QuizDataHolder.quizList.find { it.id == quizId }
            if (selectedQuiz != null) {
                quizModel = selectedQuiz
                perguntaModelList = selectedQuiz.perguntas
                time = selectedQuiz.time
                setupListeners()
                loadQuestions()
                starTimer()
            } else {
                Toast.makeText(this, "Não foi possível encontrar o quiz.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
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
            questionIndicatorTextview.text = "Pergunta ${currentQuestionIndex + 1}/${perguntaModelList.size}"
            quizProgressIndicator.progress = ((currentQuestionIndex + 1).toFloat() / perguntaModelList.size * 100).toInt()
            questionTextview.text = perguntaModelList[currentQuestionIndex].pergunta

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

    private fun starTimer() {
        val totalTimeInMillis = time.toLongOrNull()?.times(60 * 1000L) ?: 0L
        object : CountDownTimer(totalTimeInMillis, 1000L) {
            override fun onFinish() {
                finishQuiz()
            }

            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val min = seconds / 60
                val rseconds = seconds % 60
                binding.timerQuestionText.text = String.format("%02d:%02d", min, rseconds)
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

        val clickedBtn = view as Button
        if (clickedBtn.id == R.id.bottomnext) {
            if (selectedOption.isNotEmpty() && selectedOption == perguntaModelList[currentQuestionIndex].correctAnswer) {
                score++
            }
            currentQuestionIndex++
            loadQuestions()
        } else {
            selectedOption = clickedBtn.text.toString()
            clickedBtn.setBackgroundColor(getColor(R.color.orange))
        }
    }

    /**GEMINI Inicio
     * me ajude a finalizar o quiz e salvar o resultado no firebase
     **/
    private fun finishQuiz() {

        if (isFinishing) return

        val totalQuestions = perguntaModelList.size
        val percentage = if (totalQuestions > 0) ((score.toFloat() / totalQuestions.toFloat()) * 100).toInt() else 0

        saveResultToFirebase()

        val dialogBinding = ScoreDialogBinding.inflate(layoutInflater)
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
            scoreSubtext.text = "Você acertou $score de $totalQuestions perguntas"
            fimButton.setOnClickListener {
                val intent = Intent(this@QuizActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        /***gemini fim*/

        if (!isFinishing) {
            AlertDialog.Builder(this)
                .setView(dialogBinding.root)
                .setCancelable(false)
                .show()
        }
    }

    private fun saveResultToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val result = QuizResultModel(
            quizId = quizModel.id,
            title = quizModel.title,
            score = score,
            totalQuestions = perguntaModelList.size,
            timestamp = System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().reference
            .child("Results")
            .child(currentUser.uid)
            .push()
            .setValue(result)
    }
}