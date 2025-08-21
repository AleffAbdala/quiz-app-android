package com.example.myapplicationquiz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplicationquiz.databinding.ActivityHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var resultList = mutableListOf<QuizResultModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadHistoryFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(resultList)
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun loadHistoryFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid

        val databaseReference = FirebaseDatabase.getInstance().reference
            .child("Results")
            .child(userId)

        databaseReference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                resultList.clear()
                for (snapshot in dataSnapshot.children) {
                    val result = snapshot.getValue(QuizResultModel::class.java)
                    result?.let { resultList.add(it) }
                }
                // Ordena a lista pela data mais recente
                resultList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()
            }
        }
    }
}