package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.adapters.PlayersAddedAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.RolesAdapter
import com.embeddedproject.calciofemminileitaliano.adapters.TeamsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.AllTeamsJSON
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.Scorer
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress("DEPRECATION")
class AddPlayers : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    private val pickJsonFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { jsonURI ->
            val jsonString = readTextFromUri(jsonURI)
            jsonString?.let {
                val arguments = AddPlayersArgs.fromBundle(requireArguments())
                uploadToDatabase(it, arguments.season)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_players, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = AddPlayersArgs.fromBundle(requireArguments())
        val user = arguments.userNickname
        val season = arguments.season

        view.findViewById<ImageView>(R.id.back_to_select_championship).setOnClickListener {
            val navigateToSelectChampionship = AddPlayersDirections.actionAddPlayersToSelectChampionship(user)
            view.findNavController().navigate(navigateToSelectChampionship)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = AddPlayersDirections.actionAddPlayersToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
        }

        view.findViewById<TextView>(R.id.season_info).text = season

        var playersAlreadyAdded = mutableListOf<Player>()
        var playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded)
        val playersAddedRecyclerView = view.findViewById<RecyclerView>(R.id.players_team_added)
        playersAddedRecyclerView.adapter = playersAddedAdapter

        val shirtNumber = view.findViewById<NumberPicker>(R.id.player_number)
        shirtNumber.minValue = 0
        shirtNumber.maxValue = 99

