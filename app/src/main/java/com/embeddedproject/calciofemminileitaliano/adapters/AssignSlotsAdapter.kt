package com.embeddedproject.calciofemminileitaliano.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.Slot

class AssignSlotsAdapter(context: Context, resource: Int, slots: List<Slot>, private val slotsAssigned: MutableList<Int>) : ArrayAdapter<Slot>(context, resource, slots) {

    private val resourceLayout = resource
    private var selectedPosition = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resourceLayout, parent, false)

        val slot = getItem(position)

        slot.let {
            view.findViewById<TextView>(R.id.assign_net).text = slot?.slotDescription
        }

        if (slotsAssigned.contains(position + 1)) {
            view.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
        }
        else {
            view.findViewById<ImageView>(R.id.assigned).visibility = INVISIBLE
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