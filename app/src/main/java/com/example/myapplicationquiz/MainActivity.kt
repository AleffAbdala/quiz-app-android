package com.example.myapplicationquiz

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplicationquiz.databinding.ActivityMainBinding
import com.example.myapplicationquiz.local.AppDatabase
import com.example.myapplicationquiz.local.QuestionEntity
import com.example.myapplicationquiz.local.QuizEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val quizList: MutableList<QuizModel> = mutableListOf()
    private lateinit var adapter: QuizListAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AppDatabase
    private val quizzesRef by lazy { FirebaseDatabase.getInstance().reference.child("Quizzes") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = AppDatabase.getDatabase(this)

        binding.welcomeTextView.text = "Olá, ${auth.currentUser?.email ?: "usuário"}"

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.createQuizButton.setOnClickListener {
            showCreateQuizDialog()
        }

        setupRecyclerView()
        syncQuizzesFromFirebase()
        loadDashboard()
        loadRanking()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            goToLogin()
        }
    }

    private fun setupRecyclerView() {
        adapter = QuizListAdapter(
            quizList = quizList,
            onEditQuiz = { quiz -> showEditQuizDialog(quiz) },
            onDeleteQuiz = { quiz -> confirmDeleteQuiz(quiz) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun syncQuizzesFromFirebase() {
        // Codigo gerado com suporte de IA: baixa quizzes do Firebase e atualiza cache local para modo offline.
        FirebaseDatabase.getInstance().reference.child("Quizzes")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                if (!dataSnapshot.exists()) {
                    loadQuizzesFromLocal()
                    return@addOnSuccessListener
                }

                val remoteQuizzes = mutableListOf<QuizModel>()
                for (data in dataSnapshot.children) {
                    val quizModel = data.getValue(QuizModel::class.java)
                    if (quizModel != null) {
                        remoteQuizzes.add(quizModel)
                    }
                }

                if (remoteQuizzes.isEmpty()) {
                    loadQuizzesFromLocal()
                    return@addOnSuccessListener
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    val quizEntities = mutableListOf<QuizEntity>()
                    val questionEntities = mutableListOf<QuestionEntity>()
                    val now = System.currentTimeMillis()

                    remoteQuizzes.forEach { quiz ->
                        quizEntities.add(
                            QuizEntity(
                                id = quiz.id,
                                title = quiz.title,
                                subtitle = quiz.subtitle,
                                time = quiz.time,
                                updatedAt = now
                            )
                        )

                        quiz.perguntas.forEach { q ->
                            val options = q.options
                            questionEntities.add(
                                QuestionEntity(
                                    quizId = quiz.id,
                                    pergunta = q.pergunta,
                                    optionA = options.getOrElse(0) { "" },
                                    optionB = options.getOrElse(1) { "" },
                                    optionC = options.getOrElse(2) { "" },
                                    optionD = options.getOrElse(3) { "" },
                                    correctAnswer = q.correctAnswer
                                )
                            )
                        }
                    }

                    database.quizDao().clearQuestions()
                    database.quizDao().clearQuizzes()
                    database.quizDao().insertQuizzes(quizEntities)
                    database.quizDao().insertQuestions(questionEntities)

                    withContext(Dispatchers.Main) {
                        showQuizList(remoteQuizzes)
                    }
                }
            }
            .addOnFailureListener {
                loadQuizzesFromLocal()
                Toast.makeText(
                    this,
                    "Sem conexão. Exibindo quizzes salvos localmente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadQuizzesFromLocal() {
        lifecycleScope.launch(Dispatchers.IO) {
            val localQuizzes = database.quizDao().getQuizzesWithQuestions().map { quizWithQuestions ->
                QuizModel(
                    id = quizWithQuestions.quiz.id,
                    title = quizWithQuestions.quiz.title,
                    subtitle = quizWithQuestions.quiz.subtitle,
                    time = quizWithQuestions.quiz.time,
                    perguntas = quizWithQuestions.questions.map { question ->
                        PerguntaModel(
                            pergunta = question.pergunta,
                            options = listOf(
                                question.optionA,
                                question.optionB,
                                question.optionC,
                                question.optionD
                            ).filter { it.isNotBlank() },
                            correctAnswer = question.correctAnswer
                        )
                    }
                )
            }

            withContext(Dispatchers.Main) {
                showQuizList(localQuizzes)
            }
        }
    }

    private fun showQuizList(items: List<QuizModel>) {
        quizList.clear()
        quizList.addAll(items)
        QuizDataHolder.quizList = quizList
        adapter.notifyDataSetChanged()
        binding.quizCountTextView.text = "${quizList.size} quizzes disponíveis"
    }

    private fun showCreateQuizDialog() {
        showQuizEditorDialog()
    }

    private fun showEditQuizDialog(quiz: QuizModel) {
        showQuizEditorDialog(quiz)
    }

    private fun showQuizEditorDialog(existingQuiz: QuizModel? = null) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_quiz, null)

        val titleInput = view.findViewById<EditText>(R.id.quiz_title_input)
        val subtitleInput = view.findViewById<EditText>(R.id.quiz_subtitle_input)
        val timeInput = view.findViewById<EditText>(R.id.quiz_time_input)
        val questionsContainer = view.findViewById<LinearLayout>(R.id.questions_container)
        val addQuestionButton = view.findViewById<View>(R.id.add_question_button)

        titleInput.setText(existingQuiz?.title.orEmpty())
        subtitleInput.setText(existingQuiz?.subtitle.orEmpty())
        timeInput.setText(existingQuiz?.time.orEmpty())

        if (existingQuiz != null && existingQuiz.perguntas.isNotEmpty()) {
            existingQuiz.perguntas.forEach { addQuestionForm(questionsContainer, it) }
        } else {
            addQuestionForm(questionsContainer)
        }
        addQuestionButton.setOnClickListener { addQuestionForm(questionsContainer) }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existingQuiz == null) "Criar novo quiz" else "Editar quiz")
            .setView(view)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = titleInput.text?.toString()?.trim().orEmpty()
                val subtitle = subtitleInput.text?.toString()?.trim().orEmpty()
                val time = timeInput.text?.toString()?.trim().orEmpty()

                if (
                    title.isBlank() || subtitle.isBlank() || time.isBlank()
                ) {
                    Toast.makeText(this, "Preencha os dados do quiz.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val minutes = time.toIntOrNull()
                if (minutes == null || minutes <= 0) {
                    Toast.makeText(this, "Tempo inválido. Use minutos maiores que zero.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val createdQuestions = readQuestionsFromForm(questionsContainer) ?: return@setOnClickListener

                if (createdQuestions.isEmpty()) {
                    Toast.makeText(this, "Adicione pelo menos 1 pergunta.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val quizId = existingQuiz?.id
                    ?: quizzesRef.push().key
                    ?: UUID.randomUUID().toString()

                val quiz = QuizModel(
                    id = quizId,
                    title = title,
                    subtitle = subtitle,
                    time = minutes.toString(),
                    perguntas = createdQuestions
                )

                if (existingQuiz == null) {
                    saveCreatedQuiz(quiz)
                } else {
                    saveEditedQuiz(quiz)
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun addQuestionForm(container: LinearLayout, question: PerguntaModel? = null) {
        val itemView = LayoutInflater.from(this).inflate(R.layout.item_create_question, container, false)
        val header = itemView.findViewById<TextView>(R.id.question_header_text)
        header.text = "Pergunta ${container.childCount + 1}"

        itemView.findViewById<EditText>(R.id.question_input).setText(question?.pergunta.orEmpty())
        itemView.findViewById<EditText>(R.id.option_a_input).setText(question?.options?.getOrNull(0).orEmpty())
        itemView.findViewById<EditText>(R.id.option_b_input).setText(question?.options?.getOrNull(1).orEmpty())
        itemView.findViewById<EditText>(R.id.option_c_input).setText(question?.options?.getOrNull(2).orEmpty())
        itemView.findViewById<EditText>(R.id.option_d_input).setText(question?.options?.getOrNull(3).orEmpty())
        itemView.findViewById<EditText>(R.id.correct_answer_input).setText(question?.correctAnswer.orEmpty())

        container.addView(itemView)
    }

    private fun readQuestionsFromForm(container: LinearLayout): List<PerguntaModel>? {
        val createdQuestions = mutableListOf<PerguntaModel>()
        for (index in 0 until container.childCount) {
            val questionView = container.getChildAt(index)
            val question = questionView.findViewById<EditText>(R.id.question_input).text?.toString()?.trim().orEmpty()
            val optionA = questionView.findViewById<EditText>(R.id.option_a_input).text?.toString()?.trim().orEmpty()
            val optionB = questionView.findViewById<EditText>(R.id.option_b_input).text?.toString()?.trim().orEmpty()
            val optionC = questionView.findViewById<EditText>(R.id.option_c_input).text?.toString()?.trim().orEmpty()
            val optionD = questionView.findViewById<EditText>(R.id.option_d_input).text?.toString()?.trim().orEmpty()
            val correctAnswer = questionView.findViewById<EditText>(R.id.correct_answer_input).text?.toString()?.trim().orEmpty()

            if (
                question.isBlank() || optionA.isBlank() || optionB.isBlank() ||
                optionC.isBlank() || optionD.isBlank() || correctAnswer.isBlank()
            ) {
                Toast.makeText(this, "Preencha todos os campos da pergunta ${index + 1}.", Toast.LENGTH_SHORT).show()
                return null
            }

            val options = listOf(optionA, optionB, optionC, optionD)
            if (!options.contains(correctAnswer)) {
                Toast.makeText(
                    this,
                    "A resposta correta da pergunta ${index + 1} deve ser igual a uma das opções.",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }

            createdQuestions.add(
                PerguntaModel(
                    pergunta = question,
                    options = options,
                    correctAnswer = correctAnswer
                )
            )
        }
        return createdQuestions
    }

    private fun saveCreatedQuiz(quiz: QuizModel) {
        quizzesRef.child(quiz.id).setValue(quiz)
            .addOnSuccessListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    upsertQuizLocal(quiz)
                    withContext(Dispatchers.Main) {
                        quizList.add(0, quiz)
                        QuizDataHolder.quizList = quizList
                        adapter.notifyItemInserted(0)
                        binding.recyclerView.scrollToPosition(0)
                        binding.quizCountTextView.text = "${quizList.size} quizzes disponíveis"
                        Toast.makeText(this@MainActivity, "Quiz criado com sucesso.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar no Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveEditedQuiz(quiz: QuizModel) {
        quizzesRef.child(quiz.id).setValue(quiz)
            .addOnSuccessListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    upsertQuizLocal(quiz)
                    withContext(Dispatchers.Main) {
                        val index = quizList.indexOfFirst { it.id == quiz.id }
                        if (index >= 0) {
                            quizList[index] = quiz
                            QuizDataHolder.quizList = quizList
                            adapter.notifyItemChanged(index)
                            Toast.makeText(this@MainActivity, "Quiz editado com sucesso.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar no Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private suspend fun upsertQuizLocal(quiz: QuizModel) {
        val now = System.currentTimeMillis()
        database.quizDao().insertQuizzes(
            listOf(
                QuizEntity(
                    id = quiz.id,
                    title = quiz.title,
                    subtitle = quiz.subtitle,
                    time = quiz.time,
                    updatedAt = now
                )
            )
        )
        database.quizDao().deleteQuestionsByQuizId(quiz.id)
        val questionEntities = quiz.perguntas.map { question ->
            QuestionEntity(
                quizId = quiz.id,
                pergunta = question.pergunta,
                optionA = question.options.getOrElse(0) { "" },
                optionB = question.options.getOrElse(1) { "" },
                optionC = question.options.getOrElse(2) { "" },
                optionD = question.options.getOrElse(3) { "" },
                correctAnswer = question.correctAnswer
            )
        }
        if (questionEntities.isNotEmpty()) {
            database.quizDao().insertQuestions(questionEntities)
        }
    }

    private fun confirmDeleteQuiz(quiz: QuizModel) {
        AlertDialog.Builder(this)
            .setTitle("Excluir quiz")
            .setMessage("Deseja excluir \"${quiz.title}\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Excluir") { _, _ ->
                deleteQuiz(quiz)
            }
            .show()
    }

    private fun deleteQuiz(quiz: QuizModel) {
        quizzesRef.child(quiz.id).removeValue()
            .addOnSuccessListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    database.quizDao().deleteQuestionsByQuizId(quiz.id)
                    database.quizDao().deleteQuizById(quiz.id)
                    withContext(Dispatchers.Main) {
                        val index = quizList.indexOfFirst { it.id == quiz.id }
                        if (index >= 0) {
                            quizList.removeAt(index)
                            QuizDataHolder.quizList = quizList
                            adapter.notifyItemRemoved(index)
                            binding.quizCountTextView.text = "${quizList.size} quizzes disponíveis"
                        }
                        Toast.makeText(this@MainActivity, "Quiz excluído no Firebase e local.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao excluir no Firebase.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadDashboard() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val dao = database.resultDao()
            val totalQuizzes = dao.getQuizCountByUser(userId)
            val totalCorrect = dao.getTotalCorrectByUser(userId)
            val totalQuestions = dao.getTotalQuestionsByUser(userId)
            val avgPercent = dao.getAveragePercentByUser(userId)

            withContext(Dispatchers.Main) {
                binding.totalQuizValueText.text = totalQuizzes.toString()
                binding.totalHitsValueText.text = "$totalCorrect/$totalQuestions"
                binding.avgValueText.text = String.format("Média: %.1f%%", avgPercent)
            }
        }
    }

    private fun loadRanking() {
        // Codigo gerado com suporte de IA: monta ranking global com base na média de acertos por usuário.
        val db = FirebaseDatabase.getInstance().reference

        db.child("Users").get().addOnSuccessListener { usersSnapshot ->
            val userEmailMap = mutableMapOf<String, String>()
            for (userNode in usersSnapshot.children) {
                val user = userNode.getValue(UserProfileModel::class.java)
                if (user != null) {
                    userEmailMap[user.uid] = user.email
                }
            }

            db.child("Results").get().addOnSuccessListener { resultsSnapshot ->
                val ranking = mutableListOf<RankingItem>()

                for (userResults in resultsSnapshot.children) {
                    val uid = userResults.key ?: continue
                    val entries = userResults.children.mapNotNull {
                        it.getValue(QuizResultModel::class.java)
                    }
                    if (entries.isEmpty()) continue

                    val avg = entries.map {
                        if (it.totalQuestions == 0) 0.0 else (it.score * 100.0) / it.totalQuestions
                    }.average()

                    ranking.add(
                        RankingItem(
                            userId = uid,
                            email = userEmailMap[uid] ?: uid,
                            averagePercent = avg,
                            attempts = entries.size
                        )
                    )
                }

                val top = ranking.sortedByDescending { it.averagePercent }.take(5)
                val text = if (top.isEmpty()) {
                    "Ainda sem dados de ranking."
                } else {
                    top.mapIndexed { index, item ->
                        "${index + 1}. ${item.email} - ${String.format("%.1f", item.averagePercent)}% (${item.attempts})"
                    }.joinToString("\n")
                }

                binding.rankingTextView.text = text
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
