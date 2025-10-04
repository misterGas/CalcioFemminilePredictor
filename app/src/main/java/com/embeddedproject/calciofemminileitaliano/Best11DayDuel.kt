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
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Best11DayDuel : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_best11_day_duel, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = Best11DayDuelArgs.fromBundle(requireArguments())
        val userNickname = arguments.userNickname
        val vsUser = arguments.vsUser
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val userNicknameModule = arguments.userNicknameModule
        val vsUserModule = arguments.vsUserModule

        view.findViewById<ImageView>(R.id.back_to_best11_standings).setOnClickListener {
            val navigateToBest11Standings = Best11DayDuelDirections.actionBest11DayDuelToBest11Standings(userNickname, championship, season, round, userNicknameModule)
            view.findNavController().navigate(navigateToBest11Standings)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(userNickname))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = Best11DayDuelDirections.actionBest11DayDuelToLoginRegistration()
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
            121 -> {
                getString(R.string.round_16_first_leg)
            }
            122 -> {
                getString(R.string.round_16_second_leg)
            }
            125 -> { //quarterfinals
                getString(R.string.quarterfinals)
            }
            126 -> {
                getString(R.string.quarterfinals_first_leg)
            }
            127 -> {
                getString(R.string.quarterfinals_second_leg)
            }
            150 -> { //semifinals
                getString(R.string.semifinals)
            }
            151 -> {
                getString(R.string.semifinals_first_leg)
            }
            152 -> {
                getString(R.string.semifinals_second_leg)
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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.best11_duel)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.best11_duel)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }

        view.findViewById<TextView>(R.id.season_info).text = season

        val userNicknameModuleId = userNicknameModule.replace("-", "")
        val vsUserModuleId = vsUserModule.replace("-","")

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

            val best11UserNicknamePredictionsGet = inDatabase.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions").child(userNickname)
            for (playerBest11UN in best11UserNicknamePredictionsGet.child("Players").children) {
                val position = playerBest11UN.key.toString().lowercase()
                val positionView = view.findViewById<RelativeLayout>(resources.getIdentifier("${position}_$userNicknameModuleId", "id", activity?.packageName))
                val shirtView = positionView.findViewById<TextView>(R.id.shirt_number)
                val teamView = positionView.findViewById<ImageView>(R.id.player_team_image)
                val captainRelativeLayout = positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)
                shirtView?.visibility = VISIBLE
                teamView?.visibility = VISIBLE
                val shirtNumber = playerBest11UN.child("shirt").value.toString()
                val team = playerBest11UN.child("team").value.toString()
                shirtView?.text = shirtNumber
                teamView?.setImageBitmap(teamsBitmap[team])
                if (best11UserNicknamePredictionsGet.hasChild("Captain")) {
                    val captainPosition = best11UserNicknamePredictionsGet.child("Captain").value.toString().lowercase()
                    if (position == captainPosition) {
                        captainRelativeLayout?.visibility = VISIBLE
                    }
                    else {
                        captainRelativeLayout?.visibility = GONE
                    }
                }
            }

            val best11VsUserPredictionsGet = inDatabase.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions").child(vsUser)
            for (playerBest11VU in best11VsUserPredictionsGet.child("Players").children) {
                val position = playerBest11VU.key.toString().lowercase()
                val positionView = view.findViewById<RelativeLayout>(resources.getIdentifier("${position}_${vsUserModuleId}_rotated", "id", activity?.packageName))
                val shirtView = positionView.findViewById<TextView>(R.id.shirt_number)
                val teamView = positionView.findViewById<ImageView>(R.id.player_team_image)
                val captainRelativeLayout = positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)
                shirtView?.visibility = VISIBLE
                teamView?.visibility = VISIBLE
                val shirtNumber = playerBest11VU.child("shirt").value.toString()
                val team = playerBest11VU.child("team").value.toString()
                shirtView?.text = shirtNumber
                teamView?.setImageBitmap(teamsBitmap[team])
                if (best11VsUserPredictionsGet.hasChild("Captain")) {
                    val captainPosition = best11VsUserPredictionsGet.child("Captain").value.toString().lowercase()
                    if (position == captainPosition) {
                        captainRelativeLayout?.visibility = VISIBLE
                    }
                    else {
                        captainRelativeLayout?.visibility = GONE
                    }
                }
            }

            view.findViewById<ProgressBar>(R.id.progress_updating_best11).visibility = INVISIBLE

            val userNicknameModuleLayout = view.findViewById<RelativeLayout>(resources.getIdentifier("layout_${userNicknameModuleId}_user_nickname", "id", activity?.packageName))
            userNicknameModuleLayout.visibility = VISIBLE

            val vsUserModuleLayout = view.findViewById<RelativeLayout>(resources.getIdentifier("layout_${vsUserModuleId}_vs_user", "id", activity?.packageName))
            vsUserModuleLayout.visibility = VISIBLE

            view.findViewById<TextView>(R.id.module_user_nickname_numbers).text = userNicknameModule
            view.findViewById<TextView>(R.id.module_user_nickname_name).text = userNickname
            view.findViewById<TextView>(R.id.module_vs_user_numbers).text = vsUserModule
            view.findViewById<TextView>(R.id.module_vs_user_name).text = vsUser
        }
    }
}