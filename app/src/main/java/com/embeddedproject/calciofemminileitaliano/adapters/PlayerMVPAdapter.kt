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
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.google.firebase.database.DatabaseReference

class PlayerMVPAdapter(context: Context, playersList: List<Player>, private val mvpReference: DatabaseReference, private val homeBitmap: Bitmap, private val guestBitmap: Bitmap, private val homeTeam: String, private val insertNeedsConfirmation: Boolean = false) : ArrayAdapter<Player>(context, R.layout.home_scorer, playersList) {

    private val resourceLayout = R.layout.home_scorer
    private var selectedPosition = -1

    @SuppressLint("DiscouragedApi")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: LayoutInflater.from(context).inflate(resourceLayout, parent, false)

        val player = getItem(position)

        player?.let {
            val scorerFirstName = view.findViewById<TextView>(R.id.scorer_first_name)
            val scorerLastName = view.findViewById<TextView>(R.id.scorer_last_name)
            val scorerShirt = view.findViewById<TextView>(R.id.shirt_number)
            val scorerRole = view.findViewById<TextView>(R.id.role)
            val teamImage = view.findViewById<ImageView>(R.id.team_image)

            scorerFirstName?.text = player.firstName
            scorerLastName?.text = player.lastName
            scorerShirt.text = player.shirtNumber.toString()
            scorerRole.text = view.resources.getString(view.resources.getIdentifier(player.role.lowercase(), "string", view.resources.getResourcePackageName(R.string.app_name)))
            if (player.team == homeTeam) {
                teamImage.setImageBitmap(homeBitmap)
            }
            else {
                teamImage.setImageBitmap(guestBitmap)
            }

            val selectedColor = context.getColor(R.color.table_result_values)
            val transparentColor = context.getColor(android.R.color.transparent)

            if (position == selectedPosition) {
                view.setBackgroundColor(selectedColor)
                if (!insertNeedsConfirmation) {
                    mvpReference.setValue(MVPPlayer(player.team, player.shirtNumber)).addOnCompleteListener {}
                }
            }
            else {
                view.setBackgroundColor(transparentColor)
            }
        }
        return view
    }

    fun setSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
}