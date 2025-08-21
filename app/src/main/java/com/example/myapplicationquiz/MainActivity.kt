package com.example.myapplicationquiz

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplicationquiz.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var quizList: MutableList<QuizModel>
    lateinit var adapter: QuizListAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        quizList = mutableListOf()

        binding.welcomeTextView.text = "Olá, ${auth.currentUser?.email}"

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        getDataFromFirebase()
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            goToLogin()
        }
    }

    private fun setupRecyclerView() {
        adapter = QuizListAdapter(quizList)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun getDataFromFirebase() {
        QuizDataHolder.quizList = quizList
        FirebaseDatabase.getInstance().reference.child("Quizzes")
            .get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    quizList.clear()
                    for (data in dataSnapshot.children) {
                        val quizModel = data.getValue(QuizModel::class.java)
                        if (quizModel != null) {
                            quizList.add(quizModel)
                        }
                    }
                }
                setupRecyclerView()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}