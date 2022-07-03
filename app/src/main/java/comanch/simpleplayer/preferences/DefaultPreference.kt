package comanch.simpleplayer.preferences

import android.content.SharedPreferences
import javax.inject.Inject

class DefaultPreference @Inject constructor(private val preference: SharedPreferences) {

    fun getString(key: String): String {

        val defaultValue = ""
        return preference.getString(key, defaultValue)!!
    }

    fun getBoolean(key: String): Boolean {

        val defaultValue =
            when(key){
                PreferenceKeys.isAutoScroll -> true
                PreferenceKeys.screenSaverIsOn -> true
            else -> {
                null
            }
        }

        return preference.getBoolean(key, defaultValue!!)
    }

    fun putBoolean(key: String, value: Boolean) {

        with(preference.edit()) {
            putBoolean(key, value)
            apply()
        }
    }

    fun putString(key: String, value: String){

        with(preference.edit()){
            putString(key, value)
            apply()
        }
    }
}
