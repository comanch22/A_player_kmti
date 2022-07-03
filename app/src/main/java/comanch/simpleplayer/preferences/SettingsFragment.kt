package comanch.simpleplayer.preferences

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.ListPreference
import androidx.preference.SwitchPreference
import androidx.preference.PreferenceCategory
import comanch.simpleplayer.R
import comanch.simpleplayer.helpers.CompositeJob
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var preferences: DefaultPreference

    private val job: CompositeJob = CompositeJob()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@SettingsFragment,
                SettingsFragmentDirections.actionSettingsFragmentToListFragment()
            )
        }
        callback.isEnabled = true

        val isAutoScroll = findPreference<Preference>(PreferenceKeys.isAutoScroll)
        isAutoScroll?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                preferences.putBoolean(PreferenceKeys.isAutoScroll, newValue as Boolean)
                true
            }

        val screenSaverIsOn = findPreference<Preference>(PreferenceKeys.screenSaverIsOn)
        screenSaverIsOn?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                preferences.putBoolean(PreferenceKeys.screenSaverIsOn, newValue as Boolean)
                true
            }

        val screensaverDuration = findPreference<Preference>(PreferenceKeys.screensaverDuration)
        screensaverDuration?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference,
                                                    newValue ->
                preference.summary = newValue.toString()
                true
            }

        val appStyle = findPreference<Preference>(PreferenceKeys.appStyle)
        appStyle?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference,
                                                    newValue ->
                preference.summary = newValue.toString()
                setStyle()
                true
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val colorBackground = TypedValue()
        requireContext().theme.resolveAttribute(
            com.google.android.material.R.attr.colorOnPrimary,
            colorBackground,
            true
        )
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.setBackgroundColor(colorBackground.data)

        return view
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.statusBarColor =
            resources.getColor(R.color.background2, activity?.theme)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        val margin = Preference(context)
        margin.key = PreferenceKeys.margin
        margin.title = ""
        margin.layoutResource = R.layout.preference_custom_layout_margin

        val backButton = Preference(context)
        backButton.icon = ResourcesCompat.getDrawable(
            resources,
            R.drawable.ic_baseline_detail_arrow_left_36,
            context.theme
        )
        backButton.key = PreferenceKeys.backButton
        backButton.title = resources.getString(R.string.settings_title)
        backButton.layoutResource = R.layout.preference_custom_layout_back_button

        val appStyle = ListPreference(context)
        appStyle.key = PreferenceKeys.appStyle
        appStyle.title = resources.getString(R.string.appStyle)
        appStyle.summary = preferences.getString(PreferenceKeys.appStyle)
        appStyle.dialogTitle = resources.getString(R.string.setAppStyle)
        appStyle.layoutResource = R.layout.preference_custom_layout
        appStyle.setEntries(R.array.app_style)
        appStyle.setEntryValues(R.array.app_style)
        appStyle.setDefaultValue("main")

        val isAutoScroll = SwitchPreference(context)
        isAutoScroll.key = PreferenceKeys.isAutoScroll
        isAutoScroll.title = resources.getString(R.string.auto_scrolling)
        isAutoScroll.summary = resources.getString(R.string.auto_scrollinSummary)
        isAutoScroll.layoutResource = R.layout.switch_custom
        isAutoScroll.setDefaultValue(true)

        screen.addPreference(margin)
        screen.addPreference(backButton)
        screen.addPreference(appStyle)
        screen.addPreference(isAutoScroll)

        val screenSaverIsOn = SwitchPreference(context)
        screenSaverIsOn.key = PreferenceKeys.screenSaverIsOn
        screenSaverIsOn.title = resources.getString(R.string.screenSaverIsOn)
        screenSaverIsOn.summary = resources.getString(R.string.screenSaverIsOnSummary)
        screenSaverIsOn.layoutResource = R.layout.switch_custom
        screenSaverIsOn.setDefaultValue(true)

        val screensaverDuration = ListPreference(context)
        screensaverDuration.key = PreferenceKeys.screensaverDuration
        screensaverDuration.title = resources.getString(R.string.screensaverDuration)
        screensaverDuration.summary = preferences.getString(PreferenceKeys.screensaverDuration)
        screensaverDuration.dialogTitle = resources.getString(R.string.setScreensaverDuration)
        screensaverDuration.layoutResource = R.layout.preference_custom_layout
        screensaverDuration.setEntries(R.array.screen_saver_duration)
        screensaverDuration.setEntryValues(R.array.screen_saver_duration)
        screensaverDuration.setDefaultValue("15")

        val screenSaverCategory = PreferenceCategory(context)
        screenSaverCategory.key = PreferenceKeys.screenSaverCategory
        screenSaverCategory.title = resources.getString(R.string.screensaverOption)
        screen.addPreference(screenSaverCategory)
        screenSaverCategory.addPreference(screenSaverIsOn)
        screenSaverCategory.addPreference(screensaverDuration)

        preferenceScreen = screen
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {

        when (preference.key) {

            PreferenceKeys.backButton -> {
                navigation.navigateToDestination(
                    this,
                    SettingsFragmentDirections.actionSettingsFragmentToListFragment()
                )
            }
            PreferenceKeys.isAutoScroll -> {
            }
            PreferenceKeys.screensaverDuration -> {
            }
            PreferenceKeys.screenSaverIsOn -> {
            }
            PreferenceKeys.appStyle -> {
            }
            else -> {

            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun setStyle() {

        val newJob = Job()
        job.add(newJob)
        val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
        mCoroutineScope.launch {
            delay(1000)
            startActivity(Intent.makeRestartActivityTask(activity?.intent?.component))
        }
    }

    override fun onDestroy() {

        job.cancel()
        super.onDestroy()
    }
}

