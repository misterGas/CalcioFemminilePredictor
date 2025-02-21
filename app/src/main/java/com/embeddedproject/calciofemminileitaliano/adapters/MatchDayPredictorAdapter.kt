package com.embeddedproject.calciofemminileitaliano.adapters

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.embeddedproject.calciofemminileitaliano.MatchesPredictionsDirections
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import kotlin.math.abs

@Suppress("DEPRECATION")
class MatchDayPredictorAdapter(private val user: String, private val championship: String, private val season: String, private val daysList: List<MatchPredictor>, private val firstDateRound: String, private val databaseGet: DataSnapshot, private val databaseReference: DatabaseReference) : RecyclerView.Adapter<MatchDayPredictorAdapter.MatchDayPredictorViewHolder>() {

    inner class MatchDayPredictorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val matchDateTextView: TextView = itemView.findViewById(R.id.match_date)
        private val matchTimeTextView: TextView = itemView.findViewById(R.id.match_time)
        private val homeTeamPrediction: NumberPicker = itemView.findViewById(R.id.home_result_prediction)
        private val guestTeamPrediction: NumberPicker = itemView.findViewById(R.id.guest_result_prediction)
        private val home: TextView = itemView.findViewById(R.id.home_team)
        private val guest: TextView = itemView.findViewById(R.id.guest_team)
        private val hScore: TextView = itemView.findViewById(R.id.home_real_result)
        private val gScore: TextView = itemView.findViewById(R.id.guest_real_result)
        private val matchesPoints: TextView = itemView.findViewById(R.id.match_points)
        private val matchPointsImage: ImageView = itemView.findViewById(R.id.match_points_image)
        private val assignMatchScorers: ImageView = itemView.findViewById(R.id.assign_match_scorers)
        private val officialScorers: ImageView = itemView.findViewById(R.id.assign_official_scorers)
        private val doublePointsButton: TextView = itemView.findViewById(R.id.double_points)
        private val openOfficialScorers: RelativeLayout = itemView.findViewById(R.id.official_scorers_info)
        private val officialHomeScorers: RecyclerView = itemView.findViewById(R.id.recycler_view_official_home_scorers)
        private val officialGuestScorers: RecyclerView = itemView.findViewById(R.id.recycler_view_official_guest_scorers)
        private val openPredictedScorers: RelativeLayout = itemView.findViewById(R.id.predicted_scorers_info)
        private val predictedHomeScorers: RecyclerView = itemView.findViewById(R.id.recycler_view_predicted_home_scorers)
        private val predictedGuestScorers: RecyclerView = itemView.findViewById(R.id.recycler_view_predicted_guest_scorers)
        private val matchFinished: ImageView = itemView.findViewById(R.id.match_finished)
        private val assignMVP: RelativeLayout = itemView.findViewById(R.id.predict_mvp)
        private val assignOfficialMVP: RelativeLayout = itemView.findViewById(R.id.official_mvp)
        private val openMVP: RelativeLayout = itemView.findViewById(R.id.mvp_info)
        private val showPredictedMVP: RelativeLayout = itemView.findViewById(R.id.predicted_mvp_opened)
        private val showOfficialMVP: RelativeLayout = itemView.findViewById(R.id.official_mvp_opened)

        private val scorersGuessedWithMoreNets = mutableMapOf<PointsGoalOrOwnGoal, Int>()

        private val pointsRules = listOf(3, 3, 2, 6, 8, 2, 4, 10)
        /*
            pointsRules[0]: win/null/loss
            pointsRules[1]: nets scored for home/guest team
            pointsRules[2]: net difference between home and guest teams
            pointsRules[3]: correct goal scorer (for each scorer)
            pointsRules[4]: correct own goal scorer (for each scorer)
            pointsRules[5]: two or more goals for a scorer (for each score)
            pointsRules[6]: two or more own goals for a scorer (for each score)
            pointsRules[7]: mvp predicted correctly
         */

