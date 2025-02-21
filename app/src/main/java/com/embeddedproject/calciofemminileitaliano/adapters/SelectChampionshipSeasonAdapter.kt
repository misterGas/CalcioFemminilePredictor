package com.embeddedproject.calciofemminileitaliano.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.SelectChampionshipDirections
import com.embeddedproject.calciofemminileitaliano.helpers.SeasonPoints

class SelectChampionshipSeasonAdapter(private val championship: String, private val seasonsList: List<SeasonPoints>, private val user: String) : RecyclerView.Adapter<SelectChampionshipSeasonAdapter.SelectChampionshipSeasonViewHolder>() {

    class SelectChampionshipSeasonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val seasonRelativeLayout: RelativeLayout = itemView.findViewById(R.id.season_relative_layout)
        private val seasonInfo: TextView = itemView.findViewById(R.id.season_info)
        private val seasonPoints: TextView = itemView.findViewById(R.id.season_points)

        fun bind(championship: String, season: String, points: Int, user: String) {
            seasonInfo.text = season
            seasonPoints.text = points.toString()

            seasonRelativeLayout.setOnClickListener {
                val navigateToChampionship = SelectChampionshipDirections.actionSelectChampionshipToMatchesPredictions(user, championship, season)
                itemView.findNavController().navigate(navigateToChampionship)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectChampionshipSeasonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.championship_season, parent, false)
        return SelectChampionshipSeasonViewHolder(view)
    }

    override fun getItemCount(): Int {
        return seasonsList.size
    }

    override fun onBindViewHolder(holder: SelectChampionshipSeasonViewHolder, position: Int) {
        holder.bind(championship, seasonsList[position].season, seasonsList[position].points, user)
    }
}