package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.MatchPredictor
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DataSnapshot
import java.time.LocalDate
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth

class MatchDayComparisonAdapter(private val actualUser: String, private val vsUser: String, private val championship: String, private val season: String, private val daysList: List<MatchPredictor>, private val databaseGet: DataSnapshot) : RecyclerView.Adapter<MatchDayComparisonAdapter.MatchDayComparisonViewHolder>() {

    class MatchDayComparisonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val matchDateTextView: TextView = itemView.findViewById(R.id.match_date)
        private val matchTimeTextView: TextView = itemView.findViewById(R.id.match_time)
        private val home: TextView = itemView.findViewById(R.id.home_team)
        private val guest: TextView = itemView.findViewById(R.id.guest_team)
        private val hScore: TextView = itemView.findViewById(R.id.home_real_result)
        private val gScore: TextView = itemView.findViewById(R.id.guest_real_result)
        private val usersPredictions = itemView.findViewById<RelativeLayout>(R.id.users_predictions)
        private val openOfficialScorers: RelativeLayout = itemView.findViewById(R.id.official_scorers_info)
        private val officialHomeScorers: RecyclerView = itemView.findViewById(R.id.recycler_view_official_home_scorers)
        private val officialGuestScorers: RecyclerView = itemView.findViewById(R.id.recycler_view_official_guest_scorers)
        private val openPredictedScorers: RelativeLayout = itemView.findViewById(R.id.predicted_scorers_info)
        private val actualUserPredictedHomeScorers: RecyclerView = itemView.findViewById(R.id.actual_user_recycler_view_predicted_home_scorers)
        private val actualUserPredictedGuestScorers: RecyclerView = itemView.findViewById(R.id.actual_user_recycler_view_predicted_guest_scorers)
        private val vsUserPredictedHomeScorers: RecyclerView = itemView.findViewById(R.id.vs_user_recycler_view_predicted_home_scorers)
        private val vsUserPredictedGuestScorers: RecyclerView = itemView.findViewById(R.id.vs_user_recycler_view_predicted_guest_scorers)
        private val actualUserPredicted: RelativeLayout = itemView.findViewById(R.id.actual_user_predicted)
        private val vsUserPredicted: RelativeLayout = itemView.findViewById(R.id.vs_user_predicted)
        private val openMVP: RelativeLayout = itemView.findViewById(R.id.mvp_info)
        private val showPredictedMVP: RelativeLayout = itemView.findViewById(R.id.predicted_mvp_opened)
        private val showOfficialMVP: RelativeLayout = itemView.findViewById(R.id.official_mvp_opened)
        private val openTotalPoints: RelativeLayout = itemView.findViewById(R.id.total_points_info)
        private val usersTotalPoints: RelativeLayout = itemView.findViewById(R.id.users_total_points)