        @SuppressLint("DiscouragedApi", "NotifyDataSetChanged", "UseCompatLoadingForDrawables")
        fun bind(user: String, championship: String, season: String, dateToShow: String, dateNumbered: String, time: String, homeTeam: String, guestTeam: String, round: String, homeScore: String, guestScore: String, isFinished: Boolean, matchPosition: Int, firstDateRound: String, databaseGet: DataSnapshot, databaseReference: DatabaseReference) {
            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase
            val databaseRound = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round)
            val matchReference = databaseReference.child("Championships").child(championship).child(season).child("Matches").child(round).child("Matches").child("$homeTeam-$guestTeam")
            matchTimeTextView.text = if (time == "To be defined") {
                itemView.resources.getString(R.string.to_be_defined)
            }
            else {
                time
            }
            if (championship == "UEFA Womens Euro") {
                home.text = itemView.resources.getString(itemView.resources.getIdentifier(homeTeam.lowercase().replace(" ", "_"), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
                guest.text = itemView.resources.getString(itemView.resources.getIdentifier(guestTeam.lowercase().replace(" ", "_"), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            }
            else {
                home.text = homeTeam
                guest.text = guestTeam
            }
            val numberedTimeToCompare: List<String>
            var hourTime = 0
            var minutesTime = 0
            if (time != "To be defined") {
                numberedTimeToCompare = time.split(":")
                hourTime = numberedTimeToCompare[0].toInt()
                minutesTime = numberedTimeToCompare[1].toInt()
            }
            val numberedDateToCompare = dateNumbered.split("-")
            val matchYear = numberedDateToCompare[0].toInt()
            val matchMonth = numberedDateToCompare[1].toInt()
            val matchDay = numberedDateToCompare[2].toInt()

            val matchDate = LocalDate.of(matchYear, matchMonth, matchDay)
            val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)
            val actualTime = LocalDateTime.now()

            when (matchDate) {
                actualDate -> {
                    matchDateTextView.text = itemView.resources.getString(R.string.today)
                }
                actualDate.minusDays(1) -> {
                    matchDateTextView.text = itemView.resources.getString(R.string.yesterday)
                }
                actualDate.plusDays(1) -> {
                    matchDateTextView.text = itemView.resources.getString(R.string.tomorrow)
                }
                else -> {
                    matchDateTextView.text = dateToShow
                }
            }

            val isUserAManager = databaseGet.child("User-Managers").hasChild(user)
            var isManagerActive = false
            if (isUserAManager) {
                isManagerActive = databaseGet.child("User-Managers").child(user).child("Activated").value.toString().toBoolean()
            }

            val matchInfoImage = itemView.findViewById<ImageView>(R.id.match_result_info)

            val matchGet = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round).child("Matches").child("$homeTeam-$guestTeam")

            val findOfficialMVP = matchGet.child("OfficialMVP")
            var officialMVP: MVPPlayer? = null
            if (findOfficialMVP.value.toString() != "null") {
                officialMVP = MVPPlayer(findOfficialMVP.child("team").value.toString(), findOfficialMVP.child("shirt").value.toString().toInt())
            }
            val findPredictedMVP = matchGet.child("Predictions").child(user).child("MVP")
            var predictedMVP: MVPPlayer? = null
            if (findPredictedMVP.value.toString() != "null") {
                predictedMVP = MVPPlayer(findPredictedMVP.child("team").value.toString(), findPredictedMVP.child("shirt").value.toString().toInt())
            }

            matchReference.get().addOnCompleteListener { isMatchFinished ->
                if (!isMatchFinished.result.hasChild("Finished")) {
                    if ((actualDate.isEqual(matchDate) && LocalTime.of(actualTime.hour, actualTime.minute) > LocalTime.of(hourTime, minutesTime).plusMinutes(105)) && isManagerActive) {
                        matchFinished.visibility = VISIBLE
                    }
                }
                else {
                    matchFinished.visibility = GONE
                }
            }

            val scorersPredicted = matchGet.child("Predictions").child(user).child("Scorers")
            val homeScorersPredictedCount = scorersPredicted.child(homeTeam).childrenCount
            val guestScorersPredictedCount = scorersPredicted.child(guestTeam).childrenCount

            if (homeScore != "null" && guestScore != "null" && actualDate >= matchDate) {
                hScore.text = homeScore
                gScore.text = guestScore
                val userScoresPredictions = databaseRound.child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).child("Scores")
                if (userScoresPredictions.value.toString() != "null") {
                    val homePrediction = userScoresPredictions.child(homeTeam).value.toString()
                    val guestPrediction = userScoresPredictions.child(guestTeam).value.toString()
                    homeTeamPrediction.minValue = homePrediction.toInt()
                    homeTeamPrediction.maxValue = homePrediction.toInt()
                    guestTeamPrediction.minValue = guestPrediction.toInt()
                    guestTeamPrediction.maxValue = guestPrediction.toInt()
                    var totalPoints = 0
                    matchesPoints.visibility = GONE
                    itemView.findViewById<ProgressBar>(R.id.updating_points).visibility = VISIBLE
                    if (
                        (homePrediction.toInt() > guestPrediction.toInt() && homeScore.toInt() > guestScore.toInt()) ||
                        (guestPrediction.toInt() > homePrediction.toInt() && guestScore.toInt() > homeScore.toInt()) ||
                        homePrediction.toInt() == guestPrediction.toInt() && homeScore.toInt() == guestScore.toInt()) {
                        totalPoints += pointsRules[0]
                    }
                    if (homePrediction.toInt() == homeScore.toInt()) {
                        totalPoints += pointsRules[1]
                    }
                    if (guestPrediction.toInt() == guestScore.toInt()) {
                        totalPoints += pointsRules[1]
                    }
                    if (abs(homePrediction.toInt() - guestPrediction.toInt()) == abs(homeScore.toInt() - guestScore.toInt())) {
                        totalPoints += pointsRules[2]
                    }

                    val homeOfficialScorers = matchGet.child("OfficialScorers").child(homeTeam)
                    val homeScorersPredicted = matchGet.child("Predictions").child(user).child("Scorers").child(homeTeam)
                    val guestOfficialScorers = matchGet.child("OfficialScorers").child(guestTeam)
                    val guestScorersPredicted = matchGet.child("Predictions").child(user).child("Scorers").child(guestTeam)
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
                            totalPoints += if (o.goalType == "Goal") {
                                pointsRules[3] //goal for a scorer
                            } else {
                                pointsRules[4] //own goal for a scorer
                            }
                            val officialScorerNets = allOfficials[o]!!
                            val predictedScorerNets = allPredicted[o]!!
                            if (officialScorerNets > 1 && predictedScorerNets > 1) { //two or more
                                val netsGuessed = if (officialScorerNets >= predictedScorerNets) {
                                    predictedScorerNets - 1
                                } else {
                                    officialScorerNets - 1
                                }
                                scorersGuessedWithMoreNets[o] = netsGuessed
                                totalPoints += if (o.goalType == "Goal") {
                                    netsGuessed * pointsRules[5] //goals for a scorer for each goal
                                } else {
                                    netsGuessed * pointsRules[6] //own goals for a scorer for each own goal
                                }
                            }
                            else {
                                scorersGuessedWithMoreNets[o] = 0
                            }
                        }
                    }

                    if (officialMVP != null && predictedMVP != null && predictedMVP.team == officialMVP.team && predictedMVP.shirt == officialMVP.shirt) {
                        totalPoints += pointsRules[7]
                    }

                    if (databaseRound.child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).value.toString().contains("DoublePointsActivatedInMatch")) {
                        totalPoints *= 2
                    }
                    databaseReference.child("Championships").child(championship).child(season).child("TotalPoints").child(user).child(round).child("$homeTeam-$guestTeam").setValue(totalPoints).addOnCompleteListener {
                        matchesPoints.visibility = VISIBLE
                        matchesPoints.text = totalPoints.toString()
                        matchPointsImage.visibility = VISIBLE
                        itemView.findViewById<ProgressBar>(R.id.updating_points).visibility = GONE
                    }
                }
                else {
                    matchesPoints.text = itemView.resources.getString(R.string.not_predicted)
                    matchPointsImage.visibility = GONE
                    matchInfoImage.visibility = GONE
                    homeTeamPrediction.visibility = INVISIBLE
                    guestTeamPrediction.visibility = INVISIBLE
                }
                if (!isFinished && Year.now().value == matchYear && YearMonth.now().monthValue == matchMonth && MonthDay.now().dayOfMonth == matchDay) {
                    if (actualTime.hour >= hourTime) {
                        if (actualTime.hour == hourTime && actualTime.minute >= minutesTime) {
                            setRedColorWhenLive(itemView)
                            if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                                if (isManagerActive) {
                                    officialScorers.visibility = VISIBLE
                                }
                                openOfficialScorers.visibility = VISIBLE
                            }
                        }
                        else if (actualTime.hour != hourTime) {
                            setRedColorWhenLive(itemView)
                            if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                                if (isManagerActive) {
                                    officialScorers.visibility = VISIBLE
                                }
                                openOfficialScorers.visibility = VISIBLE
                            }
                        }
                    }
                }
                if (isFinished) {
                    setBlackColorWhenFinished(itemView)
                    matchInfoImage.visibility = VISIBLE
                    if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                        if (isManagerActive) {
                            officialScorers.visibility = VISIBLE
                        }
                        openOfficialScorers.visibility = VISIBLE
                    }
                    if (isManagerActive) {
                        assignOfficialMVP.visibility = VISIBLE
                    }
                }
            }
            else if (Year.now().value == matchYear && YearMonth.now().monthValue == matchMonth && MonthDay.now().dayOfMonth == matchDay) {
                if (actualTime.hour < hourTime - 6) {
                    homeTeamPrediction.minValue = 0
                    homeTeamPrediction.maxValue = 9
                    guestTeamPrediction.minValue = 0
                    guestTeamPrediction.maxValue = 9
                    matchesPoints.text = itemView.resources.getString(R.string.zero)
                }
                else {
                    val userScoresPredictions = databaseRound.child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).child("Scores")

                    if (actualTime.hour >= hourTime) {
                        if (actualTime.hour == hourTime && actualTime.minute >= minutesTime) {
                            setRedColorWhenLive(itemView)
                            if (homeScore != "null" && guestScore != "null") {
                                if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                                    if (isManagerActive) {
                                        officialScorers.visibility = VISIBLE
                                    }
                                    openOfficialScorers.visibility = VISIBLE
                                }
                            }
                        }
                        else if (actualTime.hour != hourTime) {
                            setRedColorWhenLive(itemView)
                            if (homeScore != "null" && guestScore != "null") {
                                if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                                    if (isManagerActive) {
                                        officialScorers.visibility = VISIBLE
                                    }
                                    openOfficialScorers.visibility = VISIBLE
                                }
                            }
                        }
                    }

                    if (userScoresPredictions.value.toString() != "null") {
                        val homePrediction = userScoresPredictions.child(homeTeam).value.toString()
                        val guestPrediction = userScoresPredictions.child(guestTeam).value.toString()

                        if (actualTime.hour >= hourTime - 6 && (actualTime.hour < hourTime || (actualTime.hour == hourTime && actualTime.minute < minutesTime))) {
                            if (homePrediction.toInt() > 0 || guestPrediction.toInt() > 0) {
                                assignMatchScorers.visibility = VISIBLE
                            }
                            assignMVP.visibility = VISIBLE
                        }
                        homeTeamPrediction.minValue = homePrediction.toInt()
                        homeTeamPrediction.maxValue = homePrediction.toInt()
                        guestTeamPrediction.minValue = guestPrediction.toInt()
                        guestTeamPrediction.maxValue = guestPrediction.toInt()
                        matchesPoints.text = itemView.resources.getString(R.string.zero)
                    }
                    else {
                        matchesPoints.text = itemView.resources.getString(R.string.not_predicted)
                        matchPointsImage.visibility = GONE
                        matchInfoImage.visibility = GONE
                        homeTeamPrediction.visibility = INVISIBLE
                        guestTeamPrediction.visibility = INVISIBLE
                    }
                }
            }
            else {
                homeTeamPrediction.minValue = 0
                homeTeamPrediction.maxValue = 9
                guestTeamPrediction.minValue = 0
                guestTeamPrediction.maxValue = 9
            }
            val setHomeTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(homeTeam))
            if (setHomeTeamImage.moveToFirst()) {
                itemView.findViewById<ImageView>(R.id.home_team_image).setImageBitmap(BitmapFactory.decodeByteArray(setHomeTeamImage.getBlob(0), 0, setHomeTeamImage.getBlob(0).size))
            }
            setHomeTeamImage.close()
            val setGuestTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(guestTeam))
            if (setGuestTeamImage.moveToFirst()) {
                itemView.findViewById<ImageView>(R.id.guest_team_image).setImageBitmap(BitmapFactory.decodeByteArray(setGuestTeamImage.getBlob(0), 0, setGuestTeamImage.getBlob(0).size))
            }
            setGuestTeamImage.close()

            if (homeScorersPredictedCount > 0 || guestScorersPredictedCount > 0) {
                openPredictedScorers.visibility = VISIBLE
            }

            matchReference.child("Predictions").child(user).child("Scores").get().addOnCompleteListener { userScoresPredictions ->
                if (userScoresPredictions.result.value.toString() != "null") {
                    val homePrediction = userScoresPredictions.result.child(homeTeam).value.toString()
                    val guestPrediction = userScoresPredictions.result.child(guestTeam).value.toString()
                    homeTeamPrediction.value = homePrediction.toInt()
                    guestTeamPrediction.value = guestPrediction.toInt()
                    if (matchPointsImage.visibility == GONE) {
                        matchesPoints.text = itemView.resources.getString(R.string.zero)
                        matchPointsImage.visibility = VISIBLE
                    }
                }
                else {
                    matchesPoints.text = itemView.resources.getString(R.string.not_predicted)
                    matchPointsImage.visibility = GONE
                    matchInfoImage.visibility = GONE
                }
            }

            val roundReference = databaseReference.child("Championships").child(championship).child(season).child("Matches").child(round)

            val firstDateRoundNumbered = firstDateRound.split("-")
            val firstMatchYear = firstDateRoundNumbered[0].toInt()
            val firstMatchMonth = firstDateRoundNumbered[1].toInt()
            val firstMatchDay = firstDateRoundNumbered[2].toInt()
            val firstMatchDayDate = LocalDate.of(firstMatchYear, firstMatchMonth, firstMatchDay)

            roundReference.get().addOnCompleteListener {
                if (it.result.child("ManageMatchDay").child(user).child("DoublePointsActivated").value.toString().toBoolean()) {
                    if (it.result.child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).hasChild("DoublePointsActivatedInMatch")) {
                        doublePointsButton.visibility = VISIBLE
                        doublePointsButton.background = itemView.resources.getColor(R.color.registration_submit).toDrawable()
                        doublePointsButton.text = itemView.resources.getString(R.string.double_points)
                        itemView.findViewById<RelativeLayout>(R.id.double_points_borders).background = itemView.resources.getDrawable(R.drawable.double_points_border)
                    }
                    else {
                        doublePointsButton.visibility = GONE
                    }
                }
                else {
                    if (matchesPoints.text != itemView.resources.getString(R.string.not_predicted)) {
                        if (!(homeScore != "null" && guestScore != "null" && actualDate >= matchDate)) {
                            if (!(actualTime.hour + 6 >= hourTime && actualDate == matchDate)) {
                                doublePointsButton.visibility = VISIBLE
                                doublePointsButton.background = itemView.resources.getColor(R.color.table_result_values).toDrawable()
                                doublePointsButton.text = itemView.resources.getString(R.string.activate_double_points)
                            }
                            else {
                                doublePointsButton.visibility = GONE
                            }
                        }
                        if (actualDate >= firstMatchDayDate) {
                            doublePointsButton.visibility = GONE
                        }
                    }
                }
            }

            homeTeamPrediction.setOnValueChangedListener { _, _, newVal ->
                matchReference.child("Predictions").child(user).child("Scores").child(homeTeam).setValue(newVal).addOnCompleteListener {
                    if (guestTeamPrediction.value == 0) {
                        matchReference.child("Predictions").child(user).child("Scores").child(guestTeam).setValue(0).addOnCompleteListener {}
                    }
                    if (matchPointsImage.visibility == GONE) {
                        matchPointsImage.visibility = VISIBLE
                        matchesPoints.text = itemView.resources.getString(R.string.zero)
                        if (!databaseRound.child("ManageMatchDay").child(user).child("DoublePointsActivated").value.toString().toBoolean()) {
                            if (actualDate >= firstMatchDayDate) {
                                doublePointsButton.visibility = GONE
                            }
                            else {
                                doublePointsButton.visibility = VISIBLE
                                doublePointsButton.text = itemView.resources.getString(R.string.activate_double_points)
                            }
                        }
                    }
                    updateLastRoundToShow(round, matchPosition, dbReference)
                }
            }

            guestTeamPrediction.setOnValueChangedListener { _, _, newVal ->
                matchReference.child("Predictions").child(user).child("Scores").child(guestTeam).setValue(newVal).addOnCompleteListener {
                    if (homeTeamPrediction.value == 0) {
                        matchReference.child("Predictions").child(user).child("Scores").child(homeTeam).setValue(0).addOnCompleteListener {}
                    }
                    if (matchPointsImage.visibility == GONE) {
                        matchPointsImage.visibility = VISIBLE
                        matchesPoints.text = itemView.resources.getString(R.string.zero)
                        if (!databaseRound.child("ManageMatchDay").child(user).child("DoublePointsActivated").value.toString().toBoolean()) {
                            if (actualDate >= firstMatchDayDate) {
                                doublePointsButton.visibility = GONE
                            }
                            else {
                                doublePointsButton.visibility = VISIBLE
                                doublePointsButton.text = itemView.resources.getString(R.string.activate_double_points)
                            }
                        }
                    }
                    updateLastRoundToShow(round, matchPosition, dbReference)
                }
            }

            matchInfoImage.setOnClickListener {
                if (matchesPoints.text != itemView.resources.getString(R.string.not_predicted) && itemView.findViewById<TextView>(R.id.status).text == itemView.resources.getString(R.string.finished)) {
                    updateLastRoundToShow(round, matchPosition, dbReference)
                    val navigateToMatchResultDetails = MatchesPredictionsDirections.actionMatchesPredictionsToMatchResultDetails(user, championship, season, round.toInt(), homeTeam, guestTeam)
                    itemView.findNavController().navigate(navigateToMatchResultDetails)
                }
            }

            assignMatchScorers.setOnClickListener {
                updateLastRoundToShow(round, matchPosition, dbReference)
                val navigateToMatchScorers = MatchesPredictionsDirections.actionMatchesPredictionsToMatchScorers(user, championship, season, round.toInt(), homeTeam, guestTeam)
                itemView.findNavController().navigate(navigateToMatchScorers)
            }

            assignMVP.setOnClickListener {
                updateLastRoundToShow(round, matchPosition, dbReference)
                val navigateToPredictMVP = MatchesPredictionsDirections.actionMatchesPredictionsToPredictMVP(user, championship, season, round.toInt(), homeTeam, guestTeam)
                itemView.findNavController().navigate(navigateToPredictMVP)
            }

            officialScorers.setOnClickListener {
                updateLastRoundToShow(round, matchPosition, dbReference)
                val navigateToOfficialScorers = MatchesPredictionsDirections.actionMatchesPredictionsToOfficialMatchScorers(user, championship, season, round.toInt(), homeTeam, guestTeam)
                itemView.findNavController().navigate(navigateToOfficialScorers)
            }

            assignOfficialMVP.setOnClickListener {
                updateLastRoundToShow(round, matchPosition, dbReference)
                val navigateToOfficialMVP = MatchesPredictionsDirections.actionMatchesPredictionsToOfficialMVP(user, championship, season, round.toInt(), homeTeam, guestTeam)
                itemView.findNavController().navigate(navigateToOfficialMVP)
            }

            doublePointsButton.setOnClickListener {
                val fireworksLottie = itemView.findViewById<LottieAnimationView>(R.id.fireworks_animation)
                if (actualDate < firstMatchDayDate) {
                    if (!(homeScore != "null" && guestScore != "null" && actualDate >= matchDate)) {
                        if (!(actualTime.hour + 6 >= hourTime && actualDate == matchDate)) {
                            matchReference.child("Predictions").get().addOnCompleteListener {
                                if (it.result.hasChild(user)) {
                                    itemView.findViewById<RelativeLayout>(R.id.double_points_borders).background = itemView.resources.getDrawable(R.drawable.no_double_points)
                                    fireworksLottie.visibility = GONE
                                    if (it.result.child(user).hasChild("DoublePointsActivatedInMatch")) {
                                        roundReference.child("ManageMatchDay").child(user).child("DoublePointsActivated").setValue(false).addOnCompleteListener {}
                                        roundReference.child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).child("DoublePointsActivatedInMatch").removeValue().addOnCompleteListener {
                                            updateLastRoundToShow(round, matchPosition, dbReference)
                                            notifyDataSetChanged()
                                        }
                                    }
                                    else {
                                        itemView.findViewById<RelativeLayout>(R.id.double_points_borders).background = itemView.resources.getDrawable(R.drawable.double_points_border)
                                        fireworksLottie.visibility = VISIBLE
                                        fireworksLottie.addAnimatorListener(object : Animator.AnimatorListener {
                                            override fun onAnimationStart(animation: Animator) {}

                                            override fun onAnimationEnd(animation: Animator) {
                                                fireworksLottie.visibility = GONE
                                            }

                                            override fun onAnimationCancel(animation: Animator) {}

                                            override fun onAnimationRepeat(animation: Animator) {}
                                        })
                                        roundReference.child("ManageMatchDay").child(user).child("DoublePointsActivated").setValue(true).addOnCompleteListener {}
                                        roundReference.child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).child("DoublePointsActivatedInMatch").setValue("Activated").addOnCompleteListener {
                                            updateLastRoundToShow(round, matchPosition, dbReference)
                                            notifyDataSetChanged()
                                        }
                                    }
                                }
                                else {
                                    Toast.makeText(itemView.context, R.string.match_not_predicted, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
                else {
                    if (itemView.findViewById<TextView>(R.id.status).text.toString() == itemView.resources.getString(R.string.programmed)) {
                        val builder = AlertDialog.Builder(itemView.context)
                        builder.setMessage(itemView.resources.getString(R.string.match_day_ongoing))

                        builder.setPositiveButton(itemView.resources.getString(R.string.ok)) { dialog, _ ->
                            dialog.dismiss()
                        }

                        val dialog = builder.create()
                        dialog.show()
                    }
                }
            }

            openOfficialScorers.setOnClickListener {
                val officialScorersOpened = itemView.findViewById<ImageView>(R.id.open_official_scorers)
                if ((homeScore.toInt() > 0 && officialHomeScorers.visibility == GONE) || (guestScore.toInt() > 0 && officialGuestScorers.visibility == GONE)) {
                    officialScorersOpened.setImageResource(R.drawable.arrow_down)
                    if (homeScore.toInt() > 0) {
                        officialScorersAdapter(officialHomeScorers, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, matchGet)
                    }
                    if (guestScore.toInt() > 0) {
                        officialScorersAdapter(officialGuestScorers, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, matchGet)
                    }
                }
                else {
                    officialScorersOpened.setImageResource(R.drawable.arrow_right)
                    officialHomeScorers.visibility = GONE
                    officialGuestScorers.visibility = GONE
                }
                updateLastRoundToShow(round, matchPosition, dbReference)
            }

            openPredictedScorers.setOnClickListener {
                val predictedScorers = itemView.findViewById<ImageView>(R.id.open_predicted_scorers)
                if ((homeScorersPredictedCount > 0 && predictedHomeScorers.visibility == GONE) || (guestScorersPredictedCount > 0 && predictedGuestScorers.visibility == GONE)) {
                    predictedScorers.setImageResource(R.drawable.arrow_down)
                    if (homeScorersPredictedCount > 0) {
                        predictedScorersAdapter(predictedHomeScorers, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, matchGet, matchDate, actualDate, hourTime, actualTime)
                    }
                    if (guestScorersPredictedCount > 0) {
                        predictedScorersAdapter(predictedGuestScorers, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, matchGet, matchDate, actualDate, hourTime, actualTime)
                    }
                }
                else {
                    predictedScorers.setImageResource(R.drawable.arrow_right)
                    predictedHomeScorers.visibility = GONE
                    predictedGuestScorers.visibility = GONE
                }
                updateLastRoundToShow(round, matchPosition, dbReference)
            }

            matchFinished.setOnClickListener {
                showConfirmationMatchFinished(itemView.context, matchReference)
                updateLastRoundToShow(round, matchPosition, dbReference)
            }

            if (officialMVP != null || predictedMVP != null) {
                openMVP.visibility = VISIBLE
            }

            openMVP.setOnClickListener {
                val mvp = itemView.findViewById<ImageView>(R.id.open_mvp)
                if (showPredictedMVP.visibility == GONE && showOfficialMVP.visibility == GONE) {
                    mvp.setImageResource(R.drawable.arrow_down)
                    if (predictedMVP != null) {
                        showPredictedMVP.visibility = VISIBLE
                        val getPredictedPlayer = databaseGet.child("Players").child(season).child(predictedMVP.team).child(predictedMVP.shirt.toString())
                        val playerNameToShow = "${getPredictedPlayer.child("firstName").value.toString()} ${getPredictedPlayer.child("lastName").value.toString()} (${predictedMVP.team})"
                        itemView.findViewById<TextView>(R.id.predicted_mvp_value).text = playerNameToShow
                    }
                    if (officialMVP != null) {
                        showOfficialMVP.visibility = VISIBLE
                        val getOfficialPlayer = databaseGet.child("Players").child(season).child(officialMVP.team).child(officialMVP.shirt.toString())
                        val playerNameToShow = "${getOfficialPlayer.child("firstName").value.toString()} ${getOfficialPlayer.child("lastName").value.toString()} (${officialMVP.team})"
                        itemView.findViewById<TextView>(R.id.official_mvp_value).text = playerNameToShow
                    }

                    if (predictedMVP != null && officialMVP != null) {
                        val mvpGuessed = itemView.findViewById<ImageView>(R.id.mvp_guessed)
                        mvpGuessed.visibility = VISIBLE
                        if (predictedMVP.team == officialMVP.team && predictedMVP.shirt == officialMVP.shirt) {
                            mvpGuessed.setImageResource(R.drawable.completed)
                        }
                        else {
                            mvpGuessed.setImageResource(R.drawable.wrong)
                        }
                    }
                }
                else {
                    mvp.setImageResource(R.drawable.arrow_right)
                    showPredictedMVP.visibility = GONE
                    showOfficialMVP.visibility = GONE
                }
                updateLastRoundToShow(round, matchPosition, dbReference)
            }
        }

        private fun setRedColorWhenLive(itemView: View) {
            itemView.findViewById<TextView>(R.id.status).text = itemView.resources.getString(R.string.live)
            val redColor = itemView.resources.getColor(R.color.red)
            itemView.findViewById<TextView>(R.id.status).setTextColor(redColor)
            itemView.findViewById<TextView>(R.id.home_real_result).setTextColor(redColor)
            itemView.findViewById<TextView>(R.id.vs).setTextColor(redColor)
            itemView.findViewById<TextView>(R.id.guest_real_result).setTextColor(redColor)
            matchesPoints.setTextColor(redColor)
        }

        private fun setBlackColorWhenFinished(itemView: View) {
            itemView.findViewById<TextView>(R.id.status).text = itemView.resources.getString(R.string.finished)
            val blackColor = itemView.resources.getColor(R.color.black)
            itemView.findViewById<TextView>(R.id.status).setTextColor(blackColor)
            itemView.findViewById<TextView>(R.id.home_real_result).setTextColor(blackColor)
            itemView.findViewById<TextView>(R.id.vs).setTextColor(blackColor)
            itemView.findViewById<TextView>(R.id.guest_real_result).setTextColor(blackColor)
            matchesPoints.setTextColor(blackColor)
        }

        private fun updateLastRoundToShow(round: String, matchPosition: Int, dbReference: SQLiteDatabase) {
            val lastRoundToShow = ContentValues()
            lastRoundToShow.put("LastRound", round.toInt() - 1)
            lastRoundToShow.put("LastMatchInRound", matchPosition)
            dbReference.update("USER_LAST_ACCESSED", lastRoundToShow, "UserNickname = ? AND Championship = ? AND Season = ?", arrayOf(user, championship, season))
        }

        private fun officialScorersAdapter(scorersRecyclerView: RecyclerView, team: String, resource: Int, goalTeam: String, ownGoalTeam: String, matchGet: DataSnapshot) {
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

        private fun predictedScorersAdapter(scorersRecyclerView: RecyclerView, team: String, resource: Int, goalTeam: String, ownGoalTeam: String, matchGet: DataSnapshot, matchDate: LocalDate, actualDate: LocalDate, hourTime: Int, actualTime: LocalDateTime) {
            scorersRecyclerView.visibility = VISIBLE
            val scorers = mutableListOf<Player>()
            val scoresPerPlayer = mutableMapOf<Player, Int>()
            val scorerTypes = mutableListOf<String>()
            for (s in matchGet.child("Predictions").child(user).child("Scorers").child(team).children) {
                val goalType = s.child("goalType").value.toString()
                val shirt = s.child("shirt").value.toString()
                val goalTypeTeam = if (goalType == "Goal") {
                    goalTeam
                }
                else {
                    ownGoalTeam
                }
                val scorerInfo = databaseGet.child("Players").child(season).child(goalTypeTeam).child(shirt)
                val firstName = scorerInfo.child("firstName").value.toString()
                val lastName = scorerInfo.child("lastName").value.toString()
                val role = scorerInfo.child("role").value.toString()
                val newPlayer = Player(firstName, lastName, shirt.toInt(), role, goalTypeTeam)
                if (!scorers.contains(newPlayer)) {
                    scorers.add(Player(firstName, lastName, shirt.toInt(), role, goalTypeTeam))
                    scorerTypes.add(goalType)
                }
                if (scoresPerPlayer.containsKey(newPlayer)) {
                    scoresPerPlayer[newPlayer] = scoresPerPlayer[newPlayer]!! + 1
                }
                else {
                    scoresPerPlayer[newPlayer] = 1
                }
                val isMatchStarted = actualDate.isEqual(matchDate) && actualTime.hour >= hourTime
                val isMatchFinished = actualDate.isAfter(matchDate)
                val scorersAdapter = PredictedScorersAdapter(scorers, scorerTypes, scoresPerPlayer, scorersGuessedWithMoreNets, isMatchStarted, isMatchFinished, resource)
                scorersRecyclerView.adapter = scorersAdapter
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        private fun showConfirmationMatchFinished(context: Context, matchReference: DatabaseReference) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage("${itemView.resources.getString(R.string.confirmation_finished)} ${home.text}-${guest.text} ${itemView.resources.getString(R.string.as_finished)}")

            builder.setPositiveButton(itemView.resources.getString(R.string.confirm)) { _, _ ->
                matchReference.child("Finished").setValue(true).addOnCompleteListener {
                    notifyDataSetChanged()
                    Toast.makeText(context, "${home.text}-${guest.text} ${itemView.resources.getString(R.string.match_finished_assigned)}", Toast.LENGTH_SHORT).show()
                }
            }

            builder.setNegativeButton(itemView.resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchDayPredictorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.match_predictor_details, parent, false)
        return MatchDayPredictorViewHolder(view)
    }

    override fun getItemCount(): Int {
        return daysList.size
    }

    override fun onBindViewHolder(holder: MatchDayPredictorViewHolder, position: Int) {
        val round = daysList[position].round
        val homeTeam = daysList[position].homeTeam
        val guestTeam = daysList[position].guestTeam
        val numberedDateToCompare = daysList[position].dateNumbered.split("-")
        val matchYear = numberedDateToCompare[0].toInt()
        val matchMonth = numberedDateToCompare[1].toInt()
        val matchDay = numberedDateToCompare[2].toInt()

        val matchDate = LocalDate.of(matchYear, matchMonth, matchDay)
        val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)
        if (!daysList[position].isFinished && actualDate.isEqual(matchDate)) {
            databaseReference.child("Championships").child(championship).child(season).child("Matches").child(round).child("Matches").child("$homeTeam-$guestTeam").get().addOnCompleteListener { isMatchFinished ->
                if (isMatchFinished.result.hasChild("Finished")) {
                    holder.bind(user, championship, season, daysList[position].dateToShow, daysList[position].dateNumbered, daysList[position].time, homeTeam, guestTeam, round, daysList[position].homeScore, daysList[position].guestScore, true, position, firstDateRound, databaseGet, databaseReference)
                }
                else {
                    holder.bind(user, championship, season, daysList[position].dateToShow, daysList[position].dateNumbered, daysList[position].time, homeTeam, guestTeam, round, daysList[position].homeScore, daysList[position].guestScore, false, position, firstDateRound, databaseGet, databaseReference)
                }
            }
        }
        else if (!daysList[position].isFinished) {
            holder.bind(user, championship, season, daysList[position].dateToShow, daysList[position].dateNumbered, daysList[position].time, homeTeam, guestTeam, round, daysList[position].homeScore, daysList[position].guestScore, false, position, firstDateRound, databaseGet, databaseReference)
        }
        else {
            holder.bind(user, championship, season, daysList[position].dateToShow, daysList[position].dateNumbered, daysList[position].time, homeTeam, guestTeam, round, daysList[position].homeScore, daysList[position].guestScore, true, position, firstDateRound, databaseGet, databaseReference)
        }
    }
}