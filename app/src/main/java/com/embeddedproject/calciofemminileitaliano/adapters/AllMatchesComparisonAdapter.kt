package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.google.firebase.database.DataSnapshot

class AllMatchesComparisonAdapter(private val actualUser: String, private val vsUser: String, private val championship: String, private val season: String, private val matchesList: List<List<MatchPredictor>>, private val rounds: List<String>, private val databaseGet: DataSnapshot) : RecyclerView.Adapter<AllMatchesComparisonAdapter.AllMatchesComparisonViewHolder>() {

    class AllMatchesComparisonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
        fun bind(matchDayComparisonAdapter: MatchDayComparisonAdapter, round: Int) {
            itemView.findViewById<RecyclerView>(R.id.recycler_view_day_comparison).adapter = matchDayComparisonAdapter
            val dayDescription = when (round) {
                in 1..100 -> { //regular season
                    "${itemView.resources.getString(R.string.regular_season)}\n${itemView.resources.getString(
                        R.string.day)} $round"
                }
                120 -> { //round of 16
                    itemView.resources.getString(R.string.round_16)
                }
                125 -> { //quarterfinals
                    itemView.resources.getString(R.string.quarterfinals)
                }
                150 -> { //semifinals
                    itemView.resources.getString(R.string.semifinals)
                }
                200 -> { //final
                    itemView.resources.getString(R.string.final_)
                }
                in 201..250 -> { //shield group
                    "${itemView.resources.getString(R.string.shield_group)}\n${itemView.resources.getString(
                        R.string.day)} ${round - 200}"
                }
                in 251..300 -> { //salvation group
                    "${itemView.resources.getString(R.string.salvation_group)}\n${itemView.resources.getString(
                        R.string.day)} ${round - 250}"
                }
                400 -> { //qualifications
                    itemView.resources.getString(R.string.qualifications)
                }
                in 401..499 -> { //qualifications
                    "${itemView.resources.getString(R.string.qualifications)}\n${itemView.resources.getString(
                        R.string.day)} ${round - 400}"
                }
                else -> { //other days
                    "${itemView.resources.getString(R.string.day)} $round"
                }
            }
            itemView.findViewById<TextView>(R.id.season_day).text = dayDescription
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllMatchesComparisonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_comparison, parent, false)
        return AllMatchesComparisonViewHolder(view)
    }

    override fun getItemCount(): Int {
        return matchesList.size
    }

    override fun onBindViewHolder(holder: AllMatchesComparisonViewHolder, position: Int) {
        val list = matchesList[position].sortedWith(compareBy({ it.dateNumbered }, { it.time }))
        holder.bind(MatchDayComparisonAdapter(actualUser, vsUser, championship, season, list, databaseGet), rounds[position].toInt())
    }
}