package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var results: List<QuizResultEntity>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val topicTextView: TextView = itemView.findViewById(R.id.item_topic_text)
        val scoreTextView: TextView = itemView.findViewById(R.id.item_score_text)
        val dateTextView: TextView = itemView.findViewById(R.id.item_date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val result = results[position]

        holder.topicTextView.text = result.topic
        holder.scoreTextView.text = "Score: ${result.score}"

        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            val dateString = sdf.format(Date(result.timestamp))
            holder.dateTextView.text = dateString
        } catch (e: Exception) {
            holder.dateTextView.text = "Invalid Date"
        }
    }

    override fun getItemCount(): Int {
        return results.size
    }

    fun updateData(newResults: List<QuizResultEntity>) {
        this.results = newResults
        notifyDataSetChanged()
    }
}
