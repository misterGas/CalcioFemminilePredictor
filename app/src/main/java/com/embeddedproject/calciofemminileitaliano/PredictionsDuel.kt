package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.embeddedproject.calciofemminileitaliano.adapters.AllMatchesComparisonAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class PredictionsDuel : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_predictions_duel, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = PredictionsDuelArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val vsUser = arguments.vsUser
        val championship = arguments.championship
        val season = arguments.season

        val translatedChampionship = getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))
        val comparison = "$translatedChampionship\n${getString(R.string.comparison)}\n$user vs $vsUser"
        view.findViewById<TextView>(R.id.predictions_comparison).text = comparison
        view.findViewById<TextView>(R.id.season_info).text = season

        view.findViewById<ImageView>(R.id.back_to_standings).setOnClickListener {
            val navigateToStandings = PredictionsDuelDirections.actionPredictionsDuelToStandings(user, championship, season)
            view.findNavController().navigate(navigateToStandings)
        }

        view.findViewById<ImageView>(R.id.comparison_info).setOnClickListener {
            showComparisonInfo(view.context)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = PredictionsDuelDirections.actionPredictionsDuelToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        reference.get().addOnCompleteListener {
            val championshipReference = it.result.child("Championships").child(championship).child(season)

            var allMatchesPredictedFromBoth = mutableListOf<List<MatchPredictor>>()
            var roundsList = mutableListOf<String>()
            for (r in championshipReference.child("Matches").children) {
                val round = r.key.toString()
                val matchesRoundFromBoth = mutableListOf<MatchPredictor>()
                val matches = championshipReference.child("Matches").child(round).child("Matches")
                for (m in matches.children) {
                    val predictions = m.child("Predictions")
                    if (predictions.hasChild(user) && predictions.hasChild(vsUser)) {
                        val matchInfo = m.child("MatchInfo")
                        val date = matchInfo.child("date").value.toString()
                        val time = matchInfo.child("time").value.toString()
                        val homeTeam = matchInfo.child("homeTeam").value.toString()
                        val guestTeam = matchInfo.child("guestTeam").value.toString()
                        val homeScore = matchInfo.child("homeScore").value.toString()
                        val guestScore = matchInfo.child("guestScore").value.toString()
                        val finished = m.hasChild("Finished")
                        if (finished) {
                            val utcDateTime = LocalDateTime.parse(date + "T" + time)
                            val utcZone = utcDateTime.atZone(ZoneId.of("UTC"))
                            val localDateTimeWithZone = utcZone.withZoneSameInstant(ZoneId.systemDefault())
                            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                            val localDate = localDateTimeWithZone.format(dateFormatter)
                            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                            val localTime = localDateTimeWithZone.format(timeFormatter)
                            matchesRoundFromBoth.add(MatchPredictor(translateDate(localDate), localDate, localTime, homeTeam, guestTeam, round, homeScore, guestScore, true))
                        }
                    }
                }
                if (matchesRoundFromBoth.size > 0) {
                    if (round.toInt() in 400..499) {
                        val tempList = mutableListOf<List<MatchPredictor>>()
                        val tempRound = mutableListOf<String>()
                        val tempRoundQualifier = mutableListOf<String>()
                        val tempRoundNotQualifier = mutableListOf<String>()
                        for (rnd in roundsList) {
                            if (rnd.toInt() in 400..499) {
                                tempRound.add(rnd)
                                tempRoundQualifier.add(rnd)
                            }
                            else {
                                tempRoundNotQualifier.add(rnd)
                            }
                        }
                        for (i in tempRoundQualifier) {
                            tempList.add(allMatchesPredictedFromBoth[i.toInt() - 401])
                        }
                        tempList.add(matchesRoundFromBoth)
                        tempRound.add(round)
                        for (rnd in roundsList) {
                            if (rnd.toInt() !in 400..499) {
                                tempRound.add(rnd)
                            }
                        }
                        for (i in tempRoundNotQualifier) {
                            tempList.add(allMatchesPredictedFromBoth[allMatchesPredictedFromBoth.size - i.toInt()])
                        }
                        allMatchesPredictedFromBoth = tempList
                        roundsList = tempRound
                    }
                    else {
                        allMatchesPredictedFromBoth.add(matchesRoundFromBoth)
                        roundsList.add(round)
                    }
                }
            }
            val matchPredictor = AllMatchesComparisonAdapter(user, vsUser, championship, season, allMatchesPredictedFromBoth, roundsList, it.result)
            val comparisonsRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_comparisons)
            comparisonsRecyclerView.adapter = matchPredictor
            val snapHelper: SnapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(comparisonsRecyclerView)
            view.findViewById<ProgressBar>(R.id.progress_updating_comparison).visibility = INVISIBLE
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun translateDate(date: String): String {
        val components = date.split("-")
        val year = components[0].toInt()
        val month = components[1].toInt()
        val day = components[2].toInt()
        val d = Calendar.Builder()
        d.setDate(year, month - 1, day)
        val fullDate = d.build().toString()
        var weekDay = englishDaysWeek[fullDate.substring(fullDate.indexOf("DAY_OF_WEEK=") + "DAY_OF_WEEK=".length, fullDate.indexOf(",DAY_OF_WEEK_IN_MONTH")).toInt() - 1]
        weekDay = weekDay.lowercase()
        weekDay = getString(resources.getIdentifier(weekDay, "string", activity?.packageName))
        var monthName = englishMonths[month - 1]
        monthName = monthName.lowercase()
        monthName = getString(resources.getIdentifier(monthName, "string", activity?.packageName))
        return "$weekDay $day $monthName $year"
    }

    private fun showComparisonInfo(context: Context) {
        val builder = AlertDialog.Builder(context).setTitle(getString(R.string.comparison_info))
        builder.setMessage(getString(R.string.comparison_details))

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}