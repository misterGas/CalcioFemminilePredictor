package com.embeddedproject.calciofemminileitaliano

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.AllObjectivesAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.ObjectiveType
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.DecimalFormat

class Objectives : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_objectives, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference
        val arguments = ObjectivesArgs.fromBundle(requireArguments())
        val user = arguments.userNickname

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        view.findViewById<ImageView>(R.id.back_to_user_info).setOnClickListener {
            val navigateToUserInfo = ObjectivesDirections.actionObjectivesToUserInfo(user)
            view.findNavController().navigate(navigateToUserInfo)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = ObjectivesDirections.actionObjectivesToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        reference.child("Championships").get().addOnCompleteListener {
            var totalGoalsPredicted = 0
            var matchesPredicted = 0
            var matchesPredictedCorrectly = 0
            var totalPoints = 0
            var actualMaxPointsRound = 0
            var countRoundPoints = 0
            var actualMaxMatchPoints = 0
            var countMatchPoints = 0
            var goalScorersPredictedCorrectly = 0
            var ownGoalScorersPredictedCorrectly = 0
            var goalBonus = 0
            var ownGoalBonus = 0
            var mvpGuessed = 0
            for (championship in it.result.children) {
                for (season in championship.children) {
                    for (round in season.child("Matches").children) {
                        for (match in round.child("Matches").children) {
                            val matchInfo = match.child("MatchInfo")
                            val homeTeam = matchInfo.child("homeTeam").value.toString()
                            val guestTeam = matchInfo.child("guestTeam").value.toString()
                            val homeScore = matchInfo.child("homeScore").value.toString()
                            val guestScore = matchInfo.child("guestScore").value.toString()
                            val isFinished = match.hasChild("Finished")
                            if (isFinished) {
                                val pointsInDatabase = season.child("TotalPoints").child(user).child(round.key.toString()).child("$homeTeam-$guestTeam").value.toString()
                                if (pointsInDatabase != "null") {
                                    val matchTotalPoints = pointsInDatabase.toInt()
                                    totalPoints += matchTotalPoints
                                    countRoundPoints += matchTotalPoints
                                    countMatchPoints = matchTotalPoints
                                }
                            }
                            if (match.child("Predictions").hasChild(user)) {
                                if (homeScore != "null" && guestScore != "null") {
                                    matchesPredicted++
                                    val userPredictions = match.child("Predictions").child(user).child("Scores")
                                    val homePrediction = userPredictions.child(homeTeam).value.toString()
                                    val guestPrediction = userPredictions.child(guestTeam).value.toString()
                                    if (homePrediction.toInt() == homeScore.toInt() && guestPrediction.toInt() == guestScore.toInt()) {
                                        matchesPredictedCorrectly++
                                    }
                                    totalGoalsPredicted += homePrediction.toInt() + guestPrediction.toInt()
                                }
                            }
                            val homeOfficialScorers = match.child("OfficialScorers").child(homeTeam)
                            val homeScorersPredicted = match.child("Predictions").child(user).child("Scorers").child(homeTeam)
                            val guestOfficialScorers = match.child("OfficialScorers").child(guestTeam)
                            val guestScorersPredicted = match.child("Predictions").child(user).child("Scorers").child(guestTeam)
                            val allOfficials = mutableMapOf<PointsGoalOrOwnGoal, Int>()
                            for (s in homeOfficialScorers.children) {
                                val goalType = s.child("goalType").value.toString()
                                val shirt = s.child("shirt").value.toString().toInt()
                                val newOfficialToAdd = PointsGoalOrOwnGoal(goalType, shirt, homeTeam)
                                if (allOfficials.containsKey(newOfficialToAdd)) {
                                    allOfficials[newOfficialToAdd] = allOfficials[newOfficialToAdd]!! + 1
                                }
                                else {
                                    allOfficials[newOfficialToAdd] = 1
                                }
                            }
                            for (s in guestOfficialScorers.children) {
                                val goalType = s.child("goalType").value.toString()
                                val shirt = s.child("shirt").value.toString().toInt()
                                val newOfficialToAdd = PointsGoalOrOwnGoal(goalType, shirt, guestTeam)
                                if (allOfficials.containsKey(newOfficialToAdd)) {
                                    allOfficials[newOfficialToAdd] = allOfficials[newOfficialToAdd]!! + 1
                                }
                                else {
                                    allOfficials[newOfficialToAdd] = 1
                                }
                            }
                            val allPredicted = mutableMapOf<PointsGoalOrOwnGoal, Int>()
                            for (p in homeScorersPredicted.children) {
                                val goalType = p.child("goalType").value.toString()
                                val shirt = p.child("shirt").value.toString().toInt()
                                val newPredictedToAdd = PointsGoalOrOwnGoal(goalType, shirt, homeTeam)
                                if (allPredicted.containsKey(newPredictedToAdd)) {
                                    allPredicted[newPredictedToAdd] = allPredicted[newPredictedToAdd]!! + 1
                                }
                                else {
                                    allPredicted[newPredictedToAdd] = 1
                                }
                            }
                            for (p in guestScorersPredicted.children) {
                                val goalType = p.child("goalType").value.toString()
                                val shirt = p.child("shirt").value.toString().toInt()
                                val newPredictedToAdd = PointsGoalOrOwnGoal(goalType, shirt, guestTeam)
                                if (allPredicted.containsKey(newPredictedToAdd)) {
                                    allPredicted[newPredictedToAdd] = allPredicted[newPredictedToAdd]!! + 1
                                }
                                else {
                                    allPredicted[newPredictedToAdd] = 1
                                }
                            }
                            for (o in allOfficials.keys) {
                                if (allPredicted.containsKey(o)) {
                                    if (o.goalType == "Goal") {
                                        goalScorersPredictedCorrectly++
                                    }
                                    else {
                                        ownGoalScorersPredictedCorrectly++
                                    }
                                    val officialScorerNets = allOfficials[o]!!
                                    val predictedScorerNets = allPredicted[o]!!
                                    if (officialScorerNets > 1 && predictedScorerNets > 1) { //two or more
                                        val netsGuessed = if (officialScorerNets >= predictedScorerNets) {
                                            predictedScorerNets - 1
                                        } else {
                                            officialScorerNets - 1
                                        }
                                        if (o.goalType == "Goal") {
                                            goalBonus += netsGuessed
                                        } else {
                                            ownGoalBonus += netsGuessed
                                        }
                                    }
                                }
                            }

                            val findOfficialMVP = match.child("OfficialMVP")
                            var officialMVP: MVPPlayer? = null
                            if (findOfficialMVP.value.toString() != "null") {
                                officialMVP = MVPPlayer(findOfficialMVP.child("team").value.toString(), findOfficialMVP.child("shirt").value.toString().toInt())
                            }
                            val findPredictedMVP = match.child("Predictions").child(user).child("MVP")
                            var predictedMVP: MVPPlayer? = null
                            if (findPredictedMVP.value.toString() != "null") {
                                predictedMVP = MVPPlayer(findPredictedMVP.child("team").value.toString(), findPredictedMVP.child("shirt").value.toString().toInt())
                            }
                            if (officialMVP != null && predictedMVP != null && predictedMVP.team == officialMVP.team && predictedMVP.shirt == officialMVP.shirt) {
                                mvpGuessed++
                            }

                            if (countMatchPoints >= actualMaxMatchPoints) {
                                actualMaxMatchPoints = countMatchPoints
                            }
                        }
                        if (countRoundPoints >= actualMaxPointsRound) {
                            actualMaxPointsRound = countRoundPoints
                        }
                        countRoundPoints = 0
                    }
                }
            }

            reference.child("Objectives").get().addOnCompleteListener { it2 ->
                val objectivesList = mutableListOf<List<Int>>()
                val objectivesTypesList = mutableListOf<ObjectiveType>()
                var numberOfObjectives = 0
                var objectivesCompleted = 0
                var reachedValue = 0
                for (type in it2.result.children) {
                    val typeName = type.key.toString()
                    val typeNameTranslated = getString(view.resources.getIdentifier("objectives_${typeName.lowercase().replace("-", "_")}", "string", activity?.packageName))
                    val description = getString(view.resources.getIdentifier("objectives_${typeName.lowercase().replace("-", "_")}_description", "string", activity?.packageName))
                    when (typeName) {
                        "Goals-Predicted" -> {
                            reachedValue = totalGoalsPredicted
                        }
                        "MVP-Predicted-Correctly" -> {
                            reachedValue = mvpGuessed
                        }
                        "Match-Points" -> {
                            reachedValue = actualMaxMatchPoints
                        }
                        "Matches-Predicted" -> {
                            reachedValue = matchesPredicted
                        }
                        "Matches-Predicted-Correctly" -> {
                            reachedValue = matchesPredictedCorrectly
                        }
                        "Points" -> {
                            reachedValue = totalPoints
                        }
                        "Round-Points" -> {
                            reachedValue = actualMaxPointsRound
                        }
                        "Scorers-Goal" -> {
                            reachedValue = goalScorersPredictedCorrectly
                        }
                        "Scorers-Goal-Bonus" -> {
                            reachedValue = goalBonus
                        }
                        "Scorers-Own-Goal" -> {
                            reachedValue = ownGoalScorersPredictedCorrectly
                        }
                        "Scorers-Own-Goal-Bonus" -> {
                            reachedValue = ownGoalBonus
                        }
                    }
                    objectivesTypesList.add(ObjectiveType(typeNameTranslated, description, reachedValue))
                    val typeObjectives = mutableListOf<Int>()
                    for (objective in type.children) {
                        val maxValue = objective.child("maxValue").value.toString().toInt()
                        typeObjectives.add(maxValue)
                        if (reachedValue >= maxValue) {
                            objectivesCompleted++
                        }
                        numberOfObjectives++
                    }
                    objectivesList.add(typeObjectives)
                    val totalObjectivesCompleted = view.findViewById<ProgressBar>(R.id.total_objectives_completed)
                    totalObjectivesCompleted.visibility = VISIBLE
                    totalObjectivesCompleted.min = 0
                    totalObjectivesCompleted.max = numberOfObjectives
                    totalObjectivesCompleted.progress = objectivesCompleted
                    val animation = ObjectAnimator.ofInt(totalObjectivesCompleted, "progress", 0, objectivesCompleted)
                    animation.start()
                    val completionPercentage = if (objectivesCompleted > 0) {
                        DecimalFormat("#.00").format(objectivesCompleted * 100.0/numberOfObjectives)
                    } else {
                        "0,00"
                    }
                    val completion = "$objectivesCompleted/$numberOfObjectives ($completionPercentage%)"
                    view.findViewById<TextView>(R.id.total_objectives_completed_number).text = completion
                }
                val objectivesAdapter = AllObjectivesAdapter(objectivesList, objectivesTypesList)
                view.findViewById<RecyclerView>(R.id.recycler_view_all_objectives).adapter = objectivesAdapter
                view.findViewById<ProgressBar>(R.id.progress_updating_objectives).visibility = INVISIBLE
            }
        }
    }
}