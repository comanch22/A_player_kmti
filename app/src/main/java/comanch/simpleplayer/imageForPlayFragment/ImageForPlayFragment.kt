package comanch.simpleplayer.imageForPlayFragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadata
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import comanch.simpleplayer.R
import comanch.simpleplayer.StateViewModel
import comanch.simpleplayer.databinding.ImageForPlayFragmentBinding
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.helpers.NavigationCorrespondent
import comanch.simpleplayer.musicManagment.ServicePlay
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.preferences.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ImageForPlayFragment : Fragment() {

    private val imageForPlayViewModel: ImageForPlayViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()
    private val args: ImageForPlayFragmentArgs by navArgs()

    private var servicePlayBinder: Binder? = null
    private var mediaController: MediaControllerCompat? = null

    private var mBound: Boolean = false
    private var playbackState: Int = -1

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private var controlPanelIsHide: Boolean? = null

    private var callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {

            playbackState = state.state

            if (state.state != PlaybackStateCompat.STATE_PLAYING) {
                imageForPlayViewModel.cancelCheckCurrentPosCoroutine()
                imageForPlayViewModel.setPlayPauseVisible(false)
                if (state.state == PlaybackStateCompat.STATE_PAUSED){
                    imageForPlayViewModel.startCheckCurrentPosOnce()
                    mediaController?.metadata?.mediaMetadata?.let {
                        try {
                            stateViewModel.setCurrentTitle(
                                (it as MediaMetadata).getString(
                                    MediaMetadata.METADATA_KEY_TITLE
                                )
                            )
                        } catch (e: Exception) {
                            stateViewModel.setCurrentTitle("title not found")
                        }
                    }
                }
                return
            }

            stateViewModel.setIsPlaying(state.state == PlaybackStateCompat.STATE_PLAYING)
            stateViewModel.setIsPause(state.state == PlaybackStateCompat.STATE_PAUSED)
            mediaController?.metadata?.mediaMetadata?.let {
                if (state.state == PlaybackStateCompat.STATE_PLAYING ||
                    state.state == PlaybackStateCompat.STATE_PAUSED
                ) {
                    try {
                        stateViewModel.setCurrentTitle(
                            (it as MediaMetadata).getString(
                                MediaMetadata.METADATA_KEY_TITLE
                            )
                        )
                    } catch (e: Exception) {
                        stateViewModel.setCurrentTitle("title not found")
                    }
                }
            }
        }
    }

    private var connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            servicePlayBinder = ServicePlay.servicePlayerBinder
            mediaController = MediaControllerCompat(
                context,
                ServicePlay.getMediaSessionToken()!!
            )
            callback.let {
                mediaController?.registerCallback(callback)
                mediaController?.playbackState?.let { state ->
                    callback.onPlaybackStateChanged(state)
                }

            }
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            servicePlayBinder = null
            if (mediaController != null) {
                callback.let {
                    mediaController?.unregisterCallback(callback)
                }
                mediaController = null
            }
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        activity?.window?.statusBarColor =
            resources.getColor(R.color.transparent_background100, activity?.theme)
        activity?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }

        val action = if (args.correspondent == NavigationCorrespondent.PlayFragment) {
            ImageForPlayFragmentDirections.actionImageForPlayFragmentToPlayFragment(
                NavigationCorrespondent.ImageForPlayFragment
            )
        } else {
            ImageForPlayFragmentDirections.actionImageForPlayFragmentToImageFragment()
        }
        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@ImageForPlayFragment,
                action
            )
        }
        callback.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: ImageForPlayFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.image_for_play_fragment, container, false)

        if (args.correspondent == NavigationCorrespondent.ImageChoiceFragment) {
            binding.seekbar.visibility = View.INVISIBLE
            binding.stop.visibility = View.INVISIBLE
            binding.play.visibility = View.INVISIBLE
            binding.pause.visibility = View.INVISIBLE
            binding.previous.visibility = View.INVISIBLE
            binding.next.visibility = View.INVISIBLE
            binding.time.visibility = View.INVISIBLE
            binding.swipeDown.visibility = View.INVISIBLE
            binding.currentTrack.visibility = View.INVISIBLE

            imageForPlayViewModel.setPlayScreenImage()

            imageForPlayViewModel.setPlayScreenImage.observe(viewLifecycleOwner) { content ->
                content.getContentIfNotHandled()?.let {

                    val mUri = preferences.getString(PreferenceKeys.playScreenUri)
                    if (mUri.isNotEmpty()) {
                        Glide.with(this)
                            .asDrawable()
                            .load(mUri)
                            .placeholder(R.drawable.ic_baseline_download_24)
                            .error(R.drawable.ic_baseline_error_24)
                            .centerCrop()
                            .into(binding.playScreenImage)
                    }
                }

            }

            binding.playScreenImage.setOnClickListener {
                navigation.navigateToDestination(
                    this@ImageForPlayFragment,
                    if (args.correspondent == NavigationCorrespondent.PlayFragment)
                        ImageForPlayFragmentDirections.actionImageForPlayFragmentToPlayFragment(
                            NavigationCorrespondent.ImageForPlayFragment
                        )
                    else
                        ImageForPlayFragmentDirections.actionImageForPlayFragmentToImageFragment()
                )
            }
            return binding.root
        }
        imageForPlayViewModel.setPlayScreenImage()

        imageForPlayViewModel.setPlayScreenImage.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {

                val mUri = preferences.getString(PreferenceKeys.playScreenUri)
                if (mUri.isNotEmpty()) {
                    Glide.with(this)
                        .asDrawable()
                        .load(mUri)
                        .placeholder(R.drawable.ic_baseline_download_24)
                        .error(R.drawable.ic_baseline_error_24)
                        .centerCrop()
                        .into(binding.playScreenImage)
                }
            }
        }

        imageForPlayViewModel.setPlayPauseVisible.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                setPlayPauseVisible(it, binding)
            }
        }

        imageForPlayViewModel.setControlPanelVisible.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                setControlPanelVisible(it, binding)
            }
        }

        imageForPlayViewModel.setControlPanelAnimation.observe(viewLifecycleOwner) { isVisible ->
            isVisible.getContentIfNotHandled()?.let {
                if (it) {
                    setControlPanelVisible(true, binding)
                    AnimatorSet().apply {

                        setUpAnimation(binding)
                        start()
                    }
                } else {
                    AnimatorSet().apply {

                        setDownAnimation(binding)
                        play(setViewNotTransparent(binding.swipeLayoutUp)).with(
                            setViewTranslateUp(
                                binding.swipeLayoutUp
                            )
                        )
                        start()
                        addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                setControlPanelVisible(false, binding)
                            }
                        })
                    }
                }
            }
        }

        imageForPlayViewModel.checkPos.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    setSeekBarPosition(it, binding)
                    imageForPlayViewModel.startCheckCurrentPos()
                }
            }
        }

        imageForPlayViewModel.checkPosOnce.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    setSeekBarPosition(it, binding)
                }
            }
        }

        stateViewModel.isPlaying.observe(viewLifecycleOwner) { isPlay ->
            isPlay.getContentIfNotHandled()?.let {

                if (args.correspondent ==
                    NavigationCorrespondent.ImageChoiceFragment
                ) {
                    return@observe
                }

                imageForPlayViewModel.setIsCheckPos(it)
                imageForPlayViewModel.addCurrentPosJob()
                imageForPlayViewModel.startCheckCurrentPos()
                imageForPlayViewModel.setPlayPauseVisible(it)
            }
        }

        stateViewModel.isPause.observe(viewLifecycleOwner) { isPlay ->
            isPlay.getContentIfNotHandled()?.let {
                imageForPlayViewModel.startCheckCurrentPosOnce()
            }
        }

        stateViewModel.currentTitle.observe(viewLifecycleOwner) { title ->
            title.getContentIfNotHandled()?.let {
                binding.currentTrack.text = it
            }
        }

        binding.seekbar.addOnChangeListener { _, value, fromUser ->

            if (fromUser) {
                val duration =
                    mediaController?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                if (duration != null && duration != 0L) {
                    val k = duration / 200
                    mediaController?.let {
                        mediaController?.transportControls?.seekTo(value.toLong() * k)
                    }
                }
            }
        }

        binding.playScreenImage.setOnClickListener {
            navigation.navigateToDestination(
                this@ImageForPlayFragment,
                if (args.correspondent == NavigationCorrespondent.PlayFragment)
                    ImageForPlayFragmentDirections.actionImageForPlayFragmentToPlayFragment(
                        NavigationCorrespondent.ImageForPlayFragment
                    )
                else
                    ImageForPlayFragmentDirections.actionImageForPlayFragmentToImageFragment()
            )
        }

        binding.play.setOnClickListener {
            mediaController?.let {
                mediaController?.transportControls?.play()
            }
        }

        binding.pause.setOnClickListener {
            mediaController?.let {
                mediaController?.transportControls?.pause()
            }
        }

        binding.stop.setOnClickListener {
            mediaController?.let {
                mediaController?.transportControls?.stop()
            }
        }

        binding.next.setOnClickListener {
            mediaController?.let {
                mediaController?.transportControls?.skipToNext()
            }
            imageForPlayViewModel.startCheckCurrentPosOnce()
        }

        binding.previous.setOnClickListener {
            mediaController?.let {
                mediaController?.transportControls?.skipToPrevious()
            }
            imageForPlayViewModel.startCheckCurrentPosOnce()
        }

        binding.swipeLayoutUp.setOnClickListener {
            imageForPlayViewModel.setControlPanelVisible(true)
        }

        binding.swipeLayoutDown.setOnClickListener {
            imageForPlayViewModel.setControlPanelVisible(false)
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Intent(context, ServicePlay::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun AnimatorSet.setDownAnimation(binding: ImageForPlayFragmentBinding) {

        play(setViewTransparent(binding.currentTrack)).with(setViewTranslateDown(binding.currentTrack))
        play(setViewTransparent(binding.seekbarLayout)).with(setViewTranslateDown(binding.seekbarLayout))
        play(setViewTransparent(binding.swipeLayoutDown)).with(setViewTranslateDown(binding.swipeLayoutDown))
        play(setViewTransparent(binding.play)).with(setViewTranslateDown(binding.play))
        play(setViewTransparent(binding.pause)).with(setViewTranslateDown(binding.pause))
        play(setViewTransparent(binding.stop)).with(setViewTranslateDown(binding.stop))
        play(setViewTransparent(binding.next)).with(setViewTranslateDown(binding.next))
        play(setViewTransparent(binding.previous)).with(setViewTranslateDown(binding.previous))
        play(setViewTransparent(binding.time)).with(setViewTranslateDown(binding.time))
    }

    private fun AnimatorSet.setUpAnimation(binding: ImageForPlayFragmentBinding) {

        play(setViewNotTransparent(binding.currentTrack)).with(setViewTranslateUp(binding.currentTrack))
        play(setViewNotTransparent(binding.seekbarLayout)).with(setViewTranslateUp(binding.seekbarLayout))
        play(setViewNotTransparent(binding.swipeLayoutDown)).with(setViewTranslateUp(binding.swipeLayoutDown))
        play(setViewNotTransparent(binding.play)).with(setViewTranslateUp(binding.play))
        play(setViewNotTransparent(binding.pause)).with(setViewTranslateUp(binding.pause))
        play(setViewNotTransparent(binding.stop)).with(setViewTranslateUp(binding.stop))
        play(setViewNotTransparent(binding.next)).with(setViewTranslateUp(binding.next))
        play(setViewNotTransparent(binding.previous)).with(setViewTranslateUp(binding.previous))
        play(setViewNotTransparent(binding.time)).with(setViewTranslateUp(binding.time))
        play(setViewTransparent(binding.swipeLayoutUp)).with(setViewTranslateDown(binding.swipeLayoutUp))
    }

    private fun setViewTranslateUp(view: View): Animator {
        return ObjectAnimator.ofFloat(
            view,
            "translationY",
            0F
        ).apply {
            duration = 700
        }
    }

    private fun setViewNotTransparent(view: View): Animator {
        return ObjectAnimator.ofFloat(
            view,
            "alpha",
            0f,
            1f
        ).apply {
            startDelay = 350
            duration = 350
        }
    }

    private fun setViewTranslateDown(view: View): Animator {
        return ObjectAnimator.ofFloat(
            view,
            "translationY",
            0F,
            168F
        ).apply {
            duration = 700
        }
    }

    private fun setViewTransparent(view: View): Animator {
        return ObjectAnimator.ofFloat(
            view,
            "alpha",
            1f,
            0f
        ).apply {
            startDelay = 350
            duration = 350
        }
    }

    private fun setControlPanelVisible(isVisible: Boolean, binding: ImageForPlayFragmentBinding) {

        controlPanelIsHide = !isVisible

        if (isVisible) {

            imageForPlayViewModel.setPlayPauseVisible()
            binding.stop.visibility = View.VISIBLE
            binding.swipeLayoutDown.visibility = View.VISIBLE
            binding.seekbarLayout.visibility = View.VISIBLE
            binding.currentTrack.visibility = View.VISIBLE
            binding.time.visibility = View.VISIBLE
            binding.next.visibility = View.VISIBLE
            binding.previous.visibility = View.VISIBLE
            binding.swipeLayoutUp.visibility = View.GONE

        } else {

            binding.stop.visibility = View.GONE
            binding.swipeLayoutDown.visibility = View.GONE
            binding.seekbarLayout.visibility = View.GONE
            binding.currentTrack.visibility = View.GONE
            binding.time.visibility = View.GONE
            binding.pause.visibility = View.GONE
            binding.play.visibility = View.GONE
            binding.next.visibility = View.GONE
            binding.previous.visibility = View.GONE

            binding.swipeLayoutUp.visibility = View.VISIBLE
        }
    }

    private fun setPlayPauseVisible(
        it: Boolean,
        binding: ImageForPlayFragmentBinding
    ) {
        if (controlPanelIsHide == true) {
            return
        }
        if (it) {
            binding.play.isEnabled = false
            binding.play.visibility = View.INVISIBLE
            binding.pause.isEnabled = true
            binding.pause.visibility = View.VISIBLE
        } else {
            binding.play.isEnabled = true
            binding.play.visibility = View.VISIBLE
            binding.pause.isEnabled = false
            binding.pause.visibility = View.INVISIBLE
        }
    }

    private fun setSeekBarPosition(
        it: MediaControllerCompat,
        binding: ImageForPlayFragmentBinding
    ) {
        val currentPos = it.playbackState.position
        val duration =
            mediaController?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        if (duration != null && duration != 0L) {
            val k = duration / 200
            if (k > 0) {
                binding.seekbar.value = (currentPos / k).toFloat()
                binding.time.text =
                    getString(R.string.duration_to_time, getMin(currentPos), getSec(currentPos))
            }
        }
    }

    override fun onResume() {

        super.onResume()

        if (playbackState == PlaybackStateCompat.STATE_PLAYING ||
            playbackState == PlaybackStateCompat.STATE_PAUSED
        ) {
            imageForPlayViewModel.addCurrentPosJob()
            imageForPlayViewModel.startCheckCurrentPos()
        }
        imageForPlayViewModel.setControlPanelVisible()
    }

    override fun onPause() {
        imageForPlayViewModel.cancelJobCycleCoroutines()
        super.onPause()
    }

    override fun onStop() {
        servicePlayBinder = null
        mediaController = null
        requireContext().unbindService(connection)
        mBound = false
        super.onStop()
    }

    override fun onDestroy() {

        activity?.window?.statusBarColor =
            resources.getColor(R.color.background, activity?.theme)

        activity?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, true)
        }

        imageForPlayViewModel.cancelJobCycleCoroutines()
        servicePlayBinder = null
        callback.let {

            mediaController?.unregisterCallback(callback)

        }
        mediaController = null
        mBound = false

        super.onDestroy()
    }

    private fun getMin(l: Long): String {

        val secLong = l / 1000 / 60
        return if ("$secLong".length == 1) {
            "0$secLong"
        } else {
            "$secLong"
        }
    }

    private fun getSec(l: Long): String {

        val secLong = l / 1000 % 60
        return if ("$secLong".length == 1) {
            "0$secLong"
        } else {
            "$secLong"
        }
    }
}


