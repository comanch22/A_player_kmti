package comanch.simpleplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager


class MusicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action ==
            AudioManager.ACTION_AUDIO_BECOMING_NOISY
        ) {
            val mIntent = Intent(context, ServicePlay::class.java).apply {
                action = StringKey.playerPause
            }
            context?.startService(mIntent)
        }
    }
}

