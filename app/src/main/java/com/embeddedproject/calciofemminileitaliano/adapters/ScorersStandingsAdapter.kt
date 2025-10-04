package com.embeddedproject.calciofemminileitaliano.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.ScorerStanding
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DataSnapshot

class ScorersStandingsAdapter(private val scorersGoal: Map<ScorerStanding, Int>, private val databaseGet: DataSnapshot, private val season: String) : RecyclerView.Adapter<ScorersStandingsAdapter.ScorersStandingsViewHolder>() {

     inner class ScorersStandingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val teamImage: ImageView = itemView.findViewById(R.id.team_image)
        private val firstName: TextView = itemView.findViewById(R.id.scorer_first_name)
        private val lastName: TextView = itemView.findViewById(R.id.scorer_last_name)
        private val goalsScored: TextView = itemView.findViewById(R.id.goals_scored)

        fun bind(scorer: ScorerStanding, value: Int, databaseGet: DataSnapshot, season: String) {
            val findPlayerInfo = databaseGet.child("Players").child(season).child(scorer.team).child(scorer.shirt.toString())
            val playerFirstName = findPlayerInfo.child("firstName").value.toString()
            val playerLastName = findPlayerInfo.child("lastName").value.toString()
            firstName.text = playerFirstName
            lastName.text = playerLastName
            goalsScored.text = value.toString()

            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase
            val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(scorer.team))
            if (setTeamImage.moveToFirst()) {
                teamImage.setImageBitmap(BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size))
            }
            setTeamImage.close()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScorersStandingsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.scorer_standing_dialog, parent, false)
        return ScorersStandingsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return scorersGoal.size
    }

    override fun onBindViewHolder(holder: ScorersStandingsViewHolder, position: Int) {
        holder.bind(scorersGoal.keys.elementAt(position), scorersGoal.values.elementAt(position), databaseGet, season)
    }
}