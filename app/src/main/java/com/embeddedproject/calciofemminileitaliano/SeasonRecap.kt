package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.ScorersStandingsAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.SeasonResultsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.ScorerStanding
import com.embeddedproject.calciofemminileitaliano.helpers.TeamMatch
import com.embeddedproject.calciofemminileitaliano.helpers.TeamResults
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SeasonRecap : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_season_recap, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = SeasonRecapArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season

        val seasonRecapInfo = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n${getString(R.string.season_recap)}"
        view.findViewById<TextView>(R.id.championship_name_season_recap).text = seasonRecapInfo
        view.findViewById<TextView>(R.id.season_info).text = season

        view.findViewById<ImageView>(R.id.back_to_championship_prediction).setOnClickListener {
            val navigateToMatchesPredictions = SeasonRecapDirections.actionSeasonRecapToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = SeasonRecapDirections.actionSeasonRecapToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        val teamsResultsRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_results)

        reference.get().addOnCompleteListener { databaseReference ->
            val championshipReference = databaseReference.result.child("Championships").child(championship).child(season)
            val teamsReference = championshipReference.child("Teams")
            val matchesReference = championshipReference.child("Matches")
            var allTeamsResults = mutableListOf<TeamResults>()
            val scorersStandings = mutableMapOf<ScorerStanding, Int>()

            for (t in teamsReference.children) {
                val team = t.key.toString()
                var played = 0
                var goalsScored = 0
                var goalsSuffered = 0
                var wins = 0
                var nulls = 0
                var lost = 0
                val teamMatches = mutableListOf<TeamMatch>()

                for (r in matchesReference.children) {
                    for (m in r.child("Matches").children) {
                        if (m.hasChild("Finished")) {
                            val findActualTeamInMatch = m.key.toString().split("-")
                            if (findActualTeamInMatch.contains(team)) {
                                played++
                                val matchInfo = m.child("MatchInfo")
                                val (scored, suffered, vsTeam) = if (findActualTeamInMatch[0] == team) {
                                    Triple(
                                        matchInfo.child("homeScore").value.toString().toInt(),
                                        matchInfo.child("guestScore").value.toString().toInt(),
                                        findActualTeamInMatch[1]
                                    )
                                } else {
                                    Triple(
                                        matchInfo.child("guestScore").value.toString().toInt(),
                                        matchInfo.child("homeScore").value.toString().toInt(),
                                        findActualTeamInMatch[0]
                                    )
                                }
                                val location = if (findActualTeamInMatch[0] == team) "HOME" else "AWAY"

                                goalsScored += scored
                                goalsSuffered += suffered

                                val outcome = when {
                                    scored > suffered -> {
                                        wins++
                                        "WIN"
                                    }
                                    scored == suffered -> {
                                        nulls++
                                        "NULL"
                                    }
                                    else -> {
                                        lost++
                                        "LOST"
                                    }
                                }
                                if (location == "HOME") {
                                    val teamMatch = TeamMatch(vsTeam, outcome, location, scored, suffered)
                                    teamMatches.add(teamMatch)
                                }
                                else {
                                    val teamMatch = TeamMatch(vsTeam, outcome, location, suffered, scored)
                                    teamMatches.add(teamMatch)
                                }
                            }
                            if (m.hasChild("OfficialScorers")) {
                                val findOfficialScorers = m.child("OfficialScorers")
                                for (teamScorers in findOfficialScorers.children) {
                                    for (scorer in teamScorers.children) {
                                        if (scorer.child("goalType").value.toString() == "Goal") {
                                            val teamName = teamScorers.key.toString()
                                            val shirt = scorer.child("shirt").value.toString().toInt()
                                            val scorerInfo = ScorerStanding(teamName, shirt)
                                            if (scorersStandings.contains(scorerInfo)) {
                                                scorersStandings[scorerInfo] = scorersStandings[scorerInfo]!! + 1
                                            }
                                            else {
                                                scorersStandings[scorerInfo] = 1
                                            }
                                        }
                                    }
                                }
                            }
                            val sortedScorersStandings = scorersStandings.toList().sortedByDescending { it.second }.toMap()
                            view.findViewById<ImageView>(R.id.scorers_standings).setOnClickListener {
                                val dialogView = layoutInflater.inflate(R.layout.scorers_standing, null)
                                val scorersRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_scorers_standings)
                                scorersRecyclerView.adapter = ScorersStandingsAdapter(sortedScorersStandings, databaseReference.result, season)

                                val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                                    .setTitle(getString(R.string.scorers_standings))
                                    .setPositiveButton(R.string.ok, null)
                                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .create()

                                dialog.show()
                            }
                        }
                    }
                }

                val newTeamResults = TeamResults(team, played, wins * 3 + nulls, wins, nulls, lost, goalsScored, goalsSuffered, teamMatches)
                allTeamsResults.add(newTeamResults)
            }

            fun TeamResults.getDirectMatchGoalDifference(opponent: String): Int {
                return teamMatches
                    .filter { it.vsTeam == opponent }
                    .sumOf { (if (it.location == "HOME") it.homeScore - it.guestScore else it.guestScore - it.homeScore) }
            }

            allTeamsResults = allTeamsResults.sortedWith(
                compareByDescending<TeamResults> { it.totalPoints }
                    .thenByDescending { teamA ->
                        allTeamsResults
                            .filter { it.totalPoints == teamA.totalPoints && it.team != teamA.team }
                            .sumOf { teamB -> teamA.getDirectMatchGoalDifference(teamB.team) }
                    }
                    .thenByDescending { it.goalsScored - it.goalsSuffered }
                    .thenByDescending { it.goalsScored }
            ).toMutableList()

            view.findViewById<ProgressBar>(R.id.progress_updating_season_results).visibility = INVISIBLE
            val seasonRecapAdapter = SeasonResultsAdapter(allTeamsResults)
            teamsResultsRecyclerView.adapter = seasonRecapAdapter
        }
    }
}