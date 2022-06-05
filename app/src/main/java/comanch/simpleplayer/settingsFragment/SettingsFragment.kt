package comanch.simpleplayer.settingsFragment

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import comanch.simpleplayer.R
import androidx.preference.*
import comanch.simpleplayer.DefaultPreference
import comanch.simpleplayer.NavigationBetweenFragments
import comanch.simpleplayer.PreferenceKeys
import comanch.simpleplayer.SoundPoolForFragments
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@SettingsFragment,
                SettingsFragmentDirections.actionSettingsFragmentToListFragment()
            )
        }
        callback.isEnabled = true

        val backButton = findPreference<Preference>(PreferenceKeys.backButton)
        backButton?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                navigation.navigateToDestination(
                    this,
                    SettingsFragmentDirections.actionSettingsFragmentToListFragment())
                true
            }
        }

        val isAutoScroll = findPreference<Preference>(PreferenceKeys.isAutoScroll)
        isAutoScroll?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                preferences.putBoolean(PreferenceKeys.isAutoScroll, newValue as Boolean)
                true
            }

        isAutoScroll?.let {
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
                true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val colorBackground = TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryVariant, colorBackground, true)
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.setBackgroundColor(colorBackground.data)
        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        val backButton = Preference(context)
        backButton.icon = ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_baseline_detail_arrow_left_36,
            context.theme
        )
        backButton.key = PreferenceKeys.backButton
        backButton.title = resources.getString(R.string.settings_title)
       // backButton.summary = resources.getString(R.string.back_button)
        backButton.layoutResource = R.layout.preference_custom_layout

        val isAutoScroll = SwitchPreference(context)
        isAutoScroll.key = PreferenceKeys.isAutoScroll
        isAutoScroll.title = resources.getString(R.string.auto_scrolling)
        isAutoScroll.layoutResource = R.layout.switch_custom

        screen.addPreference(backButton)
        screen.addPreference(isAutoScroll)

        preferenceScreen = screen
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }

}

