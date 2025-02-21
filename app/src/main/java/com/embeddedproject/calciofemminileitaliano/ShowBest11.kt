package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.PlayersBest11Adapter
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ShowBest11 : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_show_best11, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = ShowBest11Args.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val module = arguments.module

        view.findViewById<ImageView>(R.id.back_to_championship_prediction).setOnClickListener {
            val navigateToMatchesPredictions = ShowBest11Directions.actionShowBest11ToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = ShowBest11Directions.actionShowBest11ToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        var dayDescription = when (round) {
            in 1..100 -> { //regular season
                "${getString(R.string.regular_season)}\n${view.resources.getString(R.string.day)} $round"
            }
            120 -> { //round of 16
                getString(R.string.round_16)
            }
            125 -> { //quarterfinals
                getString(R.string.quarterfinals)
            }
            150 -> { //semifinals
                getString(R.string.semifinals)
            }
            200 -> { //final
                getString(R.string.final_)
            }
            in 201..250 -> { //shield group
                "${getString(R.string.shield_group)}\n${getString(R.string.day)} ${round - 200}"
            }
            in 251..300 -> { //salvation group
                "${getString(R.string.salvation_group)}\n${getString(R.string.day)} ${round - 250}"
            }
            400 -> { //qualifications
                getString(R.string.qualifications)
            }
            in 401..499 -> { //qualifications
                "${getString(R.string.qualifications)}\n${getString(R.string.day)} ${round - 400}"
            }
            else -> { //other days
                "${getString(R.string.day)} $round"
            }
        }

        val configuration = resources.configuration
        dayDescription = dayDescription.replace("\n", " (")
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.best11)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.best11)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }

        view.findViewById<TextView>(R.id.season_info).text = season
        view.findViewById<TextView>(R.id.module).text = module

        val moduleId = module.replace("-", "")

        val best11RecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_players_best11)

        reference.get().addOnCompleteListener { inDatabase ->
            val teamsBitmap = mutableMapOf<String, Bitmap>()
            val teamsGet = inDatabase.result.child("Championships").child(championship).child(season).child("Teams")
            for (team in teamsGet.children) {
                val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team.key.toString()))
                if (setTeamImage.moveToFirst()) {
                    val bitmap = BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size)
                    teamsBitmap[team.key.toString()] = bitmap
                }
                setTeamImage.close()
            }

            val best11PredictionsGet = inDatabase.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions").child(user)
            val playersGet = inDatabase.result.child("Players").child(season)
            var playersInBest11 = mutableListOf<Player>()
            for (playerBest11 in best11PredictionsGet.child("Players").children) {
                val position = playerBest11.key.toString().lowercase()
                val positionView = view.findViewById<RelativeLayout>(resources.getIdentifier("${position}_$moduleId", "id", activity?.packageName))
                val shirtView = positionView.findViewById<TextView>(R.id.shirt_number)
                val teamView = positionView.findViewById<ImageView>(R.id.player_team_image)
                shirtView?.visibility = VISIBLE
                teamView?.visibility = VISIBLE
                val shirtNumber = playerBest11.child("shirt").value.toString()
                val team = playerBest11.child("team").value.toString()
                shirtView?.text = shirtNumber
                teamView?.setImageBitmap(teamsBitmap[team])
                val findPlayer = playersGet.child(team).child(shirtNumber)
                val playerFirstName = findPlayer.child("firstName").value.toString()
                val playerLastName = findPlayer.child("lastName").value.toString()
                val playerRole = findPlayer.child("role").value.toString()
                val newBest11Player = Player(playerFirstName, playerLastName, shirtNumber.toInt(), playerRole, team)
                playersInBest11.add(newBest11Player)

                positionView.setOnClickListener {
                    best11RecyclerView.smoothScrollToPosition(playersInBest11.indexOf(newBest11Player))
                }
            }

            playersInBest11 = playersInBest11.sortedWith(compareBy({ p -> if (p.role == "Goalkeeper") 0 else 1 }, { p -> if (p.role == "Defender") 0 else 1 }, { p -> if (p.role == "Midfielder") 0 else 1 })).toMutableList()

            view.findViewById<ProgressBar>(R.id.progress_updating_best11).visibility = INVISIBLE

            val moduleLayout = view.findViewById<RelativeLayout>(resources.getIdentifier("layout_$moduleId", "id", activity?.packageName))
            moduleLayout.visibility = VISIBLE

            best11RecyclerView.visibility = VISIBLE

            val best11Adapter = PlayersBest11Adapter(playersInBest11, teamsBitmap)
            best11RecyclerView?.adapter = best11Adapter
        }
    }
}