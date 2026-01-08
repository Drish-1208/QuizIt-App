package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- THIS LINE HAS BEEN DELETED ---
// private val QuizResultEntity.timestamp: Any

class HistoryAdapter(
    private var results: List<QuizResultEntity>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    // This class holds the views for a single item in the list.
    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val topicTextView: TextView = itemView.findViewById(R.id.item_topic_text)
        val scoreTextView: TextView = itemView.findViewById(R.id.item_score_text)
        val dateTextView: TextView = itemView.findViewById(R.id.item_date_text)
    }

    // Creates a new view holder when the RecyclerView needs one.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // Inflate the XML layout for a single row.
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    // Binds the data from your list to the views in the view holder.
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val result = results[position]

        holder.topicTextView.text = result.topic
        holder.scoreTextView.text = "Score: ${result.score}"

        // Format the timestamp into a readable date string
        try {
            // This code is now correct because `result.timestamp` is correctly recognized as a Long
            val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            val dateString = sdf.format(Date(result.timestamp))
            holder.dateTextView.text = dateString
        } catch (e: Exception) {
            holder.dateTextView.text = "Invalid Date"
        }
    }

    // Returns the total number of items in the list.
    override fun getItemCount(): Int {
        return results.size
    }

    // A helper function to update the data in the adapter.
    fun updateData(newResults: List<QuizResultEntity>) {
        this.results = newResults
        notifyDataSetChanged() // Refreshes the RecyclerView
    }
}
