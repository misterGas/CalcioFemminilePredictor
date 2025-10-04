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
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import java.util.stream.IntStream

class PredictedCardsAdapter(private val playersCardsList: Map<Player, String>, private val redCardsTypes: Map<Player, String>, private val yellowCardsGuessed: List<MVPPlayer>, private val redCardsGuessed: Map<MVPPlayer, Boolean>, private val isStarted: Boolean, private val isFinished: Boolean, private val resource: Int) : RecyclerView.Adapter<PredictedCardsAdapter.PredictedCardsViewHolder>() {

    class PredictedCardsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerNameTextView = itemView.findViewById<TextView>(R.id.player_name)
        private val cardTypeImageView = itemView.findViewById<ImageView>(R.id.card_type)
        private val twoYellowCardsRedRelativeLayout = itemView.findViewById<RelativeLayout>(R.id.two_yellows_info)

        fun bind(player: Player, cardColor: String, redCardsTypes: Map<Player, String>, yellowCardsGuessed: List<MVPPlayer>, redCardsGuessed: Map<MVPPlayer, Boolean>, isStarted: Boolean, isFinished: Boolean) {
            var lastNameReduced = ""
            if (player.lastName.contains(" ")) {
                val lastNames = player.lastName.split(" ")
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
                lastNameReduced = player.lastName
            }
            playerNameTextView.text = lastNameReduced
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
            val guessed = itemView.findViewById<ImageView>(R.id.card_guessed)
            val redCardTypeGuessed = itemView.findViewById<ImageView>(R.id.two_yellows_guessed)
            if (isStarted || isFinished) {
                guessed.visibility = VISIBLE
                if (yellowCardsGuessed.isEmpty() && redCardsGuessed.isEmpty()) {
                    guessed.setImageResource(R.drawable.wrong)
                }
                else {
                    if (cardColor == "Yellow") {
                        if (yellowCardsGuessed.contains(MVPPlayer(player.team, player.shirtNumber))) {
                            guessed.setImageResource(R.drawable.completed)
                        }
                        else {
                            guessed.setImageResource(R.drawable.wrong)
                        }
                    }
                    else {
                        if (redCardsGuessed.contains(MVPPlayer(player.team, player.shirtNumber))) {
                            guessed.setImageResource(R.drawable.completed)
                            redCardTypeGuessed.visibility = VISIBLE
                            if (redCardsGuessed[MVPPlayer(player.team, player.shirtNumber)] == true) {
                                redCardTypeGuessed.setImageResource(R.drawable.completed)
                            }
                            else {
                                redCardTypeGuessed.setImageResource(R.drawable.wrong)
                            }
                        }
                        else {
                            guessed.setImageResource(R.drawable.wrong)
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PredictedCardsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(resource, parent, false)
        return PredictedCardsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playersCardsList.size
    }

    override fun onBindViewHolder(holder: PredictedCardsViewHolder, position: Int) {
        holder.bind(playersCardsList.toList()[position].first, playersCardsList.toList()[position].second, redCardsTypes, yellowCardsGuessed, redCardsGuessed, isStarted, isFinished)
    }
}