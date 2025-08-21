package com.example.myapplicationquiz

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val resultList: List<QuizResultModel>) :
    RecyclerView.Adapter<HistoryAdapter.MyViewHolder>() {

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.quiz_title_text)
        val scoreText: TextView = itemView.findViewById(R.id.score_text)
        val dateText: TextView = itemView.findViewById(R.id.date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item_row, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val result = resultList[position]
        holder.titleText.text = result.title
        holder.scoreText.text = "Pontuação: ${result.score}/${result.totalQuestions}"

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.dateText.text = "Data: ${sdf.format(Date(result.timestamp))}"
    }

    override fun getItemCount(): Int {
        return resultList.size
    }
}