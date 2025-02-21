package com.embeddedproject.calciofemminileitaliano.adapters

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.embeddedproject.calciofemminileitaliano.R
import com.embeddedproject.calciofemminileitaliano.SelectChampionshipDirections
import com.embeddedproject.calciofemminileitaliano.helpers.SpecialEvent
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import java.util.Calendar

class SpecialEventsAdapter(private val specialEvents: List<SpecialEvent>, private val user: String, private val team: String? = null) :RecyclerView.Adapter<SpecialEventsAdapter.SpecialEventsViewHolder>() {

    class SpecialEventsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val englishDaysWeek = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        private val englishMonths = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        private val infoTextView: TextView = itemView.findViewById(R.id.info)
        private val eventImageView: ImageView = itemView.findViewById(R.id.event_image)
        private val dateTextView: TextView = itemView.findViewById(R.id.date)
        private val timeTextView: TextView = itemView.findViewById(R.id.time)
        private val team1ImageView: ImageView = itemView.findViewById(R.id.team1_image)
        private val team2ImageView: ImageView = itemView.findViewById(R.id.team2_image)
        private val joinEventButton: Button = itemView.findViewById(R.id.join_special_event)


        fun bind(specialEvent: SpecialEvent, user: String, team: String?) {
            val sqlDB = UserLoggedInHelper(itemView.context)
            val dbReference = sqlDB.writableDatabase

            val eventName = specialEvent.eventName
            val season = specialEvent.season

            val info = "$eventName $season"
            infoTextView.text = info
            dateTextView.text = translateDate(specialEvent.date)
            timeTextView.text = specialEvent.time

            val team1 = specialEvent.team1
            val team2 = specialEvent.team2

            val setHomeTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team1))
            if (setHomeTeamImage.moveToFirst()) {
                team1ImageView.setImageBitmap(BitmapFactory.decodeByteArray(setHomeTeamImage.getBlob(0), 0, setHomeTeamImage.getBlob(0).size))
            }
            setHomeTeamImage.close()
            val setGuestTeamImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(team2))
            if (setGuestTeamImage.moveToFirst()) {
                team2ImageView.setImageBitmap(BitmapFactory.decodeByteArray(setGuestTeamImage.getBlob(0), 0, setGuestTeamImage.getBlob(0).size))
            }
            setGuestTeamImage.close()

            val setEventImage = dbReference.rawQuery("SELECT ImageBitmap FROM TEAM_IMAGE WHERE TeamName = ?", arrayOf(eventName))
            if (setEventImage.moveToFirst()) {
                eventImageView.setImageBitmap(BitmapFactory.decodeByteArray(setEventImage.getBlob(0), 0, setEventImage.getBlob(0).size))
            }
            setEventImage.close()

            joinEventButton.setOnClickListener {
                if (team != null) {
                    val navigateToSpecialEvent = SelectChampionshipDirections.actionSelectChampionshipToSpecialEvent(user, team, eventName, season, specialEvent.id)
                    itemView.findNavController().navigate(navigateToSpecialEvent)
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
            weekDay = itemView.resources.getString(itemView.resources.getIdentifier(weekDay, "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            var monthName = englishMonths[month - 1]
            monthName = monthName.lowercase()
            monthName = itemView.resources.getString(itemView.resources.getIdentifier(monthName, "string", itemView.resources.getResourcePackageName(R.string.app_name)))
            return "$weekDay $day $monthName $year"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpecialEventsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.special_event, parent, false)
        return SpecialEventsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return specialEvents.size
    }

    override fun onBindViewHolder(holder: SpecialEventsViewHolder, position: Int) {
        holder.bind(specialEvents[position], user, team)
    }
}