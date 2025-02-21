package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import com.embeddedproject.calciofemminileitaliano.adapters.StandingsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.embeddedproject.calciofemminileitaliano.helpers.UserTotalPoints
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Standings : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_standings, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = StandingsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season

        val standingsFor = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n${getString(R.string.standings)}"
        view.findViewById<TextView>(R.id.championship_name_standings).text = standingsFor
        view.findViewById<TextView>(R.id.season_info).text = season

        view.findViewById<ImageView>(R.id.back_to_championship_prediction).setOnClickListener {
            val navigateToMatchesPredictions = StandingsDirections.actionStandingsToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = StandingsDirections.actionStandingsToLoginRegistration()
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

        reference.child("Championships").child(championship).child(season).child("TotalPoints").get().addOnCompleteListener {
            val totalPointsPerUser = mutableListOf<UserTotalPoints>()
            var pos = 0
            for (userNickname in it.result.children) {
                var userPoints = 0
                for (round in userNickname.children) {
                    for (matchPoints in round.children) {
                        userPoints += matchPoints.value.toString().toInt()
                    }
                }
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
            val standings = StandingsAdapter(user, totalPointsUpdatingPositions, championship, season)
            val showStandings = view.findViewById<RecyclerView>(R.id.recycler_view_users_standings)
            showStandings.adapter = standings
            view.findViewById<TextView>(R.id.actual_user_nickname).text = user
            view.findViewById<RelativeLayout>(R.id.actual_user_info).setOnClickListener {
                showStandings.scrollToPosition(i)
            }
        }

        searchNickname.doOnTextChanged { usersSearched, _, _, _ ->
            reference.child("Championships").child(championship).child(season).child("TotalPoints").get().addOnCompleteListener {
                val totalPointsPerUser = mutableListOf<UserTotalPoints>()
                for (userNickname in it.result.children) {
                    var userPoints = 0
                    for (round in userNickname.children) {
                        for (matchPoints in round.children) {
                            userPoints += matchPoints.value.toString().toInt()
                        }
                    }
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
                val standings = StandingsAdapter(user, totalPointsPerUser, championship, season)
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