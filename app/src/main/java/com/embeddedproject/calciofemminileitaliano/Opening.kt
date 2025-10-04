package com.embeddedproject.calciofemminileitaliano

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.navOptions
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

    private val navigateRunnableToLoginRegistration = Runnable {
        view?.findNavController()?.navigate(
            R.id.action_opening_to_loginRegistration,
            null,
            navOptions {
                anim {
                    enter = R.anim.fade_in
                    exit = R.anim.fade_out
                    popEnter = R.anim.fade_in
                    popExit = R.anim.fade_out
                }
            }
        )
    }

    private var user: String? = null

    private val navigateRunnableToSelectChampionship = Runnable {
        val args = Bundle().apply {
            putString("user_nickname", user)
        }
        view?.findNavController()?.navigate(
            R.id.action_opening_to_selectChampionship,
            args,
            navOptions {
                anim {
                    enter = R.anim.fade_in
                    exit = R.anim.fade_out
                    popEnter = R.anim.fade_in
                    popExit = R.anim.fade_out
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Handler(Looper.getMainLooper()).removeCallbacks(navigateRunnableToLoginRegistration)
        Handler(Looper.getMainLooper()).removeCallbacks(navigateRunnableToSelectChampionship)
    }

    @SuppressLint("ScheduleExactAlarm")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sqlDB = UserLoggedInHelper(view.context)
        val dbReference = sqlDB.writableDatabase

        activity?.window?.statusBarColor = ContextCompat.getColor(requireContext(), R.color.background_icon)
        activity?.window?.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.background_icon)

        val retryConnectionTextView = view.findViewById<TextView>(R.id.retry_connection)

        var connectivityManager = view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var network = connectivityManager.activeNetwork
        var activeNetwork = connectivityManager.getNetworkCapabilities(network)
        if (!(activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)) {
            Toast.makeText(view.context, getString(R.string.no_connection), Toast.LENGTH_LONG).show()
            retryConnectionTextView.visibility = VISIBLE
        }
        else {
            activity?.runOnUiThread {
                val findLoggedInUser = dbReference.rawQuery("SELECT * FROM USER", null)
                val userLoggedIn = findLoggedInUser.count
                if (userLoggedIn == 1) {
                    if (findLoggedInUser.moveToFirst()) {
                        user = findLoggedInUser.getString(0)
                        Handler(Looper.getMainLooper()).postDelayed(navigateRunnableToSelectChampionship, 1500)
                    }
                }
                else {
                    Handler(Looper.getMainLooper()).postDelayed(navigateRunnableToLoginRegistration, 1500)
                }
                findLoggedInUser.close()
            }
        }

        retryConnectionTextView.setOnClickListener {
            connectivityManager = view.context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            network = connectivityManager.activeNetwork
            activeNetwork = connectivityManager.getNetworkCapabilities(network)
            if (!(activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true || activeNetwork?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)) {
                Toast.makeText(view.context, getString(R.string.no_connection), Toast.LENGTH_LONG).show()
            }
            else {
                retryConnectionTextView.visibility = INVISIBLE
                activity?.runOnUiThread {
                    val findLoggedInUser = dbReference.rawQuery("SELECT * FROM USER", null)
                    val userLoggedIn = findLoggedInUser.count
                    if (userLoggedIn == 1) {
                        if (findLoggedInUser.moveToFirst()) {
                            user = findLoggedInUser.getString(0)
                            Handler(Looper.getMainLooper()).postDelayed(navigateRunnableToSelectChampionship, 1500)
                        }
                    }
                    else {
                        Handler(Looper.getMainLooper()).postDelayed(navigateRunnableToLoginRegistration, 1500)
                    }
                    findLoggedInUser.close()
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