package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.OfficialScorersAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.PredictedScorersAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
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
import kotlin.math.abs

class MatchResultDetails : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_match_result_details, container, false)
    }

    @SuppressLint("DiscouragedApi", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = MatchResultDetailsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val homeTeam = arguments.homeTeam
        val guestTeam = arguments.guestTeam

        view.findViewById<ImageView>(R.id.back_to_championship_prediction).setOnClickListener {
            val navigateToMatchesPredictions = MatchResultDetailsDirections.actionMatchResultDetailsToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.show_histograms).setOnClickListener {
            val navigateToMatchHistograms = MatchResultDetailsDirections.actionMatchResultDetailsToMatchHistograms(user, championship, season, round, homeTeam, guestTeam)
            view.findNavController().navigate(navigateToMatchHistograms)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = MatchResultDetailsDirections.actionMatchResultDetailsToLoginRegistration()
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
        val homeTeamPrediction: NumberPicker = view.findViewById(R.id.home_result_prediction)
        val guestTeamPrediction: NumberPicker = view.findViewById(R.id.guest_result_prediction)
        val home: TextView = view.findViewById(R.id.home_team)
        val guest: TextView = view.findViewById(R.id.guest_team)
        val hScore: TextView = view.findViewById(R.id.home_real_result)
        val gScore: TextView = view.findViewById(R.id.guest_real_result)
        val matchesPoints: TextView = view.findViewById(R.id.match_points)
        val openOfficialScorers = view.findViewById<RelativeLayout>(R.id.official_scorers_info)
        val officialHomeScorers = view.findViewById<RecyclerView>(R.id.recycler_view_official_home_scorers)
        val officialGuestScorers = view.findViewById<RecyclerView>(R.id.recycler_view_official_guest_scorers)
        val openPredictedScorers = view.findViewById<RelativeLayout>(R.id.predicted_scorers_info)
        val predictedHomeScorers = view.findViewById<RecyclerView>(R.id.recycler_view_predicted_home_scorers)
        val predictedGuestScorers = view.findViewById<RecyclerView>(R.id.recycler_view_predicted_guest_scorers)

        if (championship == "UEFA Womens Euro") {
            home.text = view.resources.getString(view.resources.getIdentifier(homeTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
            guest.text = view.resources.getString(view.resources.getIdentifier(guestTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
        }
        else {
            home.text = homeTeam
            guest.text = guestTeam
        }

        var dayDescription = when (round) {
            in 1..100 -> { //regular season
                "${getString(R.string.regular_season)}\n${view.resources.getString(R.string.day)} $round"
            }
            120 -> { //round of 16
                getString(R.string.round_16)
            }
            125 -> { //quarterfinals
                getString(R.string.quarterfinals)
            }
            150 -> { //semifinals
                getString(R.string.semifinals)
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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name_result).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)"
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

        view.findViewById<TextView>(R.id.status).text = getString(R.string.finished)

        reference.get().addOnCompleteListener {
            val databaseGet = it.result
            val matchGet = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam")
            val scorersPredicted = matchGet.child("Predictions").child(user).child("Scorers")
            val homeScorersPredictedCount = scorersPredicted.child(homeTeam).childrenCount
            val guestScorersPredictedCount = scorersPredicted.child(guestTeam).childrenCount
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
            val scoresPredictions = matchGet.child("Predictions").child(user).child("Scores")
            homeTeamPrediction.minValue = scoresPredictions.child(homeTeam).value.toString().toInt()
            homeTeamPrediction.maxValue = scoresPredictions.child(homeTeam).value.toString().toInt()
            homeTeamPrediction.value = scoresPredictions.child(homeTeam).value.toString().toInt()
            guestTeamPrediction.minValue = scoresPredictions.child(guestTeam).value.toString().toInt()
            guestTeamPrediction.maxValue = scoresPredictions.child(guestTeam).value.toString().toInt()
            guestTeamPrediction.value = scoresPredictions.child(guestTeam).value.toString().toInt()
            matchesPoints.text = it.result.child("Championships").child(championship).child(season).child("TotalPoints").child(user).child(round.toString()).child("$homeTeam-$guestTeam").value.toString()
            view.findViewById<TextView>(R.id.home_team_goals).text = "${getString(R.string.goals)}: ${home.text}"
            view.findViewById<TextView>(R.id.guest_team_goals).text = "${getString(R.string.goals)}: ${guest.text}"

            if (homeTeamPrediction.value > guestTeamPrediction.value) {
                view.findViewById<TextView>(R.id.match_result_prediction).text = "${home.text}\n(${getString(R.string.match_result)[0]})"
            }
            else if (guestTeamPrediction.value > homeTeamPrediction.value) {
                view.findViewById<TextView>(R.id.match_result_prediction).text = "${guest.text}\n(${getString(R.string.match_result)[0]})"
            }
            else {
                view.findViewById<TextView>(R.id.match_result_prediction).text = "${getString(R.string.match_result)[2]}"
            }

            if (hScore.text.toString().toInt() > gScore.text.toString().toInt()) {
                view.findViewById<TextView>(R.id.match_result_real).text = "${home.text}\n(${getString(R.string.match_result)[0]})"
            }
            else if (gScore.text.toString().toInt() > hScore.text.toString().toInt()) {
                view.findViewById<TextView>(R.id.match_result_real).text = "${guest.text}\n(${getString(R.string.match_result)[0]})"
            }
            else {
                view.findViewById<TextView>(R.id.match_result_real).text = "${getString(R.string.match_result)[2]}"
            }

            val matchResultPoints = view.findViewById<TextView>(R.id.match_result_points)
            val homePoints = view.findViewById<TextView>(R.id.home_team_goals_points)
            val guestPoints = view.findViewById<TextView>(R.id.guest_team_goals_points)
            val differencePoints = view.findViewById<TextView>(R.id.goal_difference_points)

            if ((homeTeamPrediction.value > guestTeamPrediction.value && hScore.text.toString().toInt() > gScore.text.toString().toInt()) ||
                (guestTeamPrediction.value > homeTeamPrediction.value && gScore.text.toString().toInt() > hScore.text.toString().toInt()) ||
                (homeTeamPrediction.value == guestTeamPrediction.value && hScore.text.toString().toInt() == gScore.text.toString().toInt())) {
                matchResultPoints.text = pointsRules[0].toString()
            }
            else {
                matchResultPoints.text = "0"
            }

            view.findViewById<TextView>(R.id.home_team_goals_prediction).text = homeTeamPrediction.value.toString()
            view.findViewById<TextView>(R.id.home_team_goals_real).text = hScore.text.toString()

            if (homeTeamPrediction.value == hScore.text.toString().toInt()) {
                homePoints.text = pointsRules[1].toString()
            }
            else {
                homePoints.text = "0"
            }

            view.findViewById<TextView>(R.id.guest_team_goals_prediction).text = guestTeamPrediction.value.toString()
            view.findViewById<TextView>(R.id.guest_team_goals_real).text = gScore.text.toString()

            if (guestTeamPrediction.value == gScore.text.toString().toInt()) {
                guestPoints.text = pointsRules[1].toString()
            }
            else {
                guestPoints.text = "0"
            }

            view.findViewById<TextView>(R.id.goal_difference_prediction).text = abs(homeTeamPrediction.value - guestTeamPrediction.value).toString()
            view.findViewById<TextView>(R.id.goal_difference_real).text = abs(hScore.text.toString().toInt() - gScore.text.toString().toInt()).toString()

            if (abs(homeTeamPrediction.value - guestTeamPrediction.value) == abs(hScore.text.toString().toInt() - gScore.text.toString().toInt())) {
                differencePoints.text = pointsRules[2].toString()
            }
            else {
                differencePoints.text = "0"
            }
            var totalPoints = matchResultPoints.text.toString().toInt() + homePoints.text.toString().toInt() + guestPoints.text.toString().toInt() + differencePoints.text.toString().toInt()

            if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                openOfficialScorers.visibility = VISIBLE
            }

            if (homeScorersPredictedCount > 0 || guestScorersPredictedCount > 0) {
                openPredictedScorers.visibility = VISIBLE
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
            val scorersGuessedWithMoreNets = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            var goalNumberPredicted = 0
            var ownGoalNumberPredicted = 0
            var goalsBonus = 0
            var ownGoalsBonus = 0
            for (o in allOfficials.keys) {
                if (allPredicted.containsKey(o)) {
                    if (o.goalType == "Goal") {
                        totalPoints += pointsRules[3] //goal for a scorer
                        goalNumberPredicted++
                    } else {
                        totalPoints += pointsRules[4] //own goal for a scorer
                        ownGoalNumberPredicted++
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
                        if (o.goalType == "Goal") {
                            totalPoints += netsGuessed * pointsRules[5] //goals for a scorer for each goal
                            goalsBonus += netsGuessed
                        }
                        else {
                            totalPoints += netsGuessed * pointsRules[6] //own goals for a scorer for each own goal
                            ownGoalsBonus += netsGuessed
                        }
                    }
                    else {
                        scorersGuessedWithMoreNets[o] = 0
                    }
                }
            }

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

            val mvpPredictedCorrectly = view.findViewById<TextView>(R.id.mvp_guessed_correctly_value)
            val mvpGuessedResult: String
            if (officialMVP != null && predictedMVP != null && predictedMVP.team == officialMVP.team && predictedMVP.shirt == officialMVP.shirt) {
                totalPoints += pointsRules[7]
                mvpGuessedResult = "${getString(R.string.yes)} (${pointsRules[7]} ${getString(R.string.points)})"
            }
            else {
                mvpGuessedResult = "${getString(R.string.no)} (${getString(R.string.zero)} ${getString(R.string.points)})"
            }

            mvpPredictedCorrectly.text = mvpGuessedResult

            val goalTotalPoints = goalNumberPredicted * pointsRules[3]
            val ownGoalTotalPoints = ownGoalNumberPredicted * pointsRules[4]
            view.findViewById<TextView>(R.id.goal_scorers_number).text = "$goalNumberPredicted ($goalTotalPoints ${getString(R.string.points)})"
            view.findViewById<TextView>(R.id.own_goal_scorers_number).text = "$ownGoalNumberPredicted ($ownGoalTotalPoints ${getString(R.string.points)})"

            val goalBonusTotalPoints = goalsBonus * pointsRules[5]
            val ownGoalBonusTotalPoints = ownGoalsBonus * pointsRules[6]
            view.findViewById<TextView>(R.id.goal_bonus_number).text = "$goalsBonus ($goalBonusTotalPoints ${getString(R.string.points)})"
            view.findViewById<TextView>(R.id.own_goal_bonus_number).text = "$ownGoalsBonus ($ownGoalBonusTotalPoints ${getString(R.string.points)})"

            if (it.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).value.toString().contains("DoublePointsActivatedInMatch")) {
                view.findViewById<TextView>(R.id.double_points).text = getString(R.string.yes)
                totalPoints *= 2
            }
            else {
                view.findViewById<TextView>(R.id.double_points).text = getString(R.string.no)
            }

            view.findViewById<TextView>(R.id.total_points_value).text = totalPoints.toString()

            openOfficialScorers.setOnClickListener {
                val officialScorers = view.findViewById<ImageView>(R.id.open_official_scorers)
                if ((homeScore.toInt() > 0 && officialHomeScorers.visibility == View.GONE) || (guestScore.toInt() > 0 && officialGuestScorers.visibility == View.GONE)) {
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
                    officialHomeScorers.visibility = View.GONE
                    officialGuestScorers.visibility = View.GONE
                }
            }

            openPredictedScorers.setOnClickListener {
                val predictedScorers = view.findViewById<ImageView>(R.id.open_predicted_scorers)
                if ((homeScorersPredictedCount > 0 && predictedHomeScorers.visibility == View.GONE) || (guestScorersPredictedCount > 0 && predictedGuestScorers.visibility == View.GONE)) {
                    predictedScorers.setImageResource(R.drawable.arrow_down)
                    if (homeScorersPredictedCount > 0) {
                        predictedScorersAdapter(predictedHomeScorers, season, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, databaseGet, matchGet, user, scorersGuessedWithMoreNets)
                    }
                    if (guestScorersPredictedCount > 0) {
                        predictedScorersAdapter(predictedGuestScorers, season, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, databaseGet, matchGet, user, scorersGuessedWithMoreNets)
                    }
                }
                else {
                    predictedScorers.setImageResource(R.drawable.arrow_right)
                    predictedHomeScorers.visibility = View.GONE
                    predictedGuestScorers.visibility = View.GONE
                }
            }

            val openMVP: RelativeLayout = view.findViewById(R.id.mvp_info)
            val showPredictedMVP: RelativeLayout = view.findViewById(R.id.predicted_mvp_opened)
            val showOfficialMVP: RelativeLayout = view.findViewById(R.id.official_mvp_opened)

            if (officialMVP != null || predictedMVP != null) {
                openMVP.visibility = VISIBLE
            }

            openMVP.setOnClickListener {
                val mvp = view.findViewById<ImageView>(R.id.open_mvp)
                if (showPredictedMVP.visibility == View.GONE && showOfficialMVP.visibility == View.GONE) {
                    mvp.setImageResource(R.drawable.arrow_down)
                    if (predictedMVP != null) {
                        showPredictedMVP.visibility = VISIBLE
                        val getPredictedPlayer = databaseGet.child("Players").child(season).child(predictedMVP.team).child(predictedMVP.shirt.toString())
                        val playerNameToShow = "${getPredictedPlayer.child("firstName").value.toString()} ${getPredictedPlayer.child("lastName").value.toString()} (${predictedMVP.team})"
                        view.findViewById<TextView>(R.id.predicted_mvp_value).text = playerNameToShow
                    }
                    if (officialMVP != null) {
                        showOfficialMVP.visibility = VISIBLE
                        val getOfficialPlayer = databaseGet.child("Players").child(season).child(officialMVP.team).child(officialMVP.shirt.toString())
                        val playerNameToShow = "${getOfficialPlayer.child("firstName").value.toString()} ${getOfficialPlayer.child("lastName").value.toString()} (${officialMVP.team})"
                        view.findViewById<TextView>(R.id.official_mvp_value).text = playerNameToShow
                    }

                    if (predictedMVP != null && officialMVP != null) {
                        val mvpGuessed = view.findViewById<ImageView>(R.id.mvp_guessed)
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
                    showPredictedMVP.visibility = View.GONE
                    showOfficialMVP.visibility = View.GONE
                }
            }
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

    private fun predictedScorersAdapter(scorersRecyclerView: RecyclerView, season: String, team: String, resource: Int, goalTeam: String, ownGoalTeam: String, databaseGet: DataSnapshot, matchGet: DataSnapshot, user: String, scorersGuessed: Map<PointsGoalOrOwnGoal, Int>) {
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
            val scorersAdapter = PredictedScorersAdapter(scorers, scorerTypes, scoresPerPlayer, scorersGuessed, isStarted = true, isFinished = true, resource)
            scorersRecyclerView.adapter = scorersAdapter
        }
    }
}