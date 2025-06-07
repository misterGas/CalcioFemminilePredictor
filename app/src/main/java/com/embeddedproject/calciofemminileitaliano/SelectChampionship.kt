package com.embeddedproject.calciofemminileitaliano

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.SeasonsAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.SelectChampionshipAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.SpecialEventsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.MatchInfo
import com.embeddedproject.calciofemminileitaliano.helpers.PointsGoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.SeasonPoints
import com.embeddedproject.calciofemminileitaliano.helpers.SpecialEvent
import com.embeddedproject.calciofemminileitaliano.helpers.Team
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class SelectChampionship : Fragment() {

    private val soccerDB = "https://www.thesportsdb.com/api/v1/json/3/"
    private lateinit var app: MainApplication

    //private val championshipsList = listOf("Italy Serie A Women", "UEFA Womens Euro", "English Womens Super League", "UEFA Womens Champions League")
    private var championshipsList = listOf("Italy Serie A Women")

    private val stringsToExclude = listOf("Women", "Milano", "AC", "WFC", "FC", "W", "Femenino", "Feminino")

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
        return inflater.inflate(R.layout.fragment_select_championship, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference
        val arguments = SelectChampionshipArgs.fromBundle(requireArguments())
        val user = arguments.userNickname

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        view.findViewById<ImageView>(R.id.profile).setOnClickListener {
            val navigateToUserInfo = SelectChampionshipDirections.actionSelectChampionshipToUserInfo(user)
            view.findNavController().navigate(navigateToUserInfo)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = SelectChampionshipDirections.actionSelectChampionshipToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        view.findViewById<Button>(R.id.prediction_rules).setOnClickListener {
            val navigateToPredictionRules = SelectChampionshipDirections.actionSelectChampionshipToPredictionsRules(user)
            view.findNavController().navigate(navigateToPredictionRules)
        }

        val addNewPlayers = view.findViewById<ImageView>(R.id.add_players)

        championshipsList = championshipsList.sorted()

        reference.child("User-Managers").get().addOnCompleteListener {
            val showManager = view.findViewById<ImageView>(R.id.manager)
            val managerActive = view.findViewById<ImageView>(R.id.manager_active)
            var isActive = false
            if (it.result.hasChild(user)) {
                showManager.visibility = VISIBLE
                isActive = it.result.child(user).child("Activated").value.toString().toBoolean()
                if (isActive) {
                    managerActive.setImageResource(R.drawable.completed)
                    showManager.tooltipText = getString(R.string.switch_to_normal_user)
                    addNewPlayers.visibility = VISIBLE
                }
                else {
                    managerActive.setImageResource(R.drawable.wrong)
                    showManager.tooltipText = getString(R.string.switch_to_manager)
                    addNewPlayers.visibility = GONE
                }
            }
            else {
                showManager.visibility = GONE
                addNewPlayers.visibility = GONE
            }

            showManager.setOnClickListener {
                val builder = AlertDialog.Builder(view.context)
                if (!isActive) {
                    builder.setMessage(getString(R.string.manager_mode_info))
                }
                else {
                    builder.setMessage(getString(R.string.normal_user_mode_info))
                }

                builder.setPositiveButton(getString(R.string.confirm)) { _, _ ->
                    if (!isActive) {
                        isActive = true
                        Toast.makeText(view.context, getString(R.string.manager_mode_activated), Toast.LENGTH_SHORT).show()
                        managerActive.setImageResource(R.drawable.completed)
                        showManager.tooltipText = getString(R.string.switch_to_normal_user)
                        addNewPlayers.visibility = VISIBLE

                    }
                    else {
                        isActive = false
                        Toast.makeText(view.context, getString(R.string.manager_mode_deactivated), Toast.LENGTH_SHORT).show()
                        managerActive.setImageResource(R.drawable.wrong)
                        showManager.tooltipText = getString(R.string.switch_to_manager)
                        addNewPlayers.visibility = GONE
                    }
                    reference.child("User-Managers").child(user).child("Activated").setValue(isActive).addOnCompleteListener {}
                }

                builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }

                val dialog = builder.create()
                dialog.show()
            }
        }

        reference.child("Users").get().addOnCompleteListener {
            for (u in it.result.children) {
                if (u.child("nickname").value == user) {
                    val initials = u.child("firstName").value.toString()[0].toString() + u.child("lastName").value.toString()[0].toString()
                    val tooltip = "${u.child("firstName").value.toString()} ${u.child("lastName").value.toString()}\n($user)"
                    view.findViewById<TextView>(R.id.profile_text).text = initials.uppercase()
                    view.findViewById<ImageView>(R.id.profile).tooltipText = tooltip
                    view.findViewById<TextView>(R.id.profile_text).text = initials.uppercase()
                    view.findViewById<ImageView>(R.id.profile).tooltipText = tooltip
                }
            }
        }

        //updates latest results data in database
        app = activity?.application as MainApplication
        db = FirebaseDatabase.getInstance()
        reference = db.reference
        var canBeUpdated = false
        val findLastUpdate = dbReference.rawQuery("SELECT UpdatedTime FROM LAST_UPDATED_TIME WHERE UserNickname = ?", arrayOf(user))
        val lastUpdateFound = findLastUpdate.count

        if (lastUpdateFound == 1) {
            if (findLastUpdate.moveToFirst()) {
                val lastUpdateString = findLastUpdate.getString(0)
                val lastUpdate = LocalDateTime.parse(lastUpdateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                val actualDateTime = LocalDateTime.now()

                if (lastUpdate.plusMinutes(3).isBefore(actualDateTime) || lastUpdate.plusMinutes(3).isEqual(actualDateTime)) {
                    canBeUpdated = true
                    val updateLastDataUpdatedTime = ContentValues()
                    val buildDateTime = actualDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    updateLastDataUpdatedTime.put("UpdatedTime", buildDateTime)
                    dbReference.update("LAST_UPDATED_TIME", updateLastDataUpdatedTime, "UserNickname = ?", arrayOf(user))
                }
            }
        }
        else {
            val insertLastDataUpdatedTime = ContentValues()
            val actualDateTime = LocalDateTime.now()
            val buildDateTime = actualDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            insertLastDataUpdatedTime.put("UserNickname", user)
            insertLastDataUpdatedTime.put("UpdatedTime", buildDateTime)
            dbReference.insert("LAST_UPDATED_TIME", null, insertLastDataUpdatedTime)
            canBeUpdated = true
        }
        findLastUpdate.close()
        reference.child("UpdatesActive").get().addOnCompleteListener { updateInDatabase ->
            if (canBeUpdated && updateInDatabase.result.value.toString().toBoolean()) {
                for (ch in championshipsList) {
                    reference.child("Championships").child(ch).get().addOnCompleteListener {
                        for (season in it.result.children) {
                            val s = season.key.toString()
                            val championshipInfo = season.child("Info")
                            val leagueName = championshipInfo.child("Name").value.toString()
                            val leagueId = championshipInfo.child("Id").value.toString()
                            val allRounds = championshipInfo.child("Rounds").value.toString().split(",")
                            seasonMatches(leagueName, leagueId, s, view, allRounds)
                        }
                    }
                }
            }
        }

        val allSeasons = mutableListOf<String>()

        val allChampionships = mutableListOf<List<SeasonPoints>>()
        //updates total points for predictions for all users
        reference.child("Championships").get().addOnCompleteListener {
            for (championship in it.result.children) {
                val c = championship.key.toString()
                val championshipSeasons = mutableListOf<SeasonPoints>()
                for (season in championship.children) {
                    val s = season.key.toString()
                    if (!allSeasons.contains(s)) {
                        allSeasons.add(s)
                    }
                    var seasonTotalPoints = 0
                    for (round in season.child("Matches").children) {
                        val r = round.key.toString()
                        for (match in round.child("Matches").children) {
                            val homeTeam = match.child("MatchInfo").child("homeTeam").value.toString()
                            val guestTeam = match.child("MatchInfo").child("guestTeam").value.toString()
                            val homeScore = match.child("MatchInfo").child("homeScore").value.toString()
                            val guestScore = match.child("MatchInfo").child("guestScore").value.toString()
                            for (userPrediction in match.child("Predictions").children) {
                                val userInDatabase = userPrediction.key.toString()
                                val homePrediction = userPrediction.child("Scores").child(homeTeam).value.toString()
                                val guestPrediction = userPrediction.child("Scores").child(guestTeam).value.toString()
                                var totalPoints = 0
                                if (homePrediction != "null" && guestPrediction != "null" && homeScore != "null" && guestScore != "null") {
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
                                    val find = it.result.child(c).child(s).child("Matches").child(r).child("Matches").child("$homeTeam-$guestTeam")
                                    val homeOfficialScorers = find.child("OfficialScorers").child(homeTeam)
                                    val homeScorersPredicted = find.child("Predictions").child(userInDatabase).child("Scorers").child(homeTeam)
                                    val guestOfficialScorers = find.child("OfficialScorers").child(guestTeam)
                                    val guestScorersPredicted = find.child("Predictions").child(userInDatabase).child("Scorers").child(guestTeam)
                                    val allOfficials = mutableMapOf<PointsGoalOrOwnGoal, Int>()
                                    for (o in homeOfficialScorers.children) {
                                        val goalType = o.child("goalType").value.toString()
                                        val shirt = o.child("shirt").value.toString().toInt()
                                        val newOfficialToAdd = PointsGoalOrOwnGoal(goalType, shirt, homeTeam)
                                        if (allOfficials.containsKey(newOfficialToAdd)) {
                                            allOfficials[newOfficialToAdd] = allOfficials[newOfficialToAdd]!! + 1
                                        }
                                        else {
                                            allOfficials[newOfficialToAdd] = 1
                                        }
                                    }
                                    for (o in guestOfficialScorers.children) {
                                        val goalType = o.child("goalType").value.toString()
                                        val shirt = o.child("shirt").value.toString().toInt()
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
                                                totalPoints += if (o.goalType == "Goal") {
                                                    netsGuessed * pointsRules[5] //goals for a scorer for each goal
                                                } else {
                                                    netsGuessed * pointsRules[6] //own goals for a scorer for each own goal
                                                }
                                            }
                                        }
                                    }

                                    val findOfficialMVP = find.child("OfficialMVP")
                                    var officialMVP: MVPPlayer? = null
                                    if (findOfficialMVP.value.toString() != "null") {
                                        officialMVP = MVPPlayer(findOfficialMVP.child("team").value.toString(), findOfficialMVP.child("shirt").value.toString().toInt())
                                    }
                                    val findPredictedMVP = find.child("Predictions").child(userInDatabase).child("MVP")
                                    var predictedMVP: MVPPlayer? = null
                                    if (findPredictedMVP.value.toString() != "null") {
                                        predictedMVP = MVPPlayer(findPredictedMVP.child("team").value.toString(), findPredictedMVP.child("shirt").value.toString().toInt())
                                    }
                                    if (officialMVP != null && predictedMVP != null && predictedMVP.team == officialMVP.team && predictedMVP.shirt == officialMVP.shirt) {
                                        totalPoints += pointsRules[7]
                                    }

                                    val hasDoublePoints = it.result.child(c).child(s).child("Matches").child(r).child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(userInDatabase)
                                    if (hasDoublePoints.value.toString().contains("DoublePointsActivatedInMatch")) {
                                        totalPoints *= 2
                                    }
                                    reference.child("Championships").child(c).child(s).child("TotalPoints").child(userInDatabase).child(r).child("$homeTeam-$guestTeam").setValue(totalPoints).addOnCompleteListener {}
                                }
                                if (userInDatabase == user) {
                                    seasonTotalPoints += totalPoints
                                }
                            }
                        }
                    }
                    championshipSeasons.add(SeasonPoints(c, s, seasonTotalPoints))
                }
                allChampionships.add(championshipSeasons.sortedByDescending { cs -> cs.season })
            }
            view.findViewById<ProgressBar>(R.id.progress_updating_total_points).visibility = INVISIBLE
            view.findViewById<RelativeLayout>(R.id.show_selection).visibility = VISIBLE
            val selectChampionship = SelectChampionshipAdapter(championshipsList, allChampionships, user)
            view.findViewById<RecyclerView>(R.id.recycler_view_championships_buttons).adapter = selectChampionship
            val seasonsAdapter = SeasonsAdapter(view.context, R.layout.season_dialog, allSeasons.sortedByDescending { s -> s })

            addNewPlayers.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.select_season_dialog, null)
                val seasonListView = dialogView.findViewById<ListView>(R.id.seasons)
                seasonListView.adapter = seasonsAdapter

                val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                    .setTitle(getString(R.string.select_season))
                    .setPositiveButton(R.string.confirm, null)
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                dialog.show()

                var seasonSelected: String? = null

                seasonListView.setOnItemClickListener { _, seasonView, season, _ ->
                    seasonSelected = seasonView.findViewById<TextView>(R.id.season_info).text.toString()
                    seasonsAdapter.setSelectedPosition(season)
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    if (seasonSelected != null) {
                        val navigateToAddPlayers = SelectChampionshipDirections.actionSelectChampionshipToAddPlayers(user, seasonSelected!!)
                        view.findNavController().navigate(navigateToAddPlayers)
                        dialog.dismiss()
                    }
                    else {
                        Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_LONG).show()
                    }
                }
                seasonsAdapter.setSelectedPosition(-1)
            }
        }

        val specialEventsRecyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_special_events)
        var userTeamName: String? = null

        var userFoundInTeams = false
        reference.get().addOnCompleteListener {
            for (t in it.result.child("TeamsEvents").children) {
                val team = t.key.toString()
                val creator = t.child("Creator").value.toString()
                if (creator == user) {
                    userFoundInTeams = true
                    userTeamName = team
                }
                for (c in t.child("Components").children) {
                    val teamComponent = c.key.toString()
                    if (teamComponent == user) {
                        userFoundInTeams = true
                        userTeamName = team
                    }
                }
            }
            val joinTeamButton = view.findViewById<Button>(R.id.join_team)
            if (userFoundInTeams) {
                joinTeamButton.visibility = GONE
                view.findViewById<TextView>(R.id.join_team_now).visibility = GONE
                view.findViewById<ProgressBar>(R.id.progress_updating_special_events).visibility = VISIBLE
                specialEventsRecyclerView.visibility = VISIBLE
            }
            else {
                joinTeamButton.visibility = VISIBLE
                view.findViewById<TextView>(R.id.join_team_now).visibility = VISIBLE
                view.findViewById<ProgressBar>(R.id.progress_updating_special_events).visibility = INVISIBLE
                specialEventsRecyclerView.visibility = GONE
            }
            joinTeamButton.setOnClickListener {
                val navigateToUserInfo = SelectChampionshipDirections.actionSelectChampionshipToUserInfo(user)
                view.findNavController().navigate(navigateToUserInfo)
            }

            val specialEvents = mutableListOf<SpecialEvent>()
            for (e in it.result.child("SpecialEvents").children) {
                for (s in e.children) {
                    for (id in s.children) {
                        val eventInfo = id.child("Info")
                        val date = eventInfo.child("date").value.toString()
                        val time = eventInfo.child("time").value.toString()
                        val season = s.key.toString()
                        val team1 = eventInfo.child("homeTeam").child("teamName").value.toString()
                        val team2 = eventInfo.child("guestTeam").child("teamName").value.toString()
                        val eventImage = eventInfo.child("image").value.toString()
                        val eventName = e.key.toString()

                        val utcDateTime = LocalDateTime.parse(date + "T" + time)
                        val utcZone = utcDateTime.atZone(ZoneId.of("UTC"))
                        val localDateTimeWithZone = utcZone.withZoneSameInstant(ZoneId.systemDefault())
                        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        val localDate = localDateTimeWithZone.format(dateFormatter)
                        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                        val localTime = localDateTimeWithZone.format(timeFormatter)

                        Thread {
                            val findTeam1ImageInDB = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team1))
                            if (findTeam1ImageInDB.count == 0) {
                                val imageBitmapToAdd = ContentValues()
                                imageBitmapToAdd.put("TeamName", team1)
                                val stream = ByteArrayOutputStream()
                                val teamImage = eventInfo.child("homeTeam").child("imageUrl").value.toString()
                                BitmapFactory.decodeStream(URL(teamImage).openConnection().getInputStream()).compress(Bitmap.CompressFormat.PNG, 80, stream)
                                imageBitmapToAdd.put("ImageBitmap", stream.toByteArray())
                                dbReference.insert("TEAM_IMAGE", null, imageBitmapToAdd)
                            }
                            findTeam1ImageInDB.close()

                            val findTeam2ImageInDB = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team2))
                            if (findTeam2ImageInDB.count == 0) {
                                val imageBitmapToAdd = ContentValues()
                                imageBitmapToAdd.put("TeamName", team2)
                                val stream = ByteArrayOutputStream()
                                val teamImage = eventInfo.child("guestTeam").child("imageUrl").value.toString()
                                BitmapFactory.decodeStream(URL(teamImage).openConnection().getInputStream()).compress(Bitmap.CompressFormat.PNG, 80, stream)
                                imageBitmapToAdd.put("ImageBitmap", stream.toByteArray())
                                dbReference.insert("TEAM_IMAGE", null, imageBitmapToAdd)
                            }
                            findTeam2ImageInDB.close()


                            val findEventNameImageInDB = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(eventName))
                            if (findEventNameImageInDB.count == 0) {
                                val imageBitmapToAdd = ContentValues()
                                imageBitmapToAdd.put("TeamName", eventName)
                                val stream = ByteArrayOutputStream()
                                BitmapFactory.decodeStream(URL(eventImage).openConnection().getInputStream()).compress(Bitmap.CompressFormat.PNG, 80, stream)
                                imageBitmapToAdd.put("ImageBitmap", stream.toByteArray())
                                dbReference.insert("TEAM_IMAGE", null, imageBitmapToAdd)
                            }
                            findEventNameImageInDB.close()
                            activity?.runOnUiThread {
                                view.findViewById<ProgressBar>(R.id.progress_updating_special_events).visibility = INVISIBLE
                                val specialEventsAdapter = SpecialEventsAdapter(specialEvents, user, userTeamName)
                                specialEventsRecyclerView.adapter = specialEventsAdapter
                            }
                        }.start()

                        val newSpecialEvent = SpecialEvent(localDate, localTime, eventName, season, id.key.toString().toInt(), team1, team2)
                        specialEvents.add(newSpecialEvent)
                    }
                }
            }
        }
    }

    private fun seasonMatches(leagueName: String, leagueId: String, season: String, view: View, allRounds: List<String>) {
        for (r in allRounds) {
            app.query = "eventsround.php?id=$leagueId&r=$r&s=$season"
            app.url = "$soccerDB${app.query}"
            app.request = Request.Builder().cacheControl(
                CacheControl.Builder()
                .maxAge(5, TimeUnit.MINUTES)
                .build()).url(app.url).build()
            app.client.newCall(app.request).enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {}

                override fun onResponse(call: Call, response: Response) {
                    val inputStream: BufferedInputStream = if (response.cacheResponse != null) {
                        BufferedInputStream(response.cacheResponse?.body?.byteStream())
                    }
                    else {
                        BufferedInputStream(response.body?.byteStream())
                    }
                    val read = BufferedReader(InputStreamReader(inputStream))
                    val text = read.readText()
                    val matches = text.split("\"strEvent\":")
                    val teamsList = mutableMapOf<String, String>()
                    for (m in matches) {
                        if (m.contains(",\"strFilename\":")) {
                            val teams = m.substring(0, m.indexOf(",")).removePrefix("\"").removeSuffix("\"").split(" vs ")
                            var homeTeam = teams[0].replace(".", "").replace("-", " ")
                            var guestTeam = teams[1].replace(".", "").replace("-", " ")
                            val keys = mutableListOf<String>()
                            for (e in stringsToExclude) {
                                keys.add(e.removePrefix("{").removeSuffix("}").removePrefix(", ").removeSuffix(", "))
                            }
                            for (k in keys) {
                                homeTeam = homeTeam.removePrefix("$k ").removeSuffix(" $k")
                                guestTeam = guestTeam.removePrefix("$k ").removeSuffix(" $k")
                            }
                            val homeTeamImage = m.substring(m.indexOf("\"strHomeTeamBadge\":") + "\"strHomeTeamBadge\":".length, m.indexOf(",", m.indexOf("\"strHomeTeamBadge\":"))).removePrefix("\"").removeSuffix("\"")
                            val guestTeamImage = m.substring(m.indexOf("\"strAwayTeamBadge\":") + "\"strAwayTeamBadge\":".length, m.indexOf(",", m.indexOf("\"strAwayTeamBadge\":"))).removePrefix("\"").removeSuffix("\"")
                            if (!teamsList.containsKey(homeTeam)) {
                                teamsList[homeTeam] = homeTeamImage
                            }
                            if (!teamsList.containsKey(guestTeam)) {
                                teamsList[guestTeam] = guestTeamImage
                            }
                            val day = m.substring(m.indexOf("\"dateEvent\":") + "\"dateEvent\":".length, m.indexOf(",", m.indexOf("\"dateEvent\":"))).removePrefix("\"").removeSuffix("\"")
                            var time = m.substring(m.indexOf("\"strTime\":") + "\"strTime\":".length, m.indexOf(",", m.indexOf("\"strTime\""))).removePrefix("\"").removeSuffix("\"")
                            time = if (time == "00:00:00") {
                                "To be defined"
                            }
                            else {
                                time.substring(0, 5)
                            }
                            val round = m.substring(m.indexOf("\"intRound\":") + "\"intRound\":".length, m.indexOf(",", m.indexOf("\"intRound\":"))).removePrefix("\"").removeSuffix("\"")
                            val homeScore = m.substring(m.indexOf("\"intHomeScore\":") + "\"intHomeScore\":".length, m.indexOf(",", m.indexOf("\"intHomeScore\":"))).removePrefix("\"").removeSuffix("\"")
                            val guestScore = m.substring(m.indexOf("\"intAwayScore\":") + "\"intAwayScore\":".length, m.indexOf(",", m.indexOf("\"intAwayScore\":"))).removePrefix("\"").removeSuffix("\"")
                            val status = m.substring(m.indexOf("\"strStatus\":") + "\"strStatus\":".length, m.indexOf(",", m.indexOf("\"strStatus\":"))).removePrefix("\"").removeSuffix("\"")
                            reference.child("Championships").child(leagueName).child(season).child("Matches").child(round).child("Matches").child("$homeTeam-$guestTeam").child("MatchInfo").setValue(MatchInfo(day, time, homeTeam, guestTeam, homeScore, guestScore)).addOnCompleteListener {}
                        }
                    }

                    val sqlDB = UserLoggedInHelper(view.context)
                    val dbReference = sqlDB.writableDatabase
                    for (t in teamsList) {
                        val teamName = t.key
                        val teamImage = t.value
                        val findTeamImageInDB = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(teamName))
                        if (findTeamImageInDB.count == 0) {
                            val imageBitmapToAdd = ContentValues()
                            imageBitmapToAdd.put("TeamName", teamName)
                            val stream = ByteArrayOutputStream()
                            BitmapFactory.decodeStream(URL(teamImage).openConnection().getInputStream()).compress(Bitmap.CompressFormat.PNG, 80, stream)
                            imageBitmapToAdd.put("ImageBitmap", stream.toByteArray())
                            dbReference.insert("TEAM_IMAGE", null, imageBitmapToAdd)
                        }
                        findTeamImageInDB.close()
                        reference.child("Championships").child(leagueName).child(season).child("Teams").child(teamName).setValue(Team(teamName, teamImage)).addOnCompleteListener {}
                    }
                }
            })
        }
    }
}