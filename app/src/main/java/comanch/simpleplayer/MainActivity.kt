package comanch.simpleplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.findNavController
import comanch.simpleplayer.listFragment.ListFragmentDirections
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val channelId = "myAlarm_Channel_Id"
    private val channelName = "myAlarm_Channel_Name"
    private val notificationId = 17131415

    override fun onCreate(savedInstanceState: Bundle?) {

        setTheme(R.style.Theme_SimplePlayer)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(this)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "MainActivityNotificationChannel$notificationId"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
        intent?.extras?.let {
            if (it.containsKey("openPlayList")) {
                intent.removeExtra("openPlayList")
                findNavController(R.id.nav_host_fragment).navigate(
                    ListFragmentDirections.actionListFragmentToPlayFragment()
                )
            }
        }
    }
}