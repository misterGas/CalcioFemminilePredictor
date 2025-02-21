package com.embeddedproject.calciofemminileitaliano

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.TeamResultsPredictionsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.OfficialSpecialEventPrediction
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

class SpecialEvent : Fragment() {

    private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private val outcomeTranslations = mutableMapOf<String, String>()

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_special_event, container, false)
    }

    @SuppressLint("DiscouragedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = SpecialEventArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val teamEvent = arguments.teamEvent
        val eventName = arguments.eventName
        val eventSeason = arguments.eventSeason
        val eventId = arguments.id

        view.findViewById<ImageView>(R.id.back_to_select_championship).setOnClickListener {
            val navigateToSelectChampionship = SpecialEventDirections.actionSpecialEventToSelectChampionship(user)
            view.findNavController().navigate(navigateToSelectChampionship)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = SpecialEventDirections.actionSpecialEventToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        view.findViewById<TextView>(R.id.team_info).text = teamEvent
        view.findViewById<TextView>(R.id.season_info).text = eventSeason
        val specialEvent = "${getString(R.string.special_events)}\n${eventName}"
        view.findViewById<TextView>(R.id.special_event_info).text = specialEvent

        reference.get().addOnCompleteListener {
            val specialEventReference = it.result.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString())
            val specialEventInfo = specialEventReference.child("Info")
            val date = specialEventInfo.child("date").value.toString()
            val time = specialEventInfo.child("time").value.toString()
            val team1 = specialEventInfo.child("homeTeam").child("teamName").value.toString()
            val team2 = specialEventInfo.child("guestTeam").child("teamName").value.toString()
            val playersFromSeason = specialEventInfo.child("playersFrom").value.toString()

            val utcDateTime = LocalDateTime.parse(date + "T" + time)
            val utcZone = utcDateTime.atZone(ZoneId.of("UTC"))
            val localDateTimeWithZone = utcZone.withZoneSameInstant(ZoneId.systemDefault())
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val localDate = localDateTimeWithZone.format(dateFormatter)
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val localTime = localDateTimeWithZone.format(timeFormatter)

            if (specialEventInfo.hasChild("hasTeamsToTranslate")) {
                view.findViewById<TextView>(R.id.home_team).text = view.resources.getString(view.resources.getIdentifier(team1.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                view.findViewById<TextView>(R.id.guest_team).text = view.resources.getString(view.resources.getIdentifier(team2.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
            }
            else {
                view.findViewById<TextView>(R.id.home_team).text = team1
                view.findViewById<TextView>(R.id.guest_team).text = team2
            }
            view.findViewById<TextView>(R.id.match_date).text = translateDate(localDate)
            view.findViewById<TextView>(R.id.match_time).text = localTime

            val setHomeTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team1))
            if (setHomeTeamImage.moveToFirst()) {
                view.findViewById<ImageView>(R.id.home_team_image).setImageBitmap(BitmapFactory.decodeByteArray(setHomeTeamImage.getBlob(0), 0, setHomeTeamImage.getBlob(0).size))
            }
            setHomeTeamImage.close()
            val setGuestTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team2))
            if (setGuestTeamImage.moveToFirst()) {
                view.findViewById<ImageView>(R.id.guest_team_image).setImageBitmap(BitmapFactory.decodeByteArray(setGuestTeamImage.getBlob(0), 0, setGuestTeamImage.getBlob(0).size))
            }
            setGuestTeamImage.close()

            if (specialEventInfo.hasChild("isEliminationMatch")) {
                outcomeTranslations["Normal time"] = "Normal time"
                outcomeTranslations["Tempi regolamentari"] = "Normal time"
                outcomeTranslations["Extra time"] = "Extra time"
                outcomeTranslations["Tempi supplementari"] = "Extra time"
                outcomeTranslations["Penalty shoot-out"] = "Penalty shootout"
                outcomeTranslations["Tiri di rigore"] = "Penalty shootout"

                val outcomesPredicted = mutableMapOf<String, Int>()

                val teamInfo = it.result.child("TeamsEvents").child(teamEvent)

                val componentsNumber = teamInfo.child("Components").childrenCount.toInt() + 1
                val captain = teamInfo.child("Creator").value.toString()
                var outcomeAnswered = 0

                val componentsPredictions = specialEventReference.child("Predictions").child(teamEvent)
                for (cP in componentsPredictions.children) {
                    if (cP.hasChild("Outcome")) {
                        outcomeAnswered++
                        val outcomeValue = cP.child("Outcome").value.toString()
                        if (outcomesPredicted.contains(outcomeValue)) {
                            outcomesPredicted[outcomeValue] = outcomesPredicted[outcomeValue]!! + 1
                        }
                        else {
                            outcomesPredicted[outcomeValue] = 1
                        }
                    }
                }

                val outcomeAnsweredTextView = view.findViewById<TextView>(R.id.outcome_answered_components_number)
                val normalTimeProgress = view.findViewById<ProgressBar>(R.id.normal_time_progress)
                normalTimeProgress.max = componentsNumber
                normalTimeProgress.tooltipText = "0/$componentsNumber"
                if (outcomesPredicted.contains("Normal time")) {
                    val animation = ObjectAnimator.ofInt(normalTimeProgress, "progress", 0, outcomesPredicted["Normal time"]!!)
                    normalTimeProgress.tooltipText = "${outcomesPredicted["Normal time"]!!}/$componentsNumber"
                    animation.duration = 200
                    animation.start()
                }
                val extraTimeProgress = view.findViewById<ProgressBar>(R.id.extra_time_progress)
                extraTimeProgress.max = componentsNumber
                extraTimeProgress.tooltipText = "0/$componentsNumber"
                if (outcomesPredicted.contains("Extra time")) {
                    val animation = ObjectAnimator.ofInt(extraTimeProgress, "progress", 0, outcomesPredicted["Extra time"]!!)
                    extraTimeProgress.tooltipText = "${outcomesPredicted["Extra time"]!!}/$componentsNumber"
                    animation.duration = 200
                    animation.start()
                }
                val penaltyShootoutProgress = view.findViewById<ProgressBar>(R.id.penalty_shootout_progress)
                penaltyShootoutProgress.max = componentsNumber
                penaltyShootoutProgress.tooltipText = "0/$componentsNumber"
                if (outcomesPredicted.contains("Penalty shootout")) {
                    val animation = ObjectAnimator.ofInt(penaltyShootoutProgress, "progress", 0, outcomesPredicted["Penalty shootout"]!!)
                    penaltyShootoutProgress.tooltipText = "${outcomesPredicted["Penalty shootout"]!!}/$componentsNumber"
                    animation.duration = 200
                    animation.start()
                }

                val outcomeAnsweredNumber = "${getString(R.string.votes)} $outcomeAnswered/$componentsNumber"
                outcomeAnsweredTextView.text = outcomeAnsweredNumber

                val outcomeRadioGroup = view.findViewById<RadioGroup>(R.id.outcome_radio_group)
                val userTeamPredictions = specialEventReference.child("Predictions").child(teamEvent).child(user)
                if (userTeamPredictions.hasChild("Outcome")) {
                    val findOutcomeInDatabase = userTeamPredictions.child("Outcome").value.toString()
                    val selectedRadioButton = view.findViewById<RadioButton>(resources.getIdentifier(findOutcomeInDatabase.replace(" ", "_").lowercase(), "id", activity?.packageName))
                    selectedRadioButton.isChecked = true
                    view.findViewById<ImageView>(R.id.outcome_completed).visibility = VISIBLE
                }
                outcomeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                    val outcomeValue = view.findViewById<RadioButton>(checkedId).text
                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).child(user).child("Outcome").setValue(outcomeTranslations[outcomeValue]).addOnCompleteListener {
                        view.findViewById<ImageView>(R.id.outcome_completed).visibility = VISIBLE
                        reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).get().addOnCompleteListener { userHasPredictedOutcome ->
                            var updateAnswered = 0
                            for (cP in userHasPredictedOutcome.result.children) {
                                if (cP.hasChild("Outcome")) {
                                    updateAnswered++
                                }
                            }
                            val updateAnsweredText = "${getString(R.string.votes)} ${updateAnswered}/$componentsNumber"
                            outcomeAnsweredTextView.text = updateAnsweredText

                        }
                    }

                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).get().addOnCompleteListener { updateOutcomePredictions ->
                        val updateOutcomesPredicted = mutableMapOf<String, Int>()
                        for (cP in updateOutcomePredictions.result.children) {
                            if (cP.hasChild("Outcome")) {
                                val outcomeValueUpdated = cP.child("Outcome").value.toString()
                                if (updateOutcomesPredicted.contains(outcomeValueUpdated)) {
                                    updateOutcomesPredicted[outcomeValueUpdated] = updateOutcomesPredicted[outcomeValueUpdated]!! + 1
                                }
                                else {
                                    updateOutcomesPredicted[outcomeValueUpdated] = 1
                                }
                            }
                        }
                        normalTimeProgress.progress = 0
                        normalTimeProgress.tooltipText = "0/$componentsNumber"
                        extraTimeProgress.progress = 0
                        extraTimeProgress.tooltipText = "0/$componentsNumber"
                        penaltyShootoutProgress.progress = 0
                        penaltyShootoutProgress.tooltipText = "0/$componentsNumber"
                        if (updateOutcomesPredicted.contains("Normal time")) {
                            val animation = ObjectAnimator.ofInt(normalTimeProgress, "progress", 0, updateOutcomesPredicted["Normal time"]!!)
                            normalTimeProgress.tooltipText = "${updateOutcomesPredicted["Normal time"]!!}/$componentsNumber"
                            animation.duration = 200
                            animation.start()
                        }
                        if (updateOutcomesPredicted.contains("Extra time")) {
                            val animation = ObjectAnimator.ofInt(extraTimeProgress, "progress", 0, updateOutcomesPredicted["Extra time"]!!)
                            extraTimeProgress.tooltipText = "${updateOutcomesPredicted["Extra time"]!!}/$componentsNumber"
                            animation.duration = 200
                            animation.start()
                        }
                        if (updateOutcomesPredicted.contains("Penalty shootout")) {
                            val animation = ObjectAnimator.ofInt(penaltyShootoutProgress, "progress", 0, updateOutcomesPredicted["Penalty shootout"]!!)
                            penaltyShootoutProgress.tooltipText = "${updateOutcomesPredicted["Penalty shootout"]!!}/$componentsNumber"
                            animation.duration = 200
                            animation.start()
                        }
                    }
                }

                val buildLocalDate = localDate.split("-")
                val localDateYear = buildLocalDate[0].toInt()
                val localDateMonth = buildLocalDate[1].toInt()
                val localDateDay = buildLocalDate[2].toInt()
                val localDateObject = LocalDate.of(localDateYear, localDateMonth, localDateDay)
                val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)

                val lastShowOutcomeSurvey = localDateObject.minusDays(4)
                val lastShowResultSurvey = localDateObject.minusDays(2)

                val outcomeInfo = view.findViewById<RelativeLayout>(R.id.outcome_info)
                val outcomeRelativeLayout = view.findViewById<RelativeLayout>(R.id.outcome)
                val officialOutcomeInfoRelativeLayout = view.findViewById<RelativeLayout>(R.id.official_outcome_info_relative_layout)
                val officialOutcomeRelativeLayout = view.findViewById<RelativeLayout>(R.id.official_outcome)
                val officialOutcomeDecisionValue = view.findViewById<TextView>(R.id.official_outcome_final_decision)

                if (actualDate <= lastShowOutcomeSurvey) {
                    outcomeInfo.visibility = VISIBLE
                    officialOutcomeInfoRelativeLayout.visibility = GONE
                    view.findViewById<ProgressBar>(R.id.progress_updating_special_event).visibility = INVISIBLE
                    view.findViewById<RelativeLayout>(R.id.event_info).visibility = VISIBLE
                }
                else {
                    officialOutcomeInfoRelativeLayout.visibility = VISIBLE
                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).get().addOnCompleteListener { findOfficialTeamOutcome ->
                        val outcomePredictionsGet = findOfficialTeamOutcome.result.child("Predictions").child(teamEvent)
                        val officialOutcomeGet = findOfficialTeamOutcome.result.child("OfficialPredictions").child(teamEvent)
                        val outcomeSurveyResults = mutableMapOf<String, Int>()
                        var captainChoice: String? = null
                        var numberOfOutcomeVotes = 0
                        for (uOP in outcomePredictionsGet.children) {
                            if (uOP.hasChild("Outcome")) {
                                val vote = uOP.child("Outcome").value.toString()
                                numberOfOutcomeVotes++
                                if (outcomeSurveyResults.contains(vote)) {
                                    outcomeSurveyResults[vote] = outcomeSurveyResults[vote]!! + 1
                                }
                                else {
                                    outcomeSurveyResults[vote] = 1
                                }
                                if (uOP.key.toString() == captain) {
                                    captainChoice = vote
                                }
                            }
                        }
                        val officialOutcomeInfo = "${getString(R.string.official_outcome_info1)} $numberOfOutcomeVotes ${getString(R.string.official_outcome_info2)}"
                        view.findViewById<TextView>(R.id.official_outcome_info).text = officialOutcomeInfo
                        if (outcomeSurveyResults.contains("Normal time")) {
                            view.findViewById<TextView>(R.id.official_outcome_result1_votes).text = outcomeSurveyResults["Normal time"]!!.toString()
                        }
                        else {
                            view.findViewById<TextView>(R.id.official_outcome_result1_votes).text = "0"
                        }
                        if (outcomeSurveyResults.contains("Extra time")) {
                            view.findViewById<TextView>(R.id.official_outcome_result2_votes).text = outcomeSurveyResults["Extra time"]!!.toString()
                        }
                        else {
                            view.findViewById<TextView>(R.id.official_outcome_result2_votes).text = "0"
                        }
                        if (outcomeSurveyResults.contains("Penalty shootout")) {
                            view.findViewById<TextView>(R.id.official_outcome_result3_votes).text = outcomeSurveyResults["Penalty shootout"]!!.toString()
                        }
                        else {
                            view.findViewById<TextView>(R.id.official_outcome_result3_votes).text = "0"
                        }
                        if (!(officialOutcomeGet.hasChild("Outcome"))) {
                            var findMaxVotes = mutableListOf<String>()
                            for (oV in outcomeSurveyResults) {
                                if (findMaxVotes.isNotEmpty()) {
                                    if (findMaxVotes[0].split("//")[1].toInt() == oV.value) {
                                        findMaxVotes.add("${oV.key}//${oV.value}")
                                    }
                                    else if (findMaxVotes[0].split("//")[1].toInt() < oV.value) {
                                        findMaxVotes = emptyList<String>().toMutableList()
                                        findMaxVotes.add("${oV.key}//${oV.value}")
                                    }
                                }
                                else {
                                    findMaxVotes.add("${oV.key}//${oV.value}")
                                }
                            }
                            if (findMaxVotes.size == 1) { //Votes criteria for outcome
                                val officialPrediction = OfficialSpecialEventPrediction(findMaxVotes[0].split("//")[0], "Votes")
                                reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("OfficialPredictions").child(teamEvent).child("Outcome").setValue(officialPrediction)
                                view.findViewById<TextView>(R.id.official_outcome_decision).text = getString(R.string.decision_by_votes)
                                officialOutcomeDecisionValue.text = getString(resources.getIdentifier(officialPrediction.value, "string", activity?.packageName))
                            }
                            else {
                                var captainFound = false
                                if (captainChoice != null) {
                                    for (v in findMaxVotes) {
                                        if (v.contains(captainChoice)) {
                                            captainFound = true
                                            break
                                        }
                                    }
                                }
                                if (captainFound) { //Captain privilege criteria for outcome
                                    val officialPrediction = OfficialSpecialEventPrediction(captainChoice!!, "Captain privilege")
                                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("OfficialPredictions").child(teamEvent).child("Outcome").setValue(officialPrediction)
                                    view.findViewById<TextView>(R.id.official_outcome_decision).text = getString(R.string.decision_by_captain_privilege)
                                    officialOutcomeDecisionValue.text = getString(resources.getIdentifier(officialPrediction.value, "string", activity?.packageName))
                                }
                                else { //Random criteria for outcome
                                    val randomCriteria = (0..<findMaxVotes.size).random()
                                    val officialPrediction = OfficialSpecialEventPrediction(findMaxVotes[randomCriteria].split("//")[0], "Random")
                                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("OfficialPredictions").child(teamEvent).child("Outcome").setValue(officialPrediction)
                                    view.findViewById<TextView>(R.id.official_outcome_decision).text = getString(R.string.decision_by_random)
                                    officialOutcomeDecisionValue.text = getString(resources.getIdentifier(officialPrediction.value, "string", activity?.packageName))
                                }
                            }
                        }
                        else {
                            val outcomeCriteria = officialOutcomeGet.child("Outcome").child("criteria").value.toString().replace(" ", "_").lowercase()
                            val outcomeValue = getString(resources.getIdentifier(officialOutcomeGet.child("Outcome").child("value").value.toString().replace(" ", "_").lowercase(), "string", activity?.packageName))
                            view.findViewById<TextView>(R.id.official_outcome_decision).text = getString(resources.getIdentifier("decision_by_${outcomeCriteria}", "string", activity?.packageName))
                            officialOutcomeDecisionValue.text = outcomeValue
                        }
                    }
                    outcomeInfo.visibility = GONE
                    view.findViewById<ProgressBar>(R.id.progress_updating_special_event).visibility = INVISIBLE
                    view.findViewById<RelativeLayout>(R.id.event_info).visibility = VISIBLE
                }

                outcomeInfo.setOnClickListener {
                    val openOutcome = view.findViewById<ImageView>(R.id.open_outcome)
                    if (outcomeRelativeLayout.visibility == GONE) {
                        outcomeRelativeLayout.visibility = VISIBLE
                        openOutcome.setImageResource(R.drawable.arrow_down)
                    }
                    else {
                        outcomeRelativeLayout.visibility = GONE
                        openOutcome.setImageResource(R.drawable.arrow_right)
                    }
                }

                officialOutcomeInfoRelativeLayout.setOnClickListener {
                    val openOutcome = view.findViewById<ImageView>(R.id.open_official_outcome)
                    if (officialOutcomeRelativeLayout.visibility == GONE) {
                        officialOutcomeRelativeLayout.visibility = VISIBLE
                        openOutcome.setImageResource(R.drawable.arrow_down)
                    }
                    else {
                        officialOutcomeRelativeLayout.visibility = GONE
                        openOutcome.setImageResource(R.drawable.arrow_right)
                    }
                }
            }
            else {
                val resultsList = mutableListOf<String>()
                val resultsPredicted = mutableMapOf<String, Int>()

                val teamInfo = it.result.child("TeamsEvents").child(teamEvent)

                val componentsNumber = teamInfo.child("Components").childrenCount.toInt() + 1
                val captain = teamInfo.child("Creator").value.toString()

                var resultsAnswered = 0

                val componentsPredictions = specialEventReference.child("Predictions").child(teamEvent)
                for (cP in componentsPredictions.children) {
                    if (cP.hasChild("Result")) {
                        resultsAnswered++
                        val resultValue = cP.child("Result").value.toString()
                        if (!resultsList.contains(resultValue)) {
                            resultsList.add(resultValue)
                        }
                        if (resultsPredicted.contains(resultValue)) {
                            resultsPredicted[resultValue] = resultsPredicted[resultValue]!! + 1
                        }
                        else {
                            resultsPredicted[resultValue] = 1
                        }
                    }
                }

                val userTeamPredictions = specialEventReference.child("Predictions").child(teamEvent).child(user)
                if (userTeamPredictions.hasChild("Result")) {
                    view.findViewById<ImageView>(R.id.result_prediction_completed).visibility = VISIBLE
                }
                else {
                    view.findViewById<ImageView>(R.id.result_prediction_completed).visibility = GONE
                }

                val resultsVotesRecyclerView = view.findViewById<RecyclerView>(R.id.result_prediction_votes_recycler_view)
                val resultsPredictedAdapter = TeamResultsPredictionsAdapter(resultsList, resultsPredicted, componentsNumber)
                resultsVotesRecyclerView.adapter = resultsPredictedAdapter

                val resultsAnsweredTextView = view.findViewById<TextView>(R.id.result_prediction_answered_components_number)
                val resultsAnsweredNumber = "${getString(R.string.votes)} $resultsAnswered/$componentsNumber"
                resultsAnsweredTextView.text = resultsAnsweredNumber

                val buildLocalDate = localDate.split("-")
                val localDateYear = buildLocalDate[0].toInt()
                val localDateMonth = buildLocalDate[1].toInt()
                val localDateDay = buildLocalDate[2].toInt()
                val localDateObject = LocalDate.of(localDateYear, localDateMonth, localDateDay)
                val actualDate = LocalDate.of(Year.now().value, YearMonth.now().monthValue, MonthDay.now().dayOfMonth)

                val lastShowResultsSurvey = localDateObject.minusDays(4)

                val resultsInfo = view.findViewById<RelativeLayout>(R.id.result_prediction_info)
                val resultsRelativeLayout = view.findViewById<RelativeLayout>(R.id.result_prediction)
                val officialResultsInfoRelativeLayout = view.findViewById<RelativeLayout>(R.id.official_result_prediction_info_relative_layout)
                val officialResultsRelativeLayout = view.findViewById<RelativeLayout>(R.id.official_result_prediction)
                val officialResultsDecisionValue = view.findViewById<TextView>(R.id.official_result_prediction_final_decision)

                if (actualDate <= lastShowResultsSurvey) {
                    resultsInfo.visibility = VISIBLE
                    officialResultsInfoRelativeLayout.visibility = GONE
                    view.findViewById<ProgressBar>(R.id.progress_updating_special_event).visibility = INVISIBLE
                    view.findViewById<RelativeLayout>(R.id.event_info).visibility = VISIBLE

                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).get().addOnCompleteListener { findOfficialTeamResults ->
                        val resultPredictionsGet = findOfficialTeamResults.result.child("Predictions").child(teamEvent)
                        val addUserPredictionImageView = view.findViewById<ImageView>(R.id.add_result_prediction)
                        val removeUserPredictionImageView = view.findViewById<ImageView>(R.id.remove_result_prediction)
                        if (!resultPredictionsGet.child(user).hasChild("Result")) {
                            addUserPredictionImageView.visibility = VISIBLE
                            removeUserPredictionImageView.visibility = GONE
                        }
                        else {
                            removeUserPredictionImageView.visibility = VISIBLE
                            addUserPredictionImageView.visibility = GONE
                        }
                        addUserPredictionImageView.setOnClickListener {
                            val dialogView = layoutInflater.inflate(R.layout.add_prediction_dialog, null)

                            val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                                .setPositiveButton(R.string.confirm, null)
                                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()

                            dialog.show()

                            val homePrediction = dialogView.findViewById<NumberPicker>(R.id.home_result_prediction)
                            val guestPrediction = dialogView.findViewById<NumberPicker>(R.id.guest_result_prediction)
                            homePrediction.minValue = 0
                            homePrediction.maxValue = 9
                            guestPrediction.minValue = 0
                            guestPrediction.maxValue = 9
                            val homeTeamName = dialogView.findViewById<TextView>(R.id.home_team)
                            val guestTeamName = dialogView.findViewById<TextView>(R.id.guest_team)
                            if (specialEventInfo.hasChild("hasTeamsToTranslate")) {
                                homeTeamName.text = view.resources.getString(view.resources.getIdentifier(team1.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                                guestTeamName.text = view.resources.getString(view.resources.getIdentifier(team2.lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                            }
                            else {
                                homeTeamName.text = team1
                                guestTeamName.text = team2
                            }
                            val homeTeamImage = dialogView.findViewById<ImageView>(R.id.home_team_image)
                            val guestTeamImage = dialogView.findViewById<ImageView>(R.id.guest_team_image)
                            val setHomeImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team1))
                            if (setHomeImage.moveToFirst()) {
                                homeTeamImage.setImageBitmap(BitmapFactory.decodeByteArray(setHomeImage.getBlob(0), 0, setHomeImage.getBlob(0).size))
                            }
                            setHomeImage.close()
                            val setGuestImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team2))
                            if (setGuestImage.moveToFirst()) {
                                guestTeamImage.setImageBitmap(BitmapFactory.decodeByteArray(setGuestImage.getBlob(0), 0, setGuestImage.getBlob(0).size))
                            }
                            setGuestImage.close()

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                                val homeScore = homePrediction.value
                                val guestScore = guestPrediction.value
                                reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).child(user).child("Result").setValue("$homeScore - $guestScore").addOnCompleteListener {
                                    dialog.dismiss()
                                    removeUserPredictionImageView.visibility = VISIBLE
                                    addUserPredictionImageView.visibility = GONE
                                    view.findViewById<ImageView>(R.id.result_prediction_completed).visibility = VISIBLE
                                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).get().addOnCompleteListener { userHasPredictedResult ->
                                        var updateAnswered = 0
                                        val resultsListUpdate = mutableListOf<String>()
                                        val resultsPredictedUpdate = mutableMapOf<String, Int>()
                                        for (cP in userHasPredictedResult.result.children) {
                                            if (cP.hasChild("Result")) {
                                                updateAnswered++
                                                val resultValue = cP.child("Result").value.toString()
                                                if (!resultsListUpdate.contains(resultValue)) {
                                                    resultsListUpdate.add(resultValue)
                                                }
                                                if (resultsPredictedUpdate.contains(resultValue)) {
                                                    resultsPredictedUpdate[resultValue] = resultsPredictedUpdate[resultValue]!! + 1
                                                }
                                                else {
                                                    resultsPredictedUpdate[resultValue] = 1
                                                }
                                            }
                                        }
                                        val updateAnsweredText = "${getString(R.string.votes)} ${updateAnswered}/$componentsNumber"
                                        resultsAnsweredTextView.text = updateAnsweredText
                                        val resultsPredictedUpdateAdapter = TeamResultsPredictionsAdapter(resultsListUpdate, resultsPredictedUpdate, componentsNumber)
                                        resultsVotesRecyclerView.adapter = resultsPredictedUpdateAdapter
                                    }
                                }
                            }
                        }

                        removeUserPredictionImageView.setOnClickListener {
                            val builder = AlertDialog.Builder(view.context)
                            builder.setMessage(getString(R.string.remove_prediction_confirm))
                            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                                reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).child(user).child("Result").removeValue().addOnCompleteListener {
                                    dialog.dismiss()
                                    addUserPredictionImageView.visibility = VISIBLE
                                    removeUserPredictionImageView.visibility = GONE
                                    view.findViewById<ImageView>(R.id.result_prediction_completed).visibility = GONE
                                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("Predictions").child(teamEvent).get().addOnCompleteListener { userHasPredictedResult ->
                                        var updateAnswered = 0
                                        val resultsListUpdate = mutableListOf<String>()
                                        val resultsPredictedUpdate = mutableMapOf<String, Int>()
                                        for (cP in userHasPredictedResult.result.children) {
                                            if (cP.hasChild("Result")) {
                                                updateAnswered++
                                                val resultValue = cP.child("Result").value.toString()
                                                if (!resultsListUpdate.contains(resultValue)) {
                                                    resultsListUpdate.add(resultValue)
                                                }
                                                if (resultsPredictedUpdate.contains(resultValue)) {
                                                    resultsPredictedUpdate[resultValue] = resultsPredictedUpdate[resultValue]!! + 1
                                                }
                                                else {
                                                    resultsPredictedUpdate[resultValue] = 1
                                                }
                                            }
                                        }
                                        val updateAnsweredText = "${getString(R.string.votes)} ${updateAnswered}/$componentsNumber"
                                        resultsAnsweredTextView.text = updateAnsweredText
                                        val resultsPredictedUpdateAdapter = TeamResultsPredictionsAdapter(resultsListUpdate, resultsPredictedUpdate, componentsNumber)
                                        resultsVotesRecyclerView.adapter = resultsPredictedUpdateAdapter
                                    }
                                }
                            }

                            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                                dialog.dismiss()
                            }

                            val dialog = builder.create()
                            dialog.show()
                        }
                    }
                }
                else {
                    officialResultsInfoRelativeLayout.visibility = VISIBLE
                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).get().addOnCompleteListener { findOfficialTeamResults ->
                        val resultPredictionsGet = findOfficialTeamResults.result.child("Predictions").child(teamEvent)
                        val officialResultsGet = findOfficialTeamResults.result.child("OfficialPredictions").child(teamEvent)
                        val resultsSurveyResults = mutableMapOf<String, Int>()
                        var captainResultsChoice: String? = null
                        var numberOfResultsVotes = 0
                        for (uOP in resultPredictionsGet.children) {
                            if (uOP.hasChild("Result")) {
                                val vote = uOP.child("Result").value.toString()
                                numberOfResultsVotes++
                                if (resultsSurveyResults.contains(vote)) {
                                    resultsSurveyResults[vote] = resultsSurveyResults[vote]!! + 1
                                }
                                else {
                                    resultsSurveyResults[vote] = 1
                                }
                                if (uOP.key.toString() == captain) {
                                    captainResultsChoice = vote
                                }
                            }
                        }
                        val officialResultInfo = "${getString(R.string.official_result_info1)} $numberOfResultsVotes ${getString(R.string.official_outcome_info2)}"
                        view.findViewById<TextView>(R.id.official_result_prediction_info).text = officialResultInfo

                        val officialResultsVotesRecyclerView = view.findViewById<RecyclerView>(R.id.result_prediction_official_votes_recycler_view)
                        val officialResultsPredictedAdapter = TeamResultsPredictionsAdapter(resultsList, resultsPredicted, componentsNumber)
                        officialResultsVotesRecyclerView.adapter = officialResultsPredictedAdapter

                        if (!(officialResultsGet.hasChild("Result"))) {
                            var findMaxVotes = mutableListOf<String>()
                            for (oV in resultsSurveyResults) {
                                if (findMaxVotes.isNotEmpty()) {
                                    if (findMaxVotes[0].split("//")[1].toInt() == oV.value) {
                                        findMaxVotes.add("${oV.key}//${oV.value}")
                                    }
                                    else if (findMaxVotes[0].split("//")[1].toInt() < oV.value) {
                                        findMaxVotes = emptyList<String>().toMutableList()
                                        findMaxVotes.add("${oV.key}//${oV.value}")
                                    }
                                }
                                else {
                                    findMaxVotes.add("${oV.key}//${oV.value}")
                                }
                            }
                            if (findMaxVotes.size == 1) { //Votes criteria for result prediction
                                val officialPrediction = OfficialSpecialEventPrediction(findMaxVotes[0].split("//")[0], "Votes")
                                reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("OfficialPredictions").child(teamEvent).child("Result").setValue(officialPrediction)
                                view.findViewById<TextView>(R.id.official_result_prediction_decision).text = getString(R.string.decision_by_votes)
                                officialResultsDecisionValue.text = officialPrediction.value
                            }
                            else {
                                var captainFound = false
                                if (captainResultsChoice != null) {
                                    for (v in findMaxVotes) {
                                        if (v.contains(captainResultsChoice)) {
                                            captainFound = true
                                            break
                                        }
                                    }
                                }
                                if (captainFound) { //Captain privilege criteria for result prediction
                                    val officialPrediction = OfficialSpecialEventPrediction(captainResultsChoice!!, "Captain privilege")
                                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("OfficialPredictions").child(teamEvent).child("Result").setValue(officialPrediction)
                                    view.findViewById<TextView>(R.id.official_result_prediction_decision).text = getString(R.string.decision_by_captain_privilege)
                                    officialResultsDecisionValue.text = officialPrediction.value
                                }
                                else { //Random criteria for result prediction
                                    val randomCriteria = (0..<findMaxVotes.size).random()
                                    val officialPrediction = OfficialSpecialEventPrediction(findMaxVotes[randomCriteria].split("//")[0], "Random")
                                    reference.child("SpecialEvents").child(eventName).child(eventSeason).child(eventId.toString()).child("OfficialPredictions").child(teamEvent).child("Result").setValue(officialPrediction)
                                    view.findViewById<TextView>(R.id.official_result_prediction_decision).text = getString(R.string.decision_by_random)
                                    officialResultsDecisionValue.text = officialPrediction.value
                                }
                            }
                        }
                        else {
                            val resultCriteria = officialResultsGet.child("Result").child("criteria").value.toString().replace(" ", "_").lowercase()
                            val resultValue = officialResultsGet.child("Result").child("value").value.toString()
                            view.findViewById<TextView>(R.id.official_result_prediction_decision).text = getString(resources.getIdentifier("decision_by_${resultCriteria}", "string", activity?.packageName))
                            officialResultsDecisionValue.text = resultValue
                        }
                    }
                    resultsInfo.visibility = GONE
                    view.findViewById<ProgressBar>(R.id.progress_updating_special_event).visibility = INVISIBLE
                    view.findViewById<RelativeLayout>(R.id.event_info).visibility = VISIBLE
                }

                resultsInfo.setOnClickListener {
                    val openResult = view.findViewById<ImageView>(R.id.open_result_prediction)
                    if (resultsRelativeLayout.visibility == GONE) {
                        resultsRelativeLayout.visibility = VISIBLE
                        openResult.setImageResource(R.drawable.arrow_down)
                    }
                    else {
                        resultsRelativeLayout.visibility = GONE
                        openResult.setImageResource(R.drawable.arrow_right)
                    }
                }

                officialResultsInfoRelativeLayout.setOnClickListener {
                    val openResult = view.findViewById<ImageView>(R.id.open_official_result_prediction)
                    if (officialResultsRelativeLayout.visibility == GONE) {
                        officialResultsRelativeLayout.visibility = VISIBLE
                        openResult.setImageResource(R.drawable.arrow_down)
                    }
                    else {
                        officialResultsRelativeLayout.visibility = GONE
                        openResult.setImageResource(R.drawable.arrow_right)
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