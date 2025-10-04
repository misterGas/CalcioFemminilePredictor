package com.embeddedproject.calciofemminileitaliano.adapters

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
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import java.util.stream.IntStream.range

class OfficialCardsAdapter(private val playersCardsList: Map<Player, String>, private val redCardsTypes: Map<Player, String>, private val timelines: Map<Player, String>, private val isStarted: Boolean, private val isFinished: Boolean, private val resource: Int) : RecyclerView.Adapter<OfficialCardsAdapter.OfficialCardsViewHolder>() {

    class OfficialCardsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerNameTextView = itemView.findViewById<TextView>(R.id.player_name)
        private val cardTypeImageView = itemView.findViewById<ImageView>(R.id.card_type)
        private val twoYellowCardsRedRelativeLayout = itemView.findViewById<RelativeLayout>(R.id.two_yellows_info)

        fun bind(player: Player, cardColor: String, redCardsTypes: Map<Player, String>, timelines: Map<Player, String>, isStarted: Boolean, isFinished: Boolean, resource: Int) {
            var name = ""
            var lastNameReduced = ""
            if (player.lastName.contains(" ")) {
                val lastNames = player.lastName.split(" ")
                for (i in range(0, lastNames.size - 1)) {
                    lastNameReduced += if (lastNames[i].length <= 3) {
                        "${lastNames[i]} "
                    } else {
                        "${lastNames[i][0]}. "
                    }
                }
                lastNameReduced += lastNames[lastNames.size - 1]
            }
            else {
                lastNameReduced = player.lastName
            }
            if (resource == R.layout.home_player_card_info) {
                name = lastNameReduced
                if (timelines.contains(player)) {
                    name = "$name - ${timelines[player]!!}'"
                }
            }
            else {
                if (timelines.contains(player)) {
                    name = "${timelines[player]!!}'"
                    name = "$name -"
                }
                name = "$name $lastNameReduced"
            }
            playerNameTextView.text = name
            if (cardColor == "Yellow") {
                cardTypeImageView.setImageResource(R.drawable.yellow_card)
            }
            else {
                cardTypeImageView.setImageResource(R.drawable.red_card)
                if (redCardsTypes.contains(player) && redCardsTypes[player] == "2Yellows") {
                    twoYellowCardsRedRelativeLayout.visibility = VISIBLE
                }
                else {
                    twoYellowCardsRedRelativeLayout.visibility = GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfficialCardsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(resource, parent, false)
        return OfficialCardsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playersCardsList.size
    }

    override fun onBindViewHolder(holder: OfficialCardsViewHolder, position: Int) {
        holder.bind(playersCardsList.toList()[position].first, playersCardsList.toList()[position].second, redCardsTypes, timelines, isStarted, isFinished, resource)
    }
}