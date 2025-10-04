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

class OfficialDisciplinaryCards : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    private val yellowSlots = 16 //16 slots for yellow cards for each team
    private val redSlots = 3 //3 slots for red cards for each team

    private val findHomeYellowSlotsSelected = mutableListOf<Int>()
    private val findHomeRedSlotsSelected = mutableListOf<Int>()
    private val findGuestYellowSlotsSelected = mutableListOf<Int>()
    private val findGuestRedSlotsSelected = mutableListOf<Int>()

    private lateinit var assignHomeYellowSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignHomeRedSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignGuestYellowSlotsAdapter: AssignSlotsAdapter
    private lateinit var assignGuestRedSlotsAdapter: AssignSlotsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_official_disciplinary_cards, container, false)
    }

    @SuppressLint("InflateParams", "DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = OfficialDisciplinaryCardsArgs.fromBundle(requireArguments())
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
            val navigateToMatchesPredictions = OfficialDisciplinaryCardsDirections.actionOfficialDisciplinaryCardsToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = OfficialDisciplinaryCardsDirections.actionOfficialDisciplinaryCardsToLoginRegistration()
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
        val homeTeamResultTextView: TextView = view.findViewById(R.id.home_real_result)
        val guestTeamResultTextView: TextView = view.findViewById(R.id.guest_real_result)
        val status: TextView = view.findViewById(R.id.status)
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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.add_official_discipline)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_discipline).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.add_official_discipline)}"
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

        val databaseOfficialDiscipline = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("OfficialDiscipline")
        val databaseOfficialHomeDiscipline = databaseOfficialDiscipline.child(homeTeam)
        val databaseOfficialGuestDiscipline = databaseOfficialDiscipline.child(guestTeam)
        val databaseOfficialHomeYellowCards = databaseOfficialHomeDiscipline.child("YellowCards")
        val databaseOfficialHomeRedCards = databaseOfficialHomeDiscipline.child("RedCards")
        val databaseOfficialGuestYellowCards = databaseOfficialGuestDiscipline.child("YellowCards")
        val databaseOfficialGuestRedCards = databaseOfficialGuestDiscipline.child("RedCards")

        val databaseOfficialDisciplineTimeline = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("OfficialDisciplineTimeline")
        val databaseOfficialHomeDisciplineTimeline = databaseOfficialDisciplineTimeline.child(homeTeam)
        val databaseOfficialGuestDisciplineTimeline = databaseOfficialDisciplineTimeline.child(guestTeam)
        val databaseOfficialHomeYellowCardsTimeline = databaseOfficialHomeDisciplineTimeline.child("YellowCards")
        val databaseOfficialHomeRedCardsTimeline = databaseOfficialHomeDisciplineTimeline.child("RedCards")
        val databaseOfficialGuestYellowCardsTimeline = databaseOfficialGuestDisciplineTimeline.child("YellowCards")
        val databaseOfficialGuestRedCardsTimeline = databaseOfficialGuestDisciplineTimeline.child("RedCards")

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

        val championshipReference = reference.child("Championships").child(championship).child(season)
        championshipReference.get().addOnCompleteListener {
            val isRoundWithExtraTime = it.result.child("Info").hasChild("roundsWithExtraTime") && it.result.child("Info").child("roundsWithExtraTime").value.toString().split(",").contains(round.toString())
            val matchesInfo = it.result.child("Matches").child(round.toString()).child("Matches").child("$homeTeam-$guestTeam").child("MatchInfo")
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
            val homeResult = matchesInfo.child("homeScore").value.toString()
            homeTeamResultTextView.text = homeResult
            val guestResult = matchesInfo.child("guestScore").value.toString()
            guestTeamResultTextView.text = guestResult

            val finished = it.result.hasChild("Finished")
            if (finished) {
                status.text = getString(R.string.finished)
            }
            else {
                status.text = getString(R.string.live)
                val redColor = view.resources.getColor(R.color.red)
                status.setTextColor(redColor)
                homeTeamResultTextView.setTextColor(redColor)
                guestTeamResultTextView.setTextColor(redColor)
                view.findViewById<TextView>(R.id.vs).setTextColor(redColor)
            }

            databaseOfficialHomeYellowCards.get().addOnCompleteListener { findYellowSlots ->
                for (yc in findYellowSlots.result.children) {
                    findHomeYellowSlotsSelected.add(yc.key.toString().last() - '0')
                }
                assignHomeYellowSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.home_assign_scorer_slot, yellowSlotsList, findHomeYellowSlotsSelected, R.drawable.add_yellow_card)
                listViewAssignHomeYellowSlots.adapter = assignHomeYellowSlotsAdapter
            }
            databaseOfficialHomeRedCards.get().addOnCompleteListener { findRedSlots ->
                for (rc in findRedSlots.result.children) {
                    findHomeRedSlotsSelected.add(rc.key.toString().last() - '0')
                }
                assignHomeRedSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.home_assign_scorer_slot, redSlotsList, findHomeRedSlotsSelected, R.drawable.add_red_card)
                listViewAssignHomeRedSlots.adapter = assignHomeRedSlotsAdapter
            }
            databaseOfficialGuestYellowCards.get().addOnCompleteListener { findYellowSlots ->
                for (yc in findYellowSlots.result.children) {
                    findGuestYellowSlotsSelected.add(yc.key.toString().last() - '0')
                }
                assignGuestYellowSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.guest_assign_scorer_slot, yellowSlotsList, findGuestYellowSlotsSelected, R.drawable.add_yellow_card)
                listViewAssignGuestYellowSlots.adapter = assignGuestYellowSlotsAdapter
            }
            databaseOfficialGuestRedCards.get().addOnCompleteListener { findRedSlots ->
                for (rc in findRedSlots.result.children) {
                    findGuestRedSlotsSelected.add(rc.key.toString().last() - '0')
                }
                assignGuestRedSlotsAdapter = AssignSlotsAdapter(view.context, R.layout.guest_assign_scorer_slot, redSlotsList, findGuestRedSlotsSelected, R.drawable.add_red_card)
                listViewAssignGuestRedSlots.adapter = assignGuestRedSlotsAdapter
            }

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
                assignCardToPlayer(view, listViewAssignHomeYellowSlots, databaseOfficialHomeDiscipline, assignHomeYellowSlotsAdapter, homePlayersList, homeTeam, homeBitmap, guestBitmap, databaseOfficialHomeYellowCards, databaseOfficialHomeYellowCardsTimeline, R.color.yellow, findHomeYellowSlotsSelected, "Home", isRoundWithExtraTime)
                assignCardToPlayer(view, listViewAssignHomeRedSlots, databaseOfficialHomeDiscipline, assignHomeRedSlotsAdapter, homePlayersList, homeTeam, homeBitmap, guestBitmap, databaseOfficialHomeRedCards, databaseOfficialHomeRedCardsTimeline, R.color.red, findHomeRedSlotsSelected, "Home", isRoundWithExtraTime)
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
                assignCardToPlayer(view, listViewAssignGuestYellowSlots, databaseOfficialGuestDiscipline, assignGuestYellowSlotsAdapter, guestPlayersList, guestTeam, homeBitmap, guestBitmap, databaseOfficialGuestYellowCards, databaseOfficialGuestYellowCardsTimeline, R.color.yellow, findGuestYellowSlotsSelected, "Guest", isRoundWithExtraTime)
                assignCardToPlayer(view, listViewAssignGuestRedSlots, databaseOfficialGuestDiscipline, assignGuestRedSlotsAdapter, guestPlayersList, guestTeam, homeBitmap, guestBitmap, databaseOfficialGuestRedCards, databaseOfficialGuestRedCardsTimeline, R.color.red, findGuestRedSlotsSelected, "Guest", isRoundWithExtraTime)
            }
            view.findViewById<ProgressBar>(R.id.progress_updating_slots).visibility = INVISIBLE
            view.findViewById<RelativeLayout>(R.id.yellow_cards_info).visibility = VISIBLE
            view.findViewById<RelativeLayout>(R.id.red_cards_info).visibility = VISIBLE

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
    private fun assignCardToPlayer(view: View, listViewSlots: ListView, databaseTeamCardsOfficial: DatabaseReference, slotsAdapter: AssignSlotsAdapter, allPlayersList: List<Player>, team: String, homeBitmap: Bitmap?, guestBitmap: Bitmap?, databaseCardsOfficial: DatabaseReference, databaseTimelineOfficial: DatabaseReference, cardColor: Int, findSlotsSelected: MutableList<Int>, type: String, isRoundWithExtraTime: Boolean) {
        listViewSlots.setOnItemClickListener { _, slotView, slot, _ ->
            slotsAdapter.setSelectedPosition(slot)

            val playersList = allPlayersList.toMutableList()

            databaseTeamCardsOfficial.get().addOnCompleteListener { findTeamCardsAdded ->
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

                databaseCardsOfficial.child("Slot${slot + 1}").get().addOnCompleteListener { findPlayer ->
                    val selectCardPlayerDialog = if (cardColor == R.color.yellow)
                        layoutInflater.inflate(R.layout.assign_official_yellow_card_to_player_dialog, null)
                    else
                        layoutInflater.inflate(R.layout.assign_official_red_card_to_player_dialog, null)
                    val teamName = selectCardPlayerDialog.findViewById<TextView>(R.id.team)
                    val teamImage = selectCardPlayerDialog.findViewById<ImageView>(R.id.team_image)
                    val cardTimeline = selectCardPlayerDialog.findViewById<NumberPicker>(R.id.card_minute)
                    cardTimeline.minValue = 0
                    if (isRoundWithExtraTime) {
                        cardTimeline.maxValue = 120
                    }
                    else {
                        cardTimeline.maxValue = 90
                    }
                    val playersViewList = selectCardPlayerDialog.findViewById<ListView>(R.id.list_view_team_players)

                    activity?.runOnUiThread {
                        slotView.setBackgroundColor(view.context.resources.getColor(R.color.objective))
                    }

                    teamName.text = team
                    if (type == "Home") {
                        teamImage.setImageBitmap(homeBitmap!!)
                    }
                    else {
                        teamImage.setImageBitmap(guestBitmap!!)
                    }
                    var playersAdapter = if (type == "Home")
                        PlayerMVPAdapter(view.context, playersList, databaseCardsOfficial.child("Slot${slot + 1}"), homeBitmap!!, guestBitmap!!, team, true)
                    else
                        PlayerMVPAdapter(view.context, playersList, databaseCardsOfficial.child("Slot${slot + 1}"), guestBitmap!!, homeBitmap!!, team, true)
                    playersViewList.adapter = playersAdapter

                    databaseTimelineOfficial.child("Slot${slot + 1}").get().addOnCompleteListener { findMinute ->
                        if (findMinute.result.value.toString() != "null") {
                            cardTimeline.value = findMinute.result.value.toString().toInt()
                        }
                    }

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
                            PlayerMVPAdapter(view.context, playersList, databaseCardsOfficial.child("Slot${slot + 1}"), homeBitmap, guestBitmap, team, true)
                        else
                            PlayerMVPAdapter(view.context, playersList, databaseCardsOfficial.child("Slot${slot + 1}"), guestBitmap, homeBitmap, team, true)
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
                                if (cardTimeline.value != 0) {
                                    databaseCardsOfficial.child("Slot${slot + 1}").child("shirt").setValue(playerShirt).addOnCompleteListener {
                                        databaseTimelineOfficial.child("Slot${slot + 1}").setValue(cardTimeline.value).addOnCompleteListener {
                                            dialog.dismiss()
                                            findSlotsSelected.add(slot + 1)
                                            slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                            slotView.findViewById<ImageView>(R.id.add).visibility = INVISIBLE
                                        }
                                    }
                                }
                                else {
                                    Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_LONG).show()
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
                                if (redCardTypeToInsert != "" && cardTimeline.value != 0) {
                                    val assignedRedCardPlayer = RedCardPlayer(playerShirt, redCardTypeToInsert)
                                    databaseCardsOfficial.child("Slot${slot + 1}").setValue(assignedRedCardPlayer).addOnCompleteListener {
                                        databaseTimelineOfficial.child("Slot${slot + 1}").setValue(cardTimeline.value).addOnCompleteListener {
                                            dialog.dismiss()
                                            findSlotsSelected.add(slot + 1)
                                            slotView.findViewById<ImageView>(R.id.assigned).visibility = VISIBLE
                                            slotView.findViewById<ImageView>(R.id.add).visibility = INVISIBLE
                                        }
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
                        databaseCardsOfficial.child("Slot${slot + 1}").removeValue().addOnCompleteListener {
                            databaseTimelineOfficial.child("Slot${slot + 1}").removeValue().addOnCompleteListener {
                                playersAdapter.setSelectedPosition(-1)
                                findSlotsSelected.remove(slot + 1)
                                slotsAdapter.setSelectedPosition(-1)
                                selectedPlayer = null
                                dialog.dismiss()
                                slotView.findViewById<ImageView>(R.id.add).visibility = VISIBLE
                            }
                        }
                    }
                    slotsAdapter.setSelectedPosition(-1)
                    slotView.setBackgroundColor(view.context.resources.getColor(android.R.color.transparent))
                }
            }
        }
    }
}