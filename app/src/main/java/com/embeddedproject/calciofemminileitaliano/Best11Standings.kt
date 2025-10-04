package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.Best11StandingsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.embeddedproject.calciofemminileitaliano.helpers.UserTotalPoints
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Best11Standings : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_best11_standings, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = Best11StandingsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val module = arguments.module

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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.best11_standings)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name_standings).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.best11_standings)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name_standings).text = resultDetails
        }

        view.findViewById<TextView>(R.id.season_info).text = season

        view.findViewById<ImageView>(R.id.back_to_best11).setOnClickListener {
            val navigateToBest11Prediction = Best11StandingsDirections.actionBest11StandingsToShowBest11(user, championship, season, round, module, true)
            view.findNavController().navigate(navigateToBest11Prediction)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = Best11StandingsDirections.actionBest11StandingsToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        val searchNickname = view.findViewById<EditText>(R.id.search_nickname)

        val savePositions = mutableMapOf<String, Int>()

        reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).get().addOnCompleteListener {
            val totalPointsPerUser = mutableListOf<UserTotalPoints>()
            var pos = 0
            for (userNickname in it.result.child("Best11PredictionsPoints").children) {
                val userPoints = userNickname.child("TotalPoints").value.toString().toInt()
                val userTotalPointsToAdd = UserTotalPoints(userNickname.key.toString(), userPoints)
                totalPointsPerUser.add(userTotalPointsToAdd)
            }
            totalPointsPerUser.sortByDescending { it2 ->
                it2.getTotalPoints()
            }
            val totalPointsUpdatingPositions = mutableListOf<UserTotalPoints>()
            for (u in totalPointsPerUser) {
                pos++
                savePositions[u.getUserNickname()] = pos
                totalPointsUpdatingPositions.add(UserTotalPoints(u.getUserNickname(), u.getTotalPoints(), pos))
            }
            var i = 0
            for (u in totalPointsUpdatingPositions) {
                i++
                if (u.getUserNickname() == user) {
                    view.findViewById<TextView>(R.id.actual_user_standing_position).text = i.toString()
                    view.findViewById<TextView>(R.id.actual_user_points).text = u.getTotalPoints().toString()
                    break
                }
            }
            val standings = Best11StandingsAdapter(user, totalPointsUpdatingPositions, championship, season, round, module, it.result.child("Best11Predictions"))
            val showStandings = view.findViewById<RecyclerView>(R.id.recycler_view_users_standings)
            showStandings.adapter = standings
            view.findViewById<TextView>(R.id.actual_user_nickname).text = user
            view.findViewById<RelativeLayout>(R.id.actual_user_info).setOnClickListener {
                showStandings.scrollToPosition(i)
            }
        }

        searchNickname.doOnTextChanged { usersSearched, _, _, _ ->
            reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).get().addOnCompleteListener {
                val totalPointsPerUser = mutableListOf<UserTotalPoints>()
                for (userNickname in it.result.child("Best11PredictionsPoints").children) {
                    val userPoints = userNickname.child("TotalPoints").value.toString().toInt()
                    if (usersSearched.toString().isEmpty()) {
                        totalPointsPerUser.add(UserTotalPoints(userNickname.key.toString(), userPoints, savePositions[userNickname.key.toString()]!!))
                    }
                    else if (userNickname.key.toString().contains(usersSearched.toString(), true)) {
                        totalPointsPerUser.add(UserTotalPoints(userNickname.key.toString(), userPoints, savePositions[userNickname.key.toString()]!!))
                    }
                }
                totalPointsPerUser.sortByDescending { it2 ->
                    it2.getTotalPoints()
                }
                var i = 0
                for (u in totalPointsPerUser) {
                    i++
                    if (u.getUserNickname() == user) {
                        break
                    }
                }
                val standings = Best11StandingsAdapter(user, totalPointsPerUser, championship, season, round, module, it.result.child("Best11Predictions"))
                activity?.runOnUiThread {
                    val showStandings = view.findViewById<RecyclerView>(R.id.recycler_view_users_standings)
                    showStandings.adapter = standings
                    view.findViewById<TextView>(R.id.actual_user_nickname).text = user
                    view.findViewById<RelativeLayout>(R.id.actual_user_info).setOnClickListener {
                        showStandings.scrollToPosition(i)
                    }
                }
            }
        }
    }
}