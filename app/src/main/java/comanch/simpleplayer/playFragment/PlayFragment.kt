package comanch.simpleplayer.playFragment

import android.content.ComponentName
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import comanch.simpleplayer.*
import comanch.simpleplayer.databinding.PlayFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class PlayFragment : Fragment() {

    private val playViewModel: PlayViewModel by viewModels()
    private val stateViewModel: StateViewModel by activityViewModels()

    private var servicePlayBinder: ServicePlay.ServicePlayerBinder? = null
    private var mediaController: MediaControllerCompat? = null

    private var mBound: Boolean = false
    private var isRepeat: Boolean = false
    private var pos = 0
    private val job: CompositeJob = CompositeJob()

    @Inject
    lateinit var soundPoolContainer: SoundPoolForFragments

    @Inject
    lateinit var navigation: NavigationBetweenFragments

    private var adapter: PlayItemAdapter? = null

    private var callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            stateViewModel.setIsPlaying(state.state == PlaybackStateCompat.STATE_PLAYING)
            Log.e("fgdgdgdgdfgdfg", "${state.state}")
            playViewModel.scrollList()
        }
    }

    private var connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            servicePlayBinder = service as ServicePlay.ServicePlayerBinder
            mediaController = MediaControllerCompat(
                context,
                servicePlayBinder?.getMediaSessionToken()!!
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
        val action = PlayFragmentDirections.actionPlayFragmentToListFragment()
        val callbackBackPressed = requireActivity().onBackPressedDispatcher.addCallback(this) {
            navigation.navigateToDestination(
                this@PlayFragment,
                action
            )
        }
        callbackBackPressed.isEnabled = true

        Intent(context, ServicePlay::class.java).also { intent ->
            context?.bindService(intent, connection, BIND_AUTO_CREATE)
        }

        soundPoolContainer.soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            soundPoolContainer.soundMap[sampleId] = status
        }

        savedInstanceState?.getBoolean("isRepeat", false)?.let {
            isRepeat = it
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

        adapter = PlayItemAdapter(
            PlayItemListener { itemId ->
                playViewModel.onItemClicked(itemId)
            },
            PlayItemLongListener { b ->
                playViewModel.onItemLongClicked(b)
            }
        )

        binding.list.adapter = adapter
        binding.lifecycleOwner = viewLifecycleOwner

        setRepeatBackGround(binding.repeat)

        playViewModel.items.observe(viewLifecycleOwner) { list ->
            list?.let { newList ->
                adapter?.setData(newList)
            }
        }

        stateViewModel.startPlay.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {

                val intent = Intent("startPlay")
                when (it) {
                    1 -> {
                        intent.putExtra("startPlay", "start")
                    }
                    2 -> {
                        intent.putExtra("startPlay", "stopStart")
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
                //  playViewModel.scrollList()
                if (it) {
                    playViewModel.startCheckCurrentPos()
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
        }

        playViewModel.sendStop.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                val intent = Intent("StopIfCurrentTrack")
                intent.putExtra("id", it)
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        playViewModel.scrollList.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                if (pos != it) {
                    when {
                        pos == 0 -> {
                            val newJob = Job()
                            job.add(newJob)
                            val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                            mCoroutineScope.launch {
                                delay(200)
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
                            val newJob = Job()
                            job.add(newJob)
                            val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                            mCoroutineScope.launch {
                                delay(300)
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
            }
        }

        stateViewModel.deleteArray.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                val intent = Intent("deleteTrack")
                intent.putExtra("deleteArray", it)
                LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
            }
        }

        playViewModel.click.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                if (it.active == 1) {
                    playViewModel.trackActiveDisable(it)
                } else {
                    playViewModel.trackActiveEnable(it)
                }
                adapter?.notifyItemChanged(it.position)
            }
        }

        playViewModel.longClick.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                playViewModel.trackActiveLongEnable(it)
                adapter?.notifyItemChanged(it.position)
            }
        }

        playViewModel.getActiveListForDelete.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                stateViewModel.deleteItems()
            }
        }

        playViewModel.longStart.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    mediaController?.transportControls?.play()
                }
            }
        }

        playViewModel.longStop.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    mediaController?.transportControls?.stop()
                }
            }
        }

        playViewModel.checkPos.observe(viewLifecycleOwner) { content ->
            content.getContentIfNotHandled()?.let {
                mediaController?.let {
                    val currentPos = it.playbackState.position
                    val duration =
                        mediaController?.metadata?.bundle?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                    if (duration != null && duration != 0L) {
                        val k = duration / 200
                        if (k > 0) {
                            binding.seekbar.value = (currentPos / k).toFloat()
                        }
                    }
                    playViewModel.startCheckCurrentPos()
                }
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


        binding.save.setOnClickListener {

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
            adapter?.isDelete = true
            stateViewModel.deleteItems()
        }

        binding.up.setOnClickListener {
            adapter?.itemCount?.let {
                binding.list.smoothScrollToPosition(0)
            }
        }

        binding.down.setOnClickListener {
            val lastPos = adapter?.itemCount
            lastPos?.let {
                binding.list.smoothScrollToPosition(lastPos)
            }
        }

        binding.sort.setOnClickListener {
            stateViewModel.sortCurrentList()
        }

        binding.shuffle.setOnClickListener {
            stateViewModel.shuffleCurrentList()
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
            playViewModel.startCheckCurrentPosOnce()
        }

        binding.previous.setOnClickListener {
            mediaController?.let {
                mediaController?.transportControls?.skipToPrevious()
            }
            playViewModel.startCheckCurrentPosOnce()
        }

        binding.repeat.setOnClickListener {

            isRepeat = !isRepeat
            setRepeatBackGround(binding.repeat)
            val intent = Intent("isRepeat")
            intent.putExtra("isRepeat", isRepeat)
            LocalBroadcastManager.getInstance(this.requireContext()).sendBroadcast(intent)
        }

        return binding.root
    }

    private fun setRepeatBackGround(view: TextView) {
        if (isRepeat) {
            view.setBackgroundResource(R.drawable.ic_baseline_repeat_on_36)
        } else {
            view.setBackgroundResource(R.drawable.ic_baseline_repeat_off_48)
        }
    }

    override fun onResume() {
        super.onResume()
        soundPoolContainer.setTouchSound()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("isRepeat", isRepeat)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {

        job.cancel()
        if (mBound) {
            servicePlayBinder = null
            mediaController = null
            connection.let { context?.unbindService(it) }
            mBound = false
        }
        super.onDestroy()
    }
}