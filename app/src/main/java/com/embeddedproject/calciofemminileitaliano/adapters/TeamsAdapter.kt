package com.embeddedproject.calciofemminileitaliano.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.embeddedproject.calciofemminileitaliano.R

class TeamsAdapter(context: Context, resource: Int, teams: List<String>, teamsTranslated: Map<String,String>) : ArrayAdapter<String>(context, resource, teams) {

    private val resourceLayout = resource
    private var selectedPosition = -1
    private val teamsTranslations = teamsTranslated

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resourceLayout, parent, false)

        val team = getItem(position)

        team.let {
            view.findViewById<TextView>(R.id.team_name).text = teamsTranslations[team]
        }

        if (position == selectedPosition) {
            view.setBackgroundColor(context.resources.getColor(R.color.objective))
        }
        else {
            view.setBackgroundColor(context.resources.getColor(android.R.color.transparent))
        }
        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
}