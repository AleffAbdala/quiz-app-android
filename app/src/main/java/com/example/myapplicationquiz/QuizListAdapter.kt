package com.example.myapplicationquiz

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationquiz.databinding.QuizItemRecyclerRowBinding

class QuizListAdapter(private val quizList: List<QuizModel>) :
    RecyclerView.Adapter<QuizListAdapter.MyViewHolder>() {

    class MyViewHolder(private val binding: QuizItemRecyclerRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(quizModel: QuizModel) {
            binding.apply {
                quizTitleText.text = quizModel.title
                quizSubtitleText.text = quizModel.subtitle
                quizTimeText.text = quizModel.time + " min"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = QuizItemRecyclerRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return quizList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.bind(quizList[position])


        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, QuizActivity::class.java)

            intent.putExtra("QUIZ_ID", quizList[position].id)
            holder.itemView.context.startActivity(intent)
        }
    }
}