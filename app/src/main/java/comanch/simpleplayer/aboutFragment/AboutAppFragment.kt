package comanch.simpleplayer.aboutFragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.R
import comanch.simpleplayer.databinding.AboutAppFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AboutAppFragment : Fragment() {

    @Inject
    lateinit var navigation: NavigationBetweenFragments


   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       activity?.window?.statusBarColor =
           resources.getColor(R.color.background2, activity?.theme)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {

            navigation.navigateToDestination(
               this@AboutAppFragment, AboutAppFragmentDirections.actionAboutAppFragmentToListFragment()
           )
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: AboutAppFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.about_app_fragment, container, false
        )

        binding.lifecycleOwner = viewLifecycleOwner

        binding.arrowBack.setOnClickListener {
            navigation.navigateToDestination(
                this, AboutAppFragmentDirections.actionAboutAppFragmentToListFragment()
            )
        }
        return binding.root
    }
}