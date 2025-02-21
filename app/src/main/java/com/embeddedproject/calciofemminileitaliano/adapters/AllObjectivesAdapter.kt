package com.embeddedproject.calciofemminileitaliano.adapters

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.ObjectiveType

class AllObjectivesAdapter(private val allObjectivesList: List<List<Int>>, private val objectiveTypesList: List<ObjectiveType>) : RecyclerView.Adapter<AllObjectivesAdapter.AllObjectivesViewHolder>() {

    class AllObjectivesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val objectivesType: TextView = itemView.findViewById(R.id.objectives_type)
        private val objectivesTypeDescription: TextView = itemView.findViewById(R.id.objectives_type_description)
        private val objectiveTypeCompleted: TextView = itemView.findViewById(R.id.objectives_type_completed)
        private val objectivesTypeProgress: ProgressBar = itemView.findViewById(R.id.objectives_type_progress)

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(typeObjectivesAdapter: TypeObjectivesAdapter, typeObjectivesNumber: Int, typeObjectives: List<Int>, objectivesInfoList: ObjectiveType) {
            val objectivesTypeRecyclerView = itemView.findViewById<RecyclerView>(R.id.recycler_view_objectives_type)
            objectivesTypeRecyclerView.adapter = typeObjectivesAdapter
            objectivesType.text = objectivesInfoList.type
            objectivesTypeDescription.text = objectivesInfoList.description
            objectivesTypeProgress.max = typeObjectivesNumber
            val reachedValue = objectivesInfoList.reachedValue
            var objectivesCompleted = 0
            for (tO in typeObjectives) {
                if (reachedValue >= tO) {
                    objectivesCompleted++
                }
            }
            objectivesTypeProgress.progress = objectivesCompleted
            val completedText = "$objectivesCompleted/$typeObjectivesNumber ($reachedValue)"
            objectiveTypeCompleted.text = completedText

            val animation = ObjectAnimator.ofInt(objectivesTypeProgress, "progress", 0, objectivesCompleted)
            animation.start()

            itemView.findViewById<RelativeLayout>(R.id.show_objectives).setOnClickListener {
                val openImage = itemView.findViewById<ImageView>(R.id.open_objectives)
                if (objectivesTypeRecyclerView.visibility == GONE) {
                    openImage.setImageResource(R.drawable.arrow_down)
                    objectivesTypeRecyclerView.visibility = VISIBLE
                }
                else {
                    openImage.setImageResource(R.drawable.arrow_right)
                    objectivesTypeRecyclerView.visibility = GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllObjectivesViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.objectives_type, parent, false)
        return AllObjectivesViewHolder(view)
    }

    override fun getItemCount(): Int {
        return allObjectivesList.size
    }

    override fun onBindViewHolder(holder: AllObjectivesViewHolder, position: Int) {
        holder.bind(TypeObjectivesAdapter(allObjectivesList[position], objectiveTypesList[position].reachedValue), allObjectivesList[position].size, allObjectivesList[position], objectiveTypesList[position])
    }
}