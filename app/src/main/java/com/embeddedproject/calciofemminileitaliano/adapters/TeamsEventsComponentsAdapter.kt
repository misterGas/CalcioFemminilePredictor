package com.embeddedproject.calciofemminileitaliano.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R

class TeamsEventsComponentsAdapter(private val captain: String, private val components: List<String>, private val actualUser: String? = null) : RecyclerView.Adapter<TeamsEventsComponentsAdapter.TeamsEventsComponentsViewHolder>() {

    class TeamsEventsComponentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val componentNicknameTextView = itemView.findViewById<TextView>(R.id.component_nickname)
        private val showCaptainRelativeLayout = itemView.findViewById<RelativeLayout>(R.id.captain_relative_layout)
        private val background = itemView.findViewById<RelativeLayout>(R.id.component_relative_layout)

        fun bind(captain: String, component: String, actualUser: String?, position: Int) {
            componentNicknameTextView.text = component
            if (captain == component) {
                showCaptainRelativeLayout.visibility = VISIBLE
            }
            else {
                showCaptainRelativeLayout.visibility = GONE
            }

            if (position % 2 == 0) {
                background.setBackgroundColor(itemView.resources.getColor(android.R.color.holo_blue_bright))
            }
            else {
                background.setBackgroundColor(itemView.resources.getColor(android.R.color.holo_green_light))
            }

            if (actualUser != null) {
                if (actualUser == component) {
                    background.setBackgroundColor(itemView.resources.getColor(android.R.color.holo_orange_light))
                }
                else {
                    background.setBackgroundColor(itemView.resources.getColor(android.R.color.holo_blue_bright))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamsEventsComponentsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_component, parent, false)
        return TeamsEventsComponentsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return components.size
    }

    override fun onBindViewHolder(holder: TeamsEventsComponentsViewHolder, position: Int) {
        holder.bind(captain, components[position], actualUser, position)
    }
}