package comanch.simpleplayer.playFragment

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import comanch.simpleplayer.preferences.PreferenceKeys
import comanch.simpleplayer.StateViewModel
import comanch.simpleplayer.musicManagment.ServicePlay
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.helpers.NavigationBetweenFragments
import comanch.simpleplayer.helpers.NavigationCorrespondent
import comanch.simpleplayer.R
import comanch.simpleplayer.dialogFragment.DialogPlayList
import comanch.simpleplayer.databinding.PlayFragmentBinding
import comanch.simpleplayer.helpers.StringKey
import comanch.simpleplayer.musicManagment.ServicePlay.Companion.getMediaSessionToken
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject


@AndroidEntryPoint
class PlayFragment : Fragment() {

    private val playViewModel: PlayViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()
    private val args: PlayFragmentArgs by navArgs()
    private var isStarted: Boolean = false

    private var servicePlayBinder: Binder? = null
    private var mediaController: MediaControllerCompat? = null

    private var mBound: Boolean? = false
    private var pos = 0
    private var isAutoScroll = true
    private var screensaverDuration = 20000L
    private var timeToClick = 0L
    private var imageUri: String? = null
    private var screenSaverIsOn: Boolean? = null
    private var playbackState: Int = -1
    private var recycleView: RecyclerView? = null

    @Inject
    lateinit var preferences: DefaultPreference

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private var adapter: PlayItemAdapter? = null

