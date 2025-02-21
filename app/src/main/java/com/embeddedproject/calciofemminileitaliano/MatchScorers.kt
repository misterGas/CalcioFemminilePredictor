package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.adapters.AssignSlotsAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.ScorerAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.Slot
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

class MatchScorers : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    private val findHomeSlotsSelected = mutableListOf<Int>()
    private val findGuestSlotsSelected = mutableListOf<Int>()
    private lateinit var assignHomeSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignGuestSlotsAdapter: AssignSlotsAdapter
    private var lastHomeSlot = -1
    private var lastGuestSlot = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_match_scorers, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = MatchScorersArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val homeTeam = arguments.homeTeam
        val guestTeam = arguments.guestTeam

        view.findViewById<ImageView>(R.id.back_to_championship_prediction).setOnClickListener {
            val navigateToMatchesPredictions = MatchScorersDirections.actionMatchScorersToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = MatchScorersDirections.actionMatchScorersToLoginRegistration()
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

        if (championship == "UEFA Womens Euro") {
            home.text = getString(view.resources.getIdentifier(homeTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
            guest.text = getString(view.resources.getIdentifier(guestTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.assign_scorers)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_scorers).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.assign_scorers)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_scorers).text = resultDetails
        }

        view.findViewById<TextView>(R.id.season_info).text = season

        val setHomeTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(homeTeam))
        var homeBitmap: Bitmap? = null
        if (setHomeTeamImage.moveToFirst()) {
            homeBitmap = BitmapFactory.decodeByteArray(setHomeTeamImage.getBlob(0), 0, setHomeTeamImage.getBlob(0).size)
            view.findViewById<ImageView>(R.id.home_team_image).setImageBitmap(homeBitmap)
        }
        setHomeTeamImage.close()
        val setGuestTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(guestTeam))
        var guestBitmap: Bitmap? = null
        if (setGuestTeamImage.moveToFirst()) {
            guestBitmap = BitmapFactory.decodeByteArray(setGuestTeamImage.getBlob(0), 0, setGuestTeamImage.getBlob(0).size)
            view.findViewById<ImageView>(R.id.guest_team_image).setImageBitmap(guestBitmap)
        }
        setGuestTeamImage.close()

        val databaseUserScorersPredictions = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).child("Scorers")
        val databaseUserHomeScorersPredictions = databaseUserScorersPredictions.child(homeTeam)
        val databaseUserGuestScorersPredictions = databaseUserScorersPredictions.child(guestTeam)
        val listViewAssignHomeSlots = view.findViewById<ListView>(R.id.assign_home_nets)
        val listViewHomeScorers = view.findViewById<ListView>(R.id.list_view_home_scorers)
        listViewHomeScorers.visibility = INVISIBLE
        val listViewAssignGuestSlots = view.findViewById<ListView>(R.id.assign_guest_nets)
        val listViewGuestScorers = view.findViewById<ListView>(R.id.list_view_guest_scorers)
        listViewGuestScorers.visibility = INVISIBLE

        view.findViewById<ImageView>(R.id.reset_assignments).setOnClickListener {
            findHomeSlotsSelected.removeAll(findHomeSlotsSelected)
            findGuestSlotsSelected.removeAll(findGuestSlotsSelected)
            databaseUserHomeScorersPredictions.removeValue().addOnCompleteListener {
                for (hI in range(0, listViewAssignHomeSlots.childCount)) {
                    listViewAssignHomeSlots[hI].findViewById<RelativeLayout>(R.id.assign).findViewById<ImageView>(R.id.assigned).visibility = INVISIBLE
                }
                listViewHomeScorers.visibility = INVISIBLE
                lastHomeSlot = -1
            }
            databaseUserGuestScorersPredictions.removeValue().addOnCompleteListener {
                for (gI in range(0, listViewAssignGuestSlots.childCount)) {
                    listViewAssignGuestSlots[gI].findViewById<RelativeLayout>(R.id.assign).findViewById<ImageView>(R.id.assigned).visibility = INVISIBLE
                }
                listViewGuestScorers.visibility = INVISIBLE
                lastGuestSlot = -1
            }
        }

        val databaseMatch = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam")
        databaseMatch.child("MatchInfo").get().addOnCompleteListener {
            val matchesInfo = it.result
            val date = matchesInfo.child("date").value.toString()
            val time = matchesInfo.child("time").value.toString()

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
            databaseMatch.child("Predictions").child(user).child("Scores").get().addOnCompleteListener { it2 ->
                val homePrediction = it2.result.child(homeTeam).value.toString().toInt()
                val guestPrediction = it2.result.child(guestTeam).value.toString().toInt()
                homeTeamPrediction.minValue = homePrediction
                homeTeamPrediction.maxValue = homePrediction
                homeTeamPrediction.value = homePrediction
                guestTeamPrediction.minValue = guestPrediction
                guestTeamPrediction.maxValue = guestPrediction
                guestTeamPrediction.value = guestPrediction

                var homeScorersList = mutableListOf<Player>()
                reference.child("Players").child(season).get().addOnCompleteListener { players ->
                    val homePlayers = players.result.child(homeTeam)
                    for (player in homePlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        homeScorersList.add(Player(firstName, lastName, shirt, role, homeTeam))
                    }
                    val guestPlayers = players.result.child(guestTeam)
                    for (player in guestPlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        homeScorersList.add(Player(firstName, lastName, shirt, role, guestTeam))
                    }
                    homeScorersList = homeScorersList.sortedWith(compareBy({ sc -> if (sc.team == homeTeam) 0 else 1 }, { sc -> if (sc.role == "Forward") 0 else 1 }, { sc -> if (sc.role == "Midfielder") 0 else 1 }, { sc -> if (sc.role == "Defender") 0 else 1 }, { sc -> sc.shirtNumber })).toMutableList()
                    if (homePrediction > 0) {
                        val addHomeSlots = mutableListOf<Slot>()
                        for (i in 1..homePrediction) {
                            addHomeSlots.add(Slot("${getString(R.string.slot)} $i"))
                        }
                        databaseUserHomeScorersPredictions.get().addOnCompleteListener { findSlots ->
                            for (s in findSlots.result.children) {
                                findHomeSlotsSelected.add(s.key.toString().last() - '0')
                            }
                            assignHomeSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.home_assign_scorer_slot, addHomeSlots, findHomeSlotsSelected)
                            listViewAssignHomeSlots.adapter = assignHomeSlotsAdapter

                            view.findViewById<ProgressBar>(R.id.progress_updating_slots).visibility = INVISIBLE

                            listViewAssignHomeSlots.setOnItemClickListener { _, slotView, slot, _ ->
                                if (listViewHomeScorers.visibility == INVISIBLE) {
                                    view.findViewById<ProgressBar>(R.id.progress_updating_home_scorers).visibility = VISIBLE
                                }
                                assignHomeSlotsAdapter.setSelectedPosition(slot)
                                if (lastHomeSlot != slot) {
                                    lastHomeSlot = slot
                                    databaseUserHomeScorersPredictions.child("Scorer${slot + 1}").get().addOnCompleteListener { findScorers ->
                                        if (findScorers.result.value.toString() != "null") {
                                            listViewHomeScorers.visibility = VISIBLE
                                            val goalType = findScorers.result.child("goalType").value.toString()
                                            val team = if (goalType == "Goal") {
                                                homeTeam
                                            }
                                            else {
                                                guestTeam
                                            }
                                            val shirt = findScorers.result.child("shirt").value.toString().toInt()
                                            val homeScorersAdapter = ScorerAdapter(view.context, R.layout.home_scorer, homeScorersList, databaseUserHomeScorersPredictions.child("Scorer${slot + 1}"), shirt, homeBitmap!!, guestBitmap!!, homeTeam)
                                            listViewHomeScorers.adapter = homeScorersAdapter

                                            val selectedPosition = homeScorersList.indexOfFirst { find ->
                                                find.shirtNumber == shirt && find.team == team
                                            }

                                            listViewHomeScorers.post {
                                                listViewHomeScorers.smoothScrollToPosition(selectedPosition)
                                            }

                                            if (selectedPosition != -1) {
                                                homeScorersAdapter.setSelectedPosition(selectedPosition)
                                                val selectedView = listViewHomeScorers.getChildAt(selectedPosition - listViewHomeScorers.firstVisiblePosition)
                                                selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                            }

                                            view.findViewById<ProgressBar>(R.id.progress_updating_home_scorers).visibility = INVISIBLE

                                            listViewHomeScorers.setOnItemClickListener  { _, scorerView, position, _ ->
                                                scorerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                                findHomeSlotsSelected.add(slot + 1)
                                                homeScorersAdapter.setSelectedPosition(position)
                                                slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                            }
                                        }
                                        else {
                                            val homeScorersAdapter = ScorerAdapter(view.context, R.layout.home_scorer, homeScorersList, databaseUserHomeScorersPredictions.child("Scorer${slot + 1}"), -1, homeBitmap!!, guestBitmap!!, homeTeam)
                                            listViewHomeScorers.visibility = VISIBLE
                                            listViewHomeScorers.adapter = homeScorersAdapter

                                            val selectedPosition = homeScorersList.indexOfFirst { find ->
                                                find.shirtNumber == -1
                                            }

                                            if (selectedPosition != -1) {
                                                homeScorersAdapter.setSelectedPosition(selectedPosition)
                                                val selectedView = listViewHomeScorers.getChildAt(selectedPosition - listViewHomeScorers.firstVisiblePosition)
                                                selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                            }
                                            view.findViewById<ProgressBar>(R.id.progress_updating_home_scorers).visibility = INVISIBLE

                                            listViewHomeScorers.setOnItemClickListener  { _, scorerView, position, _ ->
                                                scorerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                                findHomeSlotsSelected.add(slot + 1)
                                                homeScorersAdapter.setSelectedPosition(position)
                                                slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                var guestScorersList = mutableListOf<Player>()
                reference.child("Players").child(season).get().addOnCompleteListener { players ->
                    val guestPlayers = players.result.child(guestTeam)
                    for (player in guestPlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        guestScorersList.add(Player(firstName, lastName, shirt, role, guestTeam))
                    }
                    val homePlayers = players.result.child(homeTeam)
                    for (player in homePlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        guestScorersList.add(Player(firstName, lastName, shirt, role, homeTeam))
                    }
                    guestScorersList = guestScorersList.sortedWith(compareBy({ sc -> if (sc.team == guestTeam) 0 else 1 }, { sc -> if (sc.role == "Forward") 0 else 1 }, { sc -> if (sc.role == "Midfielder") 0 else 1 }, { sc -> if (sc.role == "Defender") 0 else 1 }, { sc -> sc.shirtNumber })).toMutableList()
                    if (guestPrediction > 0) {
                        val addGuestSlots = mutableListOf<Slot>()
                        for (i in 1..guestPrediction) {
                            addGuestSlots.add(Slot("${getString(R.string.slot)} $i"))
                        }
                        databaseUserGuestScorersPredictions.get().addOnCompleteListener { findSlots ->
                            for (s in findSlots.result.children) {
                                findGuestSlotsSelected.add(s.key.toString().last() - '0')
                            }
                            assignGuestSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.guest_assign_scorer_slot, addGuestSlots, findGuestSlotsSelected)
                            listViewAssignGuestSlots.adapter = assignGuestSlotsAdapter

                            view.findViewById<ProgressBar>(R.id.progress_updating_slots).visibility = INVISIBLE

                            listViewAssignGuestSlots.setOnItemClickListener { _, slotView, slot, _ ->
                                if (listViewGuestScorers.visibility == INVISIBLE) {
                                    view.findViewById<ProgressBar>(R.id.progress_updating_guest_scorers).visibility = VISIBLE
                                }
                                assignGuestSlotsAdapter.setSelectedPosition(slot)
                                if (lastGuestSlot != slot) {
                                    lastGuestSlot = slot
                                    databaseUserGuestScorersPredictions.child("Scorer${slot + 1}").get().addOnCompleteListener { findScorers ->
                                        if (findScorers.result.value.toString() != "null") {
                                            listViewGuestScorers.visibility = VISIBLE
                                            val goalType = findScorers.result.child("goalType").value.toString()
                                            val team = if (goalType == "Goal") {
                                                guestTeam
                                            }
                                            else {
                                                homeTeam
                                            }
                                            val shirt = findScorers.result.child("shirt").value.toString().toInt()
                                            val guestScorersAdapter = ScorerAdapter(view.context, R.layout.guest_scorer, guestScorersList, databaseUserGuestScorersPredictions.child("Scorer${slot + 1}"), shirt, guestBitmap!!, homeBitmap!!, guestTeam)
                                            listViewGuestScorers.adapter = guestScorersAdapter

                                            val selectedPosition = guestScorersList.indexOfFirst { find ->
                                                find.shirtNumber == shirt && find.team == team
                                            }

                                            listViewGuestScorers.post {
                                                listViewGuestScorers.smoothScrollToPosition(selectedPosition)
                                            }

                                            if (selectedPosition != -1) {
                                                guestScorersAdapter.setSelectedPosition(selectedPosition)
                                                val selectedView = listViewGuestScorers.getChildAt(selectedPosition - listViewGuestScorers.firstVisiblePosition)
                                                selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                            }

                                            view.findViewById<ProgressBar>(R.id.progress_updating_guest_scorers).visibility = INVISIBLE

                                            listViewGuestScorers.setOnItemClickListener { _, scorerView, position, _ ->
                                                scorerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                                findGuestSlotsSelected.add(slot + 1)
                                                guestScorersAdapter.setSelectedPosition(position)
                                                slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                            }
                                        }
                                        else {
                                            val guestScorersAdapter = ScorerAdapter(view.context, R.layout.guest_scorer, guestScorersList, databaseUserGuestScorersPredictions.child("Scorer${slot + 1}"), -1, guestBitmap!!, homeBitmap!!, guestTeam)
                                            listViewGuestScorers.visibility = VISIBLE
                                            listViewGuestScorers.adapter = guestScorersAdapter

                                            val selectedPosition = guestScorersList.indexOfFirst { find ->
                                                find.shirtNumber == -1
                                            }

                                            if (selectedPosition != -1) {
                                                guestScorersAdapter.setSelectedPosition(selectedPosition)
                                                val selectedView = listViewGuestScorers.getChildAt(selectedPosition - listViewGuestScorers.firstVisiblePosition)
                                                selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                            }

                                            view.findViewById<ProgressBar>(R.id.progress_updating_guest_scorers).visibility = INVISIBLE

                                            listViewGuestScorers.setOnItemClickListener { _, scorerView, position, _ ->
                                                scorerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                                findGuestSlotsSelected.add(slot + 1)
                                                guestScorersAdapter.setSelectedPosition(position)
                                                slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
}