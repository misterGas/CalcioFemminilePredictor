package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.adapters.AssignSlotsAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.PlayerMVPAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.RedCardPlayer
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

class PredictDisciplinaryCards : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    private val yellowSlots = 3 //3 slots for yellow cards for each team
    private val redSlots = 1 //1 slot for red cards for each team

    private val findHomeYellowSlotsSelected = mutableListOf<Int>()
    private val findHomeRedSlotsSelected = mutableListOf<Int>()
    private val findGuestYellowSlotsSelected = mutableListOf<Int>()
    private val findGuestRedSlotsSelected = mutableListOf<Int>()

    private lateinit var assignHomeYellowSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignHomeRedSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignGuestYellowSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignGuestRedSlotsAdapter: AssignSlotsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_predict_disciplinary_cards, container, false)
    }

    @SuppressLint("InflateParams", "DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = PredictDisciplinaryCardsArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
        val homeTeam = arguments.homeTeam
        val guestTeam = arguments.guestTeam
        val disqualifiedPlayers = arguments.disqualifiedPlayersRoundList.replace("[", "").split("]")
        val disqualifiedPlayersList = mutableListOf<MVPPlayer>()
        for (dp in disqualifiedPlayers) {
            if (dp.isNotEmpty()) {
                val dpTeam = dp.split(",")[0]
                val dpShirt = dp.split(",")[1]
                disqualifiedPlayersList.add(MVPPlayer(dpTeam, dpShirt.toInt()))
            }
        }

        view.findViewById<ImageView>(R.id.back_to_championship_prediction).setOnClickListener {
            val navigateToMatchesPredictions = PredictDisciplinaryCardsDirections.actionPredictDisciplinaryCardsToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = PredictDisciplinaryCardsDirections.actionPredictDisciplinaryCardsToLoginRegistration()
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

        reference.child("Championships").child(championship).child(season).child("Info").get().addOnCompleteListener {
            if (it.result.hasChild("hasInternationalTeams")) {
                home.text = getString(view.resources.getIdentifier(homeTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                guest.text = getString(view.resources.getIdentifier(guestTeam.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
            }
            else {
                home.text = homeTeam
                guest.text = guestTeam
            }
        }

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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.predict_discipline)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_discipline).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.predict_discipline)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_discipline).text = resultDetails
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

        val databaseUserDisciplinePrediction = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("Predictions").child(user).child("Discipline")
        val databaseUserHomeDisciplinePrediction = databaseUserDisciplinePrediction.child(homeTeam)
        val databaseUserGuestDisciplinePrediction = databaseUserDisciplinePrediction.child(guestTeam)
        val databaseUserHomeYellowCardsPrediction = databaseUserHomeDisciplinePrediction.child("YellowCards")
        val databaseUserHomeRedCardsPrediction = databaseUserHomeDisciplinePrediction.child("RedCards")
        val databaseUserGuestYellowCardsPrediction = databaseUserGuestDisciplinePrediction.child("YellowCards")
        val databaseUserGuestRedCardsPrediction = databaseUserGuestDisciplinePrediction.child("RedCards")

        val listViewAssignHomeYellowSlots = view.findViewById<ListView>(R.id.assign_home_yellow_cards)
        val listViewAssignHomeRedSlots = view.findViewById<ListView>(R.id.assign_home_red_cards)
        val listViewAssignGuestYellowSlots = view.findViewById<ListView>(R.id.assign_guest_yellow_cards)
        val listViewAssignGuestRedSlots = view.findViewById<ListView>(R.id.assign_guest_red_cards)

        val yellowSlotsList = mutableListOf<Slot>()
        for (i in 1..yellowSlots) {
            yellowSlotsList.add(Slot("${getString(R.string.slot)} $i"))
        }

        val redSlotsList = mutableListOf<Slot>()
        for (i in 1..redSlots) {
            redSlotsList.add(Slot("${getString(R.string.slot)} $i"))
        }

        val databaseMatch = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam")
        reference.get().addOnCompleteListener {
            val matchInfo = it.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("MatchInfo")
            val hasInternationalTeams = it.result.child("Championships").child(championship).child(season).child("Info").hasChild("hasInternationalTeams")
            val date = matchInfo.child("date").value.toString()
            val time = matchInfo.child("time").value.toString()

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
            databaseUserHomeYellowCardsPrediction.get().addOnCompleteListener { findYellowSlots ->
                for (yc in findYellowSlots.result.children) {
                    findHomeYellowSlotsSelected.add(yc.key.toString().last() - '0')
                }
                assignHomeYellowSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.home_assign_scorer_slot, yellowSlotsList, findHomeYellowSlotsSelected, R.drawable.add_yellow_card)
                listViewAssignHomeYellowSlots.adapter = assignHomeYellowSlotsAdapter
            }
            databaseUserHomeRedCardsPrediction.get().addOnCompleteListener { findRedSlots ->
                for (rc in findRedSlots.result.children) {
                    findHomeRedSlotsSelected.add(rc.key.toString().last() - '0')
                }
                assignHomeRedSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.home_assign_scorer_slot, redSlotsList, findHomeRedSlotsSelected, R.drawable.add_red_card)
                listViewAssignHomeRedSlots.adapter = assignHomeRedSlotsAdapter
            }
            databaseUserGuestYellowCardsPrediction.get().addOnCompleteListener { findYellowSlots ->
                for (yc in findYellowSlots.result.children) {
                    findGuestYellowSlotsSelected.add(yc.key.toString().last() - '0')
                }
                assignGuestYellowSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.guest_assign_scorer_slot, yellowSlotsList, findGuestYellowSlotsSelected, R.drawable.add_yellow_card)
                listViewAssignGuestYellowSlots.adapter = assignGuestYellowSlotsAdapter
            }
            databaseUserGuestRedCardsPrediction.get().addOnCompleteListener { findRedSlots ->
                for (rc in findRedSlots.result.children) {
                    findGuestRedSlotsSelected.add(rc.key.toString().last() - '0')
                }
                assignGuestRedSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.guest_assign_scorer_slot, redSlotsList, findGuestRedSlotsSelected, R.drawable.add_red_card)
                listViewAssignGuestRedSlots.adapter = assignGuestRedSlotsAdapter
            }

            databaseMatch.child("Predictions").child(user).child("Scores").get().addOnCompleteListener { it2 ->
                val homePrediction = it2.result.child(homeTeam).value.toString().toInt()
                val guestPrediction = it2.result.child(guestTeam).value.toString().toInt()
                homeTeamPrediction.minValue = homePrediction
                homeTeamPrediction.maxValue = homePrediction
                homeTeamPrediction.value = homePrediction
                guestTeamPrediction.minValue = guestPrediction
                guestTeamPrediction.maxValue = guestPrediction
                guestTeamPrediction.value = guestPrediction

                var homePlayersList = mutableListOf<Player>()
                reference.child("Players").child(season).get().addOnCompleteListener { players ->
                    val homePlayers = players.result.child(homeTeam)
                    for (player in homePlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        if (!disqualifiedPlayersList.contains(MVPPlayer(homeTeam, shirt))) {
                            homePlayersList.add(Player(firstName, lastName, shirt, role, homeTeam))
                        }
                    }
                    homePlayersList = homePlayersList.sortedWith(compareBy({ sc -> if (sc.role == "Goalkeeper") 0 else 1 }, { sc -> if (sc.role == "Defender") 0 else 1 }, { sc -> if (sc.role == "Midfielder") 0 else 1 }, { sc -> sc.shirtNumber })).toMutableList()
                    assignCardToPlayer(view, listViewAssignHomeYellowSlots, databaseUserHomeDisciplinePrediction, assignHomeYellowSlotsAdapter, homePlayersList, homeTeam, homeBitmap, guestBitmap, databaseUserHomeYellowCardsPrediction, R.color.yellow, findHomeYellowSlotsSelected, "Home", hasInternationalTeams)
                    assignCardToPlayer(view, listViewAssignHomeRedSlots, databaseUserHomeDisciplinePrediction, assignHomeRedSlotsAdapter, homePlayersList, homeTeam, homeBitmap, guestBitmap, databaseUserHomeRedCardsPrediction, R.color.red, findHomeRedSlotsSelected, "Home", hasInternationalTeams)
                }

                var guestPlayersList = mutableListOf<Player>()
                reference.child("Players").child(season).get().addOnCompleteListener { players ->
                    val guestPlayers = players.result.child(guestTeam)
                    for (player in guestPlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        if (!disqualifiedPlayersList.contains(MVPPlayer(guestTeam, shirt))) {
                            guestPlayersList.add(Player(firstName, lastName, shirt, role, guestTeam))
                        }
                    }
                    guestPlayersList = guestPlayersList.sortedWith(compareBy({ sc -> if (sc.role == "Goalkeeper") 0 else 1 }, { sc -> if (sc.role == "Defender") 0 else 1 }, { sc -> if (sc.role == "Midfielder") 0 else 1 }, { sc -> sc.shirtNumber })).toMutableList()
                    assignCardToPlayer(view, listViewAssignGuestYellowSlots, databaseUserGuestDisciplinePrediction, assignGuestYellowSlotsAdapter, guestPlayersList, guestTeam, homeBitmap, guestBitmap, databaseUserGuestYellowCardsPrediction, R.color.yellow, findGuestYellowSlotsSelected, "Guest", hasInternationalTeams)
                    assignCardToPlayer(view, listViewAssignGuestRedSlots, databaseUserGuestDisciplinePrediction, assignGuestRedSlotsAdapter, guestPlayersList, guestTeam, homeBitmap, guestBitmap, databaseUserGuestRedCardsPrediction, R.color.red, findGuestRedSlotsSelected, "Guest", hasInternationalTeams)
                }
                view.findViewById<ProgressBar>(R.id.progress_updating_slots).visibility = INVISIBLE
                view.findViewById<RelativeLayout>(R.id.yellow_cards_info).visibility = VISIBLE
                view.findViewById<RelativeLayout>(R.id.red_cards_info).visibility = VISIBLE
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

    @SuppressLint("CutPasteId")
    private fun assignCardToPlayer(view: View, listViewSlots: ListView, databaseTeamCardsPredictions: DatabaseReference, slotsAdapter: AssignSlotsAdapter, allPlayersList: List<Player>, team: String, homeBitmap: Bitmap?, guestBitmap: Bitmap?, databaseCardsPrediction: DatabaseReference, cardColor: Int, findSlotsSelected: MutableList<Int>, type: String, hasInternationalTeams: Boolean) {
        listViewSlots.setOnItemClickListener { _, slotView, slot, _ ->
            slotsAdapter.setSelectedPosition(slot)

            val playersList = allPlayersList.toMutableList()

            databaseTeamCardsPredictions.get().addOnCompleteListener { findTeamCardsAdded ->
                val playersToIgnore = mutableListOf<MVPPlayer>()
                val findYellowCards = findTeamCardsAdded.result.child("YellowCards")
                for (yc in findYellowCards.children) {
                    if (yc.key != "Slot${slot + 1}" || (yc.key == "Slot${slot + 1}" && cardColor == R.color.red)) {
                        val shirt = yc.child("shirt").value.toString().toInt()
                        playersToIgnore.add(MVPPlayer(team, shirt))
                    }
                }
                val findRedCards = findTeamCardsAdded.result.child("RedCards")
                for (rc in findRedCards.children) {
                    if (rc.key != "Slot${slot + 1}" || (rc.key == "Slot${slot + 1}" && cardColor == R.color.yellow)) {
                        val shirt = rc.child("shirt").value.toString().toInt()
                        playersToIgnore.add(MVPPlayer(team, shirt))
                    }
                }
                for (pi in playersToIgnore) {
                    playersList.removeIf { removeP -> removeP.team == pi.team && removeP.shirtNumber == pi.shirt }
                }

                databaseCardsPrediction.child("Slot${slot + 1}").get().addOnCompleteListener { findPlayer ->
                    val selectCardPlayerDialog = if (cardColor == R.color.yellow)
                            layoutInflater.inflate(R.layout.assign_yellow_card_to_player_dialog, null)
                        else
                            layoutInflater.inflate(R.layout.assign_red_card_to_player_dialog, null)
                    val teamName = selectCardPlayerDialog.findViewById<TextView>(R.id.team)
                    val teamImage = selectCardPlayerDialog.findViewById<ImageView>(R.id.team_image)
                    val playersViewList = selectCardPlayerDialog.findViewById<ListView>(R.id.list_view_team_players)

                    activity?.runOnUiThread {
                        slotView.setBackgroundColor(view.context.resources.getColor(R.color.objective))
                    }

                    if (hasInternationalTeams) {
                        teamName.text = getString(view.resources.getIdentifier(team.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                    }
                    else {
                        teamName.text = team
                    }
                    if (type == "Home") {
                        teamImage.setImageBitmap(homeBitmap!!)
                    }
                    else {
                        teamImage.setImageBitmap(guestBitmap!!)
                    }
                    var playersAdapter = if (type == "Home")
                        PlayerMVPAdapter(view.context, playersList, databaseCardsPrediction.child("Slot${slot + 1}"), homeBitmap!!, guestBitmap!!, team, true)
                    else
                        PlayerMVPAdapter(view.context, playersList, databaseCardsPrediction.child("Slot${slot + 1}"), guestBitmap!!, homeBitmap!!, team, true)
                    playersViewList.adapter = playersAdapter

                    val dialog = AlertDialog.Builder(view.context).setView(selectCardPlayerDialog)
                        .setPositiveButton(R.string.confirm, null)
                        .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setNeutralButton(R.string.reset, null)
                        .create()
                    if (cardColor == R.color.yellow) {
                        dialog.window?.setBackgroundDrawableResource(R.drawable.assign_yellow_card_to_player_border)
                    }
                    else {
                        dialog.window?.setBackgroundDrawableResource(R.drawable.assign_red_card_to_player_border)
                        val redCardType = findPlayer.result.child("type").value.toString()
                        if (redCardType == "2Yellows") {
                            selectCardPlayerDialog.findViewById<RadioButton>(R.id.two_yellow_cards).isChecked = true
                            selectCardPlayerDialog.findViewById<RadioButton>(R.id.direct_red_card).isChecked = false
                        }
                        else if (redCardType == "Direct") {
                            selectCardPlayerDialog.findViewById<RadioButton>(R.id.direct_red_card).isChecked = true
                            selectCardPlayerDialog.findViewById<RadioButton>(R.id.two_yellow_cards).isChecked = false
                        }
                        else {
                            selectCardPlayerDialog.findViewById<RadioButton>(R.id.two_yellow_cards).isChecked = false
                            selectCardPlayerDialog.findViewById<RadioButton>(R.id.direct_red_card).isChecked = false
                        }
                    }

                    dialog.show()

                    var selectedPlayer: Int? = null

                    if (findPlayer.result.value.toString() != "null") {
                        activity?.runOnUiThread {
                            slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                            slotView.findViewById<ImageView>(R.id.add).visibility = VISIBLE
                        }
                        val shirt = findPlayer.result.child("shirt").value.toString().toInt()

                        val selectedPosition = playersList.indexOfFirst { find ->
                            find.shirtNumber == shirt && find.team == team
                        }

                        playersViewList.post {
                            playersViewList.smoothScrollToPosition(selectedPosition)
                        }

                        if (selectedPosition != -1) {
                            playersAdapter.setSelectedPosition(selectedPosition)
                            val selectedView = playersViewList.getChildAt(selectedPosition - playersViewList.firstVisiblePosition)
                            selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                            selectedPlayer = selectedPosition
                        }

                        playersViewList.setOnItemClickListener  { _, playerView, position, _ ->
                            playerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                            playersAdapter.setSelectedPosition(position)
                            selectedPlayer = position
                        }
                    }
                    else {
                        playersAdapter = if (type == "Home")
                            PlayerMVPAdapter(view.context, playersList, databaseCardsPrediction.child("Slot${slot + 1}"), homeBitmap, guestBitmap, team, true)
                        else
                            PlayerMVPAdapter(view.context, playersList, databaseCardsPrediction.child("Slot${slot + 1}"), guestBitmap, homeBitmap, team, true)
                        playersViewList.adapter = playersAdapter
                        val selectedPosition = playersList.indexOfFirst { find ->
                            find.shirtNumber == -1
                        }

                        if (selectedPosition != -1) {
                            playersAdapter.setSelectedPosition(selectedPosition)
                            val selectedView = playersViewList.getChildAt(selectedPosition - playersViewList.firstVisiblePosition)
                            selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                            selectedPlayer = selectedPosition
                        }

                        playersViewList.setOnItemClickListener  { _, playerView, position, _ ->
                            playerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                            playersAdapter.setSelectedPosition(position)
                            selectedPlayer = position
                        }
                    }

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(view.context, cardColor))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(view.context, cardColor))
                    if (cardColor == R.color.yellow) {
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(view.context, R.color.yellow_reset))
                    }
                    else {
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(view.context, R.color.red_reset))
                    }

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (selectedPlayer != null) {
                            val playerShirt = playersList[selectedPlayer!!].shirtNumber
                            if (cardColor == R.color.yellow) {
                                databaseCardsPrediction.child("Slot${slot + 1}").child("shirt").setValue(playerShirt).addOnCompleteListener {
                                    dialog.dismiss()
                                    findSlotsSelected.add(slot + 1)
                                    slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                    slotView.findViewById<ImageView>(R.id.add).visibility = INVISIBLE
                                }
                            }
                            else {
                                var redCardTypeToInsert = ""
                                if (selectCardPlayerDialog.findViewById<RadioButton>(R.id.two_yellow_cards).isChecked) {
                                    redCardTypeToInsert = "2Yellows"
                                }
                                else if (selectCardPlayerDialog.findViewById<RadioButton>(R.id.direct_red_card).isChecked) {
                                    redCardTypeToInsert = "Direct"
                                }
                                if (redCardTypeToInsert != "") {
                                    val assignedRedCardPlayer = RedCardPlayer(playerShirt, redCardTypeToInsert)
                                    databaseCardsPrediction.child("Slot${slot + 1}").setValue(assignedRedCardPlayer).addOnCompleteListener {
                                        dialog.dismiss()
                                        findSlotsSelected.add(slot + 1)
                                        slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                        slotView.findViewById<ImageView>(R.id.add).visibility = INVISIBLE
                                    }
                                }
                                else {
                                    Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        else {
                            Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_LONG).show()
                        }
                    }

                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        databaseCardsPrediction.child("Slot${slot + 1}").removeValue().addOnCompleteListener {
                            playersAdapter.setSelectedPosition(-1)
                            findSlotsSelected.remove(slot + 1)
                            slotsAdapter.setSelectedPosition(-1)
                            selectedPlayer = null
                            dialog.dismiss()
                            slotView.findViewById<ImageView>(R.id.add).visibility = VISIBLE
                        }
                    }
                    slotsAdapter.setSelectedPosition(-1)
                    slotView.setBackgroundColor(view.context.resources.getColor(android.R.color.transparent))
                }
            }
        }
    }
}