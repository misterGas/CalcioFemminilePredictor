package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.MatchesPredictionsDirections
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import java.util.Calendar

class AllMatchesPredictorAdapter(private val user: String, private val championship: String, private val season: String, private val matchesList: List<List<MatchPredictor>>, private val rounds: List<String>, private val roundDisqualifiedPlayers: List<List<Player>>, private val playersTeamsBitmapMap: Map<Player, Bitmap>, private val databaseGet: DataSnapshot, private val databaseReference: DatabaseReference) : RecyclerView.Adapter<AllMatchesPredictorAdapter.AllMatchesPredictorViewHolder>() {

    class AllMatchesPredictorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility", "InflateParams")
        fun bind(matchDayAdapter : MatchDayPredictorAdapter, round: Int, user: String, championship: String, season: String, firstDateRound: String, lastDateRound: String, databaseGet: DataSnapshot, databaseReference: DatabaseReference, thisRoundDisqualifiedPlayers: List<Player>, playersTeamsBitmapMap: Map<Player, Bitmap>) {
            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase
            val findLastMatchAccessed = dbReference.rawQuery("SELECT LastMatchInRound FROM USER_LAST_ACCESSED WHERE UserNickname = ? AND Championship = ? AND Season = ? AND LastRound = ?", arrayOf(user, championship, season, round.toString()))
            val lastRoundFound = findLastMatchAccessed.count
            if (lastRoundFound == 1) {
                if (findLastMatchAccessed.moveToFirst()) {
                    val lastMatch = findLastMatchAccessed.getString(0).toInt()
                    itemView.findViewById<RecyclerView>(R.id.recycler_view_day_match_predictor).scrollToPosition(lastMatch)
                }
            }
            findLastMatchAccessed.close()
            var roundToShow = ContentValues()
            roundToShow.put("LastRound", round)
            roundToShow.put("LastMatchInRound", 0)
            dbReference.update("USER_LAST_ACCESSED", roundToShow, "UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))

            val configuration = itemView.resources.configuration
            val dayDescription = when (round) {
                in 1..100 -> { //regular season
                    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        "${itemView.resources.getString(R.string.regular_season)}\n${itemView.resources.getString(R.string.day)} $round"
                    }
                    else {
                        "${itemView.resources.getString(R.string.regular_season)} (${itemView.resources.getString(R.string.day)} $round)"
                    }
                }
                120 -> { //round of 16
                    itemView.resources.getString(R.string.round_16)
                }
                121 -> {
                    itemView.resources.getString(R.string.round_16_first_leg)
                }
                122 -> {
                    itemView.resources.getString(R.string.round_16_second_leg)
                }
                125 -> { //quarterfinals
                    itemView.resources.getString(R.string.quarterfinals)
                }
                126 -> {
                    itemView.resources.getString(R.string.quarterfinals_first_leg)
                }
                127 -> {
                    itemView.resources.getString(R.string.quarterfinals_second_leg)
                }
                150 -> { //semifinals
                    itemView.resources.getString(R.string.semifinals)
                }
                151 -> {
                    itemView.resources.getString(R.string.semifinals_first_leg)
                }
                152 -> {
                    itemView.resources.getString(R.string.semifinals_second_leg)
                }
                200 -> { //final
                    itemView.resources.getString(R.string.final_)
                }
                in 201..250 -> { //shield group
                    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        "${itemView.resources.getString(R.string.shield_group)}\n${itemView.resources.getString(
                                R.string.day
                            )} ${round - 200}"
                    }
                    else {
                        "${itemView.resources.getString(R.string.shield_group)} (${itemView.resources.getString(
                            R.string.day
                        )} ${round - 200})"
                    }
                }
                in 251..300 -> { //salvation group
                    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        "${itemView.resources.getString(R.string.salvation_group)}\n${itemView.resources.getString(
                            R.string.day
                        )} ${round - 250}"
                    }
                    else {
                        "${itemView.resources.getString(R.string.salvation_group)} (${itemView.resources.getString(
                            R.string.day
                        )} ${round - 250})"
                    }
                }
                400 -> { //qualifications
                    itemView.resources.getString(R.string.qualifications)
                }
                in 401..499 -> { //qualifications
                    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        "${itemView.resources.getString(R.string.qualifications)}\n${itemView.resources.getString(
                            R.string.day
                        )} ${round - 400}"
                    }
                    else {
                        "${itemView.resources.getString(R.string.qualifications)} (${itemView.resources.getString(R.string.day)} ${round - 400})"
                    }
                }
                else -> { //other days
                    "${itemView.resources.getString(R.string.day)} $round"
                }
            }
            itemView.findViewById<TextView>(R.id.season_day).text = dayDescription

            val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)
            val firstDateRoundNumbered = firstDateRound.split("-")
            val firstMatchYear = firstDateRoundNumbered[0].toInt()
            val firstMatchMonth = firstDateRoundNumbered[1].toInt()
            val firstMatchDay = firstDateRoundNumbered[2].toInt()
            val firstMatchDayDate = LocalDate.of(firstMatchYear, firstMatchMonth, firstMatchDay)

            val unlockDate = firstMatchDayDate.minusDays(7)

            val dayMatchesRecyclerView = itemView.findViewById<RecyclerView>(R.id.recycler_view_day_match_predictor)
            val showPredictBest11 = itemView.findViewById<ImageView>(R.id.predict_best11)
            val pointsImageView = itemView.findViewById<ImageView>(R.id.season_day_points_image)
            val showDisqualifiedPlayers = itemView.findViewById<ImageView>(R.id.disqualified_players)

            if (actualDate >= unlockDate) {
                dayMatchesRecyclerView.visibility = VISIBLE
                dayMatchesRecyclerView.adapter = matchDayAdapter
                if (thisRoundDisqualifiedPlayers.isNotEmpty()) {
                    showDisqualifiedPlayers.visibility = VISIBLE
                }
                else {
                    showDisqualifiedPlayers.visibility = GONE
                }

                var roundDisqualifiedPlayersListParam = ""
                for (dp in thisRoundDisqualifiedPlayers) {
                    val shirt = dp.shirtNumber
                    val team = dp.team
                    roundDisqualifiedPlayersListParam += "[$team,$shirt]"
                }

                databaseReference.child("Championships").child(championship).child(season).child("TotalPoints").child(user).child(round.toString()).get().addOnCompleteListener {
                    if (it.result.value.toString() != "null") {
                        var totalPoints = 0
                        for (m in it.result.children) {
                            totalPoints += m.value.toString().toInt()
                        }
                        itemView.findViewById<TextView>(R.id.season_day_points).text = totalPoints.toString()
                    }
                    else {
                        itemView.findViewById<TextView>(R.id.season_day_points).text = "0"
                    }
                }

                pointsImageView.visibility = VISIBLE

                val lastDateRoundNumbered = lastDateRound.split("-")
                val lastMatchYear = lastDateRoundNumbered[0].toInt()
                val lastMatchMonth = lastDateRoundNumbered[1].toInt()
                val lastMatchDay = lastDateRoundNumbered[2].toInt()
                val lastMatchDayDate = LocalDate.of(lastMatchYear, lastMatchMonth, lastMatchDay)

                var moduleBest11 : String? = null
                var showBest11Points = true
                var predictionAvailable = false
                if (actualDate >= firstMatchDayDate) {
                    val best11PredictionGet = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions")
                    if (best11PredictionGet.hasChild(user)) {
                        if (best11PredictionGet.child(user).hasChild("Players")) {
                            moduleBest11 = best11PredictionGet.child(user).child("Module").value.toString()
                            showPredictBest11.visibility = VISIBLE
                            showPredictBest11.tooltipText = itemView.resources.getString(R.string.show_best11)
                            if (actualDate <= lastMatchDayDate) {
                                showBest11Points = false
                            }
                        }
                        else {
                            databaseReference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions").child(user).removeValue()
                            showPredictBest11.visibility = GONE
                        }
                    }
                    else {
                        showPredictBest11.visibility = GONE
                    }
                }
                else {
                    predictionAvailable = true
                    showPredictBest11.visibility = VISIBLE
                    showPredictBest11.tooltipText = itemView.resources.getString(R.string.predict_best11)
                }

                showPredictBest11.setOnClickListener {
                    if (predictionAvailable) {
                        val navigateToPredictBest11 = MatchesPredictionsDirections.actionMatchesPredictionsToPredictBest11(user, championship, season, round, roundDisqualifiedPlayersListParam)
                        itemView.findNavController().navigate(navigateToPredictBest11)
                    }
                    else {
                        if (moduleBest11 != null) {
                            val navigateToShowBest11 = MatchesPredictionsDirections.actionMatchesPredictionsToShowBest11(user, championship, season, round, moduleBest11, showBest11Points)
                            itemView.findNavController().navigate(navigateToShowBest11)
                        }

                    }
                    roundToShow = ContentValues()
                    roundToShow.put("LastRound", round)
                    roundToShow.put("LastMatchInRound", 0)
                    dbReference.update("USER_LAST_ACCESSED", roundToShow, "UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))
                }

                showDisqualifiedPlayers.setOnClickListener {
                    val disqualifiedPlayers = LayoutInflater.from(itemView.context).inflate(R.layout.round_disqualified_players_dialog, null)
                    val disqualifiedPlayersRecyclerView = disqualifiedPlayers.findViewById<RecyclerView>(R.id.recycler_view_disqualified_players)
                    val disqualifiedPlayersAdapter = PlayersAddedAdapter(thisRoundDisqualifiedPlayers.toMutableList(), resource = R.layout.home_scorer, playersTeamsBitmapMap = playersTeamsBitmapMap)
                    disqualifiedPlayersRecyclerView.adapter = disqualifiedPlayersAdapter

                    val dialog = AlertDialog.Builder(itemView.context).setView(disqualifiedPlayers)
                        .setPositiveButton(R.string.ok, null)
                        .create()
                    dialog.show()

                    roundToShow = ContentValues()
                    roundToShow.put("LastRound", round)
                    roundToShow.put("LastMatchInRound", 0)
                    dbReference.update("USER_LAST_ACCESSED", roundToShow, "UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))
                }
                itemView.findViewById<RelativeLayout>(R.id.locked).visibility = GONE
            }
            else {
                dayMatchesRecyclerView.visibility = GONE
                showPredictBest11.visibility = GONE
                pointsImageView.visibility = GONE
                showDisqualifiedPlayers.visibility = GONE
                var writableUnlockDate = translateDate(unlockDate.toString())
                when (unlockDate) {
                    actualDate.plusDays(1) -> {
                        writableUnlockDate = itemView.resources.getString(R.string.tomorrow)
                    }
                }
                val unlockFrom = "${itemView.resources.getString(R.string.unlock_from)} $writableUnlockDate"
                itemView.findViewById<TextView>(R.id.season_day_points).text = unlockFrom
                itemView.findViewById<RelativeLayout>(R.id.locked).visibility = VISIBLE
            }
        }

        private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

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
            weekDay = itemView.resources.getString(itemView.resources.getIdentifier(weekDay, "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            var monthName = englishMonths[month - 1]
            monthName = monthName.lowercase()
            monthName = itemView.resources.getString(itemView.resources.getIdentifier(monthName, "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            return "$weekDay $day $monthName $year"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllMatchesPredictorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.day_predictor, parent, false)
        return AllMatchesPredictorViewHolder(view)
    }

    override fun getItemCount(): Int {
        return matchesList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: AllMatchesPredictorViewHolder, position: Int) {
        val list = matchesList[position].sortedWith(compareBy({ it.dateNumbered }, { it.time }))
        var roundDisqualifiedPlayersListParam = ""
        for (dp in roundDisqualifiedPlayers[position]) {
            val shirt = dp.shirtNumber
            val team = dp.team
            roundDisqualifiedPlayersListParam += "[$team,$shirt]"
        }
        holder.bind(MatchDayPredictorAdapter(user, championship, season, list, list[0].dateNumbered, databaseGet, databaseReference, roundDisqualifiedPlayersListParam), rounds[position].toInt(), user, championship, season, list[0].dateNumbered, list.last().dateNumbered, databaseGet, databaseReference, roundDisqualifiedPlayers[position], playersTeamsBitmapMap)
    }
}