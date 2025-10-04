package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.indices
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.OfficialScorersAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.DataSnapshot
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
import java.util.stream.IntStream

class MatchHistograms : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_match_histograms, container, false)
    }

    @SuppressLint("DiscouragedApi", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = MatchHistogramsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val homeTeam = arguments.homeTeam
        val guestTeam = arguments.guestTeam

        view.findViewById<ImageView>(R.id.back_to_result_details).setOnClickListener {
            val navigateToResultDetails = MatchHistogramsDirections.actionMatchHistogramsToMatchResultDetails(user, championship, season, round, homeTeam, guestTeam)
            view.findNavController().navigate(navigateToResultDetails)
        }

        view.findViewById<ImageView>(R.id.histograms_info).setOnClickListener {
            showHistogramsInfo(view.context)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = MatchHistogramsDirections.actionMatchHistogramsToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        val matchDateTextView: TextView = view.findViewById(R.id.match_date)
        val matchTimeTextView: TextView = view.findViewById(R.id.match_time)
        val home: TextView = view.findViewById(R.id.home_team)
        val guest: TextView = view.findViewById(R.id.guest_team)
        val hScore: TextView = view.findViewById(R.id.home_real_result)
        val gScore: TextView = view.findViewById(R.id.guest_real_result)
        val openOfficialScorers = view.findViewById<RelativeLayout>(R.id.official_scorers_info)
        val officialHomeScorers = view.findViewById<RecyclerView>(R.id.recycler_view_official_home_scorers)
        val officialGuestScorers = view.findViewById<RecyclerView>(R.id.recycler_view_official_guest_scorers)
        val scoresPieChart = view.findViewById<PieChart>(R.id.scores_histogram)
        val homeScorersBarChart = view.findViewById<BarChart>(R.id.home_scorers_histogram)
        val guestScorersBarChart = view.findViewById<BarChart>(R.id.guest_scorers_histogram)
        val mvpBarChart = view.findViewById<BarChart>(R.id.mvp_histogram)

        if (championship == "UEFA Womens Euro") {
            home.text = view.resources.getString(view.resources.getIdentifier(homeTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
            guest.text = view.resources.getString(view.resources.getIdentifier(guestTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
        }
        else {
            home.text = homeTeam
            guest.text = guestTeam
        }

        view.findViewById<TextView>(R.id.home_scorers_histogram_info).text = "${getString(R.string.scorers_histogram)}\n$homeTeam"
        view.findViewById<TextView>(R.id.guest_scorers_histogram_info).text = "${getString(R.string.scorers_histogram)}\n$guestTeam"

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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.result_histograms)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name_result).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.result_histograms)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name_result).text = resultDetails
        }

        view.findViewById<TextView>(R.id.season_info).text = season

        val setHomeTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(homeTeam))
        if (setHomeTeamImage.moveToFirst()) {
            view.findViewById<ImageView>(R.id.home_team_image).setImageBitmap(BitmapFactory.decodeByteArray(setHomeTeamImage.getBlob(0), 0, setHomeTeamImage.getBlob(0).size))
        }
        setHomeTeamImage.close()
        val setGuestTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(guestTeam))
        if (setGuestTeamImage.moveToFirst()) {
            view.findViewById<ImageView>(R.id.guest_team_image).setImageBitmap(BitmapFactory.decodeByteArray(setGuestTeamImage.getBlob(0), 0, setGuestTeamImage.getBlob(0).size))
        }
        setGuestTeamImage.close()

        reference.get().addOnCompleteListener {
            val databaseGet = it.result
            val matchGet = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam")
            val matchInfo = matchGet.child("MatchInfo")
            val date = matchInfo.child("date").value.toString()
            val time = matchInfo.child("time").value.toString()
            val homeScore = matchInfo.child("homeScore").value.toString()
            val guestScore = matchInfo.child("guestScore").value.toString()

            val utcDateTime = LocalDateTime.parse(date + "T" + time)
            val utcZone = utcDateTime.atZone(ZoneId.of("UTC"))
            val localDateTimeWithZone = utcZone.withZoneSameInstant(ZoneId.systemDefault())
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = localDateTimeWithZone.format(dateFormatter)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val localTime = localDateTimeWithZone.format(timeFormatter)

            val numberedDateToCompare = localDate.split("-")
            val matchYear = numberedDateToCompare[0].toInt()
            val matchMonth = numberedDateToCompare[1].toInt()
            val matchDay = numberedDateToCompare[2].toInt()

            val matchDate = LocalDate.of(matchYear, matchMonth, matchDay)
            val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)

            when (matchDate) {
                actualDate -> {
                    matchDateTextView.text = view.resources.getString(R.string.today)
                }
                actualDate.minusDays(1) -> {
                    matchDateTextView.text = view.resources.getString(R.string.yesterday)
                }
                actualDate.plusDays(1) -> {
                    matchDateTextView.text = view.resources.getString(R.string.tomorrow)
                }
                else -> {
                    matchDateTextView.text = translateDate(localDate)
                }
            }
            matchTimeTextView.text = localTime
            hScore.text = homeScore
            gScore.text = guestScore

            if (!(homeScore.toInt() == 0 && guestScore.toInt() == 0)) {
                openOfficialScorers.visibility = VISIBLE
            }

            val findOfficialMVP = matchGet.child("OfficialMVP")
            var officialMVP: MVPPlayer? = null
            if (findOfficialMVP.value.toString() != "null") {
                officialMVP = MVPPlayer(findOfficialMVP.child("team").value.toString(), findOfficialMVP.child("shirt").value.toString().toInt())
            }

            openOfficialScorers.setOnClickListener {
                val officialScorers = view.findViewById<ImageView>(R.id.open_official_scorers)
                if ((homeScore.toInt() > 0 && officialHomeScorers.visibility == GONE) || (guestScore.toInt() > 0 && officialGuestScorers.visibility == GONE)) {
                    officialScorers.setImageResource(R.drawable.arrow_down)
                    if (homeScore.toInt() > 0) {
                        officialScorersAdapter(officialHomeScorers, season, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, databaseGet, matchGet)
                    }
                    if (guestScore.toInt() > 0) {
                        officialScorersAdapter(officialGuestScorers, season, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, databaseGet, matchGet)
                    }
                }
                else {
                    officialScorers.setImageResource(R.drawable.arrow_right)
                    officialHomeScorers.visibility = GONE
                    officialGuestScorers.visibility = GONE
                }
            }

            val openMVP: RelativeLayout = view.findViewById(R.id.mvp_info)
            val showOfficialMVP: RelativeLayout = view.findViewById(R.id.official_mvp_opened)

            if (officialMVP != null) {
                openMVP.visibility = VISIBLE
            }

            openMVP.setOnClickListener {
                val mvp = view.findViewById<ImageView>(R.id.open_mvp)
                if (showOfficialMVP.visibility == GONE) {
                    mvp.setImageResource(R.drawable.arrow_down)
                    if (officialMVP != null) {
                        showOfficialMVP.visibility = VISIBLE
                        val getOfficialPlayer = databaseGet.child("Players").child(season).child(officialMVP.team).child(officialMVP.shirt.toString())
                        val playerNameToShow = "${getOfficialPlayer.child("firstName").value.toString()} ${getOfficialPlayer.child("lastName").value.toString()} (${officialMVP.team})"
                        view.findViewById<TextView>(R.id.official_mvp_value).text = playerNameToShow
                    }
                }
                else {
                    mvp.setImageResource(R.drawable.arrow_right)
                    showOfficialMVP.visibility = GONE
                }
            }

            val allScores = mutableMapOf<String, Int>()
            val allHomeScorers = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            val allGuestScorers = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            val allHomeScorersStrings = mutableMapOf<String, Int>()
            val allGuestScorersStrings = mutableMapOf<String, Int>()
            val allMVP = mutableMapOf<MVPPlayer, Int>()
            val allMVPStrings = mutableMapOf<String, Int>()

            val matchPredictionsGet = matchGet.child("Predictions")
            var totalScoresVotes = 0
            for (uDB in matchPredictionsGet.children) {
                val userInDatabaseScores = uDB.child("Scores")
                val homePrediction = userInDatabaseScores.child(homeTeam).value.toString()
                val guestPrediction = userInDatabaseScores.child(guestTeam).value.toString()
                val scoresKey = "$homePrediction-$guestPrediction"
                if (allScores.containsKey(scoresKey)) {
                    allScores[scoresKey] = allScores[scoresKey]!! + 1
                }
                else {
                    allScores[scoresKey] = 1
                }
                totalScoresVotes++
                if (uDB.hasChild("Scorers")) {
                    val userInDatabaseScorers = uDB.child("Scorers")
                    if (userInDatabaseScorers.hasChild(homeTeam)) {
                        for (hS in userInDatabaseScorers.child(homeTeam).children) {
                            val goalType = hS.child("goalType").value.toString()
                            val shirt = hS.child("shirt").value.toString().toInt()
                            val predicted = PointsGoalOrOwnGoal(goalType, shirt, homeTeam)
                            if (allHomeScorers.containsKey(predicted)) {
                                allHomeScorers[predicted] = allHomeScorers[predicted]!! + 1
                            }
                            else {
                                allHomeScorers[predicted] = 1
                            }
                        }
                    }
                    if (userInDatabaseScorers.hasChild(guestTeam)) {
                        for (gS in userInDatabaseScorers.child(guestTeam).children) {
                            val goalType = gS.child("goalType").value.toString()
                            val shirt = gS.child("shirt").value.toString().toInt()
                            val predicted = PointsGoalOrOwnGoal(goalType, shirt, guestTeam)
                            if (allGuestScorers.containsKey(predicted)) {
                                allGuestScorers[predicted] = allGuestScorers[predicted]!! + 1
                            }
                            else {
                                allGuestScorers[predicted] = 1
                            }
                        }
                    }
                }

                if (uDB.hasChild("MVP")) {
                    val userInDatabaseMVP = uDB.child("MVP")
                    val team = userInDatabaseMVP.child("team").value.toString()
                    val shirt = userInDatabaseMVP.child("shirt").value.toString()
                    val predicted = MVPPlayer(team, shirt.toInt())
                    if (allMVP.containsKey(predicted)) {
                        allMVP[predicted] = allMVP[predicted]!! + 1
                    }
                    else {
                        allMVP[predicted] = 1
                    }
                }

                for (hS in allHomeScorers) {
                    if (hS.key.goalType == "Goal") {
                        val scorerInfo = databaseGet.child("Players").child(season).child(homeTeam).child(hS.key.shirt.toString())
                        val firstName = scorerInfo.child("firstName").value.toString()
                        val lastName = scorerInfo.child("lastName").value.toString()
                        var name = "${firstName[0]}."
                        var lastNameReduced = ""
                        if (lastName.contains(" ")) {
                            val lastNames = lastName.split(" ")
                            for (i in IntStream.range(0, lastNames.size - 1)) {
                                lastNameReduced += if (lastNames[i].length <= 3) {
                                    "${lastNames[i]} "
                                } else {
                                    "${lastNames[i][0]}. "
                                }
                            }
                            lastNameReduced += lastNames[lastNames.size - 1]
                        }
                        else {
                            lastNameReduced = lastName
                        }
                        name = "$name $lastNameReduced"
                        allHomeScorersStrings[name] = hS.value
                    }
                    else {
                        val scorerInfo = databaseGet.child("Players").child(season).child(guestTeam).child(hS.key.shirt.toString())
                        val firstName = scorerInfo.child("firstName").value.toString()
                        val lastName = scorerInfo.child("lastName").value.toString()
                        var name = "${firstName[0]}."
                        var lastNameReduced = ""
                        if (lastName.contains(" ")) {
                            val lastNames = lastName.split(" ")
                            for (i in IntStream.range(0, lastNames.size - 1)) {
                                lastNameReduced += if (lastNames[i].length <= 3) {
                                    "${lastNames[i]} "
                                } else {
                                    "${lastNames[i][0]}. "
                                }
                            }
                            lastNameReduced += lastNames[lastNames.size - 1]
                        }
                        else {
                            lastNameReduced = lastName
                        }
                        name = "$name $lastNameReduced ${getString(R.string.own_goal)}"
                        allHomeScorersStrings[name] = hS.value
                    }
                }

                for (gS in allGuestScorers) {
                    if (gS.key.goalType == "Goal") {
                        val scorerInfo = databaseGet.child("Players").child(season).child(guestTeam).child(gS.key.shirt.toString())
                        val firstName = scorerInfo.child("firstName").value.toString()
                        val lastName = scorerInfo.child("lastName").value.toString()
                        var name = "${firstName[0]}."
                        var lastNameReduced = ""
                        if (lastName.contains(" ")) {
                            val lastNames = lastName.split(" ")
                            for (i in IntStream.range(0, lastNames.size - 1)) {
                                lastNameReduced += if (lastNames[i].length <= 3) {
                                    "${lastNames[i]} "
                                } else {
                                    "${lastNames[i][0]}. "
                                }
                            }
                            lastNameReduced += lastNames[lastNames.size - 1]
                        }
                        else {
                            lastNameReduced = lastName
                        }
                        name = "$name $lastNameReduced"
                        allGuestScorersStrings[name] = gS.value
                    }
                    else {
                        val scorerInfo = databaseGet.child("Players").child(season).child(homeTeam).child(gS.key.shirt.toString())
                        val firstName = scorerInfo.child("firstName").value.toString()
                        val lastName = scorerInfo.child("lastName").value.toString()
                        var name = "${firstName[0]}."
                        var lastNameReduced = ""
                        if (lastName.contains(" ")) {
                            val lastNames = lastName.split(" ")
                            for (i in IntStream.range(0, lastNames.size - 1)) {
                                lastNameReduced += if (lastNames[i].length <= 3) {
                                    "${lastNames[i]} "
                                } else {
                                    "${lastNames[i][0]}. "
                                }
                            }
                            lastNameReduced += lastNames[lastNames.size - 1]
                        }
                        else {
                            lastNameReduced = lastName
                        }
                        name = "$name $lastNameReduced ${getString(R.string.own_goal)}"
                        allGuestScorersStrings[name] = gS.value
                    }
                }

                for (mvp in allMVP) {
                    val mvpInfo = databaseGet.child("Players").child(season).child(mvp.key.team).child(mvp.key.shirt.toString())
                    val firstName = mvpInfo.child("firstName").value.toString()
                    val lastName = mvpInfo.child("lastName").value.toString()
                    var name = "${firstName[0]}."
                    var lastNameReduced = ""
                    if (lastName.contains(" ")) {
                        val lastNames = lastName.split(" ")
                        for (i in IntStream.range(0, lastNames.size - 1)) {
                            lastNameReduced += if (lastNames[i].length <= 3) {
                                "${lastNames[i]} "
                            } else {
                                "${lastNames[i][0]}. "
                            }
                        }
                        lastNameReduced += lastNames[lastNames.size - 1]
                    }
                    else {
                        lastNameReduced = lastName
                    }
                    name = "$name $lastNameReduced"
                    allMVPStrings[name] = mvp.value
                }
            }

            val scoresEntries = ArrayList<PieEntry>()
            for ((result, count) in allScores) {
                scoresEntries.add(PieEntry(count.toFloat(), result))
            }
            createPieChart(scoresPieChart, scoresEntries, ColorTemplate.MATERIAL_COLORS.toList(), totalScoresVotes)

            val homeScorersEntries = ArrayList<BarEntry>()
            val homeScorersXValues = mutableListOf<String>()
            var homeScorersI = 0
            var homeScorersMaxValue = 0
            for (scH in allHomeScorersStrings) {
                homeScorersEntries.add(BarEntry(homeScorersI.toFloat(), scH.value.toFloat()))
                homeScorersI++
                if (scH.value > homeScorersMaxValue) {
                    homeScorersMaxValue = scH.value
                }
                homeScorersXValues.add(scH.key)
            }
            createBarChart(homeScorersBarChart, homeScorersEntries, homeScorersXValues, homeScorersMaxValue, "Home Scorers", ColorTemplate.COLORFUL_COLORS.toList())

            val guestScorersEntries = ArrayList<BarEntry>()
            val guestScorersXValues = mutableListOf<String>()
            var guestScorersI = 0
            var guestScorersMaxValue = 0
            for (scG in allGuestScorersStrings) {
                guestScorersEntries.add(BarEntry(guestScorersI.toFloat(), scG.value.toFloat()))
                guestScorersI++
                if (scG.value > guestScorersMaxValue) {
                    guestScorersMaxValue = scG.value
                }
                guestScorersXValues.add(scG.key)
            }
            createBarChart(guestScorersBarChart, guestScorersEntries, guestScorersXValues, guestScorersMaxValue, "Guest Scorers", ColorTemplate.COLORFUL_COLORS.toList())

            val mvpEntries = ArrayList<BarEntry>()
            val mvpXValues = mutableListOf<String>()
            var mvpI = 0
            var mvpMaxValue = 0
            for (mvp in allMVPStrings) {
                mvpEntries.add(BarEntry(mvpI.toFloat(), mvp.value.toFloat()))
                mvpI++
                if (mvp.value > mvpMaxValue) {
                    mvpMaxValue = mvp.value
                }
                mvpXValues.add(mvp.key)
            }
            createBarChart(mvpBarChart, mvpEntries, mvpXValues, mvpMaxValue, "MVP", ColorTemplate.PASTEL_COLORS.toList())
            view.findViewById<ProgressBar>(R.id.progress_updating_histograms).visibility = GONE
            view.findViewById<RelativeLayout>(R.id.histograms).visibility = VISIBLE
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

    private fun officialScorersAdapter(scorersRecyclerView: RecyclerView, season: String, team: String, resource: Int, goalTeam: String, ownGoalTeam: String, databaseGet: DataSnapshot, matchGet: DataSnapshot) {
        scorersRecyclerView.visibility = VISIBLE
        val scorers = mutableListOf<Player>()
        val scorerTypes = mutableListOf<String>()
        val timelines = mutableMapOf<Player, String>()
        for (s in matchGet.child("OfficialScorers").child(team).children) {
            val goalType = s.child("goalType").value.toString()
            val shirt = s.child("shirt").value.toString()
            val goalTypeTeam = if (goalType == "Goal") {
                goalTeam
            }
            else {
                ownGoalTeam
            }
            var minute = "null"
            val timelineGet = matchGet.child("OfficialTimelines").child(team).child(s.key.toString())
            if (timelineGet.value.toString() != "null") {
                minute = timelineGet.value.toString()
            }
            val scorerInfo = databaseGet.child("Players").child(season).child(goalTypeTeam).child(shirt)
            val firstName = scorerInfo.child("firstName").value.toString()
            val lastName = scorerInfo.child("lastName").value.toString()
            val role = scorerInfo.child("role").value.toString()
            val newPlayer = Player(firstName, lastName, shirt.toInt(), role, goalTypeTeam)
            if (!scorers.contains(newPlayer)) {
                scorers.add(newPlayer)
                scorerTypes.add(goalType)
            }
            if (timelines.containsKey(newPlayer)) {
                timelines[newPlayer] = timelines[newPlayer]!! + " $minute"
            }
            else {
                timelines[newPlayer] = minute
            }
            val scorersAdapter = OfficialScorersAdapter(scorers, scorerTypes, timelines, resource)
            scorersRecyclerView.adapter = scorersAdapter
        }
    }

    private fun createBarChart(barChart: BarChart, entries: List<BarEntry>, xValues: List<String>, maxValue: Int, info: String, colors: List<Int>) {
        barChart.axisRight.setDrawLabels(false)
        val yAxis = barChart.axisLeft
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = (maxValue + 3).toFloat()
        yAxis.axisLineColor = Color.BLACK
        yAxis.labelCount = 3

        val dataSet = BarDataSet(entries, info)
        dataSet.colors = colors
        dataSet.valueTextSize = 14f

        val barData = BarData(dataSet)
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        })

        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.setDrawGridLines(false)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(xValues)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.xAxis.isGranularityEnabled = true
        barChart.legend.isEnabled = false
        barChart.isHighlightPerTapEnabled = false
        barChart.isHighlightFullBarEnabled = false
        barChart.isHighlightPerDragEnabled = false
        barChart.xAxis.textSize = 12f
        barChart.axisLeft.textSize = 12f
        barChart.axisRight.textSize = 12f
        barChart.setExtraOffsets(0f, 0f, 0f, 10f)
        barChart.invalidate()
    }

    private fun createPieChart(pieChart: PieChart, entries: List<PieEntry>, colors: List<Int>, totalVotes: Int) {
        val dataSet = PieDataSet(entries, "Scores")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.setDrawValues(true)
        dataSet.valueTextColor = Color.WHITE
        dataSet.yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
        dataSet.xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()*100/totalVotes}%"
            }
        })

        pieChart.data = pieData
        pieChart.setUsePercentValues(false)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(14f)
        pieChart.setTransparentCircleAlpha(0)
        pieChart.description.isEnabled = false
        pieChart.legend.isEnabled = false
        pieChart.invalidate()
    }

    private fun showHistogramsInfo(context: Context) {
        val builder = AlertDialog.Builder(context).setTitle(getString(R.string.histograms_info))
        builder.setMessage(getString(R.string.histograms_details))

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
}