        @SuppressLint("DiscouragedApi")
        fun bind(actualUser: String, vsUser: String, championship: String, season: String, dateToShow: String, dateNumbered: String, time: String, homeTeam: String, guestTeam: String, round: String, homeScore: String, guestScore: String, databaseGet: DataSnapshot) {
            matchTimeTextView.text = time
            if (championship == "UEFA Womens Euro") {
                home.text = itemView.resources.getString(itemView.resources.getIdentifier(homeTeam.lowercase().replace(" ", "_"), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
                guest.text = itemView.resources.getString(itemView.resources.getIdentifier(guestTeam.lowercase().replace(" ", "_"), "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            }
            else {
                home.text = homeTeam
                guest.text = guestTeam
            }

            hScore.text = homeScore
            gScore.text = guestScore

            val numberedDateToCompare = dateNumbered.split("-")
            val matchYear = numberedDateToCompare[0].toInt()
            val matchMonth = numberedDateToCompare[1].toInt()
            val matchDay = numberedDateToCompare[2].toInt()

            val matchDate = LocalDate.of(matchYear, matchMonth, matchDay)
            val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)

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

            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase
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

            val matchGet = databaseGet.child("Championships").child(championship).child(season).child("Matches").child(round).child("Matches").child("$homeTeam-$guestTeam")
            val predictionsGet = matchGet.child("Predictions")
            val actualUserPredictionsGet = predictionsGet.child(actualUser)
            val actualUserScores = actualUserPredictionsGet.child("Scores")
            itemView.findViewById<TextView>(R.id.actual_user_scores_nickname).text = actualUser
            itemView.findViewById<TextView>(R.id.actual_user_home_prediction).text = actualUserScores.child(homeTeam).value.toString()
            itemView.findViewById<TextView>(R.id.actual_user_guest_prediction).text = actualUserScores.child(guestTeam).value.toString()

            val vsUserPredictionsGet = predictionsGet.child(vsUser)
            val vsUserScores = vsUserPredictionsGet.child("Scores")
            itemView.findViewById<TextView>(R.id.vs_user_scores_nickname).text = vsUser
            itemView.findViewById<TextView>(R.id.vs_user_home_prediction).text = vsUserScores.child(homeTeam).value.toString()
            itemView.findViewById<TextView>(R.id.vs_user_guest_prediction).text = vsUserScores.child(guestTeam).value.toString()

            itemView.findViewById<RelativeLayout>(R.id.predicted_scores_info).setOnClickListener {
                val scores = itemView.findViewById<ImageView>(R.id.open_predicted_scores)
                if (usersPredictions.visibility == GONE) {
                    scores.setImageResource(R.drawable.arrow_down)
                    usersPredictions.visibility = VISIBLE
                }
                else {
                    scores.setImageResource(R.drawable.arrow_right)
                    usersPredictions.visibility = GONE
                }
            }

            val actualUserScorersPredicted = predictionsGet.child(actualUser).child("Scorers")
            val actualUserHomeScorersPredictedCount = actualUserScorersPredicted.child(homeTeam).childrenCount
            val actualUserGuestScorersPredictedCount = actualUserScorersPredicted.child(guestTeam).childrenCount

            val vsUserScorersPredicted = predictionsGet.child(vsUser).child("Scorers")
            val vsUserHomeScorersPredictedCount = vsUserScorersPredicted.child(homeTeam).childrenCount
            val vsUserGuestScorersPredictedCount = vsUserScorersPredicted.child(guestTeam).childrenCount

            val homeOfficialScorers = matchGet.child("OfficialScorers").child(homeTeam)
            val actualUserHomeScorersPredicted = matchGet.child("Predictions").child(actualUser).child("Scorers").child(homeTeam)
            val vsUserHomeScorersPredicted = matchGet.child("Predictions").child(vsUser).child("Scorers").child(homeTeam)
            val guestOfficialScorers = matchGet.child("OfficialScorers").child(guestTeam)
            val actualUserGuestScorersPredicted = matchGet.child("Predictions").child(actualUser).child("Scorers").child(guestTeam)
            val vsUserGuestScorersPredicted = matchGet.child("Predictions").child(vsUser).child("Scorers").child(guestTeam)
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
            val actualUserAllPredicted = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            for (ap in actualUserHomeScorersPredicted.children) {
                val goalType = ap.child("goalType").value.toString()
                val shirt = ap.child("shirt").value.toString().toInt()
                val newActualUserPredictedToAdd = PointsGoalOrOwnGoal(goalType, shirt, homeTeam)
                if (actualUserAllPredicted.containsKey(newActualUserPredictedToAdd)) {
                    actualUserAllPredicted[newActualUserPredictedToAdd] = actualUserAllPredicted[newActualUserPredictedToAdd]!! + 1
                }
                else {
                    actualUserAllPredicted[newActualUserPredictedToAdd] = 1
                }
            }
            for (ap in actualUserGuestScorersPredicted.children) {
                val goalType = ap.child("goalType").value.toString()
                val shirt = ap.child("shirt").value.toString().toInt()
                val newActualUserPredictedToAdd = PointsGoalOrOwnGoal(goalType, shirt, guestTeam)
                if (actualUserAllPredicted.containsKey(newActualUserPredictedToAdd)) {
                    actualUserAllPredicted[newActualUserPredictedToAdd] = actualUserAllPredicted[newActualUserPredictedToAdd]!! + 1
                }
                else {
                    actualUserAllPredicted[newActualUserPredictedToAdd] = 1
                }
            }
            val vsUserAllPredicted = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            for (vp in vsUserHomeScorersPredicted.children) {
                val goalType = vp.child("goalType").value.toString()
                val shirt = vp.child("shirt").value.toString().toInt()
                val newVsUserPredictedToAdd = PointsGoalOrOwnGoal(goalType, shirt, homeTeam)
                if (vsUserAllPredicted.containsKey(newVsUserPredictedToAdd)) {
                    vsUserAllPredicted[newVsUserPredictedToAdd] = vsUserAllPredicted[newVsUserPredictedToAdd]!! + 1
                }
                else {
                    vsUserAllPredicted[newVsUserPredictedToAdd] = 1
                }
            }
            for (vp in vsUserGuestScorersPredicted.children) {
                val goalType = vp.child("goalType").value.toString()
                val shirt = vp.child("shirt").value.toString().toInt()
                val newVsUserPredictedToAdd = PointsGoalOrOwnGoal(goalType, shirt, guestTeam)
                if (vsUserAllPredicted.containsKey(newVsUserPredictedToAdd)) {
                    vsUserAllPredicted[newVsUserPredictedToAdd] = vsUserAllPredicted[newVsUserPredictedToAdd]!! + 1
                }
                else {
                    vsUserAllPredicted[newVsUserPredictedToAdd] = 1
                }
            }

            val actualUserScorersGuessedWithMoreNets = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            val vsUserScorersGuessedWithMoreNets = mutableMapOf<PointsGoalOrOwnGoal, Int>()
            for (o in allOfficials.keys) {
                val officialScorerNets = allOfficials[o]!!
                if (actualUserAllPredicted.containsKey(o)) {
                    val actualUserPredictedScorerNets = actualUserAllPredicted[o]!!
                    if (officialScorerNets > 1 && actualUserPredictedScorerNets > 1) {
                        if (officialScorerNets >= actualUserPredictedScorerNets) {
                            actualUserScorersGuessedWithMoreNets[o] = actualUserPredictedScorerNets - 1
                        }
                        else {
                            actualUserScorersGuessedWithMoreNets[o] = officialScorerNets - 1
                        }
                    }
                    else {
                        actualUserScorersGuessedWithMoreNets[o] = 0
                    }
                }

                if (vsUserAllPredicted.containsKey(o)) {
                    val vsUserPredictedScorerNets = vsUserAllPredicted[o]!!
                    if (officialScorerNets > 1 && vsUserPredictedScorerNets > 1) {
                        if (officialScorerNets >= vsUserPredictedScorerNets) {
                            vsUserScorersGuessedWithMoreNets[o] = vsUserPredictedScorerNets - 1
                        }
                        else {
                            vsUserScorersGuessedWithMoreNets[o] = officialScorerNets - 1
                        }
                    }
                    else {
                        vsUserScorersGuessedWithMoreNets[o] = 0
                    }
                }
            }

            if (actualUserHomeScorersPredictedCount > 0 || actualUserGuestScorersPredictedCount > 0 || vsUserHomeScorersPredictedCount > 0 || vsUserGuestScorersPredictedCount > 0) {
                openPredictedScorers.visibility = VISIBLE
            }

            if (homeScore.toInt() > 0 || guestScore.toInt() > 0) {
                openOfficialScorers.visibility = VISIBLE
            }

            openPredictedScorers.setOnClickListener {
                val predictedScorers = itemView.findViewById<ImageView>(R.id.open_predicted_scorers)
                if (actualUserPredicted.visibility == GONE && vsUserPredicted.visibility == GONE) {
                    if (actualUserHomeScorersPredictedCount > 0 || actualUserGuestScorersPredictedCount > 0) {
                        actualUserPredicted.visibility = VISIBLE
                        itemView.findViewById<TextView>(R.id.actual_user_nickname).text = actualUser
                        predictedScorers.setImageResource(R.drawable.arrow_down)
                        if (actualUserHomeScorersPredictedCount > 0) {
                            predictedScorersAdapter(actualUser, actualUserScorersGuessedWithMoreNets, actualUserPredictedHomeScorers, season, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, matchGet, databaseGet)
                        }
                        if (actualUserGuestScorersPredictedCount > 0) {
                            predictedScorersAdapter(actualUser, actualUserScorersGuessedWithMoreNets, actualUserPredictedGuestScorers, season, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, matchGet, databaseGet)
                        }
                    }
                    if (vsUserHomeScorersPredictedCount > 0 || vsUserGuestScorersPredictedCount > 0) {
                        vsUserPredicted.visibility = VISIBLE
                        itemView.findViewById<TextView>(R.id.vs_user_nickname).text = vsUser
                        predictedScorers.setImageResource(R.drawable.arrow_down)
                        if (vsUserHomeScorersPredictedCount > 0) {
                            predictedScorersAdapter(vsUser, vsUserScorersGuessedWithMoreNets, vsUserPredictedHomeScorers, season, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, matchGet, databaseGet)
                        }
                        if (vsUserGuestScorersPredictedCount > 0) {
                            predictedScorersAdapter(vsUser, vsUserScorersGuessedWithMoreNets, vsUserPredictedGuestScorers, season, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, matchGet, databaseGet)
                        }
                    }
                }
                else {
                    predictedScorers.setImageResource(R.drawable.arrow_right)
                    actualUserPredicted.visibility = GONE
                    vsUserPredicted.visibility = GONE
                }
            }

            openOfficialScorers.setOnClickListener {
                val officialScorers = itemView.findViewById<ImageView>(R.id.open_official_scorers)
                if ((homeScore.toInt() > 0 && officialHomeScorers.visibility == GONE) || (guestScore.toInt() > 0 && officialGuestScorers.visibility == GONE)) {
                    officialScorers.setImageResource(R.drawable.arrow_down)
                    if (homeScore.toInt() > 0) {
                        officialScorersAdapter(officialHomeScorers, season, homeTeam, R.layout.home_scorer_info, homeTeam, guestTeam, matchGet, databaseGet)
                    }
                    if (guestScore.toInt() > 0) {
                        officialScorersAdapter(officialGuestScorers, season, guestTeam, R.layout.guest_scorer_info, guestTeam, homeTeam, matchGet, databaseGet)
                    }
                }
                else {
                    officialScorers.setImageResource(R.drawable.arrow_right)
                    officialHomeScorers.visibility = GONE
                    officialGuestScorers.visibility = GONE
                }
            }

            val findOfficialMVP = matchGet.child("OfficialMVP")
            var officialMVP: MVPPlayer? = null
            if (findOfficialMVP.value.toString() != "null") {
                officialMVP = MVPPlayer(findOfficialMVP.child("team").value.toString(), findOfficialMVP.child("shirt").value.toString().toInt())
            }
            val findActualUserPredictedMVP = matchGet.child("Predictions").child(actualUser).child("MVP")
            var actualUserPredictedMVP: MVPPlayer? = null
            if (findActualUserPredictedMVP.value.toString() != "null") {
                actualUserPredictedMVP = MVPPlayer(findActualUserPredictedMVP.child("team").value.toString(), findActualUserPredictedMVP.child("shirt").value.toString().toInt())
            }
            val findVsUserPredictedMVP = matchGet.child("Predictions").child(vsUser).child("MVP")
            var vsUserPredictedMVP: MVPPlayer? = null
            if (findVsUserPredictedMVP.value.toString() != "null") {
                vsUserPredictedMVP = MVPPlayer(findVsUserPredictedMVP.child("team").value.toString(), findVsUserPredictedMVP.child("shirt").value.toString().toInt())
            }

            if (officialMVP != null || actualUserPredictedMVP != null || vsUserPredictedMVP != null) {
                openMVP.visibility = VISIBLE
            }

            openMVP.setOnClickListener {
                val mvp = itemView.findViewById<ImageView>(R.id.open_mvp)
                val openActualUserMVP = itemView.findViewById<RelativeLayout>(R.id.actual_user_mvp)
                val openVsUserMVP = itemView.findViewById<RelativeLayout>(R.id.vs_user_mvp)
                if (showPredictedMVP.visibility == GONE && showOfficialMVP.visibility == GONE) {
                    mvp.setImageResource(R.drawable.arrow_down)
                    if (actualUserPredictedMVP != null || vsUserPredictedMVP != null) {
                        showPredictedMVP.visibility = VISIBLE
                        if (actualUserPredictedMVP != null) {
                            openActualUserMVP.visibility = VISIBLE
                            val getPredictedPlayer = databaseGet.child("Players").child(season).child(actualUserPredictedMVP.team).child(actualUserPredictedMVP.shirt.toString())
                            val playerNameToShow = "${getPredictedPlayer.child("firstName").value.toString()} ${getPredictedPlayer.child("lastName").value.toString()} (${actualUserPredictedMVP.team})"
                            itemView.findViewById<TextView>(R.id.actual_user_predicted_mvp_nickname).text = actualUser
                            itemView.findViewById<TextView>(R.id.actual_user_predicted_mvp_value).text = playerNameToShow
                        }
                        else {
                            openActualUserMVP.visibility = GONE
                        }

                        if (vsUserPredictedMVP != null) {
                            openVsUserMVP.visibility = VISIBLE
                            val getPredictedPlayer = databaseGet.child("Players").child(season).child(vsUserPredictedMVP.team).child(vsUserPredictedMVP.shirt.toString())
                            val playerNameToShow = "${getPredictedPlayer.child("firstName").value.toString()} ${getPredictedPlayer.child("lastName").value.toString()} (${vsUserPredictedMVP.team})"
                            itemView.findViewById<TextView>(R.id.vs_user_predicted_mvp_nickname).text = vsUser
                            itemView.findViewById<TextView>(R.id.vs_user_predicted_mvp_value).text = playerNameToShow
                        }
                        else {
                            openVsUserMVP.visibility = GONE
                        }
                    }
                    if (officialMVP != null) {
                        showOfficialMVP.visibility = VISIBLE
                        val getOfficialPlayer = databaseGet.child("Players").child(season).child(officialMVP.team).child(officialMVP.shirt.toString())
                        val playerNameToShow = "${getOfficialPlayer.child("firstName").value.toString()} ${getOfficialPlayer.child("lastName").value.toString()} (${officialMVP.team})"
                        itemView.findViewById<TextView>(R.id.official_mvp_value).text = playerNameToShow
                    }

                    if (actualUserPredictedMVP != null && officialMVP != null) {
                        val mvpGuessed = itemView.findViewById<ImageView>(R.id.actual_user_mvp_guessed)
                        mvpGuessed.visibility = VISIBLE
                        if (actualUserPredictedMVP.team == officialMVP.team && actualUserPredictedMVP.shirt == officialMVP.shirt) {
                            mvpGuessed.setImageResource(R.drawable.completed)
                        }
                        else {
                            mvpGuessed.setImageResource(R.drawable.wrong)
                        }
                    }

                    if (vsUserPredictedMVP != null && officialMVP != null) {
                        val mvpGuessed = itemView.findViewById<ImageView>(R.id.vs_user_mvp_guessed)
                        mvpGuessed.visibility = VISIBLE
                        if (vsUserPredictedMVP.team == officialMVP.team && vsUserPredictedMVP.shirt == officialMVP.shirt) {
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
            }

            val homeOfficialDiscipline = matchGet.child("OfficialDiscipline").child(homeTeam)
            val homeActualUserPredictedDiscipline = matchGet.child("Predictions").child(actualUser).child("Discipline").child(homeTeam)
            val homeVsUserPredictedDiscipline = matchGet.child("Predictions").child(vsUser).child("Discipline").child(homeTeam)
            val guestOfficialDiscipline = matchGet.child("OfficialDiscipline").child(guestTeam)
            val guestActualUserPredictedDiscipline = matchGet.child("Predictions").child(actualUser).child("Discipline").child(guestTeam)
            val guestVsUserPredictedDiscipline = matchGet.child("Predictions").child(vsUser).child("Discipline").child(guestTeam)
            val allYellowOfficialsDiscipline = mutableListOf<MVPPlayer>()
            for (hodY in homeOfficialDiscipline.child("YellowCards").children) {
                val shirt = hodY.child("shirt").value.toString().toInt()
                allYellowOfficialsDiscipline.add(MVPPlayer(homeTeam, shirt))
            }
            for (godY in guestOfficialDiscipline.child("YellowCards").children) {
                val shirt = godY.child("shirt").value.toString().toInt()
                allYellowOfficialsDiscipline.add(MVPPlayer(guestTeam, shirt))
            }
            val allRedOfficialDiscipline = mutableMapOf<MVPPlayer, String>()
            for (hodR in homeOfficialDiscipline.child("RedCards").children) {
                val shirt = hodR.child("shirt").value.toString().toInt()
                val type = hodR.child("type").value.toString()
                allRedOfficialDiscipline[MVPPlayer(homeTeam, shirt)] = type
            }
            for (godR in guestOfficialDiscipline.child("RedCards").children) {
                val shirt = godR.child("shirt").value.toString().toInt()
                val type = godR.child("type").value.toString()
                allRedOfficialDiscipline[MVPPlayer(guestTeam, shirt)] = type
            }

            val allYellowActualUserPredictedDiscipline = mutableListOf<MVPPlayer>()
            for (hpdY in homeActualUserPredictedDiscipline.child("YellowCards").children) {
                val shirt = hpdY.child("shirt").value.toString().toInt()
                allYellowActualUserPredictedDiscipline.add(MVPPlayer(homeTeam, shirt))
            }
            for (gpdY in guestActualUserPredictedDiscipline.child("YellowCards").children) {
                val shirt = gpdY.child("shirt").value.toString().toInt()
                allYellowActualUserPredictedDiscipline.add(MVPPlayer(guestTeam, shirt))
            }
            val allRedActualUserPredictedDiscipline = mutableMapOf<MVPPlayer, String>()
            for (hpdR in homeActualUserPredictedDiscipline.child("RedCards").children) {
                val shirt = hpdR.child("shirt").value.toString().toInt()
                val type = hpdR.child("type").value.toString()
                allRedActualUserPredictedDiscipline[MVPPlayer(homeTeam, shirt)] = type
            }
            for (gpdR in guestActualUserPredictedDiscipline.child("RedCards").children) {
                val shirt = gpdR.child("shirt").value.toString().toInt()
                val type = gpdR.child("type").value.toString()
                allRedActualUserPredictedDiscipline[MVPPlayer(guestTeam, shirt)] = type
            }

            val allYellowVsUserPredictedDiscipline = mutableListOf<MVPPlayer>()
            for (hpdY in homeVsUserPredictedDiscipline.child("YellowCards").children) {
                val shirt = hpdY.child("shirt").value.toString().toInt()
                allYellowVsUserPredictedDiscipline.add(MVPPlayer(homeTeam, shirt))
            }
            for (gpdY in guestVsUserPredictedDiscipline.child("YellowCards").children) {
                val shirt = gpdY.child("shirt").value.toString().toInt()
                allYellowVsUserPredictedDiscipline.add(MVPPlayer(guestTeam, shirt))
            }
            val allRedVsUserPredictedDiscipline = mutableMapOf<MVPPlayer, String>()
            for (hpdR in homeVsUserPredictedDiscipline.child("RedCards").children) {
                val shirt = hpdR.child("shirt").value.toString().toInt()
                val type = hpdR.child("type").value.toString()
                allRedVsUserPredictedDiscipline[MVPPlayer(homeTeam, shirt)] = type
            }
            for (gpdR in guestVsUserPredictedDiscipline.child("RedCards").children) {
                val shirt = gpdR.child("shirt").value.toString().toInt()
                val type = gpdR.child("type").value.toString()
                allRedVsUserPredictedDiscipline[MVPPlayer(guestTeam, shirt)] = type
            }

            /*for (odY in allYellowOfficialsDiscipline) {
                if (allYellowPredictedDiscipline.contains(odY)) {
                    //totalPoints += pointsRules[8]
                }
            }

            for (odR in allRedOfficialDiscipline.keys) {
                if (allRedPredictedDiscipline.containsKey(odR)) {
                    //totalPoints += pointsRules[9]
                    if (allRedOfficialDiscipline[odR] == allRedPredictedDiscipline[odR]) {
                        //totalPoints += pointsRules[10]
                    }
                }
            }*/

            val totalPointsGet = databaseGet.child("Championships").child(championship).child(season).child("TotalPoints")
            val actualUserTotalPoints = totalPointsGet.child(actualUser).child(round).child("$homeTeam-$guestTeam").value.toString()
            val hasActualUserDoublePoints = predictionsGet.child(actualUser).hasChild("DoublePointsActivatedInMatch")
            val actualUserPointsInfo = if (hasActualUserDoublePoints) {
                "$actualUser (x2)"
            } else {
                actualUser
            }
            itemView.findViewById<TextView>(R.id.actual_user_points_nickname).text = actualUserPointsInfo
            itemView.findViewById<TextView>(R.id.actual_user_points).text = actualUserTotalPoints

            val vsUserTotalPoints = totalPointsGet.child(vsUser).child(round).child("$homeTeam-$guestTeam").value.toString()
            val hasVsUserDoublePoints = predictionsGet.child(vsUser).hasChild("DoublePointsActivatedInMatch")
            val vsUserPointsInfo = if (hasVsUserDoublePoints) {
                "$vsUser (x2)"
            } else {
                vsUser
            }
            itemView.findViewById<TextView>(R.id.vs_user_points_nickname).text = vsUserPointsInfo
            itemView.findViewById<TextView>(R.id.vs_user_points).text = vsUserTotalPoints

            openTotalPoints.setOnClickListener {
                val totalPoints = itemView.findViewById<ImageView>(R.id.open_total_points)
                if (usersTotalPoints.visibility == GONE) {
                    totalPoints.setImageResource(R.drawable.arrow_down)
                    usersTotalPoints.visibility = VISIBLE
                }
                else {
                    totalPoints.setImageResource(R.drawable.arrow_right)
                    usersTotalPoints.visibility = GONE
                }
            }
        }

        private fun officialScorersAdapter(scorersRecyclerView: RecyclerView, season: String, team: String, resource: Int, goalTeam: String, ownGoalTeam: String, matchGet: DataSnapshot, databaseGet: DataSnapshot) {
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

        private fun predictedScorersAdapter(user: String, scorersGuessed: Map<PointsGoalOrOwnGoal, Int>, scorersRecyclerView: RecyclerView, season: String, team: String, resource: Int, goalTeam: String, ownGoalTeam: String, matchGet: DataSnapshot, databaseGet: DataSnapshot) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchDayComparisonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.match_predictor_comparison, parent, false)
        return MatchDayComparisonViewHolder(view)
    }

    override fun getItemCount(): Int {
        return daysList.size
    }

    override fun onBindViewHolder(holder: MatchDayComparisonViewHolder, position: Int) {
        holder.bind(actualUser, vsUser, championship, season, daysList[position].dateToShow, daysList[position].dateNumbered, daysList[position].time, daysList[position].homeTeam, daysList[position].guestTeam, daysList[position].round, daysList[position].homeScore, daysList[position].guestScore, databaseGet)
    }
}