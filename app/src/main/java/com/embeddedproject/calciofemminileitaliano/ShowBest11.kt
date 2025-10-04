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
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.PlayersBest11Adapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
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
        val showPoints = arguments.showPoints

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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.best11)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.best11)}"
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
            var captainPosition : String? = null
            var captainPlayer : MVPPlayer? = null
            val playerPositionsInBest11 = mutableMapOf<MVPPlayer,String>()
            for (playerBest11 in best11PredictionsGet.child("Players").children) {
                val position = playerBest11.key.toString().lowercase()
                val positionView = view.findViewById<RelativeLayout>(resources.getIdentifier("${position}_$moduleId", "id", activity?.packageName))
                val shirtView = positionView.findViewById<TextView>(R.id.shirt_number)
                val teamView = positionView.findViewById<ImageView>(R.id.player_team_image)
                val captainRelativeLayout = positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)
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
                if (best11PredictionsGet.hasChild("Captain")) {
                    captainPosition = best11PredictionsGet.child("Captain").value.toString().lowercase()
                    if (position == captainPosition) {
                        captainPlayer = MVPPlayer(team, shirtNumber.toInt())
                        captainRelativeLayout?.visibility = VISIBLE
                    }
                    else {
                        captainRelativeLayout?.visibility = GONE
                    }
                }
                playersInBest11.add(newBest11Player)
                playerPositionsInBest11[MVPPlayer(team,shirtNumber.toInt())] = playerBest11.key.toString()

                positionView.setOnClickListener {
                    best11RecyclerView.smoothScrollToPosition(playersInBest11.indexOf(newBest11Player))
                }
            }
            if (showPoints) {
                view.findViewById<ImageView>(R.id.standings).visibility = VISIBLE
                val best11PointsRules = mutableMapOf<String,Map<String,Int>>()
                val best11GoalkeeperPointsRules = mutableMapOf<String,Int>()
                best11GoalkeeperPointsRules["MVP"] = 5
                best11GoalkeeperPointsRules["OwnGoal"] = -2
                best11GoalkeeperPointsRules["YellowCard"] = -1
                best11GoalkeeperPointsRules["RedCard"] = -2
                best11PointsRules["G"] = best11GoalkeeperPointsRules
                val best11DefenderPointsRules = mutableMapOf<String,Int>()
                best11DefenderPointsRules["OneGoal"] = 5
                best11DefenderPointsRules["OneMoreGoal"] = 3
                best11DefenderPointsRules["MVP"] = 3
                best11DefenderPointsRules["OwnGoal"] = -1
                best11DefenderPointsRules["YellowCard"] = -1
                best11DefenderPointsRules["RedCard"] = -2
                best11PointsRules["D"] = best11DefenderPointsRules
                val best11MidfielderPointsRules = mutableMapOf<String,Int>()
                best11MidfielderPointsRules["OneGoal"] = 4
                best11MidfielderPointsRules["OneMoreGoal"] = 2
                best11MidfielderPointsRules["MVP"] = 3
                best11MidfielderPointsRules["OwnGoal"] = -1
                best11MidfielderPointsRules["YellowCard"] = -1
                best11MidfielderPointsRules["RedCard"] = -2
                best11PointsRules["M"] = best11MidfielderPointsRules
                val best11ForwardPointsRules = mutableMapOf<String,Int>()
                best11ForwardPointsRules["OneGoal"] = 3
                best11ForwardPointsRules["OneMoreGoal"] = 2
                best11ForwardPointsRules["MVP"] = 3
                best11ForwardPointsRules["OwnGoal"] = -1
                best11ForwardPointsRules["YellowCard"] = -1
                best11ForwardPointsRules["RedCard"] = -2
                best11PointsRules["F"] = best11ForwardPointsRules
                val roundGet = inDatabase.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString())
                if (!roundGet.hasChild("Best11PredictionsPoints")) {
                    for (u in roundGet.child("Best11Predictions").children) {
                        var totalPoints = 0
                        val best11User = u.key.toString()
                        val insertPointsDetailsInDatabase = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11PredictionsPoints").child(best11User)
                        for (p in u.child("Players").children) {
                            val position = p.key.toString()
                            val role = position[0].toString()
                            val best11UserPlayerShirt = p.child("shirt").value.toString()
                            val best11UserPlayerTeam = p.child("team").value.toString()
                            val roundMatchesGet = roundGet.child("Matches")
                            for (m in roundMatchesGet.children) {
                                val teamsMatch = m.key.toString().split("-")
                                if (teamsMatch.contains(best11UserPlayerTeam)) {
                                    val otherTeam = if (teamsMatch[0] == best11UserPlayerTeam) {
                                        teamsMatch[1]
                                    }
                                    else {
                                        teamsMatch[0]
                                    }
                                    var goalsScored = 0
                                    var scorerFound = false
                                    var ownGoalDone = false
                                    var playerIsMatchMVP = false
                                    var playerHasBeenGivenYellowCard = false
                                    var playerHasBeenGivenRedCard = false
                                    for (scorer in m.child("OfficialScorers").child(best11UserPlayerTeam).children) {
                                        val scorerShirt = scorer.child("shirt").value.toString()
                                        val goalType = scorer.child("goalType").value.toString()
                                        if (scorerShirt.toInt() == best11UserPlayerShirt.toInt() && goalType == "Goal") {
                                            if (!scorerFound) {
                                                scorerFound = true
                                            }
                                            goalsScored++
                                        }
                                    }
                                    for (ownGoal in m.child("OfficialScorers").child(otherTeam).children) {
                                        val scorerShirt = ownGoal.child("shirt").value.toString()
                                        val goalType = ownGoal.child("goalType").value.toString()
                                        if (scorerShirt.toInt() == best11UserPlayerShirt.toInt() && goalType != "Goal") {
                                            ownGoalDone = true
                                            break
                                        }
                                    }
                                    if (m.hasChild("OfficialMVP")) {
                                        val matchMVP = m.child("OfficialMVP")
                                        val matchMVPTeam = matchMVP.child("team").value.toString()
                                        val matchMVPShirt = matchMVP.child("shirt").value.toString()
                                        if (matchMVPTeam == best11UserPlayerTeam && matchMVPShirt.toInt() == best11UserPlayerShirt.toInt()) {
                                            playerIsMatchMVP = true
                                        }
                                    }
                                    for (yc in m.child("OfficialDiscipline").child(best11UserPlayerTeam).child("YellowCards").children) {
                                        val yellowCardShirt = yc.child("shirt").value.toString()
                                        if (yellowCardShirt.toInt() == best11UserPlayerShirt.toInt()) {
                                            playerHasBeenGivenYellowCard = true
                                            break
                                        }
                                    }
                                    for (rc in m.child("OfficialDiscipline").child(best11UserPlayerTeam).child("RedCards").children) {
                                        val redCardShirt = rc.child("shirt").value.toString()
                                        if (redCardShirt.toInt() == best11UserPlayerShirt.toInt()) {
                                            playerHasBeenGivenRedCard = true
                                            break
                                        }
                                    }
                                    val playerRolePointsRules = best11PointsRules[role]
                                    var playerPoints = 0
                                    if (role != "G") {
                                        if (scorerFound) {
                                            playerPoints += playerRolePointsRules?.get("OneGoal")!!.toInt()
                                            insertPointsDetailsInDatabase.child("Details").child(position).child("OneGoal").setValue(true)
                                            if (goalsScored > 1) {
                                                playerPoints += playerRolePointsRules["OneMoreGoal"]!!.toInt() * (goalsScored - 1)
                                                insertPointsDetailsInDatabase.child("Details").child(position).child("OneMoreGoal").setValue("x${goalsScored - 1}")
                                            }
                                        }
                                    }
                                    if (playerIsMatchMVP) {
                                        playerPoints += playerRolePointsRules?.get("MVP")!!.toInt()
                                        insertPointsDetailsInDatabase.child("Details").child(position).child("MVP").setValue(true)
                                    }
                                    if (ownGoalDone) {
                                        playerPoints += playerRolePointsRules?.get("OwnGoal")!!.toInt()
                                        insertPointsDetailsInDatabase.child("Details").child(position).child("OwnGoal").setValue(true)
                                    }
                                    if (playerHasBeenGivenYellowCard) {
                                        playerPoints += playerRolePointsRules?.get("YellowCard")!!.toInt()
                                        insertPointsDetailsInDatabase.child("Details").child(position).child("YellowCard").setValue(true)
                                    }
                                    if (playerHasBeenGivenRedCard) {
                                        playerPoints += playerRolePointsRules?.get("RedCard")!!.toInt()
                                        insertPointsDetailsInDatabase.child("Details").child(position).child("RedCard").setValue(true)
                                    }
                                    if (captainPosition != null && captainPosition == position.lowercase() && playerPoints > 0) {
                                        playerPoints *= 2
                                        insertPointsDetailsInDatabase.child("Details").child(position).child("isCaptain").setValue(true)
                                    }
                                    totalPoints += playerPoints
                                }
                            }
                        }
                        insertPointsDetailsInDatabase.child("TotalPoints").setValue(totalPoints)
                    }
                }
                reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11PredictionsPoints").child(user).get().addOnCompleteListener { best11PredictionsPointsGet ->
                    view.findViewById<RelativeLayout>(R.id.best11_points_rel_layout).visibility = VISIBLE
                    val best11PointsText = best11PredictionsPointsGet.result.child("TotalPoints").value.toString()
                    view.findViewById<TextView>(R.id.best11_points).text = best11PointsText
                    playersInBest11 = playersInBest11.sortedWith(compareBy({ p -> if (p.role == "Goalkeeper") 0 else 1 }, { p -> if (p.role == "Defender") 0 else 1 }, { p -> if (p.role == "Midfielder") 0 else 1 })).toMutableList()
                    best11RecyclerView.visibility = VISIBLE
                    val best11Adapter = PlayersBest11Adapter(playersInBest11, teamsBitmap, R.layout.best11_points_player, captainPlayer, playerPositionsInBest11, best11PredictionsPointsGet.result.child("Details"), best11PointsRules)
                    best11RecyclerView?.adapter = best11Adapter
                    view.findViewById<ProgressBar>(R.id.progress_updating_best11).visibility = INVISIBLE
                    val moduleLayout = view.findViewById<RelativeLayout>(resources.getIdentifier("layout_$moduleId", "id", activity?.packageName))
                    moduleLayout.visibility = VISIBLE
                }

                view.findViewById<ImageView>(R.id.standings).setOnClickListener {
                    val navigateToBest11Standings = ShowBest11Directions.actionShowBest11ToBest11Standings(user, championship, season, round, module)
                    view.findNavController().navigate(navigateToBest11Standings)
                }
            }
            else {
                playersInBest11 = playersInBest11.sortedWith(compareBy({ p -> if (p.role == "Goalkeeper") 0 else 1 }, { p -> if (p.role == "Defender") 0 else 1 }, { p -> if (p.role == "Midfielder") 0 else 1 })).toMutableList()
                best11RecyclerView.visibility = VISIBLE
                val best11Adapter = PlayersBest11Adapter(playersInBest11, teamsBitmap, captainPlayer = captainPlayer)
                best11RecyclerView?.adapter = best11Adapter
                view.findViewById<ProgressBar>(R.id.progress_updating_best11).visibility = INVISIBLE
                val moduleLayout = view.findViewById<RelativeLayout>(resources.getIdentifier("layout_$moduleId", "id", activity?.packageName))
                moduleLayout.visibility = VISIBLE
            }
        }
    }
}