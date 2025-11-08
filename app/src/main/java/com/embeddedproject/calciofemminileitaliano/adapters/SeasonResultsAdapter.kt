package com.embeddedproject.calciofemminileitaliano.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.TeamResults
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper

class SeasonResultsAdapter(private val teamsResults: List<TeamResults>, private val championshipHasInternationalTeams: Boolean) : RecyclerView.Adapter<SeasonResultsAdapter.SeasonResultsViewHolder>() {

    class SeasonResultsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val teamImageView: ImageView = itemView.findViewById(R.id.team_image)
        private val teamNameTextView: TextView = itemView.findViewById(R.id.team_name)
        private val playedTextView: TextView = itemView.findViewById(R.id.played)
        private val winsTextView: TextView = itemView.findViewById(R.id.wins)
        private val nullsTextView: TextView = itemView.findViewById(R.id.nulls)
        private val lostTextView: TextView = itemView.findViewById(R.id.lost)
        private val scoredTextView: TextView = itemView.findViewById(R.id.scored)
        private val sufferedTextView: TextView = itemView.findViewById(R.id.suffered)
        private val differenceTextView: TextView = itemView.findViewById(R.id.difference)
        private val teamMatchesResultsRecyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_team_results)

        fun bind(teamResults: TeamResults, championshipHasInternationalTeams: Boolean) {
            if (championshipHasInternationalTeams) {
                teamNameTextView.text = itemView.resources.getString(itemView.resources.getIdentifier(teamResults.team.lowercase().replace(" ", "_"), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            }
            else {
                teamNameTextView.text = teamResults.team
            }
            playedTextView.text = teamResults.played.toString()
            winsTextView.text = teamResults.wins.toString()
            nullsTextView.text = teamResults.nulls.toString()
            lostTextView.text = teamResults.lost.toString()
            scoredTextView.text = teamResults.goalsScored.toString()
            sufferedTextView.text = teamResults.goalsSuffered.toString()
            differenceTextView.text = (teamResults.goalsScored - teamResults.goalsSuffered).toString()

            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase

            val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(teamResults.team))
            if (setTeamImage.moveToFirst()) {
                teamImageView.setImageBitmap(BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size))
            }
            setTeamImage.close()

            val teamResultsAdapter = TeamMatchesResultsAdapter(teamResults.teamMatches, championshipHasInternationalTeams)
            teamMatchesResultsRecyclerView.adapter = teamResultsAdapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeasonResultsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.championship_standing, parent, false)
        return SeasonResultsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return teamsResults.size
    }

    override fun onBindViewHolder(holder: SeasonResultsViewHolder, position: Int) {
        holder.bind(teamsResults[position], championshipHasInternationalTeams)
    }
}