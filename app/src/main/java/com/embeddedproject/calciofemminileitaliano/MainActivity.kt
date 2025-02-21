package com.embeddedproject.calciofemminileitaliano

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.SCHEDULE_EXACT_ALARM
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkNotificationPermission()
        createNotificationChannel(this)

        //deleteDatabase("UserAndTeamsImages")

        val app = application as MainApplication
        app.hearRulesTextToSpeech = TextToSpeech(this) { _: Int -> }
        app.client = OkHttpClient.Builder().build()
    }

    private fun checkNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
        if (ActivityCompat.checkSelfPermission(this, SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(SCHEDULE_EXACT_ALARM)
        }
    }

    private fun createNotificationChannel(context: Context) {
        val name = "Match Notifications"
        val descriptionText = "Channel for match notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("matchChannel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}