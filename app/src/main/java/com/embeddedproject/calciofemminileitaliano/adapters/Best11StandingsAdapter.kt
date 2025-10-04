package com.embeddedproject.calciofemminileitaliano.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.Best11StandingsDirections
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.UserTotalPoints
import com.google.firebase.database.DataSnapshot

class Best11StandingsAdapter(private val actualUser: String, private val usersList: List<UserTotalPoints>, private val championship: String, private val season: String, private val round: Int, private val actualUserModule: String, private val best11UsersPointsReference: DataSnapshot) : RecyclerView.Adapter<Best11StandingsAdapter.Best11StandingsAdapterViewHolder>() {

    class Best11StandingsAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val standingPosition: TextView = itemView.findViewById(R.id.standing_position)
        private val points: TextView = itemView.findViewById(R.id.user_points)
        private val userNickname: TextView = itemView.findViewById(R.id.user_nickname)
        private val comparePredictions: ImageView = itemView.findViewById(R.id.compare_predictions)

        fun bind(actualUser: String, userPoints: UserTotalPoints, championship: String, season: String, round: Int, actualUserModule: String, best11UsersPointsReference: DataSnapshot) {
            standingPosition.text = userPoints.getPosition().toString()
            points.text = userPoints.getTotalPoints().toString()
            userNickname.text = userPoints.getUserNickname()
            if (userPoints.getUserNickname() == actualUser) {
                itemView.findViewById<RelativeLayout>(R.id.user_item_standing).background = itemView.resources.getColor(R.color.table_result_values, null).toDrawable()
                comparePredictions.visibility = INVISIBLE
            }
            else {
                itemView.findViewById<RelativeLayout>(R.id.user_item_standing).background = Color.TRANSPARENT.toDrawable()
                comparePredictions.visibility = VISIBLE
            }
            comparePredictions.setOnClickListener {
                val userPointsModule = best11UsersPointsReference.child(userPoints.getUserNickname()).child("Module").value.toString()
                val navigateToBest11DayDuel = Best11StandingsDirections.actionBest11StandingsToBest11DayDuel(actualUser, userPoints.getUserNickname(), championship, season, round, actualUserModule, userPointsModule)
                itemView.findNavController().navigate(navigateToBest11DayDuel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Best11StandingsAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_standing, parent, false)
        return Best11StandingsAdapterViewHolder(view)
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    override fun onBindViewHolder(holder: Best11StandingsAdapterViewHolder, position: Int) {
        holder.bind(actualUser, usersList[position], championship, season, round, actualUserModule, best11UsersPointsReference)
    }
}