        reference.child("Championships").get().addOnCompleteListener {
            val searchTeams = view.findViewById<EditText>(R.id.search_team)
            val addPlayerButton = view.findViewById<Button>(R.id.submit_add_player)
            var lastTeamSelected = -1
            var roleSelected = -1
            val roles = mutableListOf<String>()
            roles.add("Goalkeeper")
            roles.add("Defender")
            roles.add("Midfielder")
            roles.add("Forward")
            val rolesAdapter = RolesAdapter(view.context, R.layout.role, roles)
            val rolesTeamView = view.findViewById<ListView>(R.id.roles)
            rolesTeamView.adapter = rolesAdapter

            rolesTeamView.setOnItemClickListener { _, _, role, _ ->
                rolesAdapter.setSelectedPosition(role)
                if (role != roleSelected) {
                    roleSelected = role
                }
            }

            val allTeamsTranslated = mutableMapOf<String,String>()
            val allTeams = mutableListOf<String>()
            for (c in it.result.children) {
                for (s in c.children) {
                    if (s.key.toString() == season) {
                        for (t in s.child("Teams").children) {
                            val team = if (s.child("Info").hasChild("hasInternationalTeams")) {
                                getString(view.resources.getIdentifier(t.key.toString().lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                            }
                            else {
                               t.key.toString()
                            }
                            if (!allTeams.contains(t.key.toString())) {
                                allTeamsTranslated[t.key.toString()] = team
                                allTeams.add(t.key.toString())
                            }
                        }
                    }
                }
            }
            allTeamsTranslated.keys.sorted()
            allTeams.sort()

            view.findViewById<ProgressBar>(R.id.progress_updating_adding).visibility = INVISIBLE
            val teamsAdapter = TeamsAdapter(view.context, R.layout.team, allTeams, allTeamsTranslated)
            val teamsListView = view.findViewById<ListView>(R.id.teams)
            view.findViewById<TextView>(R.id.select_team).visibility = VISIBLE
            teamsListView.visibility = VISIBLE
            searchTeams.visibility = VISIBLE
            teamsListView.adapter = teamsAdapter
            val addedPlayersTextView = view.findViewById<TextView>(R.id.added_players)

            teamsListView.setOnItemClickListener { _, _, team, _ ->
                view.findViewById<RelativeLayout>(R.id.add_new_player).visibility = VISIBLE
                playersAddedRecyclerView.visibility = VISIBLE
                if (team != lastTeamSelected) {
                    playersAlreadyAdded = emptyList<Player>().toMutableList()
                    playersAddedAdapter.dataChanged()
                    playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded)
                    playersAddedRecyclerView.adapter = playersAddedAdapter
                    teamsAdapter.setSelectedPosition(team)
                    lastTeamSelected = team
                    val teamName = allTeams[team]
                    val teamNameTranslated = allTeamsTranslated[teamName]
                    val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(teamName))
                    var teamBitmap: Bitmap? = null
                    if (setTeamImage.moveToFirst()) {
                        teamBitmap = BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size)
                    }
                    setTeamImage.close()
                    val addTeamPlayer = "${getString(R.string.add_player)}\n$teamNameTranslated"
                    view.findViewById<TextView>(R.id.add_team_player).text = addTeamPlayer
                    addPlayerButton.text = addTeamPlayer
                    addedPlayersTextView.visibility = VISIBLE
                    val addedPlayers = "${getString(R.string.added_players)}\n$teamNameTranslated"
                    addedPlayersTextView.text = addedPlayers

                    reference.child("Players").child(season).child(teamName).get().addOnCompleteListener { playersAdded ->
                        for (player in playersAdded.result.children) {
                            val firstName = player.child("firstName").value.toString()
                            val lastName = player.child("lastName").value.toString()
                            val role = player.child("role").value.toString()
                            val shirt = player.key.toString().toInt()
                            activity?.runOnUiThread {
                                playersAlreadyAdded.add(Player(firstName, lastName, shirt, role, teamName))
                            }
                        }
                        playersAlreadyAdded = playersAlreadyAdded.sortedWith(compareBy({ sc -> if (sc.role == "Goalkeeper") 0 else 1 }, { sc -> if (sc.role == "Defender") 0 else 1 }, { sc -> if (sc.role == "Midfielder") 0 else 1 }, { sc -> sc.shirtNumber })).toMutableList()
                        playersAddedAdapter.dataChanged()
                        playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded, teamBitmap)
                        playersAddedRecyclerView.adapter = playersAddedAdapter
                    }
                }
            }

            searchTeams.doOnTextChanged { teamSearched, _, _, _ ->
                teamsAdapter.setSelectedPosition(-1)
                lastTeamSelected = -1
                view.findViewById<RelativeLayout>(R.id.add_new_player).visibility = INVISIBLE
                playersAddedRecyclerView.visibility = INVISIBLE
                addedPlayersTextView.visibility = INVISIBLE
                allTeamsTranslated.clear()
                allTeams.removeAll(allTeams)
                playersAlreadyAdded = emptyList<Player>().toMutableList()
                playersAddedAdapter.dataChanged()
                playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded)
                playersAddedRecyclerView.adapter = playersAddedAdapter
                for (c in it.result.children) {
                    for (s in c.children) {
                        if (s.key.toString() == season) {
                            for (t in s.child("Teams").children) {
                                val team = if (s.child("Info").hasChild("hasInternationalTeams")) {
                                    getString(view.resources.getIdentifier(t.key.toString().lowercase().replace(" ", "_"), "string", view.resources.getResourcePackageName(R.string.app_name)))
                                }
                                else {
                                    t.key.toString()
                                }
                                if (teamSearched.toString().isEmpty()) {
                                    if (!allTeams.contains(t.key.toString())) {
                                        allTeamsTranslated[t.key.toString()] = team
                                        allTeams.add(t.key.toString())
                                    }
                                }
                                else if (team.contains(teamSearched.toString(), true)) {
                                    if (!allTeams.contains(t.key.toString())) {
                                        allTeamsTranslated[t.key.toString()] = team
                                        allTeams.add(t.key.toString())
                                    }
                                }
                            }
                        }
                    }
                }
                allTeamsTranslated.keys.sorted()
                allTeams.sort()

                teamsListView.adapter = teamsAdapter

                teamsListView.setOnItemClickListener { _, _, team, _ ->
                    view.findViewById<RelativeLayout>(R.id.add_new_player).visibility = VISIBLE
                    playersAddedRecyclerView.visibility = VISIBLE
                    if (team != lastTeamSelected) {
                        playersAlreadyAdded = emptyList<Player>().toMutableList()
                        playersAddedAdapter.dataChanged()
                        playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded)
                        playersAddedRecyclerView.adapter = playersAddedAdapter
                        teamsAdapter.setSelectedPosition(team)
                        lastTeamSelected = team
                        val teamName = allTeams[team]
                        val teamNameTranslated = allTeamsTranslated[teamName]
                        var teamBitmap: Bitmap? = null
                        val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(teamName))
                        if (setTeamImage.moveToFirst()) {
                            teamBitmap = BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size)
                        }
                        setTeamImage.close()
                        val addTeamPlayer = "${getString(R.string.add_player)}\n$teamNameTranslated"
                        view.findViewById<TextView>(R.id.add_team_player).text = addTeamPlayer
                        addPlayerButton.text = addTeamPlayer
                        addedPlayersTextView.visibility = VISIBLE
                        val addedPlayers = "${getString(R.string.added_players)}\n$teamNameTranslated"
                        addedPlayersTextView.text = addedPlayers

                        reference.child("Players").child(season).child(teamName).get().addOnCompleteListener { playersAdded ->
                            for (player in playersAdded.result.children) {
                                val firstName = player.child("firstName").value.toString()
                                val lastName = player.child("lastName").value.toString()
                                val role = player.child("role").value.toString()
                                val shirt = player.key.toString().toInt()
                                activity?.runOnUiThread {
                                    playersAlreadyAdded.add(Player(firstName, lastName, shirt, role, teamName))
                                }
                            }
                            playersAlreadyAdded = playersAlreadyAdded.sortedWith(compareBy( { sc -> sc.role }, { sc -> sc.lastName })).toMutableList()
                            playersAddedAdapter.dataChanged()
                            playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded, teamBitmap)
                            playersAddedRecyclerView.adapter = playersAddedAdapter
                        }
                    }
                }
            }

            addPlayerButton.setOnClickListener {
                val firstNameTextView = view.findViewById<TextView>(R.id.player_first_name)
                val lastNameTextView = view.findViewById<TextView>(R.id.player_last_name)
                val playerFirstName = firstNameTextView.text.toString()
                val playerLastName = lastNameTextView.text.toString()
                val number = shirtNumber.value.toString()
                if (playerFirstName.isNotEmpty() && playerLastName.isNotEmpty() && roleSelected != -1 && lastTeamSelected != -1) {
                    val newPlayer = Scorer(playerFirstName, playerLastName, roles[roleSelected])
                    reference.child("Players").child(season).child(allTeams[lastTeamSelected]).get().addOnCompleteListener { playerAdded ->
                        if (playerAdded.result.hasChild(number)) {
                            showPlayerAddedMessage(view.context, getString(R.string.player_already_added))
                        }
                        else {
                            reference.child("Players").child(season).child(allTeams[lastTeamSelected]).child(number).setValue(newPlayer).addOnCompleteListener {
                                showPlayerAddedMessage(view.context, getString(R.string.player_added))
                                playersAlreadyAdded.add(Player(playerFirstName, playerLastName, number.toInt(), roles[roleSelected], allTeams[lastTeamSelected]))
                                playersAlreadyAdded = playersAlreadyAdded.sortedWith(compareBy( { sc -> sc.role }, { sc -> sc.lastName })).toMutableList()
                                playersAddedAdapter.dataChanged()
                                var teamBitmap: Bitmap? = null
                                val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(allTeams[lastTeamSelected]))
                                if (setTeamImage.moveToFirst()) {
                                    teamBitmap = BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size)
                                }
                                setTeamImage.close()
                                playersAddedAdapter = PlayersAddedAdapter(playersAlreadyAdded, teamBitmap)
                                playersAddedRecyclerView.adapter = playersAddedAdapter
                                firstNameTextView.text = ""
                                lastNameTextView.text = ""
                                shirtNumber.value = 0
                                rolesAdapter.setSelectedPosition(-1)
                                roleSelected = -1
                            }
                        }
                    }
                }
                else if ((playerFirstName.isEmpty() || playerLastName.isEmpty()) && roleSelected == -1) {
                    Toast.makeText(view.context, R.string.all_fields_required, Toast.LENGTH_LONG).show()
                }
                else if (roleSelected == -1) {
                    Toast.makeText(view.context, R.string.role_required, Toast.LENGTH_LONG).show()
                }
                else {
                    Toast.makeText(view.context, R.string.all_fields_required, Toast.LENGTH_LONG).show()
                }
            }

            val importPlayersButton = view.findViewById<ImageView>(R.id.import_players)
            importPlayersButton.setOnClickListener {
                Toast.makeText(view.context, R.string.search_in_file_system, Toast.LENGTH_LONG).show()
                openFilePicker()
            }
        }
    }

    private fun showPlayerAddedMessage(context: Context, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(message)

        builder.setPositiveButton(getString(R.string.ok)) { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun openFilePicker() {
        pickJsonFile.launch("application/json")
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            reader.close()
            stringBuilder.toString()
        }
        catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseJson(jsonString: String): AllTeamsJSON? {
        val gson = Gson()
        return try {
            gson.fromJson(jsonString, AllTeamsJSON::class.java)
        }
        catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uploadToDatabase(jsonString: String, season: String) {
        val allTeams = parseJson(jsonString)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        reference.child("Championships").get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val teamsInDatabase = mutableListOf<String>()
                val totalPlayers = allTeams?.teams?.sumOf { it.players.size } ?: 0
                var playersAddedSuccessfully = 0
                val teamsWithIssues = mutableMapOf<String, Int>()

                for (c in task.result.children) {
                    for (s in c.children) {
                        if (s.key.toString() == season) {
                            for (t in s.child("Teams").children) {
                                val team = t.key.toString()
                                if (!teamsInDatabase.contains(team)) {
                                    teamsInDatabase.add(team)
                                }
                            }
                        }
                    }
                }

                allTeams?.teams?.forEach { team ->
                    var failedInTeam = 0

                    team.players.forEach { player ->
                        val firstName = player.firstName
                        val lastName = player.lastName
                        val shirt = player.shirt
                        val role = player.role
                        val playerImported = Scorer(firstName, lastName, role)
                        var teamFound = false
                        var realTeamName = ""

                        for (tDB in teamsInDatabase) {
                            if (team.team.contains(tDB, true)) {
                                teamFound = true
                                realTeamName = tDB
                                break
                            }
                        }

                        if (teamFound) {
                            playersAddedSuccessfully++
                            reference.child("Players").child(season).child(realTeamName).child(shirt.toString()).setValue(playerImported).addOnCompleteListener {}
                        }
                        else {
                            failedInTeam++
                        }
                    }

                    if (failedInTeam > 0) {
                        teamsWithIssues[team.team] = failedInTeam
                    }
                }
                showCompletionToast(playersAddedSuccessfully, totalPlayers, teamsWithIssues)
            }
        }
    }

    private fun showCompletionToast(playersAddedSuccessfully: Int, totalPlayers: Int, teamsWithIssues: Map<String, Int>) {
        if (playersAddedSuccessfully == totalPlayers) {
            showCustomToast("${getString(R.string.players_imported_correctly)}: $totalPlayers")
        }
        else {
            val issuesMessage = buildString {
                append("${getString(R.string.problems_during_importing)}\n")
                teamsWithIssues.forEach { (team, failedCount) ->
                    append("$team -> ${getString(R.string.players_not_imported)}: $failedCount\n")
                }
            }
            showCustomToast(issuesMessage)
        }
    }

    @SuppressLint("InflateParams")
    fun showCustomToast(message: String) {
        val inflater = layoutInflater
        val layout = inflater.inflate(R.layout.completion_toast, null)

        val textView: TextView = layout.findViewById(R.id.toast_text)
        textView.text = message

        val toast = Toast(activity?.applicationContext)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }
}