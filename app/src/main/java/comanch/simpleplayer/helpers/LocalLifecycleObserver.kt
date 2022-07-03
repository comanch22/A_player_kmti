package comanch.simpleplayer.helpers

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.preferences.PreferenceKeys
import javax.inject.Inject

class LocalLifecycleObserver @Inject constructor(
    private val registry: ActivityResultRegistry,
    private val preferences: DefaultPreference
) : DefaultLifecycleObserver {

    lateinit var getContent: ActivityResultLauncher<String>
    private var prefKey = ""

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        getContent = registry.register(
            "1514233445889",
            owner,
            ActivityResultContracts.GetContent()
        ) { uri ->
            when (prefKey) {
                PreferenceKeys.previewUri -> {
                    preferences.putString(PreferenceKeys.previewUri, if(uri == null) "" else "$uri")
                }
                PreferenceKeys.playScreenUri -> {
                    preferences.putString(PreferenceKeys.playScreenUri, if(uri == null) "" else "$uri")
                }
            }

        }
    }

    fun selectImage(_prefKey: String) {
        prefKey = _prefKey
        getContent.launch("image/*")
    }
}