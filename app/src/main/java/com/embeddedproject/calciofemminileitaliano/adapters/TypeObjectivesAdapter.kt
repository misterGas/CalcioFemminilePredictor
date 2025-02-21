package com.embeddedproject.calciofemminileitaliano.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R

class TypeObjectivesAdapter(private val objectivesList: List<Int>, private val reachedValue: Int) : RecyclerView.Adapter<TypeObjectivesAdapter.TypeObjectivesViewHolder>() {

    class TypeObjectivesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val objectiveTitle: TextView = itemView.findViewById(R.id.objective_title)
        private val objectiveProgress: ProgressBar = itemView.findViewById(R.id.objective_progress)
        private val progressTextView: TextView = itemView.findViewById(R.id.objective_progress_text)

        fun bind(maxValue: Int, reachedValue: Int) {
            objectiveTitle.text = maxValue.toString()
            objectiveProgress.min = 0
            objectiveProgress.max = maxValue
            objectiveProgress.progress = reachedValue

            if (reachedValue < maxValue) {
                val animation = ObjectAnimator.ofInt(objectiveProgress, "progress", 0, reachedValue)
                animation.start()

                animation.addUpdateListener { valueAnimator ->
                    val progress = valueAnimator.animatedValue as Int
                    progressTextView.text = progress.toString()
                    val progressBarWidth = objectiveProgress.width
                    if (progress == objectiveProgress.max) {
                        itemView.findViewById<RelativeLayout>(R.id.completed).visibility = VISIBLE
                    }
                    val translationX = (progressBarWidth * progress / maxValue) - (progressTextView.width / 2f)
                    progressTextView.translationX = translationX
                }
            }
            else {
                objectiveProgress.visibility = GONE
                progressTextView.visibility = GONE
                itemView.findViewById<RelativeLayout>(R.id.completed).visibility = VISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TypeObjectivesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.objective_details, parent, false)
        return TypeObjectivesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return objectivesList.size
    }

    override fun onBindViewHolder(holder: TypeObjectivesViewHolder, position: Int) {
        holder.bind(objectivesList[position], reachedValue)
    }
}