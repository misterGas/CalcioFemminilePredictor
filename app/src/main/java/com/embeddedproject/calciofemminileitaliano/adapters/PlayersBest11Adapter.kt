package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.google.firebase.database.DataSnapshot
import org.w3c.dom.Text

class PlayersBest11Adapter(private val playersBest11: List<Player>, private val best11TeamsBitmaps: Map<String, Bitmap>, private val resource: Int = R.layout.home_scorer, private val captainPlayer: MVPPlayer? = null, private val playersPositionsInBest11: Map<MVPPlayer, String>? = null, private val playersBest11PointsDetails: DataSnapshot? = null, private val pointsRules: Map<String,Map<String,Int>> = emptyMap()) : RecyclerView.Adapter<PlayersBest11Adapter.PlayersBest11ViewHolder>() {

    class PlayersBest11ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val firstNameTextView: TextView = itemView.findViewById(R.id.scorer_first_name)
        private val lastNameTextView: TextView = itemView.findViewById(R.id.scorer_last_name)
        private val numberTextView: TextView = itemView.findViewById(R.id.shirt_number)
        private val roleTextView: TextView = itemView.findViewById(R.id.role)
        private val teamImageView: ImageView = itemView.findViewById(R.id.team_image)

        @SuppressLint("DiscouragedApi")
        fun bind(player: Player, best11TeamsBitmaps: Map<String, Bitmap>, resource: Int, captainPlayer: MVPPlayer?, playersPositionsInBest11: Map<MVPPlayer, String>?, playersBest11PointsDetails: DataSnapshot?, pointsRules: Map<String, Map<String, Int>>) {
            firstNameTextView.text = player.firstName
            lastNameTextView.text = player.lastName
            numberTextView.text = player.shirtNumber.toString()
            roleTextView.text = itemView.resources.getString(itemView.resources.getIdentifier(player.role.lowercase(), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            teamImageView.setImageBitmap(best11TeamsBitmaps[player.team])

            if (captainPlayer != null && player.team == captainPlayer.team && player.shirtNumber == captainPlayer.shirt) {
                val captainRelativeLayout = itemView.findViewById<RelativeLayout>(R.id.captain_relative_layout)
                captainRelativeLayout.visibility = VISIBLE
            }
            else {
                val captainRelativeLayout = itemView.findViewById<RelativeLayout>(R.id.captain_relative_layout)
                captainRelativeLayout.visibility = GONE
            }

            if (resource != R.layout.home_scorer) {
                val pointsInfo = itemView.findViewById<ImageView>(R.id.player_points_info)
                if (playersBest11PointsDetails != null) {
                    val position = playersPositionsInBest11?.get(MVPPlayer(player.team, player.shirtNumber))
                    itemView.findViewById<RelativeLayout>(R.id.player_points_rel_layout).visibility = VISIBLE
                    var playerTotalPoints = 0
                    if (playersBest11PointsDetails.hasChild(position!!)) {
                        pointsInfo.visibility = VISIBLE
                        val detailsPointList = mutableListOf<String>()
                        var moreGoalsBonus = 0
                        for (d in playersBest11PointsDetails.child(position).children) {
                            if (d.key.toString() == "OneMoreGoal") {
                                detailsPointList.add(d.key.toString())
                                moreGoalsBonus = d.value.toString().replace("x","").toInt()
                            }
                            else {
                                detailsPointList.add(d.key.toString())
                            }
                        }
                        val pointsInfoDialog = LayoutInflater.from(itemView.context).inflate(R.layout.best11_player_points_details_dialog, null)
                        val firstNameInfo = pointsInfoDialog.findViewById<TextView>(R.id.scorer_first_name)
                        val lastNameInfo = pointsInfoDialog.findViewById<TextView>(R.id.scorer_last_name)
                        val shirtInfo = pointsInfoDialog.findViewById<TextView>(R.id.shirt_number)
                        val roleInfo = pointsInfoDialog.findViewById<TextView>(R.id.role)
                        val teamImageInfo = pointsInfoDialog.findViewById<ImageView>(R.id.team_image)
                        firstNameInfo.text = player.firstName
                        lastNameInfo.text = player.lastName
                        shirtInfo.text = player.shirtNumber.toString()
                        roleInfo.text = itemView.resources.getString(itemView.resources.getIdentifier(player.role.lowercase(), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
                        teamImageInfo.setImageBitmap(best11TeamsBitmaps[player.team])
                        val pointsRoleRules = pointsRules[position[0].toString()]
                        if (detailsPointList.contains("OneGoal")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.scorer).visibility = VISIBLE
                            val pointsTextView = pointsInfoDialog.findViewById<TextView>(R.id.scorer_points)
                            pointsTextView.visibility = VISIBLE
                            playerTotalPoints += pointsRoleRules?.get("OneGoal")!!.toInt()
                            pointsTextView.text = pointsRoleRules["OneGoal"].toString()
                        }
                        if (detailsPointList.contains("OneMoreGoal")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.more_goals).visibility = VISIBLE
                            val pointsTextView = pointsInfoDialog.findViewById<TextView>(R.id.more_goals_points)
                            pointsTextView.visibility = VISIBLE
                            playerTotalPoints += pointsRoleRules?.get("OneMoreGoal")!!.toInt() * moreGoalsBonus
                            val text = "(x$moreGoalsBonus) ${pointsRoleRules["OneMoreGoal"].toString().replace("x","").toInt() * moreGoalsBonus}"
                            pointsTextView.text = text
                        }
                        if (detailsPointList.contains("MVP")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.mvp).visibility = VISIBLE
                            val pointsTextView = pointsInfoDialog.findViewById<TextView>(R.id.mvp_points)
                            pointsTextView.visibility = VISIBLE
                            playerTotalPoints += pointsRoleRules?.get("MVP")!!.toInt()
                            pointsTextView.text = pointsRoleRules["MVP"].toString()
                        }
                        if (detailsPointList.contains("OwnGoal")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.own_goal).visibility = VISIBLE
                            val pointsTextView = pointsInfoDialog.findViewById<TextView>(R.id.own_goal_points)
                            pointsTextView.visibility = VISIBLE
                            playerTotalPoints += pointsRoleRules?.get("OwnGoal")!!.toInt()
                            pointsTextView.text = pointsRoleRules["OwnGoal"].toString()
                        }
                        if (detailsPointList.contains("YellowCard")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.yellow_card).visibility = VISIBLE
                            val pointsTextView = pointsInfoDialog.findViewById<TextView>(R.id.yellow_card_points)
                            pointsTextView.visibility = VISIBLE
                            playerTotalPoints += pointsRoleRules?.get("YellowCard")!!.toInt()
                            pointsTextView.text = pointsRoleRules["YellowCard"].toString()
                        }
                        if (detailsPointList.contains("RedCard")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.red_card).visibility = VISIBLE
                            val pointsTextView = pointsInfoDialog.findViewById<TextView>(R.id.red_card_points)
                            pointsTextView.visibility = VISIBLE
                            playerTotalPoints += pointsRoleRules?.get("RedCard")!!.toInt()
                            pointsTextView.text = pointsRoleRules["RedCard"].toString()
                        }
                        if (detailsPointList.contains("isCaptain")) {
                            pointsInfoDialog.findViewById<TextView>(R.id.captain).visibility = VISIBLE
                            pointsInfoDialog.findViewById<TextView>(R.id.captain_yes).visibility = VISIBLE
                            playerTotalPoints *= 2
                        }
                        itemView.findViewById<TextView>(R.id.player_points).text = playerTotalPoints.toString()
                        pointsInfo.setOnClickListener {
                            (pointsInfoDialog.parent as? ViewGroup)?.removeView(pointsInfoDialog)
                            val builder = AlertDialog.Builder(itemView.context)
                            builder.setView(pointsInfoDialog)
                            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                            }

                            val dialog = builder.create()
                            dialog.show()
                        }
                    }
                    else {
                        pointsInfo.visibility = GONE
                        itemView.findViewById<TextView>(R.id.player_points).text = "0"
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersBest11ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(resource, parent, false)
        return PlayersBest11ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playersBest11.size
    }

    override fun onBindViewHolder(holder: PlayersBest11ViewHolder, position: Int) {
        holder.bind(playersBest11[position], best11TeamsBitmaps, resource, captainPlayer, playersPositionsInBest11, playersBest11PointsDetails, pointsRules)
    }
}