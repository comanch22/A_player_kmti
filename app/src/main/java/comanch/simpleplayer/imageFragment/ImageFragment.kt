package comanch.simpleplayer.imageFragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.squareup.picasso.Picasso
import comanch.simpleplayer.*
import comanch.simpleplayer.databinding.ImageChoiceFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ImageFragment : Fragment() {

    private val detailViewModel: ImageViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()
    var binding: ImageChoiceFragmentBinding? = null

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    @Inject
    lateinit var preferences: DefaultPreference

    private val getContentPreview = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        try {
            Picasso
                .get()
                .load(uri)
                .placeholder(R.drawable.ic_baseline_download_24)
                .error(R.drawable.ic_baseline_error_24)
                .into(binding?.imagePreview)
            preferences.putString("previewUri", "$uri")
        }catch (e: Exception){
            Toast.makeText(context, "an error occurred while processing the image, select another", Toast.LENGTH_LONG).show()
        }
    }

    private val getContentPlayScreen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        try {
            Picasso
                .get()
                .load(uri)
                .placeholder(R.drawable.ic_baseline_download_24)
                .error(R.drawable.ic_baseline_error_24)
                .into(binding?.imagePlayScreen)
            preferences.putString("playScreenUri", "$uri")
        }catch (e: Exception){
            Toast.makeText(context, "an error occurred while processing the image, select another", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val action = ImageFragmentDirections.actionImageFragmentToListFragment()
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@ImageFragment,
                action
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

        binding =
            DataBindingUtil.inflate(
                inflater, R.layout.image_choice_fragment, container, false
            )

        binding?.lifecycleOwner = viewLifecycleOwner


        binding?.arrowBack?.setOnClickListener {
            navigation.navigateToDestination(
                this@ImageFragment,
                ImageFragmentDirections.actionImageFragmentToListFragment()
            )
        }

        binding?.choicePreview?.setOnClickListener {
            getContentPreview.launch("image/*")
        }

        binding?.choicePlayScreen?.setOnClickListener {
            getContentPlayScreen.launch("image/*")
        }

        return binding!!.root
    }

    override fun onResume() {

        super.onResume()
        soundPoolContainer.setTouchSound()
    }
}