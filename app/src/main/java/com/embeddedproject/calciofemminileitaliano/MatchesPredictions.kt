package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.core.graphics.contains
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.embeddedproject.calciofemminileitaliano.adapters.AllMatchesPredictorAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.ModuleStandingsAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.ScorersStandingsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.ScorerStanding
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
import java.util.stream.IntStream.range


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
            val yellowCardsForDisqualification = championshipReference.child("Info").child("yellowCardsForDisqualification").value.toString().toInt()
            val playersChosenInAllBest11 = mutableMapOf<ScorerStanding, Int>()
            val captainsInAllBest11 = mutableMapOf<ScorerStanding, Int>()
            val modulesInAllBest11 = mutableMapOf<String, Int>()
            var allBest11TotalPoints = 0
            var totalPoints = 0
            for (r in totalPointsRounds.children) {
                for (matchPoints in r.children) {
                    totalPoints += matchPoints.value.toString().toInt()
                }
            }
            view.findViewById<TextView>(R.id.season_total_points).text = totalPoints.toString()

            var allMatchesList = mutableListOf<List<MatchPredictor>>()
            var roundsList = mutableListOf<String>()
            val allRoundsDisqualifiedPlayers = mutableListOf<List<Player>>()
            val playersTeamsBitmapMap = mutableMapOf<Player, Bitmap>()
            val firstRound = if (championshipReference.hasChild("Matches") && championshipReference.child("Matches").children.first().value.toString() != "null") {
                championshipReference.child("Matches").children.first().key.toString()
            }
            else {
                ""
            }
            for (r in championshipReference.child("Matches").children) {
                val round = r.key.toString()
                val matchesRoundList = mutableListOf<MatchPredictor>()
                val matches = championshipReference.child("Matches").child(round).child("Matches")
                val previousRoundExists = round != firstRound
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

                val roundDisqualifiedPlayers = mutableListOf<Player>()
                if (previousRoundExists) {
                    for (m in matches.children) {
                        val matchInfo = m.child("MatchInfo")
                        val homeTeam = matchInfo.child("homeTeam").value.toString()
                        val guestTeam = matchInfo.child("guestTeam").value.toString()
                        val homeRoundsFound = mutableListOf<Int>()
                        var lastHomeTeamMatchPlayed = ""
                        for (i in firstRound.toInt() - 1 until roundsList.indexOf(round)) {
                            val previousRoundMatches = championshipReference.child("Matches").child(roundsList[i]).child("Matches")
                            for (prevMatch in previousRoundMatches.children) {
                                val matchTeams = prevMatch.key.toString().split("-")
                                if (matchTeams.contains(homeTeam)) {
                                    homeRoundsFound.add(roundsList[i].toInt())
                                    lastHomeTeamMatchPlayed = prevMatch.key.toString()
                                }
                            }
                        }
                        if (lastHomeTeamMatchPlayed != "") {
                            val officialHomeCardsGet = championshipReference.child("Matches").child(homeRoundsFound.last().toString()).child("Matches").child(lastHomeTeamMatchPlayed).child("OfficialDiscipline")
                            val officialHomeYellowCards = officialHomeCardsGet.child(homeTeam).child("YellowCards")
                            for (hyc in officialHomeYellowCards.children) {
                                var numberOfYellowCards = 0
                                val findPlayerShirt = hyc.child("shirt").value.toString().toInt()
                                for (i in homeRoundsFound) {
                                    val previousRoundMatches = championshipReference.child("Matches").child(i.toString()).child("Matches")
                                    for (prevMatch in previousRoundMatches.children) {
                                        val yellowCards = prevMatch.child("OfficialDiscipline").child(homeTeam).child("YellowCards")
                                        for (card in yellowCards.children) {
                                            val shirtNumber = card.child("shirt").value.toString().toInt()
                                            if (shirtNumber == findPlayerShirt) {
                                                numberOfYellowCards++
                                            }
                                        }
                                    }
                                }

                                //every time a player gets yellowCardsForDisqualification yellow cards she will be disqualified for the round that corresponds to the round after she took the last yellow card
                                if (numberOfYellowCards % yellowCardsForDisqualification == 0 && numberOfYellowCards > 0) {
                                    val findDisqualifiedPlayerInDB = it.result.child("Players").child(season).child(homeTeam).child(findPlayerShirt.toString())
                                    val playerFirstName = findDisqualifiedPlayerInDB.child("firstName").value.toString()
                                    val playerLastName = findDisqualifiedPlayerInDB.child("lastName").value.toString()
                                    val playerRole = findDisqualifiedPlayerInDB.child("role").value.toString()
                                    val addDisqualifiedPlayer = Player(playerFirstName, playerLastName, findPlayerShirt, playerRole, homeTeam)
                                    roundDisqualifiedPlayers.add(addDisqualifiedPlayer)
                                }
                            }
                        }
                        if (homeRoundsFound.isNotEmpty()) {
                            val officialHomeRedCards = championshipReference.child("Matches").child(homeRoundsFound.last().toString()).child("Matches")
                            for (prevRound in officialHomeRedCards.children) {
                                val findTeamMatch = prevRound.key.toString().split("-")
                                if (findTeamMatch.contains(homeTeam)) {
                                    val prevRoundMatchDiscipline = prevRound.child("OfficialDiscipline").child(homeTeam).child("RedCards")
                                    for (prevDiscipline in prevRoundMatchDiscipline.children) {
                                        val findPlayerShirt = prevDiscipline.child("shirt").value.toString().toInt()
                                        val findDisqualifiedPlayerInDB = it.result.child("Players").child(season).child(homeTeam).child(findPlayerShirt.toString())
                                        val playerFirstName = findDisqualifiedPlayerInDB.child("firstName").value.toString()
                                        val playerLastName = findDisqualifiedPlayerInDB.child("lastName").value.toString()
                                        val playerRole = findDisqualifiedPlayerInDB.child("role").value.toString()
                                        val addDisqualifiedPlayer = Player(playerFirstName, playerLastName, findPlayerShirt, playerRole, homeTeam)
                                        roundDisqualifiedPlayers.add(addDisqualifiedPlayer)
                                    }
                                }
                            }
                        }

                        val guestRoundsFound = mutableListOf<Int>()
                        var lastGuestTeamMatchPlayed = ""
                        for (i in firstRound.toInt() - 1 until roundsList.indexOf(round)) {
                            val previousRoundMatches = championshipReference.child("Matches").child(roundsList[i]).child("Matches")
                            for (prevMatch in previousRoundMatches.children) {
                                val matchTeams = prevMatch.key.toString().split("-")
                                if (matchTeams.contains(guestTeam)) {
                                    guestRoundsFound.add(roundsList[i].toInt())
                                    lastGuestTeamMatchPlayed = prevMatch.key.toString()
                                }
                            }
                        }
                        if (lastGuestTeamMatchPlayed != "") {
                            val officialGuestCardsGet = championshipReference.child("Matches").child(guestRoundsFound.last().toString()).child("Matches").child(lastGuestTeamMatchPlayed).child("OfficialDiscipline")
                            val officialGuestYellowCards = officialGuestCardsGet.child(guestTeam).child("YellowCards")
                            for (gyc in officialGuestYellowCards.children) {
                                var numberOfYellowCards = 0
                                val findPlayerShirt = gyc.child("shirt").value.toString().toInt()
                                for (i in guestRoundsFound) {
                                    val previousRoundMatches = championshipReference.child("Matches").child(i.toString()).child("Matches")
                                    for (prevMatch in previousRoundMatches.children) {
                                        val yellowCards = prevMatch.child("OfficialDiscipline").child(guestTeam).child("YellowCards")
                                        for (card in yellowCards.children) {
                                            val shirtNumber = card.child("shirt").value.toString().toInt()
                                            if (shirtNumber == findPlayerShirt) {
                                                numberOfYellowCards++
                                            }
                                        }
                                    }
                                }

                                //every time a player gets yellowCardsForDisqualification yellow cards she will be disqualified for the round that corresponds to the round after she took the last yellow card
                                if (numberOfYellowCards % yellowCardsForDisqualification == 0 && numberOfYellowCards > 0) {
                                    val findDisqualifiedPlayerInDB = it.result.child("Players").child(season).child(guestTeam).child(findPlayerShirt.toString())
                                    val playerFirstName = findDisqualifiedPlayerInDB.child("firstName").value.toString()
                                    val playerLastName = findDisqualifiedPlayerInDB.child("lastName").value.toString()
                                    val playerRole = findDisqualifiedPlayerInDB.child("role").value.toString()
                                    val addDisqualifiedPlayer = Player(playerFirstName, playerLastName, findPlayerShirt, playerRole, guestTeam)
                                    roundDisqualifiedPlayers.add(addDisqualifiedPlayer)
                                }
                            }
                        }

                        if (guestRoundsFound.isNotEmpty()) {
                            val officialGuestRedCards = championshipReference.child("Matches").child(guestRoundsFound.last().toString()).child("Matches")
                            for (prevRound in officialGuestRedCards.children) {
                                val findTeamMatch = prevRound.key.toString().split("-")
                                if (findTeamMatch.contains(guestTeam)) {
                                    val prevRoundMatchDiscipline = prevRound.child("OfficialDiscipline").child(guestTeam).child("RedCards")
                                    for (prevDiscipline in prevRoundMatchDiscipline.children) {
                                        val findPlayerShirt = prevDiscipline.child("shirt").value.toString().toInt()
                                        val findDisqualifiedPlayerInDB = it.result.child("Players").child(season).child(guestTeam).child(findPlayerShirt.toString())
                                        val playerFirstName = findDisqualifiedPlayerInDB.child("firstName").value.toString()
                                        val playerLastName = findDisqualifiedPlayerInDB.child("lastName").value.toString()
                                        val playerRole = findDisqualifiedPlayerInDB.child("role").value.toString()
                                        val addDisqualifiedPlayer = Player(playerFirstName, playerLastName, findPlayerShirt, playerRole, guestTeam)
                                        roundDisqualifiedPlayers.add(addDisqualifiedPlayer)
                                    }
                                }
                            }
                        }
                    }
                }
                allRoundsDisqualifiedPlayers.add(roundDisqualifiedPlayers)
                for (p in roundDisqualifiedPlayers) {
                    val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(p.team))
                    if (setTeamImage.moveToFirst()) {
                        val teamBitmap = BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size)
                        if (!playersTeamsBitmapMap.contains(p)) {
                            playersTeamsBitmapMap[p] = teamBitmap
                        }
                    }
                    setTeamImage.close()
                }

                if (r.hasChild("Best11PredictionsPoints")) {
                    if (r.child("Best11PredictionsPoints").hasChild(user)) {
                        allBest11TotalPoints += r.child("Best11PredictionsPoints").child(user).child("TotalPoints").value.toString().toInt()
                    }
                    if (r.child("Best11Predictions").hasChild(user)) {
                        val best11Predictions = r.child("Best11Predictions").child(user)
                        var captainPositionInBest11 = "null"
                        if (best11Predictions.hasChild("Captain")) {
                            captainPositionInBest11 = best11Predictions.child("Captain").value.toString()
                        }
                        val module = best11Predictions.child("Module").value.toString()
                        if (modulesInAllBest11.contains(module)) {
                            modulesInAllBest11[module] = modulesInAllBest11[module]!! + 1
                        }
                        else {
                            modulesInAllBest11[module] = 1
                        }
                        for (playerBest11 in best11Predictions.child("Players").children) {
                            val playerTeam = playerBest11.child("team").value.toString()
                            val playerShirt = playerBest11.child("shirt").value.toString().toInt()
                            val best11PlayerStanding = ScorerStanding(playerTeam, playerShirt)
                            if (playersChosenInAllBest11.contains(best11PlayerStanding)) {
                                playersChosenInAllBest11[best11PlayerStanding] = playersChosenInAllBest11[best11PlayerStanding]!! + 1
                            }
                            else {
                                playersChosenInAllBest11[best11PlayerStanding] = 1
                            }
                            if (captainPositionInBest11 != "null" && captainPositionInBest11 == playerBest11.key.toString()) {
                                if (captainsInAllBest11.contains(best11PlayerStanding)) {
                                    captainsInAllBest11[best11PlayerStanding] = captainsInAllBest11[best11PlayerStanding]!! + 1
                                }
                                else {
                                    captainsInAllBest11[best11PlayerStanding] = 1
                                }
                            }
                        }
                    }
                }
            }

            val sortedPlayersChosenInAllBest11 = playersChosenInAllBest11.toList().sortedByDescending { pc -> pc.second }.toMap()
            val sortedCaptainsInAllBest11 = captainsInAllBest11.toList().sortedByDescending { c11 -> c11.second }.toMap()
            val sortedModulesInAllBest11 = modulesInAllBest11.toList().sortedByDescending { m11 -> m11.second }.toMap()
            view.findViewById<ImageView>(R.id.best11_statistics).setOnClickListener { _ ->
                val dialogView = layoutInflater.inflate(R.layout.best11_statistics, null)
                val playersRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_players_chosen_standings)
                playersRecyclerView.adapter = ScorersStandingsAdapter(sortedPlayersChosenInAllBest11, it.result, season)
                val captainsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_captains_chosen_standings)
                captainsRecyclerView.adapter = ScorersStandingsAdapter(sortedCaptainsInAllBest11, it.result, season)
                val modulesRecyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_modules_chosen_standings)
                modulesRecyclerView.adapter = ModuleStandingsAdapter(sortedModulesInAllBest11)
                dialogView.findViewById<TextView>(R.id.points).text = allBest11TotalPoints.toString()

                val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                    .setTitle(getString(R.string.best11_statistics))
                    .setPositiveButton(R.string.ok, null)
                    .setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                dialog.show()

                val configuration = resources.configuration
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels

                    dialog.window?.setLayout((screenWidth * 0.95).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                }
            }
            
            val matchPredictor = AllMatchesPredictorAdapter(user, championship, season, allMatchesList, roundsList, allRoundsDisqualifiedPlayers, playersTeamsBitmapMap = playersTeamsBitmapMap, it.result, reference)
            val matchesRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_match_predictor)
            matchesRecyclerView.adapter = matchPredictor
            val snapHelper: SnapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(matchesRecyclerView)
            view.findViewById<ProgressBar>(R.id.progress_updating_matches).visibility = INVISIBLE
            var lastRound = 1
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
                roundToShow.put("LastRound", lastRound)
                roundToShow.put("LastMatchInRound", 0)
                dbReference.insert("USER_LAST_ACCESSED", null, roundToShow)
            }
            findLastRoundAccessed.close()
            view.findViewById<ImageView>(R.id.season_total_points_image).visibility = VISIBLE
            matchesRecyclerView.scrollToPosition(roundsList.indexOf(lastRound.toString()))
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