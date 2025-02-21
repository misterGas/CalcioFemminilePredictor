package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.embeddedproject.calciofemminileitaliano.helpers.MatchNotification
import com.embeddedproject.calciofemminileitaliano.helpers.NotificationReceiver
import com.embeddedproject.calciofemminileitaliano.helpers.UserLoggedInHelper
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Opening : Fragment() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_opening, container, false)
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val letsStartButton = view.findViewById<Button>(R.id.lets_start)
        var connectivityManager = view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var network = connectivityManager.activeNetwork
        var activeNetwork = connectivityManager.getNetworkCapabilities(network)
        if (!(activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)) {
            Toast.makeText(view.context, getString(R.string.no_connection), Toast.LENGTH_LONG).show()
            letsStartButton.text = getString(R.string.retry)
            letsStartButton.visibility = VISIBLE
            view.findViewById<ProgressBar>(R.id.progress_updating)?.visibility = INVISIBLE
        }
        else {
            letsStartButton.visibility = VISIBLE
            view.findViewById<ProgressBar>(R.id.progress_updating)?.visibility = INVISIBLE
        }

        letsStartButton.setOnClickListener {
            val sqlDB = UserLoggedInHelper(view.context)
            val dbReference = sqlDB.writableDatabase
            if (letsStartButton.text == getString(R.string.retry)) {
                connectivityManager = view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                network = connectivityManager.activeNetwork
                activeNetwork = connectivityManager.getNetworkCapabilities(network)
                if (!(activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)) {
                    Toast.makeText(view.context, getString(R.string.no_connection), Toast.LENGTH_LONG).show()
                    letsStartButton.text = getString(R.string.retry)
                }
                else {
                    Toast.makeText(view.context, getString(R.string.connection_activated), Toast.LENGTH_LONG).show()
                    letsStartButton.text = getString(R.string.lets_start)
                }
            }
            else {
                if (view.findViewById<ProgressBar>(R.id.progress_updating)?.visibility == INVISIBLE) {
                    activity?.runOnUiThread {
                        val findLoggedInUser = dbReference.rawQuery("SELECT * FROM USER", null)
                        val userLoggedIn = findLoggedInUser.count
                        if (userLoggedIn == 1) {
                            if (findLoggedInUser.moveToFirst()) {
                                val navigateToSelectChampionship = OpeningDirections.actionOpeningToSelectChampionship(findLoggedInUser.getString(0))
                                view.findNavController().navigate(navigateToSelectChampionship)
                            }
                        }
                        else {
                            val navigateToRegistration = OpeningDirections.actionOpeningToLoginRegistration()
                            view.findNavController().navigate(navigateToRegistration)
                        }
                        findLoggedInUser.close()
                    }
                }
            }
        }

        val db = FirebaseDatabase.getInstance()
        val reference = db.reference

        reference.child("Championships").get().addOnCompleteListener { championships ->
            val matchesNotification = mutableListOf<MatchNotification>()
            for (c in championships.result.children) {
                for (s in c.children) {
                    for (r in s.child("Matches").children) {
                        for (m in r.child("Matches").children) {
                            val matchInfo = m.child("MatchInfo")
                            val date = matchInfo.child("date").value.toString()
                            val time = matchInfo.child("time").value.toString()
                            val homeTeam = matchInfo.child("homeTeam").value.toString()
                            val guestTeam = matchInfo.child("guestTeam").value.toString()
                            val finished = m.hasChild("Finished")
                            if (time != "To be defined" && !finished) {
                                matchesNotification.add(MatchNotification(date, time, homeTeam, guestTeam, r.key.toString().toInt(), c.key.toString()))
                            }
                        }
                    }
                }
            }

            val alarmManager = view.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for ((i, m) in matchesNotification.withIndex()) {
                var date = m.date
                var hourTime = m.time.split(":")[0].toInt() - 6
                val minutesTime = m.time.split(":")[1]

                if (hourTime < 0) {
                    hourTime += 24
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                    val dateFormatter = LocalDate.parse(date, formatter)

                    date = dateFormatter.minusDays(1).format(formatter)
                }

                var stringHour = hourTime.toString()

                if (hourTime < 10) {
                    stringHour = "0$hourTime"
                }
                val predictionStart = "$date $stringHour:$minutesTime"

                val longPredictions = LocalDateTime.parse(predictionStart, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val info = "${m.championship}//${m.round}//${m.homeTeam}-${m.guestTeam}"

                val intent = Intent(view.context, NotificationReceiver::class.java).apply {
                    putExtra("info", info)
                    putExtra("id", i)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    view.context,
                    i,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, longPredictions, pendingIntent)
            }
        }
    }
}