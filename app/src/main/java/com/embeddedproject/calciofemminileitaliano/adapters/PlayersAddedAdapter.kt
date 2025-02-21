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

class PlayersAddedAdapter(private val playersAddedList: MutableList<Player>, private val teamBitmap: Bitmap? = null) : RecyclerView.Adapter<PlayersAddedAdapter.PlayersAddedViewHolder>() {

    class PlayersAddedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val firstNameTextView: TextView = itemView.findViewById(R.id.scorer_first_name)
        private val lastNameTextView: TextView = itemView.findViewById(R.id.scorer_last_name)
        private val numberTextView: TextView = itemView.findViewById(R.id.shirt_number)
        private val roleTextView: TextView = itemView.findViewById(R.id.role)
        private val teamImageView: ImageView = itemView.findViewById(R.id.team_image)

        @SuppressLint("DiscouragedApi")
        fun bind(player: Player, teamBitmap: Bitmap?) {
            firstNameTextView.text = player.firstName
            lastNameTextView.text = player.lastName
            numberTextView.text = player.shirtNumber.toString()
            roleTextView.text = itemView.resources.getString(itemView.resources.getIdentifier(player.role.lowercase(), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            if (teamBitmap != null) {
                teamImageView.setImageBitmap(teamBitmap)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayersAddedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.guest_scorer, parent, false)
        return PlayersAddedViewHolder(view)
    }

    override fun getItemCount(): Int {
        return playersAddedList.size
    }

    override fun onBindViewHolder(holder: PlayersAddedViewHolder, position: Int) {
        holder.bind(playersAddedList[position], teamBitmap)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun dataChanged() {
        notifyDataSetChanged()
    }
}