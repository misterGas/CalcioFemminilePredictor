package com.embeddedproject.calciofemminileitaliano.helpers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.embeddedproject.calciofemminileitaliano.MainActivity
import com.embeddedproject.calciofemminileitaliano.R

class NotificationReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val info = intent.getStringExtra("info")

        val sharedPrefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        val isNotificationSent = sharedPrefs.getBoolean("notification_$id", false)

        if (isNotificationSent) {
            return
        }

        val splitInfo = info!!.split("//")
        val championship = splitInfo[0]
        val round = splitInfo[1].toInt()
        val teams = splitInfo[2]

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("championship", championship)
            putExtra("round", round)
            putExtra("teams", teams)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, "matchChannel")
            .setSmallIcon(R.drawable.ball_notification)
            .setContentTitle("$teams ${context.resources.getString(R.string.match_about_to_start)}")
            .setContentText(context.resources.getString(R.string.assign_scorers_notification))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(id, notificationBuilder.build())

        with(sharedPrefs.edit()) {
            putBoolean("notification_$id", true)
            apply()
        }
    }
}