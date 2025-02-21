package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
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
import com.embeddedproject.calciofemminileitaliano.helpers.SeasonPoints

class SelectChampionshipAdapter(private val championshipsList: List<String>, private val championshipSeasonsList: List<List<SeasonPoints>>, private val user: String) : RecyclerView.Adapter<SelectChampionshipAdapter.SelectChampionshipViewHolder>() {

    class SelectChampionshipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val championshipInfo: RelativeLayout = itemView.findViewById(R.id.championship_name_info)
        private val championshipName: TextView = itemView.findViewById(R.id.championship_name)
        private val seasons: RelativeLayout = itemView.findViewById(R.id.seasons)
        private val championshipSeasonRecyclerView: RecyclerView = itemView.findViewById(R.id.recycler_view_championship_seasons)

        @SuppressLint("DiscouragedApi")
        fun bind(championship: String, seasonsAdapter: SelectChampionshipSeasonAdapter) {
            championshipName.text = itemView.resources.getString(itemView.resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", itemView.resources.getResourcePackageName(R.string.app_name)))

            championshipInfo.setOnClickListener {
                val openSeasons = itemView.findViewById<ImageView>(R.id.open_championship)
                if (seasons.visibility == GONE) {
                    seasons.visibility = VISIBLE
                    openSeasons.setImageResource(R.drawable.arrow_down)
                    championshipSeasonRecyclerView.adapter = seasonsAdapter
                }
                else {
                    seasons.visibility = GONE
                    openSeasons.setImageResource(R.drawable.arrow_right)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectChampionshipViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.championship_info, parent, false)
        return SelectChampionshipViewHolder(view)
    }

    override fun getItemCount(): Int {
        return championshipsList.size
    }

    override fun onBindViewHolder(holder: SelectChampionshipViewHolder, position: Int) {
        holder.bind(championshipsList[position], SelectChampionshipSeasonAdapter(championshipsList[position], championshipSeasonsList[position], user))
    }
}