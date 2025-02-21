package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.embeddedproject.calciofemminileitaliano.R

class SeasonsAdapter(context: Context, resource: Int, seasons: List<String>, private val textColor: Int = R.color.black) : ArrayAdapter<String>(context, resource, seasons) {

    private val resourceLayout = resource
    private var selectedPosition = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resourceLayout, parent, false)

        val season = getItem(position)

        season.let {
            view.findViewById<TextView>(R.id.season_info).setTextColor(context.resources.getColor(textColor))
            view.findViewById<TextView>(R.id.season_info).text = season
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