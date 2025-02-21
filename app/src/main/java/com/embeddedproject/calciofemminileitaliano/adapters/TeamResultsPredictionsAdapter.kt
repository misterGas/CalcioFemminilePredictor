package com.embeddedproject.calciofemminileitaliano.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R

class TeamResultsPredictionsAdapter(private val resultsPredictions: List<String>, private val resultsVotes: Map<String, Int>, private val totalVotes: Int) : RecyclerView.Adapter<TeamResultsPredictionsAdapter.TeamResultsPredictionsViewHolder>() {

    class TeamResultsPredictionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val predictionTextView: TextView = itemView.findViewById(R.id.prediction)
        private val votesProgressBar: ProgressBar = itemView.findViewById(R.id.prediction_votes)

        fun bind(result: String, resultVotes: Int, totalVotes: Int) {
            predictionTextView.text = result
            votesProgressBar.max = totalVotes
            val animation = ObjectAnimator.ofInt(votesProgressBar, "progress", 0, resultVotes)
            votesProgressBar.tooltipText = "$resultVotes/$totalVotes"
            animation.duration = 200
            animation.start()
            votesProgressBar.progress = resultVotes
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamResultsPredictionsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.match_prediction_team, parent, false)
        return TeamResultsPredictionsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return resultsPredictions.size
    }

    override fun onBindViewHolder(holder: TeamResultsPredictionsViewHolder, position: Int) {
        holder.bind(resultsPredictions[position], resultsVotes[resultsPredictions[position]]!!, totalVotes)
    }
}