package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.Player

class PlayersBest11Adapter(private val playersBest11: List<Player>, private val best11TeamsBitmaps: Map<String, Bitmap>) : RecyclerView.Adapter<PlayersBest11Adapter.PlayersBest11ViewHolder>() {

    class PlayersBest11ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val firstNameTextView: TextView = itemView.findViewById(R.id.scorer_first_name)
        private val lastNameTextView: TextView = itemView.findViewById(R.id.scorer_last_name)
        private val numberTextView: TextView = itemView.findViewById(R.id.shirt_number)
        private val roleTextView: TextView = itemView.findViewById(R.id.role)
        private val teamImageView: ImageView = itemView.findViewById(R.id.team_image)

        @SuppressLint("DiscouragedApi")
        fun bind(player: Player, best11TeamsBitmaps: Map<String, Bitmap>) {
            firstNameTextView.text = player.firstName
            lastNameTextView.text = player.lastName
            numberTextView.text = player.shirtNumber.toString()
            roleTextView.text = itemView.resources.getString(itemView.resources.getIdentifier(player.role.lowercase(), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            teamImageView.setImageBitmap(best11TeamsBitmaps[player.team])
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersBest11ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.home_scorer, parent, false)
        return PlayersBest11ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playersBest11.size
    }

    override fun onBindViewHolder(holder: PlayersBest11ViewHolder, position: Int) {
        holder.bind(playersBest11[position], best11TeamsBitmaps)
    }
}