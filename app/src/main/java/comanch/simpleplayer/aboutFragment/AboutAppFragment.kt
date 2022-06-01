package comanch.simpleplayer.aboutFragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import comanch.simpleplayer.NavigationBetweenFragments
import comanch.simpleplayer.R
import comanch.simpleplayer.SoundPoolForFragments
import comanch.simpleplayer.databinding.AboutAppFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AboutAppFragment : Fragment() {

    private val aboutAppViewModel: AboutAppViewModel by viewModels()

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {

            navigation.navigateToDestination(
               this@AboutAppFragment, AboutAppFragmentDirections.actionAboutAppFragmentToListFragment()
           )
        }
        callback.isEnabled = true

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: AboutAppFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.about_app_fragment, container, false
        )

        binding.aboutAppViewModel = aboutAppViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        soundPoolContainer.setTouchSound()

        aboutAppViewModel.ossLicense.observe(viewLifecycleOwner) {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }

        binding.arrowBackAboutApp.setOnClickListener {

            soundPoolContainer.playSoundIfEnable(soundPoolContainer.soundButtonTap)
            navigation.navigateToDestination(
                this, AboutAppFragmentDirections.actionAboutAppFragmentToListFragment()
            )
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }
}