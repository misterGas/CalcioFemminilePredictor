package com.embeddedproject.calciofemminileitaliano

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.SCHEDULE_EXACT_ALARM
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.findNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import okhttp3.OkHttpClient

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val updateRequestCode = 123
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_CalcioFemminileItaliano)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        networkMonitor = NetworkMonitor(this,
            onNetworkLost = {
                runOnUiThread {
                   findNavController(R.id.fragment_container).navigate(R.id.opening)
                }
            })

        checkNotificationPermission()
        createNotificationChannel(this)

        val app = application as MainApplication
        app.hearRulesTextToSpeech = TextToSpeech(this) { _: Int -> }
        app.client = OkHttpClient.Builder().build()

        checkForAppUpdate()
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

    private fun checkForAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    updateRequestCode
                )
            }
        }
    }

    @Deprecated("DEPRECATED")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == updateRequestCode && resultCode != RESULT_OK) {
            Toast.makeText(this, R.string.app_update_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        networkMonitor.startMonitoring()
    }

    override fun onStop() {
        super.onStop()
        networkMonitor.stopMonitoring()
    }
}