    private var callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            playbackState = state.state
            if (state.state != PlaybackStateCompat.STATE_PLAYING &&
                state.state != PlaybackStateCompat.STATE_PAUSED
            ) {
                playViewModel.startCheckCurrentPosOnce()
                playViewModel.cancelCheckCurrentPosCoroutine()
                playViewModel.setPlayPauseVisible(false)
                if (isAutoScroll) {
                    playViewModel.scrollList()
                }
                return
            }
            stateViewModel.setIsPlaying(state.state == PlaybackStateCompat.STATE_PLAYING)
            stateViewModel.setIsPause(state.state == PlaybackStateCompat.STATE_PAUSED)
            if (isAutoScroll) {
                playViewModel.scrollList()
            }
        }
    }

    private var connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            servicePlayBinder = ServicePlay.servicePlayerBinder
            mediaController = MediaControllerCompat(
                context,
                getMediaSessionToken()!!
            )
            callback.let {
                mediaController?.registerCallback(callback)
                mediaController?.playbackState?.let { state ->
                    callback.onPlaybackStateChanged(state)
                }

            }
            mBound = true
            if(!isStarted && (args.correspondent == NavigationCorrespondent.ListFragment ||
                args.correspondent == NavigationCorrespondent.PlayListFragment)) {
                stateViewModel.triggerStartPlay()
                isStarted = true
            }
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

        val action = PlayFragmentDirections.actionPlayFragmentToListFragment()
        val callbackBackPressed = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@PlayFragment,
                action
            )
        }
        callbackBackPressed.isEnabled = true

        if (savedInstanceState != null) {
            isStarted = savedInstanceState.getBoolean("isStarted", false)
        }

        isAutoScroll = preferences.getBoolean(PreferenceKeys.isAutoScroll)
        preferences.getString(PreferenceKeys.screensaverDuration).let {
            if (it.isNotEmpty()) {
                try {
                    screensaverDuration = it.toLong() * 1000
                } catch (e: Exception) {

                }
            }
        }

        timeToClick = Calendar.getInstance().timeInMillis
        imageUri = preferences.getString(PreferenceKeys.playScreenUri)
        screenSaverIsOn = preferences.getBoolean(PreferenceKeys.screenSaverIsOn)
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), ServicePlay::class.java).also { intent ->
            requireContext().bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding: PlayFragmentBinding =
            DataBindingUtil.inflate(
                inflater, R.layout.play_fragment, container, false
            )

        binding.list.setHasFixedSize(true)

        val backgroundPlayColor =
            resources.getColor(R.color.transparent_background100, activity?.theme)
        adapter = PlayItemAdapter(
            PlayItemListener { itemId ->
                playViewModel.onItemClicked(itemId)
            },
            ButtonPlayListener { item ->
                playViewModel.onPlayClicked(item)
            },
            backgroundPlayColor
        )

        val mIth = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP or ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {

                var pos1: Int? = null
                var pos2: Int? = null
                var move: Boolean = false

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    if (pos1 == null) {
                        pos1 = viewHolder.bindingAdapterPosition
                    }
                    pos2 = target.bindingAdapterPosition
                    adapter?.notifyItemMoved(
                        viewHolder.bindingAdapterPosition, target.bindingAdapterPosition
                    )
                    move = true
                    timeToClick = Calendar.getInstance().timeInMillis
                    return true
                }

                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    if (actionState == 0) {
                        if (move && pos1 != null && pos2 != null && pos1 != pos2) {
                            stateViewModel.moveItems(
                                adapter?.mGetItemId(pos1),
                                adapter?.mGetItemId(pos2)
                            )
                            pos1 = null
                            pos2 = null
                            move = false
                        }
                    }
                    super.onSelectedChanged(viewHolder, actionState)
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    timeToClick = Calendar.getInstance().timeInMillis
                    adapter?.mGetItemId(viewHolder.bindingAdapterPosition)?.let {
                        stateViewModel.deleteItem(
                            it
                        )
                    }
                }
            })
        mIth.attachToRecyclerView(binding.list)

        binding.list.adapter = adapter
        binding.list.itemAnimator = null
        binding.lifecycleOwner = viewLifecycleOwner

        stateViewModel.restartIsRepeat()

        playViewModel.addScreenInactiveJob()
        if (imageUri?.isNotEmpty() == true) {
            if (screenSaverIsOn == true) {
                playViewModel.startCheckScreenInactive(false)
            }
        } else {
            if (screenSaverIsOn == true) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.image_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (screenSaverIsOn == true) {
            recycleView = binding.list
            recycleView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    timeToClick = Calendar.getInstance().timeInMillis
                }
            })
        }

        playViewModel.items.observe(viewLifecycleOwner) { list ->
            list?.let { newList ->
                adapter?.setData(newList)
            }
        }

        stateViewModel.startPlay.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {

                val intent = Intent(StringKey.startPlay)
                when (it) {
                    1 -> {
                        intent.putExtra(StringKey.startPlay, StringKey.start)
                    }
                    2 -> {
                        intent.putExtra(StringKey.startPlay, StringKey.stopStart)
                    }
                }
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        stateViewModel.toast.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                when (it) {
                    "max" -> Toast.makeText(
                        context,
                        resources.getString(R.string.max_list),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        stateViewModel.isPlaying.observe(viewLifecycleOwner) { isPlay ->
            isPlay.getContentIfNotHandled()?.let {

                playViewModel.setIsCheckPos(it)
                playViewModel.addCurrentPosJob()
                playViewModel.startCheckCurrentPos()
                playViewModel.setPlayPauseVisible(it)
            }
        }

        stateViewModel.isPause.observe(viewLifecycleOwner) { isPlay ->
            isPlay.getContentIfNotHandled()?.let {
                playViewModel.startCheckCurrentPosOnce()
            }
        }

        stateViewModel.deleteArray.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                val intent = Intent(StringKey.deleteTrack)
                intent.putExtra(StringKey.deleteArray, it)
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        stateViewModel.isRepeat.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                setRepeatButtonVisible(it, binding)
                val intent = Intent(StringKey.isRepeat)
                intent.putExtra(StringKey.isRepeat, it)
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        playViewModel.navigateToImageScreen.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                if (imageUri?.isNotEmpty() == true) {
                    if (screenSaverIsOn == true) {
                        playViewModel.startCheckScreenInactive(false)
                    }
                }
                navigation.navigateToDestination(
                    this@PlayFragment,
                    PlayFragmentDirections.actionPlayFragmentToImageForPlayFragment(
                        NavigationCorrespondent.PlayFragment
                    )
                )
            }
        }

        playViewModel.setPlayPauseVisible.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                setPlayPauseVisible(it, binding)
            }
        }

        playViewModel.sendStop.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                val intent = Intent(StringKey.StopIfCurrentTrack)
                intent.putExtra("id", it)
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        playViewModel.trackActiveUpdate.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                adapter?.notifyItemChanged(it)
            }
        }

        playViewModel.scrollList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                autoScrolling(it, binding)
            }
        }

        playViewModel.click.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                timeToClick = Calendar.getInstance().timeInMillis
                if (it.active == 1) {
                    playViewModel.trackActiveDisable(it)
                } else {
                    playViewModel.trackActiveEnable(it)

                }
                adapter?.notifyItemChanged(it.position)
            }
        }

        playViewModel.playClick.observe(viewLifecycleOwner) { content ->
            timeToClick = Calendar.getInstance().timeInMillis
            content.getContentIfNotHandled()?.let {
                playViewModel.buttonPlayDisableAndStart(it)
            }
        }

        playViewModel.stopStartPlayNewTrack.observe(viewLifecycleOwner) { content ->
            timeToClick = Calendar.getInstance().timeInMillis
            content.getContentIfNotHandled()?.let {
                val intent = Intent(StringKey.startPlayByItemButtonPlay)
                intent.putExtra("id", it.musicId)
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        playViewModel.checkPos.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    setSeekBarPosition(it, binding)
                    playViewModel.startCheckCurrentPos()
                }
            }
        }

        playViewModel.checkPosOnce.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    setSeekBarPosition(it, binding)
                }
            }
        }

        playViewModel.checkScreenInactive.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {

                if (Calendar.getInstance().timeInMillis - timeToClick > screensaverDuration) {
                    playViewModel.startCheckScreenInactive(true)
                } else {
                    playViewModel.startCheckScreenInactive(false)
                }
            }
        }

        binding.seekbar.addOnChangeListener { s, value, fromUser ->
            if (fromUser) {
                if (mediaController?.playbackState?.state == PlaybackStateCompat.STATE_PAUSED ||
                    mediaController?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
                ) {
                    timeToClick = Calendar.getInstance().timeInMillis
                    val duration =
                        mediaController?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    if (duration != null && duration != 0L) {
                        val k = duration / 200
                        mediaController?.let {
                            mediaController?.transportControls?.seekTo(value.toLong() * k)
                        }
                    }
                } else {
                    s.value = 0F
                }
            }
        }
        binding.save.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            val dialogPicker = DialogPlayList()
            parentFragmentManager.let { fragmentM ->
                dialogPicker.show(fragmentM, "dialogPicker")
            }
        }

        setFragmentResultListener("dialog_playlist_key") { _, bundle ->
            when (bundle.get("dialog_playlist_extra_key")) {
                "ok" -> {
                    val name = bundle.get("dialog_playlist_result").toString()
                    stateViewModel.savePlaylist(name)
                }
            }
        }

        binding.arrowBack.setOnClickListener {
            navigation.navigateToDestination(
                this@PlayFragment,
                PlayFragmentDirections.actionPlayFragmentToListFragment()
            )
        }

        binding.delete.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            stateViewModel.deleteItems()
        }

        binding.up.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            adapter?.itemCount?.let {
                binding.list.smoothScrollToPosition(0)
            }
        }

        binding.down.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            val lastPos = adapter?.itemCount
            lastPos?.let {
                binding.list.smoothScrollToPosition(lastPos)
            }
        }

        binding.sort.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            stateViewModel.sortCurrentList()
        }

        binding.shuffle.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            stateViewModel.shuffleCurrentList()
        }

        binding.play.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            mediaController?.let {
                mediaController?.transportControls?.play()
            }
        }

        binding.pause.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            mediaController?.let {
                mediaController?.transportControls?.pause()
            }
        }

        binding.stop.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            mediaController?.let {
                mediaController?.transportControls?.stop()
            }
        }

        binding.next.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            mediaController?.let {
                mediaController?.transportControls?.skipToNext()
            }
            playViewModel.startCheckCurrentPosOnce()
        }

        binding.previous.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            mediaController?.let {
                mediaController?.transportControls?.skipToPrevious()
            }
            playViewModel.startCheckCurrentPosOnce()
        }

        binding.repeat.setOnClickListener {

            timeToClick = Calendar.getInstance().timeInMillis
            stateViewModel.setIsRepeat()
        }

        return binding.root
    }

    private fun setSeekBarPosition(
        it: MediaControllerCompat,
        binding: PlayFragmentBinding
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
            }else{
                binding.seekbar.value = 0F
                binding.time.text = "00:00"
            }
        }
    }

    private fun setRepeatButtonVisible(
        it: Int,
        binding: PlayFragmentBinding
    ) {
        when (it) {
            0 -> {
                binding.repeat.setBackgroundResource(R.drawable.ic_baseline_repeat_off_48)
                binding.repeat.contentDescription = resources.getString(R.string.repeat_off_button)
            }
            1 -> {
                binding.repeat.setBackgroundResource(R.drawable.ic_baseline_repeat_on_48)
                binding.repeat.contentDescription = resources.getString(R.string.repeat_list_button)
            }
            2 -> {
                binding.repeat.setBackgroundResource(R.drawable.ic_baseline_repeat_one_on_48)
                binding.repeat.contentDescription =
                    resources.getString(R.string.repeat_track_button)
            }
        }
    }

    private fun setPlayPauseVisible(
        it: Boolean,
        binding: PlayFragmentBinding
    ) {
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

    private fun autoScrolling(
        it: Int,
        binding: PlayFragmentBinding
    ) {
        if (pos != it) {
            when {
                pos == 0 -> {
                    when {
                        it < 3 -> binding.list.smoothScrollToPosition(0)
                        (adapter?.itemCount != null && (adapter?.itemCount
                            ?: 0 - it) < 3) ->
                            binding.list.smoothScrollToPosition(
                                adapter?.itemCount ?: it
                            )
                        else -> binding.list.smoothScrollToPosition(it)
                    }
                    pos = it
                }
                it < 3 -> {
                    binding.list.smoothScrollToPosition(0)
                    pos = it
                }
                (adapter?.itemCount != null && (adapter?.itemCount ?: 0 - it) < 3) -> {
                    binding.list.smoothScrollToPosition(
                        adapter?.itemCount ?: it
                    )
                    pos = it
                }
                else -> {
                    if (pos < it) {
                        binding.list.smoothScrollToPosition(it)
                    } else {
                        binding.list.smoothScrollToPosition(it)
                    }
                    pos = it
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (playbackState == PlaybackStateCompat.STATE_PLAYING ||
            playbackState == PlaybackStateCompat.STATE_PAUSED
        ) {
            playViewModel.addCurrentPosJob()
            playViewModel.startCheckCurrentPos()
        }

        playViewModel.addScreenInactiveJob()
        if (imageUri?.isNotEmpty() == true) {
            if (screenSaverIsOn == true) {
                playViewModel.startCheckScreenInactive(false)
            }
        }

        isAutoScroll = preferences.getBoolean(PreferenceKeys.isAutoScroll)
        preferences.getString(PreferenceKeys.screensaverDuration).let {
            if (it.isNotEmpty()) {
                try {
                    screensaverDuration = it.toLong() * 1000
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun onPause() {

        playViewModel.cancelCheckCurrentPosCoroutine()
        playViewModel.cancelCheckScreenInactiveCoroutine()
        super.onPause()
    }

    override fun onStop() {

        servicePlayBinder = null
        mediaController = null
        requireContext().unbindService(connection)
        mBound = false
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isStarted", isStarted)
        super.onSaveInstanceState(outState)
    }


    override fun onDestroyView() {

        recycleView?.clearOnScrollListeners()
        super.onDestroyView()
    }

    override fun onDestroy() {

        playViewModel.cancelJobCycleCoroutines()

        servicePlayBinder = null
        callback.let {
            mediaController?.unregisterCallback(callback)
        }
        mediaController = null
        mBound = null

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