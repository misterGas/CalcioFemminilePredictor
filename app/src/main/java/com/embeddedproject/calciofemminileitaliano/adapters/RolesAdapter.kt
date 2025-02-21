package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.embeddedproject.calciofemminileitaliano.R

class RolesAdapter(context: Context, resource: Int, roles: List<String>) : ArrayAdapter<String>(context, resource, roles) {

    private val resourceLayout = resource
    private var selectedPosition = -1

    @SuppressLint("DiscouragedApi")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resourceLayout, parent, false)

        val role = getItem(position)

        role.let {
            view.findViewById<TextView>(R.id.role_info).text = view.resources.getString(view.resources.getIdentifier("${role?.lowercase()}_info", "string", view.resources.getResourcePackageName(R.string.app_name)))
        }

        if (position == selectedPosition) {
            view.setBackgroundColor(context.resources.getColor(R.color.table_result_values))
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