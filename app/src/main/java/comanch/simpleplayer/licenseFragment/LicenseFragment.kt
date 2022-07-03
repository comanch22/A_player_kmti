package comanch.simpleplayer.licenseFragment

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
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.R
import comanch.simpleplayer.databinding.LicenseFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LicenseFragment : Fragment() {

    private val licenseViewModel: LicenseViewModel by viewModels()

    @Inject
    lateinit var navigation: NavigationBetweenFragments


   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

       activity?.window?.statusBarColor =
           resources.getColor(R.color.background2, activity?.theme)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {

            navigation.navigateToDestination(
               this@LicenseFragment, LicenseFragmentDirections.actionLicenseFragmentToListFragment()
           )
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: LicenseFragmentBinding = DataBindingUtil.inflate(
            inflater, R.layout.license_fragment, container, false
        )

        binding.licenseViewModel = licenseViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        licenseViewModel.ossLicense.observe(viewLifecycleOwner) {
            startActivity(Intent(context, OssLicensesMenuActivity::class.java))
        }

        binding.arrowBack.setOnClickListener {
            navigation.navigateToDestination(
                this, LicenseFragmentDirections.actionLicenseFragmentToListFragment()
            )
        }
        return binding.root
    }
}