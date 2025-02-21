package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.embeddedproject.calciofemminileitaliano.adapters.AllMatchesPredictorAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar


class MatchesPredictions : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_matches_predictions, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = MatchesPredictionsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season

        view.findViewById<TextView>(R.id.championship_name).text = getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))
        view.findViewById<TextView>(R.id.season_info).text = season

        view.findViewById<ImageView>(R.id.back_to_select_championship).setOnClickListener {
            val navigateToSelectChampionship = MatchesPredictionsDirections.actionMatchesPredictionsToSelectChampionship(user)
            view.findNavController().navigate(navigateToSelectChampionship)
        }

        view.findViewById<ImageView>(R.id.standings).setOnClickListener {
            val navigateToStandings = MatchesPredictionsDirections.actionMatchesPredictionsToStandings(user, championship, season)
            view.findNavController().navigate(navigateToStandings)
        }

        view.findViewById<ImageView>(R.id.championship_recap).setOnClickListener {
            val navigateToSeasonRecap = MatchesPredictionsDirections.actionMatchesPredictionsToSeasonRecap(user, championship, season)
            view.findNavController().navigate(navigateToSeasonRecap)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = MatchesPredictionsDirections.actionMatchesPredictionsToLoginRegistration()
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
            val totalPointsRounds = championshipReference.child("TotalPoints").child(user)
            var totalPoints = 0
            for (r in totalPointsRounds.children) {
                for (matchPoints in r.children) {
                    totalPoints += matchPoints.value.toString().toInt()
                }
            }
            view.findViewById<TextView>(R.id.season_total_points).text = totalPoints.toString()

            var allMatchesList = mutableListOf<List<MatchPredictor>>()
            var roundsList = mutableListOf<String>()
            for (r in championshipReference.child("Matches").children) {
                val round = r.key.toString()
                val matchesRoundList = mutableListOf<MatchPredictor>()
                val matches = championshipReference.child("Matches").child(round).child("Matches")
                for (m in matches.children) {
                    val matchInfo = m.child("MatchInfo")
                    val date = matchInfo.child("date").value.toString()
                    val time = matchInfo.child("time").value.toString()
                    val homeTeam = matchInfo.child("homeTeam").value.toString()
                    val guestTeam = matchInfo.child("guestTeam").value.toString()
                    val homeScore = matchInfo.child("homeScore").value.toString()
                    val guestScore = matchInfo.child("guestScore").value.toString()
                    var finished = m.hasChild("Finished")

                    val numberedDateToCompare = date.split("-")
                    val matchYear = numberedDateToCompare[0].toInt()
                    val matchMonth = numberedDateToCompare[1].toInt()
                    val matchDay = numberedDateToCompare[2].toInt()

                    val matchDate = LocalDate.of(matchYear, matchMonth, matchDay)
                    val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)
                    if (!finished && actualDate > matchDate) {
                        reference.child("Championships").child(championship).child(season).child("Matches").child(round).child("Matches").child("$homeTeam-$guestTeam").child("Finished").setValue(true).addOnCompleteListener {
                            activity?.runOnUiThread {
                                finished = true
                            }
                        }
                    }
                    if (time != "To be defined") {
                        val utcDateTime = LocalDateTime.parse(date + "T" + time)
                        val utcZone = utcDateTime.atZone(ZoneId.of("UTC"))
                        val localDateTimeWithZone = utcZone.withZoneSameInstant(ZoneId.systemDefault())
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val localDate = localDateTimeWithZone.format(dateFormatter)
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val localTime = localDateTimeWithZone.format(timeFormatter)
                        matchesRoundList.add(MatchPredictor(translateDate(localDate), localDate, localTime, homeTeam, guestTeam, round, homeScore, guestScore, finished))
                    }
                    else {
                        matchesRoundList.add(MatchPredictor(translateDate(date), date, time, homeTeam, guestTeam, round, homeScore, guestScore, finished))
                    }
                }
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
                        tempList.add(allMatchesList[i.toInt() - 401])
                    }
                    tempList.add(matchesRoundList)
                    tempRound.add(round)
                    for (rnd in roundsList) {
                        if (rnd.toInt() !in 400..499) {
                            tempRound.add(rnd)
                        }
                    }
                    for (i in tempRoundNotQualifier) {
                        tempList.add(allMatchesList[allMatchesList.size - i.toInt()])
                    }
                    allMatchesList = tempList
                    roundsList = tempRound
                }
                else {
                    allMatchesList.add(matchesRoundList)
                    roundsList.add(round)
                }
            }
            val matchPredictor = AllMatchesPredictorAdapter(user, championship, season, allMatchesList, roundsList, it.result, reference)
            val matchesRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_match_predictor)
            matchesRecyclerView.adapter = matchPredictor
            val snapHelper: SnapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(matchesRecyclerView)
            view.findViewById<ProgressBar>(R.id.progress_updating_matches).visibility = INVISIBLE
            var lastRound = 0
            val findLastRoundAccessed = dbReference.rawQuery("SELECT LastRound FROM USER_LAST_ACCESSED WHERE UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))
            val lastRoundFound = findLastRoundAccessed.count
            if (lastRoundFound == 1) {
                if (findLastRoundAccessed.moveToFirst()) {
                    lastRound = findLastRoundAccessed.getString(0).toInt()
                }
            }
            else {
                val roundToShow = ContentValues()
                roundToShow.put("UserNickname", user)
                roundToShow.put("Championship", championship)
                roundToShow.put("Season", season)
                roundToShow.put("LastRound", 0)
                roundToShow.put("LastMatchInRound", 0)
                dbReference.insert("USER_LAST_ACCESSED", null, roundToShow)
                lastRound = 0
            }
            findLastRoundAccessed.close()
            view.findViewById<ImageView>(R.id.season_total_points_image).visibility = VISIBLE
            matchesRecyclerView.scrollToPosition(roundsList.indexOf(lastRound.toString()) + 1)
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
}