package comanch.simpleplayer.imageFragment

import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import comanch.simpleplayer.preferences.PreferenceKeys
import comanch.simpleplayer.helpers.LocalLifecycleObserver
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.helpers.NavigationCorrespondent
import comanch.simpleplayer.R
import comanch.simpleplayer.databinding.ImageChoiceFragmentBinding
import comanch.simpleplayer.helpers.StringKey
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ImageFragment : Fragment() {

    private val imageChoiceViewModel: ImageChoiceViewModel by viewModels()

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private val mPrefsListener =
        OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                PreferenceKeys.previewUri -> {
                    setImagePreview(false)
                }
                PreferenceKeys.playScreenUri -> {
                    setImagePlayScreen(false)
                }
            }
        }

    private var binding: ImageChoiceFragmentBinding? = null
    private lateinit var observer: LocalLifecycleObserver

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

        observer = LocalLifecycleObserver(requireActivity().activityResultRegistry, preferences)
        lifecycle.addObserver(observer)
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

        imageChoiceViewModel.setImagePreview()
        imageChoiceViewModel.setImagePreview.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                setImagePreview()
                setImagePlayScreen()
            }
        }

        binding?.arrowBack?.setOnClickListener {
            navigation.navigateToDestination(
                this@ImageFragment,
                ImageFragmentDirections.actionImageFragmentToListFragment()
            )
        }

        binding?.imagePreview?.setOnClickListener {
            observer.selectImage(PreferenceKeys.previewUri)
        }

        binding?.imagePlayScreen?.setOnClickListener {
            observer.selectImage(PreferenceKeys.playScreenUri)
        }

        return binding!!.root
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(mPrefsListener)
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(mPrefsListener)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.statusBarColor =
            resources.getColor(R.color.background2, activity?.theme)
    }

    private fun setImagePreview(fromCache: Boolean = true) {

        val mUri = preferences.getString(PreferenceKeys.previewUri)

        if (fromCache) {
            binding?.imagePreview?.let {
                Glide.with(requireContext())
                    .load(mUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_baseline_download_24)
                    .error(R.drawable.ic_baseline_error_24)
                    .onlyRetrieveFromCache(true)
                    .into(it)
            }
        } else {
            binding?.imagePreview?.let {
                Glide.with(this)
                    .load(mUri)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .placeholder(R.drawable.ic_baseline_download_24)
                    .error(R.drawable.ic_baseline_error_24)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return true
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            val intent = Intent(StringKey.previewUriChange)
                            intent.putExtra(StringKey.uri, mUri)
                            LocalBroadcastManager.getInstance(this@ImageFragment.requireContext())
                                .sendBroadcast(intent)
                            return false
                        }
                    })
                    .into(it)
            }
        }
    }

    private fun setImagePlayScreen(fromCache: Boolean = true) {

        val mUri = preferences.getString(PreferenceKeys.playScreenUri)

        if (fromCache) {
            binding?.imagePlayScreen?.let {
                Glide.with(requireContext())
                    .load(mUri)
                    .centerCrop()
                    .onlyRetrieveFromCache(true)
                    .placeholder(R.drawable.ic_baseline_download_24)
                    .error(R.drawable.ic_baseline_error_24)
                    .into(it)
            }
        } else {
            binding?.imagePlayScreen?.let {
                Glide.with(this)
                    .load(mUri)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .placeholder(R.drawable.ic_baseline_download_24)
                    .error(R.drawable.ic_baseline_error_24)
                    .centerCrop()
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return true
                        }
                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            val intent = Intent(StringKey.screenUriChange)
                            intent.putExtra(StringKey.uri, mUri)
                            LocalBroadcastManager.getInstance(this@ImageFragment.requireContext())
                                .sendBroadcast(intent)
                            navigation.navigateToDestination(
                                this@ImageFragment,
                                ImageFragmentDirections.actionImageFragmentToImageForPlayFragment(
                                    NavigationCorrespondent.ImageChoiceFragment
                                )
                            )
                            return false
                        }
                    })
                    .into(it)
            }
        }
    }
}