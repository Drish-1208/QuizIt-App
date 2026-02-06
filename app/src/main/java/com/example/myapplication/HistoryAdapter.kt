package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- THE ONLY CHANGE NEEDED IS HERE ---
// It now accepts a List of the Room database 'QuizHistory' entity.
class HistoryAdapter(private val historyList: List<QuizHistory>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // These view IDs are correct and don't need to change.
        val topicTextView: TextView = itemView.findViewById(R.id.item_topic_text)
        val scoreTextView: TextView = itemView.findViewById(R.id.item_score_text)
        val timestampTextView: TextView = itemView.findViewById(R.id.item_date_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // This layout inflation is correct and doesn't need to change.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        // Because QuizHistory has the same property names, none of this logic needs to change.
        val historyItem = historyList[position]

        holder.topicTextView.text = historyItem.quizTopic
        holder.scoreTextView.text = "Score: ${historyItem.correctAnswers}/${historyItem.totalQuestions}"

        // This timestamp handling logic is also correct for the Long value from Room.
        try {
            val timestamp = historyItem.timestamp
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault())
            holder.timestampTextView.text = format.format(date)
        } catch (e: Exception) {
            holder.timestampTextView.text = "Invalid date"
        }
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    // No other functions are needed. This is complete.
}
