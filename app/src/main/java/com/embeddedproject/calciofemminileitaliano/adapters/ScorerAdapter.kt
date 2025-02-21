package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.GoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.google.firebase.database.DatabaseReference

class ScorerAdapter(context: Context, resource: Int, scorersList: List<Player>, private val databaseScorersReference: DatabaseReference, private var number: Int, private val goalBitmap: Bitmap, private val ownGoalBitmap: Bitmap, private val goalTeam: String) : ArrayAdapter<Player>(context, resource, scorersList) {

    private val resourceLayout = resource
    private var selectedPosition = -1

    @SuppressLint("DiscouragedApi")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resourceLayout, parent, false)

        val scorer = getItem(position)

        var teamSelected = ""

        scorer?.let {
            val scorerFirstName = view.findViewById<TextView>(R.id.scorer_first_name)
            val scorerLastName = view.findViewById<TextView>(R.id.scorer_last_name)
            val scorerShirt = view.findViewById<TextView>(R.id.shirt_number)
            val scorerRole = view.findViewById<TextView>(R.id.role)
            val teamImage = view.findViewById<ImageView>(R.id.team_image)

            scorerFirstName?.text = scorer.firstName
            scorerLastName?.text = scorer.lastName
            scorerShirt.text = scorer.shirtNumber.toString()
            scorerRole.text = view.resources.getString(view.resources.getIdentifier(scorer.role.lowercase(), "string", view.resources.getResourcePackageName(R.string.app_name)))
            if (scorer.team == goalTeam) {
                teamImage.setImageBitmap(goalBitmap)
                teamSelected = goalTeam
            }
            else {
                teamImage.setImageBitmap(ownGoalBitmap)
            }
        }

        val selectedColor = context.getColor(R.color.table_result_values)
        val transparentColor = context.getColor(android.R.color.transparent)

        if (position == selectedPosition) {
            view.setBackgroundColor(selectedColor)
            number = scorer!!.shirtNumber
            val goalType = if (teamSelected != "") {
                "Goal"
            }
            else {
                "Own goal"
            }
            databaseScorersReference.setValue(GoalOrOwnGoal(goalType, number)).addOnCompleteListener {}
        }
        else {
            view.setBackgroundColor(transparentColor)
        }
        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
}