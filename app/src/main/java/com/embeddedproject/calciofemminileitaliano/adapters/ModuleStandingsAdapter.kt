package com.embeddedproject.calciofemminileitaliano.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R

class ModuleStandingsAdapter(private val modulesStandings: Map<String, Int>) : RecyclerView.Adapter<ModuleStandingsAdapter.ModuleStandingsViewHolder>() {

    inner class ModuleStandingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val moduleTextView: TextView = itemView.findViewById(R.id.module)
        private val moduleNumber: TextView = itemView.findViewById(R.id.module_number)

        fun bind(module: String, number: Int) {
            moduleTextView.text = module
            moduleNumber.text = number.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleStandingsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.module_standing_dialog, parent, false)
        return ModuleStandingsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return modulesStandings.size
    }

    override fun onBindViewHolder(holder: ModuleStandingsViewHolder, position: Int) {
        holder.bind(modulesStandings.keys.elementAt(position), modulesStandings.values.elementAt(position))
    }
}