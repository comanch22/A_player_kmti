package comanch.simpleplayer

import android.content.Context
import android.media.SoundPool
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.HashMap
import javax.inject.Inject

class SoundPoolForFragments @Inject constructor(
    @ApplicationContext context: Context,
    val soundPool: SoundPool
) {
    val soundStart = soundPool.load(context, R.raw.navigation_forward_selection, 1)
    val soundButtonTap = soundPool.load(context, R.raw.navigation_forward_selection_minimal, 1)

    private val appContext = context

    val soundMap: HashMap<Int, Int> by lazy { hashMapOf() }

    private var isTouchSoundsEnabledSystem = Settings.System.getInt(
        appContext.contentResolver,
        Settings.System.SOUND_EFFECTS_ENABLED, 1
    ) != 0

    private fun playSound(id: Int) {
        soundPool.play(id, 1F, 1F, 1, 0, 1F)
    }

    fun setTouchSound() {

        isTouchSoundsEnabledSystem = Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.SOUND_EFFECTS_ENABLED, 1
        ) != 0
    }

    private fun isTouchSoundEnable(soundId: Int?): Boolean {

        return soundMap[soundId] == 0
                && isTouchSoundsEnabledSystem
    }

    fun playSoundIfEnable(soundInt: Int) {
        if (isTouchSoundEnable(soundInt)) {
            playSound(soundInt)
        }
    }
}