package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.adapters.PlayerBest11Adapter
import com.embeddedproject.calciofemminileitaliano.adapters.SeasonsAdapter
import com.embeddedproject.calciofemminileitaliano.helpers.MVPPlayer
import com.embeddedproject.calciofemminileitaliano.helpers.Player
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.stream.IntStream.range

class PredictBest11 : Fragment() {

    private lateinit var db: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_predict_best11, container, false)
    }

    @SuppressLint("DiscouragedApi", "InflateParams", "CutPasteId", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseDatabase.getInstance()
        reference = db.reference

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        val arguments = PredictBest11Args.fromBundle(requireArguments())
        val user = arguments.userNickname
        val championship = arguments.championship
        val season = arguments.season
        val round = arguments.round
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
            val navigateToMatchesPredictions = PredictBest11Directions.actionPredictBest11ToMatchesPredictions(user, championship, season)
            view.findNavController().navigate(navigateToMatchesPredictions)
        }

        view.findViewById<ImageView>(R.id.logout).setOnClickListener {
            val builder = AlertDialog.Builder(context).setTitle(getString(R.string.logout))
            builder.setMessage(getString(R.string.are_you_sure_logout))

            builder.setPositiveButton(getString(R.string.confirm)) { dialog, _ ->
                dbReference.delete("USER", "UserNickname = ?", arrayOf(user))
                Toast.makeText(view.context, getString(R.string.logout_completed), Toast.LENGTH_LONG).show()
                val navigateToLoginRegistration = PredictBest11Directions.actionPredictBest11ToLoginRegistration()
                view.findNavController().navigate(navigateToLoginRegistration)
                dialog.dismiss()
            }

            builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }

            val dialog = builder.create()
            dialog.show()
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
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))}\n$dayDescription)\n${getString(R.string.predict_best11)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }
        else if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            var resultDetails = "${getString(resources.getIdentifier(championship.lowercase().replace(" ", "_"), "string", activity?.packageName))} - $dayDescription)\n${getString(R.string.predict_best11)}"
            if (!dayDescription.contains(getString(R.string.day))) {
                resultDetails = resultDetails.replace(")", "")
            }
            view.findViewById<TextView>(R.id.championship_name).text = resultDetails
        }

        view.findViewById<TextView>(R.id.season_info).text = season

        val listViewRolePlayers = view.findViewById<ListView>(R.id.list_view_players_per_role)

        reference.get().addOnCompleteListener { inDatabase ->
            val teamsBitmap = mutableMapOf<String, Bitmap>()
            val teamsInMatchDay = mutableListOf<String>()
            val findTeamsInMatchDay = inDatabase.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Matches")
            for (m in findTeamsInMatchDay.children) {
                val matchTeams = m.key.toString().split("-")
                teamsInMatchDay.add(matchTeams[0])
                teamsInMatchDay.add(matchTeams[1])
            }
            for (team in teamsInMatchDay) {
                val setTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team))
                if (setTeamImage.moveToFirst()) {
                    val bitmap = BitmapFactory.decodeByteArray(setTeamImage.getBlob(0), 0, setTeamImage.getBlob(0).size)
                    teamsBitmap[team] = bitmap
                }
                setTeamImage.close()
            }
            val allGoalkeepers = mutableListOf<Player>()
            val allDefenders = mutableListOf<Player>()
            val allMidfielders = mutableListOf<Player>()
            val allForwards = mutableListOf<Player>()
            val playersGet = inDatabase.result.child("Players").child(season)
            for (team in teamsInMatchDay) {
                for (player in playersGet.child(team).children) {
                    val shirt = player.key.toString().toInt()
                    val firstName = player.child("firstName").value.toString()
                    val lastName = player.child("lastName").value.toString()
                    val role = player.child("role").value.toString()
                    if (!disqualifiedPlayersList.contains(MVPPlayer(team, shirt))) {
                        val newPlayer = Player(firstName, lastName, shirt, role, team)
                        when (role) {
                            "Goalkeeper" -> allGoalkeepers.add(newPlayer)
                            "Defender" -> allDefenders.add(newPlayer)
                            "Midfielder" -> allMidfielders.add(newPlayer)
                            "Forward" -> allForwards.add(newPlayer)
                        }
                    }
                }
            }

            val module433RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_433)
            val module442RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_442)
            val module4231RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_4231)
            val module352RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_352)
            val module343RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_343)
            val module3412RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_3412)
            val module4312RelativeLayout = view.findViewById<RelativeLayout>(R.id.layout_4312)

            var moduleSelected : String
            var actualModule : String
            val moduleGet = inDatabase.result.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions").child(user)
            val predictBest11Reference = reference.child("Championships").child(championship).child(season).child("Matches").child(round.toString()).child("Best11Predictions").child(user)
            if (moduleGet.hasChild("Module")) {
                moduleSelected = moduleGet.child("Module").value.toString()
                actualModule = moduleGet.child("Module").value.toString()
                when (actualModule) {
                    "4-3-3" -> {
                        module433RelativeLayout.visibility = VISIBLE
                    }
                    "4-4-2" -> {
                        module442RelativeLayout.visibility = VISIBLE
                    }
                    "4-2-3-1" -> {
                        module4231RelativeLayout.visibility = VISIBLE
                    }
                    "3-5-2" -> {
                        module352RelativeLayout.visibility = VISIBLE
                    }
                    "3-4-3" -> {
                        module343RelativeLayout.visibility = VISIBLE
                    }
                    "3-4-1-2" -> {
                        module3412RelativeLayout.visibility = VISIBLE
                    }
                    "4-3-1-2" -> {
                        module4312RelativeLayout.visibility = VISIBLE
                    }
                }
            }
            else {
                moduleSelected = "4-3-3"
                actualModule = "4-3-3"
                module433RelativeLayout.visibility = VISIBLE
                predictBest11Reference.child("Module").setValue("4-3-3")
            }

            view.findViewById<TextView>(R.id.module).text = actualModule

            val allModules = mutableListOf<String>()
            allModules.add("4-3-3")
            allModules.add("4-4-2")
            allModules.add("4-2-3-1")
            allModules.add("3-5-2")
            allModules.add("3-4-3")
            allModules.add("3-4-1-2")
            allModules.add("4-3-1-2")
            val modulesAllocations = mutableMapOf<String, String>()
            modulesAllocations["4-3-3"] = "DDDD-MMM-FFF"
            modulesAllocations["4-4-2"] = "DDDD-MMMM-FF"
            modulesAllocations["4-2-3-1"] = "DDDD-MM-FF-FF"
            modulesAllocations["3-5-2"] = "DDD-FMMMF-FF"
            modulesAllocations["3-4-3"] = "DDD-MMMM-FFF"
            modulesAllocations["3-4-1-2"] = "DDD-DMMD-F-FF"
            modulesAllocations["4-3-1-2"] = "DDDD-MMM-F-FF"
            val allModulesLayout = mutableMapOf<String, RelativeLayout>()
            allModulesLayout["4-3-3"] = module433RelativeLayout
            allModulesLayout["4-4-2"] = module442RelativeLayout
            allModulesLayout["4-2-3-1"] = module4231RelativeLayout
            allModulesLayout["3-5-2"] = module352RelativeLayout
            allModulesLayout["3-4-3"] = module343RelativeLayout
            allModulesLayout["3-4-1-2"] = module3412RelativeLayout
            allModulesLayout["4-3-1-2"] = module4312RelativeLayout
            val allModulesInclude = mutableMapOf<String, RelativeLayout>()
            allModulesInclude["4-3-3"] = view.findViewById(R.id.module_433)
            allModulesInclude["4-4-2"] = view.findViewById(R.id.module_442)
            allModulesInclude["4-2-3-1"] = view.findViewById(R.id.module_4231)
            allModulesInclude["3-5-2"] = view.findViewById(R.id.module_352)
            allModulesInclude["3-4-3"] = view.findViewById(R.id.module_343)
            allModulesInclude["3-4-1-2"] = view.findViewById(R.id.module_3412)
            allModulesInclude["4-3-1-2"] = view.findViewById(R.id.module_4312)
            val allPlayersModule = mutableMapOf<String, Map<String, View>>()
            val module433 = findModuleViews("4-3-3", allModulesInclude)
            allPlayersModule["4-3-3"] = module433
            val module442 = findModuleViews("4-4-2", allModulesInclude)
            allPlayersModule["4-4-2"] = module442
            val module4231 = findModuleViews("4-2-3-1", allModulesInclude)
            allPlayersModule["4-2-3-1"] = module4231
            val module352 = findModuleViews("3-5-2", allModulesInclude)
            allPlayersModule["3-5-2"] = module352
            val module343 = findModuleViews("3-4-3", allModulesInclude)
            allPlayersModule["3-4-3"] = module343
            val module3412 = findModuleViews("3-4-1-2", allModulesInclude)
            allPlayersModule["3-4-1-2"] = module3412
            val module4312 = findModuleViews("4-3-1-2", allModulesInclude)
            allPlayersModule["4-3-1-2"] = module4312
            val modulesAdapter = SeasonsAdapter(view.context, R.layout.season_dialog, allModules, R.color.module_select)

            val selectRole = view.findViewById<TextView>(R.id.select_role)

            val captainHandler = Handler(Looper.getMainLooper())
            var currentLongPressRunnable: Runnable? = null

            for (p in allPlayersModule[actualModule]!!.keys) {
                val positionView = allPlayersModule[actualModule]!![p]

                for (reset in allPlayersModule[actualModule]!!.keys) {
                    val resetPositionView = allPlayersModule[actualModule]!![reset]
                    resetPositionView!!.findViewById<ImageView>(R.id.add_best_player)?.setImageResource(R.drawable.add_best_player)
                    resetPositionView.findViewById<TextView>(R.id.shirt_number)?.setTextColor(resources.getColor(R.color.above_toolbar))
                }

                val playerPosition: String
                var playersRole: MutableList<Player>
                if (p.contains("GK")) {
                    playerPosition = "Goalkeeper"
                    playersRole = allGoalkeepers
                } else if (p.contains("D")) {
                    playerPosition = "Defender${p.last()}"
                    playersRole = allDefenders
                } else if (p.contains("M")) {
                    playerPosition = "Midfielder${p.last()}"
                    playersRole = allMidfielders
                } else {
                    playerPosition = "Forward${p.last()}"
                    playersRole = allForwards
                }

                predictBest11Reference.get().addOnCompleteListener { playerPredicted ->
                    if (playerPredicted.result.hasChild("Players")) {
                        if (playerPredicted.result.child("Players").hasChild(playerPosition)) {
                            val teamPredicted = playerPredicted.result.child("Players").child(playerPosition).child("team").value.toString()
                            val shirtPredicted = playerPredicted.result.child("Players").child(playerPosition).child("shirt").value.toString().toInt()
                            positionView!!.findViewById<ImageView>(R.id.add_best_player)?.visibility = GONE
                            positionView.findViewById<TextView>(R.id.shirt_number)?.visibility = VISIBLE
                            positionView.findViewById<ImageView>(R.id.player_team_image)?.visibility = VISIBLE
                            positionView.findViewById<TextView>(R.id.shirt_number)?.text = shirtPredicted.toString()
                            positionView.findViewById<ImageView>(R.id.player_team_image)?.setImageBitmap(teamsBitmap[teamPredicted])

                            if (playerPredicted.result.hasChild("Captain")) {
                                val captainPosition = playerPredicted.result.child("Captain").value.toString()
                                if (captainPosition == playerPosition) {
                                    positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = VISIBLE
                                }
                                else {
                                    positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = GONE
                                }
                            }
                        }
                        else {
                            positionView!!.findViewById<ImageView>(R.id.add_best_player)?.visibility = VISIBLE
                        }
                    }
                    else {
                        positionView!!.findViewById<ImageView>(R.id.add_best_player)?.visibility = VISIBLE
                    }
                }

                positionView!!.setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            currentLongPressRunnable?.let { captainHandler.removeCallbacks(it) }
                            val runnable = Runnable {
                                val shirtNumberText = positionView.findViewById<TextView>(R.id.shirt_number)?.text?.toString()
                                if (!shirtNumberText.isNullOrEmpty()) {
                                    val shirtNum = shirtNumberText.toInt()
                                    val playerSelected = playersRole.find { it.shirtNumber == shirtNum }
                                    if (playerSelected != null) {
                                        predictBest11Reference.child("Players").get().addOnCompleteListener { findPlayersPositionsAdded ->
                                            if (findPlayersPositionsAdded.result.hasChild(playerPosition)) {
                                                predictBest11Reference.child("Captain").setValue(playerPosition)
                                                for (reset in allPlayersModule[actualModule]!!.keys) {
                                                    val resetPositionView = allPlayersModule[actualModule]!![reset]
                                                    resetPositionView!!.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = GONE
                                                }
                                                positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = VISIBLE
                                            }
                                        }
                                    }
                                }
                            }
                            currentLongPressRunnable = runnable
                            captainHandler.postDelayed(runnable, 3000)
                            true
                        }

                        MotionEvent.ACTION_UP -> {
                            currentLongPressRunnable?.let { captainHandler.removeCallbacks(it) }
                            predictBest11Reference.child("Players").get().addOnCompleteListener { playersAdded ->
                                val alreadyAddedPlayer = mutableListOf<Player>()
                                for (pA in playersAdded.result.children) {
                                    if (pA.key.toString()[0] != 'G' && (!(p[0] == pA.key.toString()[0] && p.last() == pA.key.toString().last()))) {
                                        val addedTeam = pA.child("team").value.toString()
                                        val addedShirt = pA.child("shirt").value.toString()
                                        val findPlayer = inDatabase.result.child("Players").child(season).child(addedTeam).child(addedShirt)
                                        val firstName = findPlayer.child("firstName").value.toString()
                                        val lastName = findPlayer.child("lastName").value.toString()
                                        val role = findPlayer.child("role").value.toString()
                                        val addedPlayer = Player(firstName, lastName, addedShirt.toInt(), role, addedTeam)
                                        alreadyAddedPlayer.add(addedPlayer)
                                    }
                                }

                                val playersToAddRole = playersRole.subtract(alreadyAddedPlayer.toSet()).toMutableList()
                                positionView.findViewById<ImageView>(R.id.add_best_player)?.setImageResource(R.drawable.adding_best)
                                positionView.findViewById<TextView>(R.id.shirt_number)?.setTextColor(resources.getColor(R.color.adding_player))
                                selectRole.visibility = VISIBLE

                                val role = when {
                                    p.contains("GK") -> "Goalkeeper"
                                    p.contains("D") -> "Defender"
                                    p.contains("M") -> "Midfielder"
                                    else -> "Forward"
                                }
                                selectRole.text = getString(view.resources.getIdentifier("select_${role.lowercase()}", "string", view.resources.getResourcePackageName(R.string.app_name)))

                                listViewRolePlayers?.visibility = VISIBLE
                                val playersAdapter = PlayerBest11Adapter(view.context, playersToAddRole, teamsBitmap, predictBest11Reference.child("Players").child(playerPosition))
                                listViewRolePlayers?.adapter = playersAdapter

                                for (change in allPlayersModule[actualModule]!!.keys) {
                                    if (change != p) {
                                        val changePositionView = allPlayersModule[actualModule]!![change]
                                        changePositionView!!.findViewById<ImageView>(R.id.add_best_player)?.setImageResource(R.drawable.add_best_player)
                                        changePositionView.findViewById<TextView>(R.id.shirt_number)?.setTextColor(resources.getColor(R.color.above_toolbar))
                                    }
                                }

                                predictBest11Reference.child("Players").child(playerPosition).get().addOnCompleteListener { playerFound ->
                                    var selectedPosition = -1
                                    if (playerFound.result.value.toString() != "null") {
                                        val teamPredicted = playerFound.result.child("team").value.toString()
                                        val shirtPredicted = playerFound.result.child("shirt").value.toString().toInt()
                                        selectedPosition = playersToAddRole.indexOfFirst { find -> find.team == teamPredicted && find.shirtNumber == shirtPredicted }
                                    }

                                    listViewRolePlayers.post {
                                        listViewRolePlayers.smoothScrollToPosition(selectedPosition + 1)
                                    }

                                    if (selectedPosition != -1) {
                                        playersAdapter.setSelectedPosition(selectedPosition)
                                        val selectedView = listViewRolePlayers.getChildAt(selectedPosition - listViewRolePlayers.firstVisiblePosition)
                                        selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                    }

                                    listViewRolePlayers.setOnItemClickListener { _, playerView, position, _ ->
                                        playerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                        playersAdapter.setSelectedPosition(position)
                                        positionView.findViewById<ImageView>(R.id.add_best_player)?.visibility = GONE
                                        positionView.findViewById<TextView>(R.id.shirt_number)?.visibility = VISIBLE
                                        positionView.findViewById<ImageView>(R.id.player_team_image)?.visibility = VISIBLE
                                        positionView.findViewById<TextView>(R.id.shirt_number)?.text = playersToAddRole[position].shirtNumber.toString()
                                        positionView.findViewById<ImageView>(R.id.player_team_image)?.setImageBitmap(teamsBitmap[playersToAddRole[position].team])

                                        predictBest11Reference.get().addOnCompleteListener { findCaptain ->
                                            if (findCaptain.result.hasChild("Captain")) {
                                                val findCaptainPosition = findCaptain.result.child("Captain").value.toString()
                                                if (findCaptainPosition == playerPosition) {
                                                    predictBest11Reference.child("Captain").setValue(playerPosition)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            true
                        }

                        MotionEvent.ACTION_CANCEL -> {
                            currentLongPressRunnable?.let { captainHandler.removeCallbacks(it) }
                            true
                        }

                        else -> false
                    }
                }
            }

            view.findViewById<RelativeLayout>(R.id.change_module_rel).setOnClickListener {
                val dialogView = layoutInflater.inflate(R.layout.select_season_dialog, null)
                val modulesListView = dialogView.findViewById<ListView>(R.id.seasons)
                dialogView.findViewById<TextView>(R.id.module_description).visibility = VISIBLE
                modulesListView.adapter =  modulesAdapter
                modulesAdapter.setSelectedPosition(allModules.indexOf(actualModule))

                modulesListView.post {
                    modulesListView.smoothScrollToPosition(allModules.indexOf(actualModule))
                }

                val dialog = AlertDialog.Builder(view.context).setView(dialogView)
                    .setTitle(getString(R.string.select_module))
                    .setPositiveButton(R.string.confirm, null)
                    .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                dialog.show()

                modulesListView.setOnItemClickListener { _, moduleView, module, _ ->
                    moduleSelected = moduleView.findViewById<TextView>(R.id.season_info).text.toString()
                    modulesAdapter.setSelectedPosition(module)
                }

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    for (m in allModules) {
                        if (m != moduleSelected) {
                            allModulesLayout[m]?.visibility = GONE
                        }
                    }
                    allModulesLayout[moduleSelected]?.visibility = VISIBLE
                    val oldModule = actualModule
                    actualModule = moduleSelected
                    val oldModuleAllocations = modulesAllocations[oldModule]!!.replace("-", "")
                    val newModuleAllocations = modulesAllocations[actualModule]!!.replace("-", "")
                    val oldDefendersNumber = oldModuleAllocations.count { c -> c == 'D' }
                    val oldMidfieldersNumber = oldModuleAllocations.count { c -> c == 'M' }
                    val oldForwardsNumber = oldModuleAllocations.count { c -> c == 'F' }
                    val newDefendersNumber = newModuleAllocations.count { c -> c == 'D'}
                    val newMidfieldersNumber = newModuleAllocations.count { c -> c == 'M'}
                    val newForwardsNumber = newModuleAllocations.count { c -> c == 'F'}
                    predictBest11Reference.child("Module").setValue(actualModule).addOnCompleteListener {
                        if (newDefendersNumber < oldDefendersNumber) {
                            for (i in range(newDefendersNumber + 1, oldDefendersNumber + 1)) {
                                for (m in allModules) {
                                    if (allPlayersModule[m]!!.keys.contains("D$i")) {
                                        allPlayersModule[m]!!["D$i"]!!.findViewById<TextView>(R.id.shirt_number)?.visibility = GONE
                                        allPlayersModule[m]!!["D$i"]!!.findViewById<ImageView>(R.id.player_team_image)?.visibility = GONE
                                    }
                                }
                                predictBest11Reference.child("Players").child("Defender$i").removeValue()
                            }
                        }
                        if (newMidfieldersNumber < oldMidfieldersNumber) {
                            for (i in range(newMidfieldersNumber + 1, oldMidfieldersNumber + 1)) {
                                for (m in allModules) {
                                    if (allPlayersModule[m]!!.keys.contains("M$i")) {
                                        allPlayersModule[m]!!["M$i"]!!.findViewById<TextView>(R.id.shirt_number)?.visibility = GONE
                                        allPlayersModule[m]!!["M$i"]!!.findViewById<ImageView>(R.id.player_team_image)?.visibility = GONE
                                    }
                                }
                                predictBest11Reference.child("Players").child("Midfielder$i").removeValue()
                            }
                        }
                        if (newForwardsNumber < oldForwardsNumber) {
                            for (i in range(newForwardsNumber + 1, oldForwardsNumber + 1)) {
                                for (m in allModules) {
                                    if (allPlayersModule[m]!!.keys.contains("F$i")) {
                                        allPlayersModule[m]!!["F$i"]!!.findViewById<TextView>(R.id.shirt_number)?.visibility = GONE
                                        allPlayersModule[m]!!["F$i"]!!.findViewById<ImageView>(R.id.player_team_image)?.visibility = GONE
                                    }
                                }
                                predictBest11Reference.child("Players").child("Forward$i").removeValue()
                            }
                        }
                    }
                    view.findViewById<TextView>(R.id.module).text = actualModule
                    listViewRolePlayers.visibility = GONE
                    selectRole.visibility = GONE

                    for (p in allPlayersModule[actualModule]!!.keys) {
                        val positionView = allPlayersModule[actualModule]!![p]

                        for (reset in allPlayersModule[actualModule]!!.keys) {
                            val resetPositionView = allPlayersModule[actualModule]!![reset]
                            resetPositionView!!.findViewById<ImageView>(R.id.add_best_player)?.setImageResource(R.drawable.add_best_player)
                            resetPositionView.findViewById<TextView>(R.id.shirt_number)?.setTextColor(resources.getColor(R.color.above_toolbar))
                        }

                        val playerPosition: String
                        var playersRole: MutableList<Player>
                        if (p.contains("GK")) {
                            playerPosition = "Goalkeeper"
                            playersRole = allGoalkeepers
                        } else if (p.contains("D")) {
                            playerPosition = "Defender${p.last()}"
                            playersRole = allDefenders
                        } else if (p.contains("M")) {
                            playerPosition = "Midfielder${p.last()}"
                            playersRole = allMidfielders
                        } else {
                            playerPosition = "Forward${p.last()}"
                            playersRole = allForwards
                        }

                        predictBest11Reference.get().addOnCompleteListener { playerPredicted ->
                            if (oldModule != actualModule) {
                                predictBest11Reference.child("Captain").removeValue()
                                for (reset in allPlayersModule[actualModule]!!.keys) {
                                    val resetPositionView = allPlayersModule[actualModule]!![reset]
                                    resetPositionView!!.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = GONE
                                }
                            }
                            if (playerPredicted.result.hasChild("Players")) {
                                if (playerPredicted.result.child("Players").hasChild(playerPosition)) {
                                    val teamPredicted = playerPredicted.result.child("Players").child(playerPosition).child("team").value.toString()
                                    val shirtPredicted = playerPredicted.result.child("Players").child(playerPosition).child("shirt").value.toString().toInt()
                                    positionView!!.findViewById<ImageView>(R.id.add_best_player)?.visibility = GONE
                                    positionView.findViewById<TextView>(R.id.shirt_number)?.visibility = VISIBLE
                                    positionView.findViewById<ImageView>(R.id.player_team_image)?.visibility = VISIBLE
                                    positionView.findViewById<TextView>(R.id.shirt_number)?.text = shirtPredicted.toString()
                                    positionView.findViewById<ImageView>(R.id.player_team_image)?.setImageBitmap(teamsBitmap[teamPredicted])

                                    if (playerPredicted.result.hasChild("Captain")) {
                                        val captainPosition = playerPredicted.result.child("Captain").value.toString()
                                        if (captainPosition == playerPosition) {
                                            positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = VISIBLE
                                        }
                                        else {
                                            positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = GONE
                                        }
                                    }
                                }
                                else {
                                    positionView!!.findViewById<ImageView>(R.id.add_best_player)?.visibility = VISIBLE
                                }
                            }
                            else {
                                positionView!!.findViewById<ImageView>(R.id.add_best_player)?.visibility = VISIBLE
                            }
                        }

                        positionView!!.setOnTouchListener { _, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    currentLongPressRunnable?.let { captainHandler.removeCallbacks(it) }
                                    val runnable = Runnable {
                                        val shirtNumberText = positionView.findViewById<TextView>(R.id.shirt_number)?.text?.toString()
                                        if (!shirtNumberText.isNullOrEmpty()) {
                                            val shirtNum = shirtNumberText.toInt()
                                            val playerSelected = playersRole.find { it.shirtNumber == shirtNum }
                                            if (playerSelected != null) {
                                                predictBest11Reference.child("Players").get().addOnCompleteListener { findPlayersPositionsAdded ->
                                                    if (findPlayersPositionsAdded.result.hasChild(playerPosition)) {
                                                        predictBest11Reference.child("Captain").setValue(playerPosition)
                                                        for (reset in allPlayersModule[actualModule]!!.keys) {
                                                            val resetPositionView = allPlayersModule[actualModule]!![reset]
                                                            resetPositionView!!.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = GONE
                                                        }
                                                        positionView.findViewById<RelativeLayout>(R.id.captain_relative_layout)?.visibility = VISIBLE
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    currentLongPressRunnable = runnable
                                    captainHandler.postDelayed(runnable, 3000)
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    currentLongPressRunnable?.let { captainHandler.removeCallbacks(it) }
                                    predictBest11Reference.child("Players").get().addOnCompleteListener { playersAdded ->
                                        val alreadyAddedPlayer = mutableListOf<Player>()
                                        for (pA in playersAdded.result.children) {
                                            if (pA.key.toString()[0] != 'G' && (!(p[0] == pA.key.toString()[0] && p.last() == pA.key.toString().last()))) {
                                                val addedTeam = pA.child("team").value.toString()
                                                val addedShirt = pA.child("shirt").value.toString()
                                                val findPlayer = inDatabase.result.child("Players").child(season).child(addedTeam).child(addedShirt)
                                                val firstName = findPlayer.child("firstName").value.toString()
                                                val lastName = findPlayer.child("lastName").value.toString()
                                                val role = findPlayer.child("role").value.toString()
                                                val addedPlayer = Player(firstName, lastName, addedShirt.toInt(), role, addedTeam)
                                                alreadyAddedPlayer.add(addedPlayer)
                                            }
                                        }

                                        val playersToAddRole = playersRole.subtract(alreadyAddedPlayer.toSet()).toMutableList()
                                        positionView.findViewById<ImageView>(R.id.add_best_player)?.setImageResource(R.drawable.adding_best)
                                        positionView.findViewById<TextView>(R.id.shirt_number)?.setTextColor(resources.getColor(R.color.adding_player))
                                        selectRole.visibility = VISIBLE

                                        val role = when {
                                            p.contains("GK") -> "Goalkeeper"
                                            p.contains("D") -> "Defender"
                                            p.contains("M") -> "Midfielder"
                                            else -> "Forward"
                                        }
                                        selectRole.text = getString(view.resources.getIdentifier("select_${role.lowercase()}", "string", view.resources.getResourcePackageName(R.string.app_name)))

                                        listViewRolePlayers?.visibility = VISIBLE
                                        val playersAdapter = PlayerBest11Adapter(view.context, playersToAddRole, teamsBitmap, predictBest11Reference.child("Players").child(playerPosition))
                                        listViewRolePlayers?.adapter = playersAdapter

                                        for (change in allPlayersModule[actualModule]!!.keys) {
                                            if (change != p) {
                                                val changePositionView = allPlayersModule[actualModule]!![change]
                                                changePositionView!!.findViewById<ImageView>(R.id.add_best_player)?.setImageResource(R.drawable.add_best_player)
                                                changePositionView.findViewById<TextView>(R.id.shirt_number)?.setTextColor(resources.getColor(R.color.above_toolbar))
                                            }
                                        }

                                        predictBest11Reference.child("Players").child(playerPosition).get().addOnCompleteListener { playerFound ->
                                            var selectedPosition = -1
                                            if (playerFound.result.value.toString() != "null") {
                                                val teamPredicted = playerFound.result.child("team").value.toString()
                                                val shirtPredicted = playerFound.result.child("shirt").value.toString().toInt()
                                                selectedPosition = playersToAddRole.indexOfFirst { find -> find.team == teamPredicted && find.shirtNumber == shirtPredicted }
                                            }

                                            listViewRolePlayers.post {
                                                listViewRolePlayers.smoothScrollToPosition(selectedPosition + 1)
                                            }

                                            if (selectedPosition != -1) {
                                                playersAdapter.setSelectedPosition(selectedPosition)
                                                val selectedView = listViewRolePlayers.getChildAt(selectedPosition - listViewRolePlayers.firstVisiblePosition)
                                                selectedView?.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                            }

                                            listViewRolePlayers.setOnItemClickListener { _, playerView, position, _ ->
                                                playerView.setBackgroundColor(requireContext().getColor(R.color.table_result_values))
                                                playersAdapter.setSelectedPosition(position)
                                                positionView.findViewById<ImageView>(R.id.add_best_player)?.visibility = GONE
                                                positionView.findViewById<TextView>(R.id.shirt_number)?.visibility = VISIBLE
                                                positionView.findViewById<ImageView>(R.id.player_team_image)?.visibility = VISIBLE
                                                positionView.findViewById<TextView>(R.id.shirt_number)?.text = playersToAddRole[position].shirtNumber.toString()
                                                positionView.findViewById<ImageView>(R.id.player_team_image)?.setImageBitmap(teamsBitmap[playersToAddRole[position].team])

                                                predictBest11Reference.get().addOnCompleteListener { findCaptain ->
                                                    if (findCaptain.result.hasChild("Captain")) {
                                                        val findCaptainPosition = findCaptain.result.child("Captain").value.toString()
                                                        if (findCaptainPosition == playerPosition) {
                                                            predictBest11Reference.child("Captain").setValue(playerPosition)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    true
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    currentLongPressRunnable?.let { captainHandler.removeCallbacks(it) }
                                    true
                                }

                                else -> false
                            }
                        }
                    }
                    dialog.dismiss()
                }
            }
            view.findViewById<ProgressBar>(R.id.progress_updating_best11).visibility = INVISIBLE
        }
    }

    private fun findModuleViews(module: String, allModulesInclude: Map<String, View>): Map<String, View> {
        val modulePlayers = mutableMapOf<String, View>()
        when (module) {
            "4-3-3" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_433)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_433)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_433)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_433)
                modulePlayers["D4"] = allModulesInclude[module]!!.findViewById(R.id.defender4_433)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_433)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_433)
                modulePlayers["M3"] = allModulesInclude[module]!!.findViewById(R.id.midfielder3_433)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_433)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_433)
                modulePlayers["F3"] = allModulesInclude[module]!!.findViewById(R.id.forward3_433)
            }
            "4-4-2" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_442)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_442)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_442)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_442)
                modulePlayers["D4"] = allModulesInclude[module]!!.findViewById(R.id.defender4_442)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_442)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_442)
                modulePlayers["M3"] = allModulesInclude[module]!!.findViewById(R.id.midfielder3_442)
                modulePlayers["M4"] = allModulesInclude[module]!!.findViewById(R.id.midfielder4_442)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_442)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_442)
            }
            "4-2-3-1" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_4231)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_4231)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_4231)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_4231)
                modulePlayers["D4"] = allModulesInclude[module]!!.findViewById(R.id.defender4_4231)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_4231)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_4231)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_4231)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_4231)
                modulePlayers["F3"] = allModulesInclude[module]!!.findViewById(R.id.forward3_4231)
                modulePlayers["F4"] = allModulesInclude[module]!!.findViewById(R.id.forward4_4231)
            }
            "3-5-2" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_352)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_352)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_352)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_352)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_352)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_352)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_352)
                modulePlayers["M3"] = allModulesInclude[module]!!.findViewById(R.id.midfielder3_352)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_352)
                modulePlayers["F3"] = allModulesInclude[module]!!.findViewById(R.id.forward3_352)
                modulePlayers["F4"] = allModulesInclude[module]!!.findViewById(R.id.forward4_352)
            }
            "3-4-3" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_343)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_343)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_343)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_343)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_343)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_343)
                modulePlayers["M3"] = allModulesInclude[module]!!.findViewById(R.id.midfielder3_343)
                modulePlayers["M4"] = allModulesInclude[module]!!.findViewById(R.id.midfielder4_343)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_343)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_343)
                modulePlayers["F3"] = allModulesInclude[module]!!.findViewById(R.id.forward3_343)
            }
            "3-4-1-2" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_3412)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_3412)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_3412)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_3412)
                modulePlayers["D4"] = allModulesInclude[module]!!.findViewById(R.id.defender4_3412)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_3412)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_3412)
                modulePlayers["D5"] = allModulesInclude[module]!!.findViewById(R.id.defender5_3412)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_3412)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_3412)
                modulePlayers["F3"] = allModulesInclude[module]!!.findViewById(R.id.forward3_3412)
            }
            "4-3-1-2" -> {
                modulePlayers["GK"] = allModulesInclude[module]!!.findViewById(R.id.goalkeeper_4312)
                modulePlayers["D1"] = allModulesInclude[module]!!.findViewById(R.id.defender1_4312)
                modulePlayers["D2"] = allModulesInclude[module]!!.findViewById(R.id.defender2_4312)
                modulePlayers["D3"] = allModulesInclude[module]!!.findViewById(R.id.defender3_4312)
                modulePlayers["D4"] = allModulesInclude[module]!!.findViewById(R.id.defender4_4312)
                modulePlayers["M1"] = allModulesInclude[module]!!.findViewById(R.id.midfielder1_4312)
                modulePlayers["M2"] = allModulesInclude[module]!!.findViewById(R.id.midfielder2_4312)
                modulePlayers["M3"] = allModulesInclude[module]!!.findViewById(R.id.midfielder3_4312)
                modulePlayers["F1"] = allModulesInclude[module]!!.findViewById(R.id.forward1_4312)
                modulePlayers["F2"] = allModulesInclude[module]!!.findViewById(R.id.forward2_4312)
                modulePlayers["F3"] = allModulesInclude[module]!!.findViewById(R.id.forward3_4312)
            }
            else -> {
                return emptyMap()
            }
        }
        return modulePlayers
    }
}