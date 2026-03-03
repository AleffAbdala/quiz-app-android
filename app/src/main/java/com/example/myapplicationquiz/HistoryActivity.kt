package com.example.myapplicationquiz

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplicationquiz.databinding.ActivityHistoryBinding
import com.example.myapplicationquiz.local.AppDatabase
import com.example.myapplicationquiz.local.ResultEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val resultList = mutableListOf<QuizResultModel>()
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Codigo gerado com suporte de IA: fluxo de retorno simples para tela principal.
        binding.backToMainButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()
        loadHistoryFromRoom()
        syncHistoryFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(resultList)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun syncHistoryFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Codigo gerado com suporte de IA: sincroniza resultados da nuvem para manter histórico local atualizado.
        FirebaseDatabase.getInstance().reference
            .child("Results")
            .child(currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) return@addOnSuccessListener

                val cloudResults = snapshot.children.mapNotNull { child ->
                    val result = child.getValue(QuizResultModel::class.java) ?: return@mapNotNull null
                    if (result.resultId.isBlank()) result.copy(resultId = child.key.orEmpty()) else result
                }

                lifecycleScope.launch(Dispatchers.IO) {
                    cloudResults.forEach { result ->
                        database.resultDao().insert(
                            ResultEntity(
                                id = result.resultId,
                                userId = currentUser.uid,
                                quizId = result.quizId,
                                title = result.title,
                                score = result.score,
                                totalQuestions = result.totalQuestions,
                                elapsedSeconds = result.elapsedSeconds,
                                timestamp = result.timestamp
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        loadHistoryFromRoom()
                    }
                }
            }
    }

    private fun loadHistoryFromRoom() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Codigo gerado com suporte de IA: calcula estatísticas agregadas e atualiza UI em uma única leitura.
        lifecycleScope.launch(Dispatchers.IO) {

            val dao = database.resultDao()
            val localResults = dao.getAllResultsByUser(currentUser.uid)
            val totalQuizzes = dao.getQuizCountByUser(currentUser.uid)
            val totalCorrect = dao.getTotalCorrectByUser(currentUser.uid)
            val totalQuestions = dao.getTotalQuestionsByUser(currentUser.uid)
            val avgPercent = dao.getAveragePercentByUser(currentUser.uid)

            withContext(Dispatchers.Main) {

                resultList.clear()

                localResults.forEach {
                    resultList.add(
                        QuizResultModel(
                            resultId = it.id,
                            userId = it.userId,
                            quizId = it.quizId,
                            title = it.title,
                            score = it.score,
                            totalQuestions = it.totalQuestions,
                            elapsedSeconds = it.elapsedSeconds,
                            timestamp = it.timestamp
                        )
                    )
                }

                binding.totalHistoryValueText.text = "Quizzes: $totalQuizzes"
                binding.totalCorrectValueText.text = "Acertos: $totalCorrect/$totalQuestions"
                binding.avgHistoryValueText.text = String.format("Média: %.1f%%", avgPercent)

                adapter.notifyDataSetChanged()
            }
        }
    }
}
