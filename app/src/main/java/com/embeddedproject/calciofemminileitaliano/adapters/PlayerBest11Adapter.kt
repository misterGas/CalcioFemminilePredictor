package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import java.io.File
import java.text.Normalizer

class PlayerBest11Adapter(context: Context, playersList: List<Player>, private val teamsMap: Map<String, Bitmap>, private val best11Reference: DatabaseReference, private val storageReference: StorageReference) : ArrayAdapter<Player>(context, R.layout.home_scorer, playersList) {

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
            val playerImageView = view.findViewById<ImageView>(R.id.player_image)

            scorerFirstName?.text = player.firstName
            scorerLastName?.text = player.lastName
            scorerShirt.text = player.shirtNumber.toString()
            scorerRole.text = view.resources.getString(view.resources.getIdentifier(player.role.lowercase(), "string", view.resources.getResourcePackageName(R.string.app_name)))
            teamImage.setImageBitmap(teamsMap[player.team])

            val selectedColor = context.getColor(R.color.table_result_values)
            val transparentColor = context.getColor(android.R.color.transparent)

            /*val fileName = "n_${sanitizeString(player.firstName.lowercase().replace(" ", "_"))}_s_${sanitizeString(player.lastName.lowercase().replace(" ", "_"))}.jpg"
            val imageRef = storageReference.child("PlayersImages/$fileName")

            Glide.with(view.context).clear(playerImageView)
            playerImageView.setImageDrawable(null)

            playerImageView.tag = fileName

            imageRef.downloadUrl.addOnSuccessListener {
                if (playerImageView.tag == fileName) {
                    Glide.with(view.context).load(it).into(playerImageView)
                }
            }*/

            if (position == selectedPosition) {
                view.setBackgroundColor(selectedColor)
                best11Reference.setValue(MVPPlayer(player.team, player.shirtNumber)).addOnCompleteListener {}
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

    fun sanitizeString(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val withoutAccents = normalized.replace("\\p{M}".toRegex(), "")
        return withoutAccents.replace("[^a-zA-Z0-9_]".toRegex(), "")
    }
}