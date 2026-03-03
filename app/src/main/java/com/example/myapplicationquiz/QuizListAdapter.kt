package com.example.myapplicationquiz

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationquiz.databinding.QuizItemRecyclerRowBinding

class QuizListAdapter(
    private val quizList: List<QuizModel>,
    private val onEditQuiz: (QuizModel) -> Unit,
    private val onDeleteQuiz: (QuizModel) -> Unit
) :
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
        val quiz = quizList[position]
        holder.bind(quiz)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, QuizActivity::class.java)
            intent.putExtra("QUIZ_ID", quiz.id)
            holder.itemView.context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            androidx.appcompat.app.AlertDialog.Builder(holder.itemView.context)
                .setTitle(quiz.title)
                .setItems(arrayOf("Editar", "Excluir")) { _, which ->
                    when (which) {
                        0 -> onEditQuiz(quiz)
                        1 -> onDeleteQuiz(quiz)
                    }
                }
                .show()
            true
        }
    }
}
