package comanch.simpleplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import comanch.simpleplayer.helpers.NavigationCorrespondent
import comanch.simpleplayer.helpers.StringKey
import comanch.simpleplayer.listFragment.ListFragmentDirections
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.preferences.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val channelId = "playerKTMi channel id"
    private val channelName = "channel playerKTMi"
    private val notificationId = 17131415

    @Inject
    lateinit var preferences: DefaultPreference

    override fun onCreate(savedInstanceState: Bundle?) {

        val defaultPreference = PreferenceManager.getDefaultSharedPreferences(this)
        when (defaultPreference.getString(PreferenceKeys.appStyle, "main")) {
            "main" -> setTheme(R.style.Theme_SimplePlayer)
            "light" -> setTheme(R.style.Theme_SimplePlayerLight)
            else -> setTheme(R.style.Theme_SimplePlayer)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        intent?.extras?.let {
            if (it.containsKey(StringKey.openPlayList)) {
                intent.removeExtra(StringKey.openPlayList)
                findNavController(R.id.nav_host_fragment).navigate(
                    ListFragmentDirections.actionListFragmentToPlayFragment(
                        NavigationCorrespondent.ListFragment
                    )
                )
            }
        }
        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.imageFragment) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val windowInsetsController =
            window?.let {
                window?.decorView?.let { view ->
                    WindowCompat.getInsetsController(
                        it,
                        view
                    )
                }
            } ?: return
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }

    override fun onBackPressed() {
        if (onBackPressedDispatcher.hasEnabledCallbacks()) {
            super.onBackPressed()
        } else {
            finishAfterTransition()
        }
    }
}