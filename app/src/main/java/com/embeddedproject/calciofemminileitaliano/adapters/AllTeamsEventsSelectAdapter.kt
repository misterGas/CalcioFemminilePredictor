package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.helpers.TeamEventInfo
import com.google.firebase.database.DatabaseReference
import java.security.NoSuchAlgorithmException
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AllTeamsEventsSelectAdapter(private val userToAdd: String, private val teams: MutableList<TeamEventInfo>, private val teamsCaptains: Map<String, String>, private val teamsComponents: Map<String, MutableList<String>>, private val dialog: AlertDialog, private val view: View, private val teamsReference: DatabaseReference) : RecyclerView.Adapter<AllTeamsEventsSelectAdapter.AllTeamsEventsSelectViewHolder>() {

    inner class AllTeamsEventsSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val teamNameTextView = itemView.findViewById<TextView>(R.id.team_name)
        private val privateTeamImageView = itemView.findViewById<ImageView>(R.id.private_team)
        private val componentsNumberTextView = itemView.findViewById<TextView>(R.id.components_number)
        private val teamComponentsRecyclerView = itemView.findViewById<RecyclerView>(R.id.team_components_recycler_view)
        private val teamPasswordEditText = itemView.findViewById<EditText>(R.id.write_team_password)
        private val joinTeamButton = itemView.findViewById<Button>(R.id.join_team)
        private val showTeamInfoRelativeLayout = itemView.findViewById<RelativeLayout>(R.id.show_team_info)

        fun bind(userToAdd: String, team: TeamEventInfo, captain: String, components: MutableList<String>, dialog: AlertDialog, view: View, teamsReference: DatabaseReference) {
            teamNameTextView.text = team.getTeamName()

            if (team.getIsPrivate()) {
                privateTeamImageView.visibility = VISIBLE
            }
            else {
                privateTeamImageView.visibility = GONE
            }

            val componentsNumber = "${components.size}/11"
            componentsNumberTextView.text = componentsNumber

            val openInfo = itemView.findViewById<RelativeLayout>(R.id.open)
            val showJoinTeamRelativeLayout = view.findViewById<RelativeLayout>(R.id.show_team_join)
            val showJoinedTeamRelativeLayout = view.findViewById<RelativeLayout>(R.id.show_team_joined)

            openInfo.setOnClickListener {
                val openTeamInfoImageView = itemView.findViewById<ImageView>(R.id.open_team_info)
                if (teamComponentsRecyclerView.visibility == GONE) {
                    openTeamInfoImageView.setImageResource(R.drawable.arrow_down)
                    teamComponentsRecyclerView.visibility = VISIBLE
                    showTeamInfoRelativeLayout.visibility = VISIBLE
                    if (team.getIsPrivate()) {
                        teamPasswordEditText.visibility = VISIBLE
                    }
                    else {
                        teamPasswordEditText.visibility = GONE
                    }
                    val componentsAdapter = TeamsEventsComponentsAdapter(captain, components)
                    teamComponentsRecyclerView.adapter = componentsAdapter
                }
                else {
                    openTeamInfoImageView.setImageResource(R.drawable.arrow_right)
                    teamComponentsRecyclerView.visibility = GONE
                    showTeamInfoRelativeLayout.visibility = GONE
                }
            }

            joinTeamButton.setOnClickListener {
                if (team.getIsPrivate()) {
                    val enteredPassword = teamPasswordEditText.text.toString()
                    if (enteredPassword.isNotEmpty()) {
                        val hashOfInput: String = hashPassword(enteredPassword, team.getPasswordSalt()!!)
                        if (hashOfInput == team.getPassword()!!) {
                            dialog.dismiss()
                            showJoinTeamRelativeLayout.visibility = GONE
                            showJoinedTeamRelativeLayout.visibility = VISIBLE
                            view.findViewById<TextView>(R.id.team_joined_name).text = team.getTeamName()
                            teamsReference.child(team.getTeamName()).child("Components").get().addOnCompleteListener { findCompleteTeam ->
                                if (findCompleteTeam.result.childrenCount.toInt() + 1 != 11) {
                                    teamsReference.child(team.getTeamName()).child("Components").child(userToAdd).setValue("Added").addOnCompleteListener {
                                        Toast.makeText(view.context, R.string.team_joined_correctly, Toast.LENGTH_LONG).show()
                                        components.add(userToAdd)
                                        val teamComponentsRecyclerView = view.findViewById<RecyclerView>(R.id.team_components_recycler_view)
                                        val allComponents = components.sorted().toMutableList()
                                        allComponents.remove(captain)
                                        allComponents.add(0, captain)
                                        val teamComponentsAdapter = TeamsEventsComponentsAdapter(captain, allComponents, userToAdd)
                                        teamComponentsRecyclerView.adapter = teamComponentsAdapter
                                    }
                                }
                                else {
                                    Toast.makeText(view.context, R.string.full_team, Toast.LENGTH_LONG).show()
                                    showJoinTeamRelativeLayout.visibility = VISIBLE
                                    showJoinedTeamRelativeLayout.visibility = GONE
                                    teams.remove(team)
                                    dialog.dismiss()
                                }
                            }
                        }
                        else {
                            Toast.makeText(view.context, R.string.incorrect_password, Toast.LENGTH_LONG).show()
                        }
                    }
                    else {
                        Toast.makeText(view.context, R.string.team_password_required, Toast.LENGTH_LONG).show()
                    }
                }
                else {
                    dialog.dismiss()
                    showJoinTeamRelativeLayout.visibility = GONE
                    showJoinedTeamRelativeLayout.visibility = VISIBLE
                    view.findViewById<TextView>(R.id.team_joined_name).text = team.getTeamName()
                    teamsReference.child(team.getTeamName()).child("Components").get().addOnCompleteListener { findCompleteTeam ->
                        if (findCompleteTeam.result.childrenCount.toInt() + 1 != 11) {
                            teamsReference.child(team.getTeamName()).child("Components").child(userToAdd).setValue("Added").addOnCompleteListener {
                                Toast.makeText(view.context, R.string.team_joined_correctly, Toast.LENGTH_LONG).show()
                                components.add(userToAdd)
                                val teamComponentsRecyclerView = view.findViewById<RecyclerView>(R.id.team_components_recycler_view)
                                val allComponents = components.sorted().toMutableList()
                                allComponents.remove(captain)
                                allComponents.add(0, captain)
                                val teamComponentsAdapter = TeamsEventsComponentsAdapter(captain, allComponents, userToAdd)
                                teamComponentsRecyclerView.adapter = teamComponentsAdapter
                            }
                        }
                        else {
                            Toast.makeText(view.context, R.string.full_team, Toast.LENGTH_LONG).show()
                            showJoinTeamRelativeLayout.visibility = VISIBLE
                            showJoinedTeamRelativeLayout.visibility = GONE
                            teams.remove(team)
                            dialog.dismiss()
                        }
                    }
                }
            }
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllTeamsEventsSelectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.team_components, parent, false)
        return AllTeamsEventsSelectViewHolder(view)
    }

    override fun getItemCount(): Int {
        return teams.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: AllTeamsEventsSelectViewHolder, position: Int) {
        val team = teams[position]
        holder.bind(userToAdd, team, teamsCaptains[team.getTeamName()]!!, teamsComponents[team.getTeamName()]!!, dialog, view, teamsReference)
    }
}