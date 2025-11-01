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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.adapters.PlayerMVPAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
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

class PredictMVP : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_predict_mvp, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = PredictMVPArgs.fromBundle(requireArguments())
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
            val navigateToMatchesPredictions = PredictMVPDirections.actionPredictMVPToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = PredictMVPDirections.actionPredictMVPToLoginRegistration()
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
        val listViewPlayers = view.findViewById<ListView>(R.id.list_view_players)

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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.predict_mvp)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_mvp).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.predict_mvp)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.assign_mvp).text = resultDetails
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

                var playersList = mutableListOf<Player>()
                reference.child("Players").child(season).get().addOnCompleteListener { players ->
                    val homePlayers = players.result.child(homeTeam)
                    for (player in homePlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        if (!disqualifiedPlayersList.contains(MVPPlayer(homeTeam, shirt))) {
                            playersList.add(Player(firstName, lastName, shirt, role, homeTeam))
                        }
                    }
                    val guestPlayers = players.result.child(guestTeam)
                    for (player in guestPlayers.children) {
                        val firstName = player.child("firstName").value.toString()
                        val lastName = player.child("lastName").value.toString()
                        val role = player.child("role").value.toString()
                        val shirt = player.key.toString().toInt()
                        if (!disqualifiedPlayersList.contains(MVPPlayer(guestTeam, shirt))) {
                            playersList.add(Player(firstName, lastName, shirt, role, guestTeam))
                        }
                    }
                    playersList = playersList.sortedWith(compareBy({ sc -> if (sc.team == homeTeam) 0 else 1 }, { sc -> if (sc.role == "Goalkeeper") 0 else 1 }, { sc -> if (sc.role == "Defender") 0 else 1 }, { sc -> if (sc.role == "Midfielder") 0 else 1 }, { sc -> sc.shirtNumber })).toMutableList()

                    val predictMVPReference = databaseMatch.child("Predictions").child(user).child("MVP")
                    val playersAdapter = PlayerMVPAdapter(view.context, playersList, predictMVPReference, homeBitmap!!, guestBitmap!!, homeTeam)
                    listViewPlayers.adapter = playersAdapter
                    view.findViewById<ProgressBar>(R.id.progress_updating_players).visibility = INVISIBLE

                    predictMVPReference.get().addOnCompleteListener { mvpPredicted ->
                        var selectedPosition = -1
                        if (mvpPredicted.result.value.toString() != "null") {
                            val teamMVPPredicted = mvpPredicted.result.child("team").value.toString()
                            val shirtMVPPredicted = mvpPredicted.result.child("shirt").value.toString().toInt()

                            selectedPosition = playersList.indexOfFirst { find ->
                                find.team == teamMVPPredicted && find.shirtNumber == shirtMVPPredicted
                            }
                        }

                        listViewPlayers.post {
                            listViewPlayers.smoothScrollToPosition(selectedPosition + 1)
                        }

                        if (selectedPosition != -1) {
                            playersAdapter.setSelectedPosition(selectedPosition)
                            val selectedView = listViewPlayers.getChildAt(selectedPosition - listViewPlayers.firstVisiblePosition)
                            selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                        }

                        listViewPlayers.setOnItemClickListener  { _, playerView, position, _ ->
                            playerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                            playersAdapter.setSelectedPosition(position)
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