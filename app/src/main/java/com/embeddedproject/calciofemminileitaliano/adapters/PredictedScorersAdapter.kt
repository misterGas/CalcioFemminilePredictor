package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import java.util.stream.IntStream

class PredictedScorersAdapter(private val scorersList: List<Player>, private val scorerTypes: List<String>, private val numberOfScores: Map<Player, Int>, private val scorersGuessed: Map<PointsGoalOrOwnGoal, Int>, private val isStarted: Boolean, private val isFinished: Boolean, private val resource: Int) : RecyclerView.Adapter<PredictedScorersAdapter.PredictedScorersViewHolder>() {

    class PredictedScorersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scorerNameTextView: TextView = itemView.findViewById(R.id.scorer)

        @SuppressLint("DiscouragedApi")
        fun bind(scorer: Player, scorerType: String, scoresNumber: Int, scorersGuessed: Map<PointsGoalOrOwnGoal, Int>, isStarted: Boolean, isFinished: Boolean) {
            val type = if (scorerType == "Own goal") {
                itemView.resources.getString(R.string.own_goal)
            } else {
                ""
            }
            var name = ""
            var lastNameReduced = ""
            if (scorer.lastName.contains(" ")) {
                val lastNames = scorer.lastName.split(" ")
                for (i in IntStream.range(0, lastNames.size - 1)) {
                    lastNameReduced += if (lastNames[i].length <= 3) {
                        "${lastNames[i]} "
                    } else {
                        "${lastNames[i][0]}. "
                    }
                }
                lastNameReduced += lastNames[lastNames.size - 1]
            }
            else {
                lastNameReduced = scorer.lastName
            }
            name = lastNameReduced
            if (type != "") {
                name = "$name $type"
            }
            val moreGoalsObjective = itemView.findViewById<ImageView>(R.id.bonus_more_goals)
            val moreGoalsAchieved = itemView.findViewById<ImageView>(R.id.bonus_more_goals_achieved)
            if (scoresNumber > 1) {
                name = "$name x$scoresNumber"
            }
            scorerNameTextView.text = name
            val guessed = itemView.findViewById<ImageView>(R.id.scorer_guessed)
            guessed.visibility = VISIBLE
            var found = false
            var numberOfBonus = 0
            if (isStarted || isFinished) {
                if (scorersGuessed.isEmpty()) {
                    guessed.setImageResource(R.drawable.wrong)
                }
                else {
                    for (g in scorersGuessed.keys) {
                        if (scorerType == g.goalType && scorer.shirtNumber == g.shirt && ((scorerType == "Goal" && scorer.team == g.team) || (scorerType == "Own goal" && scorer.team != g.team))) {
                            found = true
                            if (scorersGuessed[g]!! > 0) {
                                moreGoalsAchieved.setImageResource(R.drawable.completed)
                            }
                            else {
                                moreGoalsAchieved.setImageResource(R.drawable.wrong)
                            }
                            numberOfBonus = scorersGuessed[g]!!
                            break
                        }
                    }
                    if (found) {
                        guessed.setImageResource(R.drawable.completed)
                        if (scoresNumber > 1) {
                            moreGoalsObjective.visibility = VISIBLE
                            moreGoalsAchieved.visibility = VISIBLE
                        }
                    }
                    else {
                        guessed.setImageResource(R.drawable.wrong)
                    }
                }
            }

            moreGoalsObjective.setOnClickListener {
                if (numberOfBonus > 0) {
                    if (scorerType == "Goal") {
                        showBonusMessage("${itemView.resources.getString(R.string.more_goals_bonus)} (x$numberOfBonus)")
                    }
                    else {
                        showBonusMessage("${itemView.resources.getString(R.string.more_own_goals_bonus)} (x$numberOfBonus)")
                    }
                }
                else {
                    if (scorerType == "Goal") {
                        showBonusMessage(itemView.resources.getString(R.string.more_goals_bonus_failed))
                    }
                    else {
                        showBonusMessage(itemView.resources.getString(R.string.more_own_goals_bonus_failed))
                    }
                }
            }
        }

        private fun showBonusMessage(message: String) {
            val builder = AlertDialog.Builder(itemView.context)
            builder.setMessage(message)

            builder.setPositiveButton(itemView.resources.getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictedScorersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(resource, parent, false)
        return PredictedScorersViewHolder(view)
    }

    override fun getItemCount(): Int {
        return scorersList.size
    }

    override fun onBindViewHolder(holder: PredictedScorersViewHolder, position: Int) {
        holder.bind(scorersList[position], scorerTypes[position], numberOfScores[scorersList[position]]!!, scorersGuessed, isStarted, isFinished)
    }
}