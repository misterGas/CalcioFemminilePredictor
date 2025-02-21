package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.content.ContentValues
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
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import java.util.Calendar

class AllMatchesPredictorAdapter(private val user: String, private val championship: String, private val season: String, private val matchesList: List<List<MatchPredictor>>, private val rounds: List<String>, private val databaseGet: DataSnapshot, private val databaseReference: DatabaseReference) : RecyclerView.Adapter<AllMatchesPredictorAdapter.AllMatchesPredictorViewHolder>() {

    class AllMatchesPredictorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("NotifyDataSetChanged", "ClickableViewAccessibility")
        fun bind(matchDayAdapter : MatchDayPredictorAdapter, round: Int, user: String, championship: String, season: String, firstDateRound: String, lastDateRound: String, databaseGet: DataSnapshot, databaseReference: DatabaseReference) {
            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase
            val findLastMatchAccessed = dbReference.rawQuery("SELECT LastMatchInRound FROM USER_LAST_ACCESSED WHERE UserNickname = ? AND Championship = ? AND Season = ? AND LastRound = ?", arrayOf(user, championship, season, (round - 1).toString()))
            val lastRoundFound = findLastMatchAccessed.count
            if (lastRoundFound == 1) {
                if (findLastMatchAccessed.moveToFirst()) {
                    val lastMatch = findLastMatchAccessed.getString(0).toInt()
                    itemView.findViewById<RecyclerView>(R.id.recycler_view_day_match_predictor).scrollToPosition(lastMatch)
                }
            }
            findLastMatchAccessed.close()
            var roundToShow = ContentValues()
            roundToShow.put("LastRound", round - 1)
            roundToShow.put("LastMatchInRound", 0)
            dbReference.update("USER_LAST_ACCESSED", roundToShow, "UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))
            val dayDescription = when (round) {
                in 1..100 -> { //regular season
                    "${itemView.resources.getString(R.string.regular_season)}\n${itemView.resources.getString(R.string.day)} $round"
                }
                120 -> { //round of 16
                    itemView.resources.getString(R.string.round_16)
                }
                125 -> { //quarterfinals
                    itemView.resources.getString(R.string.quarterfinals)
                }
                150 -> { //semifinals
                    itemView.resources.getString(R.string.semifinals)
                }
                200 -> { //final
                    itemView.resources.getString(R.string.final_)
                }
                in 201..250 -> { //shield group
                    "${itemView.resources.getString(R.string.shield_group)}\n${itemView.resources.getString(R.string.day)} ${round - 200}"
                }
                in 251..300 -> { //salvation group
                    "${itemView.resources.getString(R.string.salvation_group)}\n${itemView.resources.getString(R.string.day)} ${round - 250}"
                }
                400 -> { //qualifications
                    itemView.resources.getString(R.string.qualifications)
                }
                in 401..499 -> { //qualifications
                    "${itemView.resources.getString(R.string.qualifications)}\n${itemView.resources.getString(R.string.day)} ${round - 400}"
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

            if (actualDate >= unlockDate) {
                dayMatchesRecyclerView.visibility = VISIBLE
                dayMatchesRecyclerView.adapter = matchDayAdapter

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
                var predictionAvailable = false
                if (actualDate >= firstMatchDayDate) {
                    if (actualDate <= lastMatchDayDate) {
                        predictionAvailable = true
                        showPredictBest11.visibility = VISIBLE
                        showPredictBest11.tooltipText = itemView.resources.getString(R.string.predict_best11)
                    }
                    else {
                        val best11PredictionGet = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions")
                        if (best11PredictionGet.hasChild(user)) {
                            if (best11PredictionGet.child(user).hasChild("Players")) {
                                moduleBest11 = best11PredictionGet.child(user).child("Module").value.toString()
                                showPredictBest11.visibility = VISIBLE
                                showPredictBest11.tooltipText = itemView.resources.getString(R.string.show_best11)
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
                }
                else {
                    showPredictBest11.visibility = GONE
                }

                showPredictBest11.setOnClickListener {
                    if (predictionAvailable) {
                        val navigateToPredictBest11 = MatchesPredictionsDirections.actionMatchesPredictionsToPredictBest11(user, championship, season, round)
                        itemView.findNavController().navigate(navigateToPredictBest11)
                    }
                    else {
                        if (moduleBest11 != null) {
                            val navigateToShowBest11 = MatchesPredictionsDirections.actionMatchesPredictionsToShowBest11(user, championship, season, round, moduleBest11)
                            itemView.findNavController().navigate(navigateToShowBest11)
                        }

                    }
                    roundToShow = ContentValues()
                    roundToShow.put("LastRound", round - 1)
                    roundToShow.put("LastMatchInRound", 0)
                    dbReference.update("USER_LAST_ACCESSED", roundToShow, "UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))
                }
                itemView.findViewById<RelativeLayout>(R.id.locked).visibility = GONE
            }
            else {
                dayMatchesRecyclerView.visibility = GONE
                showPredictBest11.visibility = GONE
                pointsImageView.visibility = GONE
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
        holder.bind(MatchDayPredictorAdapter(user, championship, season, list, list[0].dateNumbered, databaseGet, databaseReference), rounds[position].toInt(), user, championship, season, list[0].dateNumbered, list.last().dateNumbered, databaseGet, databaseReference)
    }
}