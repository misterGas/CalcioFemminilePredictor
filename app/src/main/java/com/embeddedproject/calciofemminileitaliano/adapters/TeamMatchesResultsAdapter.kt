package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.TeamMatch

class TeamMatchesResultsAdapter(private val teamMatches: List<TeamMatch>) : RecyclerView.Adapter<TeamMatchesResultsAdapter.TeamMatchesResultsViewHolder>() {

    class TeamMatchesResultsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val resultRelativeLayout: RelativeLayout = itemView.findViewById(R.id.result)
        private val resultInfoImageView: ImageView = itemView.findViewById(R.id.result_info)
        private val lastResultRelativeLayout: RelativeLayout = itemView.findViewById(R.id.last_result)
        private val lastResultInfoImageView: ImageView = itemView.findViewById(R.id.last_result_info)

        @SuppressLint("DiscouragedApi")
        fun bind(teamMatch: TeamMatch, position: Int, totalMatches: Int) {
            val location = itemView.resources.getString(itemView.resources.getIdentifier(teamMatch.location, "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            val outcome = itemView.resources.getString(itemView.resources.getIdentifier(teamMatch.outcome, "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            if (position != totalMatches) {
                resultRelativeLayout.visibility = VISIBLE
                lastResultRelativeLayout.visibility = GONE

                when (teamMatch.outcome) {
                    "WIN" -> {
                        resultInfoImageView.setImageResource(R.drawable.completed)
                    }
                    "NULL" -> {
                        resultInfoImageView.setImageResource(R.drawable.null_result)
                    }
                    else -> {
                        resultInfoImageView.setImageResource(R.drawable.wrong)
                    }
                }
                resultRelativeLayout.tooltipText = "vs ${teamMatch.vsTeam} ($location)\n$outcome (${teamMatch.homeScore}-${teamMatch.guestScore})"
            }
            else {
                lastResultRelativeLayout.visibility = VISIBLE
                resultRelativeLayout.visibility = GONE

                when (teamMatch.outcome) {
                    "WIN" -> {
                        lastResultInfoImageView.setImageResource(R.drawable.completed)
                    }
                    "NULL" -> {
                        lastResultInfoImageView.setImageResource(R.drawable.null_result)
                    }
                    else -> {
                        lastResultInfoImageView.setImageResource(R.drawable.wrong)
                    }
                }
                lastResultRelativeLayout.tooltipText = "vs ${teamMatch.vsTeam} ($location)\n$outcome (${teamMatch.homeScore}-${teamMatch.guestScore})"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamMatchesResultsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.last_results, parent, false)
        return TeamMatchesResultsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return teamMatches.size
    }

    override fun onBindViewHolder(holder: TeamMatchesResultsViewHolder, position: Int) {
        holder.bind(teamMatches[position], position, teamMatches.size - 1)
    }
}