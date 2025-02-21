package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import java.util.stream.IntStream.range

class OfficialScorersAdapter(private val scorersList: List<Player>, private val scorerTypes: List<String>, private val timelinesList: Map<Player, String>, private val resource: Int) : RecyclerView.Adapter<OfficialScorersAdapter.OfficialScorersViewHolder>() {

    class OfficialScorersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val scorerNameTextView: TextView = itemView.findViewById(R.id.scorer)

        @SuppressLint("DiscouragedApi")
        fun bind(scorer: Player, scorerType: String, timeline: String) {
            val type = if (scorerType == "Own goal") {
                itemView.resources.getString(R.string.own_goal)
            } else {
                ""
            }
            var name = "${scorer.firstName[0]}."
            var lastNameReduced = ""
            if (scorer.lastName.contains(" ")) {
                val lastNames = scorer.lastName.split(" ")
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
                lastNameReduced = scorer.lastName
            }
            name = "$name $lastNameReduced"
            if (timeline != "null") {
                val minutes = timeline.split(" ")
                name = "$name ${minutes[0]}'"
                for (i in range(1, minutes.size)) {
                    name = "$name, ${minutes[i]}'"
                }
            }
            if (type != "") {
                name = "$name $type"
            }
            scorerNameTextView.text = name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfficialScorersViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(resource, parent, false)
        return OfficialScorersViewHolder(view)
    }

    override fun getItemCount(): Int {
        return scorersList.size
    }

    override fun onBindViewHolder(holder: OfficialScorersViewHolder, position: Int) {
        holder.bind(scorersList[position], scorerTypes[position], timelinesList[scorersList[position]]!!)
    }
}