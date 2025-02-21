package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.AllTeamsEventsSelectAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.TeamsEventsComponentsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.GoalOrOwnGoal
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.TeamEventInfo
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UserInfo : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_user_info, container, false)
    }

    @SuppressLint("DiscouragedApi", "InflateParams")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference
        val arguments = UserInfoArgs.fromBundle(requireArguments())
        var user = arguments.userNickname

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        view.findViewById<ImageView>(R.id.back_to_select_championship).setOnClickListener {
            val navigateToSelectChampionship = UserInfoDirections.actionUserInfoToSelectChampionship(user)
            view.findNavController().navigate(navigateToSelectChampionship)
        }

        view.findViewById<ImageView>(R.id.open_objectives).setOnClickListener {
            val navigateToObjectives = UserInfoDirections.actionUserInfoToObjectives(user)
            view.findNavController().navigate(navigateToObjectives)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = UserInfoDirections.actionUserInfoToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        val nicknameTextView = view.findViewById<TextView>(R.id.user_nickname_value)
        val firstNameTextView = view.findViewById<TextView>(R.id.user_first_name_value)
        val lastNameTextView = view.findViewById<TextView>(R.id.user_last_name_value)
        val emailTextView = view.findViewById<TextView>(R.id.user_email_value)

        val allNicknames = mutableListOf<String>()

        reference.child("Users").get().addOnCompleteListener {
            for (u in it.result.children) {
                val uUser = u.child("nickname").value.toString()
                if (uUser == user) {
                    val firstName = u.child("firstName").value.toString()
                    val lastName = u.child("lastName").value.toString()
                    val email = u.child("email").value.toString()
                    nicknameTextView.text = user
                    firstNameTextView.text = firstName
                    lastNameTextView.text = lastName
                    emailTextView.text = email
                }
                allNicknames.add(uUser)
            }
        }

        view.findViewById<ImageView>(R.id.edit_first_name_image).setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.edit_dialog, null)
            val newText = dialogView.findViewById<EditText>(R.id.new_text)
            newText.setText(firstNameTextView.text)

            val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                .setTitle(getString(R.string.edit_first_name))
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newFirstName = newText.text.toString()
                if (newFirstName.isNotEmpty()) {
                    reference.child("Users").child(emailTextView.text.toString().replace(".", "-")).child("firstName").setValue(newFirstName).addOnCompleteListener {
                        firstNameTextView.text = newFirstName
                        dialog.dismiss()
                    }
                }
                else {
                    Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<ImageView>(R.id.edit_last_name_image).setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.edit_dialog, null)
            val newText = dialogView.findViewById<EditText>(R.id.new_text)
            newText.setText(lastNameTextView.text)

            val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                .setTitle(getString(R.string.edit_last_name))
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newLastName = newText.text.toString()
                if (newLastName.isNotEmpty()) {
                    reference.child("Users").child(emailTextView.text.toString().replace(".", "-")).child("lastName").setValue(newLastName).addOnCompleteListener {
                        lastNameTextView.text = newLastName
                        dialog.dismiss()
                    }
                }
                else {
                    Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<ImageView>(R.id.edit_nickname_image).setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.edit_dialog, null)
            val newText = dialogView.findViewById<EditText>(R.id.new_text)
            newText.setText(user)

            val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                .setTitle(getString(R.string.edit_nickname))
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            dialog.show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newNickname = newText.text.toString()
                if (newNickname.isNotEmpty()) {
                    if (user != newNickname) {
                        if (!allNicknames.contains(newNickname)) {
                            if (canNicknameBeUpdated(dbReference, user)) {
                                val oldNickname = user
                                user = newNickname
                                allNicknames.remove(oldNickname)
                                allNicknames.add(newNickname)
                                val updateUser = ContentValues()
                                updateUser.put("UserNickname", newNickname)
                                dbReference.update("USER", updateUser, "UserNickname = ?", arrayOf(oldNickname))
                                dbReference.update("USER_LAST_ACCESSED", updateUser, "UserNickname = ?", arrayOf(oldNickname))
                                dbReference.update("LAST_UPDATED_TIME", updateUser, "UserNickname = ?", arrayOf(oldNickname))
                                dbReference.update("LAST_UPDATED_NICKNAME", updateUser, "UserNickname = ?", arrayOf(oldNickname))
                                reference.child("Users").child(emailTextView.text.toString().replace(".", "-")).child("nickname").setValue(newNickname).addOnCompleteListener {
                                    nicknameTextView.text = newNickname
                                    reference.child("User-Managers").get().addOnCompleteListener {
                                        if (it.result.hasChild(oldNickname)) {
                                            val isManagerModeActive = it.result.child(oldNickname).child("Activated").value.toString().toBoolean()
                                            reference.child("User-Managers").child(newNickname).child("Activated").setValue(isManagerModeActive)
                                            reference.child("User-Managers").child(oldNickname).removeValue()
                                        }
                                    }
                                    reference.child("Championships").get().addOnCompleteListener {
                                        for (c in it.result.children) {
                                            for (s in c.children) {
                                                for (r in s.child("Matches").children) {
                                                    val roundReference = reference.child("Championships").child(c.key.toString()).child(s.key.toString()).child("Matches").child(r.key.toString())
                                                    val updateManageMatchDay = r.child("ManageMatchDay")
                                                    if (updateManageMatchDay.hasChild(oldNickname)) {
                                                        val saveDoublePointsActivated = updateManageMatchDay.child(oldNickname).child("DoublePointsActivated").value.toString().toBoolean()
                                                        roundReference.child("ManageMatchDay").child(oldNickname).removeValue()
                                                        roundReference.child("ManageMatchDay").child(newNickname).child("DoublePointsActivated").setValue(saveDoublePointsActivated)
                                                    }
                                                    for (m in r.child("Matches").children) {
                                                        if (m.child("Predictions").hasChild(oldNickname)) {
                                                            val homeTeam = m.key.toString().split("-")[0]
                                                            val guestTeam = m.key.toString().split("-")[1]
                                                            val matchPredictionsReference = roundReference.child("Matches").child("$homeTeam-$guestTeam").child("Predictions")
                                                            val oldNicknamePredictions = m.child("Predictions").child(oldNickname)
                                                            if (oldNicknamePredictions.hasChild("DoublePointsActivatedInMatch")) {
                                                                matchPredictionsReference.child(newNickname).child("DoublePointsActivatedInMatch").setValue("Activated")
                                                            }
                                                            if (oldNicknamePredictions.hasChild("Scores")) {
                                                                val homePrediction = oldNicknamePredictions.child("Scores").child(homeTeam).value.toString()
                                                                val guestPrediction = oldNicknamePredictions.child("Scores").child(guestTeam).value.toString()
                                                                matchPredictionsReference.child(newNickname).child("Scores").child(homeTeam).setValue(homePrediction.toInt())
                                                                matchPredictionsReference.child(newNickname).child("Scores").child(guestTeam).setValue(guestPrediction.toInt())
                                                            }
                                                            if (oldNicknamePredictions.hasChild("Scorers")) {
                                                                val homeScorersPredicted = oldNicknamePredictions.child("Scorers").child(homeTeam)
                                                                val guestScorersPredicted = oldNicknamePredictions.child("Scorers").child(guestTeam)
                                                                for (hS in homeScorersPredicted.children) {
                                                                    val scorerNumber = hS.key.toString()
                                                                    val goalType = hS.child("goalType").value.toString()
                                                                    val shirt = hS.child("shirt").value.toString()
                                                                    val scorer = GoalOrOwnGoal(goalType, shirt.toInt())
                                                                    matchPredictionsReference.child(newNickname).child("Scorers").child(homeTeam).child(scorerNumber).setValue(scorer)
                                                                }
                                                                for (gS in guestScorersPredicted.children) {
                                                                    val scorerNumber = gS.key.toString()
                                                                    val goalType = gS.child("goalType").value.toString()
                                                                    val shirt = gS.child("shirt").value.toString()
                                                                    val scorer = GoalOrOwnGoal(goalType, shirt.toInt())
                                                                    matchPredictionsReference.child(newNickname).child("Scorers").child(guestTeam).child(scorerNumber).setValue(scorer)
                                                                }
                                                            }
                                                            if (oldNicknamePredictions.hasChild("MVP")) {
                                                                val mvpPredicted = oldNicknamePredictions.child("MVP")
                                                                val shirt = mvpPredicted.child("shirt").value.toString()
                                                                val team = mvpPredicted.child("team").value.toString()
                                                                val mvp = MVPPlayer(team, shirt.toInt())
                                                                matchPredictionsReference.child(newNickname).child("MVP").setValue(mvp)
                                                            }
                                                            matchPredictionsReference.child(oldNickname).removeValue()
                                                        }
                                                    }
                                                    val best11Prediction = r.child("Best11Predictions")
                                                    val best11Reference = roundReference.child("Best11Predictions")
                                                    if (best11Prediction.hasChild(oldNickname)) {
                                                        val module = best11Prediction.child(oldNickname).child("Module").value.toString()
                                                        best11Reference.child(newNickname).child("Module").setValue(module)
                                                        if (best11Prediction.child(oldNickname).hasChild("Players")) {
                                                            for (p in best11Prediction.child(oldNickname).child("Players").children) {
                                                                val role = p.key.toString()
                                                                val team = p.child("team").value.toString()
                                                                val shirt = p.child("shirt").value.toString()
                                                                val best11Player = MVPPlayer(team, shirt.toInt())
                                                                best11Reference.child(newNickname).child("Players").child(role).setValue(best11Player)
                                                            }
                                                        }
                                                        best11Reference.child(oldNickname).removeValue()
                                                    }
                                                }
                                                if (s.child("TotalPoints").hasChild(oldNickname)) {
                                                    val oldNicknameTotalPoints = s.child("TotalPoints").child(oldNickname)
                                                    val totalPointsReference = reference.child("Championships").child(c.key.toString()).child(s.key.toString()).child("TotalPoints")
                                                    for (r in oldNicknameTotalPoints.children) {
                                                        for (mP in r.children) {
                                                            val match = mP.key.toString()
                                                            val points = mP.value.toString()
                                                            totalPointsReference.child(newNickname).child(r.key.toString()).child(match).setValue(points.toInt())
                                                        }
                                                    }
                                                    totalPointsReference.child(oldNickname).removeValue()
                                                }
                                            }
                                        }
                                    }
                                    dialog.dismiss()
                                }
                            }
                            else {
                                Toast.makeText(view.context, getString(R.string.nickname_cannot_be_updated), Toast.LENGTH_SHORT).show()
                            }
                        }
                        else {
                            Toast.makeText(view.context, getString(R.string.nickname_already_used), Toast.LENGTH_SHORT).show()
                        }
                    }
                    else {
                        dialog.dismiss()
                    }
                }
                else {
                    Toast.makeText(view.context, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show()
                }
            }
        }

        val allTeamsEvents = mutableListOf<TeamEventInfo>()
        val allTeamsNames = mutableListOf<String>()
        val allTeamsCaptains = mutableMapOf<String, String>()
        val allTeamsComponents = mutableMapOf<String, MutableList<String>>()
        var userFoundInTeams = false

        val showJoinTeamRelativeLayout = view.findViewById<RelativeLayout>(R.id.show_team_join)
        val showJoinedTeamRelativeLayout = view.findViewById<RelativeLayout>(R.id.show_team_joined)

        var userTeam = ""
        reference.child("TeamsEvents").get().addOnCompleteListener { findTeams ->
            for (t in findTeams.result.children) {
                val teamName = t.key.toString()
                allTeamsNames.add(teamName)
                val teamInfo = t.child("Info")
                val isPrivate = teamInfo.child("isPrivate").value.toString().toBoolean()
                if (isPrivate) {
                    val passwordHash = teamInfo.child("password").value.toString()
                    val passwordSalt = teamInfo.child("passwordSalt").value.toString()
                    val teamEvent = TeamEventInfo(teamName, true, passwordHash, passwordSalt)
                    allTeamsEvents.add(teamEvent)
                }
                else {
                    val teamEvent = TeamEventInfo(teamName)
                    allTeamsEvents.add(teamEvent)
                }
                val teamComponents = mutableListOf<String>()
                val creator = t.child("Creator").value.toString()
                teamComponents.add(creator)
                allTeamsCaptains[teamName] = creator
                if (creator == user) {
                    userFoundInTeams = true
                    userTeam = teamName
                }
                for (c in t.child("Components").children) {
                    val teamComponent = c.key.toString()
                    teamComponents.add(teamComponent)
                    if (teamComponent == user) {
                        userFoundInTeams = true
                        userTeam = teamName
                    }
                }
                allTeamsComponents[teamName] = teamComponents
                if (allTeamsComponents[teamName]!!.size == 11) {
                    allTeamsEvents.removeLast()
                }
            }
            if (userFoundInTeams) {
                showJoinedTeamRelativeLayout.visibility = VISIBLE
                view.findViewById<TextView>(R.id.team_joined_name).text = userTeam
                val teamComponentsRecyclerView = view.findViewById<RecyclerView>(R.id.team_components_recycler_view)
                val teamComponentsAdapter = TeamsEventsComponentsAdapter(allTeamsCaptains[userTeam]!!, allTeamsComponents[userTeam]!!, user)
                teamComponentsRecyclerView.adapter = teamComponentsAdapter
                showJoinTeamRelativeLayout.visibility = GONE
            }
            else {
                showJoinTeamRelativeLayout.visibility = VISIBLE
                showJoinedTeamRelativeLayout.visibility = GONE
            }

            val createTeamButton = view.findViewById<Button>(R.id.create_team)
            createTeamButton.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.create_team_dialog, null)
                val teamNameEditText = dialogView.findViewById<EditText>(R.id.team_name)
                val privateTeamCheckBox = dialogView.findViewById<CheckBox>(R.id.private_team_checkbox)
                val privateTeamPasswordEditText = dialogView.findViewById<EditText>(R.id.team_password)
                val createTeam = dialogView.findViewById<Button>(R.id.create_team_button)

                val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                    .setTitle(getString(R.string.create_team))
                    .create()

                dialog.show()

                privateTeamCheckBox.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        privateTeamPasswordEditText.visibility = VISIBLE
                    }
                    else {
                        privateTeamPasswordEditText.visibility = GONE
                        privateTeamPasswordEditText.setText("")
                    }
                }

                createTeam.setOnClickListener {
                    val newTeamName = teamNameEditText.text.toString()
                    val isPrivate = privateTeamCheckBox.isChecked
                    val password = privateTeamPasswordEditText.text.toString()
                    if (newTeamName.isNotEmpty()) {
                        if (!allTeamsNames.contains(newTeamName)) {
                            if (!isPrivate) {
                                val newTeam = TeamEventInfo(newTeamName)
                                reference.child("TeamsEvents").child(newTeamName).child("Info").setValue(newTeam).addOnCompleteListener {
                                    Toast.makeText(view.context, R.string.team_created_correctly, Toast.LENGTH_LONG).show()
                                    teamNameEditText.setText("")
                                    dialog.dismiss()
                                    showJoinTeamRelativeLayout.visibility = GONE
                                    showJoinedTeamRelativeLayout.visibility = VISIBLE
                                }
                                reference.child("TeamsEvents").child(newTeamName).child("Creator").setValue(user)
                            }
                            else {
                                if (password.isNotEmpty()) {
                                    val salt = generateSalt()
                                    val newTeam = TeamEventInfo(newTeamName, true, hashPassword(password, salt), salt)
                                    reference.child("TeamsEvents").child(newTeamName).child("Info").setValue(newTeam).addOnCompleteListener {
                                        Toast.makeText(view.context, R.string.team_created_correctly, Toast.LENGTH_LONG).show()
                                        teamNameEditText.setText("")
                                        privateTeamCheckBox.isChecked = false
                                        privateTeamPasswordEditText.setText("")
                                        dialog.dismiss()
                                        showJoinTeamRelativeLayout.visibility = GONE
                                        showJoinedTeamRelativeLayout.visibility = VISIBLE
                                    }
                                    reference.child("TeamsEvents").child(newTeamName).child("Creator").setValue(user)
                                }
                                else {
                                    Toast.makeText(view.context, R.string.team_password_required, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        else {
                            Toast.makeText(view.context, R.string.team_name_already_used, Toast.LENGTH_LONG).show()
                        }
                    }
                    else {
                        Toast.makeText(view.context, R.string.team_name_required, Toast.LENGTH_LONG).show()
                    }
                }
            }

            val addTeamComponentReference = reference.child("TeamsEvents")

            val showAllTeamsButton = view.findViewById<Button>(R.id.show_teams)
            showAllTeamsButton.setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.teams_dialog, null)
                val teamsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.teams_recycler_view)

                val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                    .setTitle(getString(R.string.select_team))
                    .create()

                var allTeamsAdapter = AllTeamsEventsSelectAdapter(user, allTeamsEvents, allTeamsCaptains, allTeamsComponents, dialog, view, addTeamComponentReference)
                teamsRecyclerView.adapter = allTeamsAdapter

                val searchTeamEditText = dialogView.findViewById<EditText>(R.id.search_team)
                searchTeamEditText.doOnTextChanged { teamSearched, _, _, _ ->
                    if (teamSearched.toString().isNotEmpty()) {
                        val teamsAvailable = mutableListOf<TeamEventInfo>()
                        for (t in allTeamsEvents) {
                            if (t.getTeamName().contains(teamSearched.toString(), true)) {
                                teamsAvailable.add(t)
                            }
                        }
                        allTeamsAdapter = AllTeamsEventsSelectAdapter(user, teamsAvailable, allTeamsCaptains, allTeamsComponents, dialog, view, addTeamComponentReference)
                        teamsRecyclerView.adapter = allTeamsAdapter
                    }
                    else {
                        allTeamsAdapter = AllTeamsEventsSelectAdapter(user, allTeamsEvents, allTeamsCaptains, allTeamsComponents, dialog, view, addTeamComponentReference)
                        teamsRecyclerView.adapter = allTeamsAdapter
                    }
                }

                dialog.show()
            }

            view.findViewById<ProgressBar>(R.id.progress_updating_user_info).visibility = INVISIBLE
        }
    }

    private fun canNicknameBeUpdated(dbReference: SQLiteDatabase, user: String): Boolean {
        var canBeUpdated = false
        val findLastUpdate = dbReference.rawQuery("SELECT UpdatedTime FROM LAST_UPDATED_NICKNAME WHERE UserNickname = ?", arrayOf(user))
        val lastUpdateFound = findLastUpdate.count

        if (lastUpdateFound == 1) {
            if (findLastUpdate.moveToFirst()) {
                val lastUpdateString = findLastUpdate.getString(0)
                val lastUpdate = LocalDateTime.parse(lastUpdateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

                val actualDateTime = LocalDateTime.now()

                if (lastUpdate.plusMonths(1).isBefore(actualDateTime) || lastUpdate.plusMonths(1).isEqual(actualDateTime)) {
                    canBeUpdated = true
                    val updateLastDataUpdatedTime = ContentValues()
                    val buildDateTime = actualDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    updateLastDataUpdatedTime.put("UpdatedTime", buildDateTime)
                    dbReference.update("LAST_UPDATED_NICKNAME", updateLastDataUpdatedTime, "UserNickname = ?", arrayOf(user))
                }
            }
        }
        else {
            val insertLastDataUpdatedTime = ContentValues()
            val actualDateTime = LocalDateTime.now()
            val buildDateTime = actualDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            insertLastDataUpdatedTime.put("UserNickname", user)
            insertLastDataUpdatedTime.put("UpdatedTime", buildDateTime)
            dbReference.insert("LAST_UPDATED_NICKNAME", null, insertLastDataUpdatedTime)
            canBeUpdated = true
        }
        findLastUpdate.close()
        return canBeUpdated
    }

    private fun hashPassword(password: String, salt: String): String {
        return try {
            val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 65536, 128)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val hash = factory.generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(hash)
        }
        catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("", e)
        }
        catch (e: InvalidKeySpecException) {
            throw RuntimeException("", e)
        }
    }

    private fun generateSalt(): String {
        return Base64.getEncoder().encodeToString(SecureRandom.getSeed(16))
    }
}