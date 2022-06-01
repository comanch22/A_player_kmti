package comanch.simpleplayer

import android.app.*
import android.content.*
import android.media.*
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.session.MediaButtonReceiver
import comanch.simpleplayer.MediaStyleHelper.fromHelper
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.dataBase.MusicTrackDAO
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class ServicePlay : Service(),
    OnPreparedListener,
    OnErrorListener,
    OnCompletionListener,
    OnAudioFocusChangeListener {

    @Inject
    lateinit var databaseMusic: MusicTrackDAO
    private val job: CompositeJob = CompositeJob()

    private var mMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var pausePosition: Int = 0

    private val channelId = "playerKTMi_Channel_Id"
    private val channelName = "playerKTMi_Channel_Name"
    private val notificationId = 17131415
    private val intentFilterNOISY = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
    private val intentFilterUpdateData = IntentFilter("UpdateCurrentTrack")
    private var localBroadcastManager: LocalBroadcastManager? = null
    private var context: Context? = null
    private var currentTrack: MusicTrack? = null
    private var isRepeat: Boolean = false
    private var databaseIsBlock = false

    private val metadataBuilder: MediaMetadataCompat.Builder = MediaMetadataCompat.Builder()
    private val stateBuilder = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    or PlaybackStateCompat.ACTION_SEEK_TO
        )

    private var mediaSession: MediaSessionCompat? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        context = this.applicationContext
        context?.let {
            createNotificationChannel(context!!)
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSessionCompat(this, "PlayerService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(stateBuilder.build())
            setCallback(mediaCallback)
        }

        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.action = "openPlayList"
        activityIntent.putExtra("openPlayList", true)
        mediaSession?.setSessionActivity(
            PendingIntent.getActivity(
                context,
                234,
                activityIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        val mediaButtonIntent = Intent(
            Intent.ACTION_MEDIA_BUTTON,
            null,
            context,
            MediaButtonReceiver::class.java
        )

        mediaSession?.setMediaButtonReceiver(
            PendingIntent.getBroadcast(
                context,
                0,
                mediaButtonIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        )

        initMediaPlayer()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        intentFilterUpdateData.addAction("StopIfCurrentTrack")
        intentFilterUpdateData.addAction("deleteTrack")
        intentFilterUpdateData.addAction("isRepeat")
        intentFilterUpdateData.addAction("startPlay")
        localBroadcastManager?.registerReceiver(updateDataReceiver, intentFilterUpdateData)
    }

    private val mediaCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {

            startService(Intent(context, ServicePlay::class.java))

            if (requestAudioFocus()) {

                if (mMediaPlayer == null) {
                    initMediaPlayer()
                }
                mediaSession?.isActive = true
                mediaSession?.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        pausePosition.toLong(),
                        1F
                    ).build()
                )
                updateData(PlaybackStateCompat.STATE_PLAYING)
                registerReceiver(becomingNoisyReceiver, intentFilterNOISY)
            }
        }

        override fun onPause() {
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (e: Exception) {
                //
            }
            pause()
            mediaSession?.setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_PAUSED,
                    pausePosition.toLong(),
                    1F
                )
                    .build()
            )
            NotificationManagerCompat.from(this@ServicePlay)
                .notify(notificationId, getNotification(PlaybackStateCompat.STATE_PAUSED))
        }

        override fun onSkipToNext() {

            when (mediaSession?.controller?.playbackState?.state) {
                PlaybackStateCompat.STATE_PAUSED -> {
                    mediaSession?.setPlaybackState(
                        stateBuilder.setState(
                            PlaybackStateCompat.STATE_PAUSED,
                            0,
                            1F
                        )
                            .build()
                    )
                    pausePosition = 0
                }
                else -> {
                    updateMetadata(-1)
                }
            }
            updateData(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)
        }

        override fun onSkipToPrevious() {

            when (mediaSession?.controller?.playbackState?.state) {
                PlaybackStateCompat.STATE_PAUSED -> {
                    mediaSession?.setPlaybackState(
                        stateBuilder.setState(
                            PlaybackStateCompat.STATE_PAUSED,
                            0,
                            1F
                        )
                            .build()
                    )
                    pausePosition = 0
                }
                else -> {
                    updateMetadata(-1)
                }
            }
            updateData(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS)
        }

        override fun onStop() {

            abandonAudioFocus()
            try {
                unregisterReceiver(becomingNoisyReceiver)
            } catch (e: Exception) {

            }

            this@ServicePlay.stopSelf()

            mediaSession?.isActive = false
            mediaSession?.setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_STOPPED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1F
                )
                    .build()
            )

            pausePosition = 0
            disableActiveItems()
            releasePlayer()
            this@ServicePlay.stopForeground(true)
        }

        override fun onSeekTo(pos: Long) {

            mMediaPlayer?.seekTo(pos.toInt())
            if (mediaSession?.controller?.playbackState?.state ==
                PlaybackStateCompat.STATE_PAUSED
            ) {
                mediaSession?.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        pos,
                        1F
                    )
                        .build()
                )
                pausePosition = pos.toInt()
            } else {
                mediaSession?.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        pos,
                        1F
                    )
                        .build()
                )
            }
        }
    }

    private fun updateMetadata(duration: Long = 0L) {
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack?.title)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentTrack?.album)
        metadataBuilder.putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            currentTrack?.artist
        )
        metadataBuilder.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            if (duration == 0L) parseDuration(currentTrack?.duration ?: "0") else duration
        )
        mediaSession!!.setMetadata(metadataBuilder.build())
    }

    private fun initMediaPlayer() {
        mMediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
        mMediaPlayer?.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer?.apply {
            setOnErrorListener(this@ServicePlay)
            setOnPreparedListener(this@ServicePlay)
            setOnCompletionListener(this@ServicePlay)
        }
    }

    private fun preparePlayer() {
        mMediaPlayer?.prepareAsync()
    }

    private fun setSource() {
        mMediaPlayer?.reset()
        val contentUri: Uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            currentTrack?.musicId?.toLong()!!
        )
        mMediaPlayer?.setDataSource(
            this@ServicePlay.applicationContext,
            contentUri
        )
    }

    fun startPlay() {

        if (currentTrack == null) {
            return
        }

        setSource()
        when (mediaSession?.controller?.playbackState?.state) {
            PlaybackStateCompat.STATE_PAUSED -> {

            }
            else -> {
                preparePlayer()
                mediaSession?.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PLAYING,
                        pausePosition.toLong(),
                        1F
                    ).build()
                )
            }
        }
        updateMetadata()
        this@ServicePlay.startForeground(
            notificationId,
            getNotification(
                mediaSession?.controller?.playbackState?.state ?: PlaybackStateCompat.STATE_NONE
            )
        )
    }

    fun pause() {

        mMediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                pausePosition = it.currentPosition
            }
        }
    }

    fun releasePlayer() {

        mMediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
        }
        mMediaPlayer?.reset()
        mMediaPlayer?.release()
        mMediaPlayer = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        updateData(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)
    }

    override fun onPrepared(mediaPlayer: MediaPlayer) {
        mediaPlayer.setVolume(1.0f, 1.0f)
        if (pausePosition > 0) {
            mediaPlayer.seekTo(pausePosition)
            pausePosition = 0
        }
        mediaPlayer.start()
    }

    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {

        when (what) {
            MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK $extra"
            )
            MEDIA_ERROR_SERVER_DIED -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR SERVER DIED $extra"
            )
            MEDIA_ERROR_UNKNOWN -> Log.d(
                "MediaPlayer Error",
                "MEDIA ERROR UNKNOWN $extra"
            )
        }
        releasePlayer()
        stopSelf()
        return false
    }

    override fun onDestroy() {
        abandonAudioFocus()
        releasePlayer()
        job.cancel()
        try {
            localBroadcastManager?.unregisterReceiver(updateDataReceiver)
        } catch (e: Exception) {
            //
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return ServicePlayerBinder()
    }


    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let {
                audioManager?.abandonAudioFocusRequest(focusRequest!!)
            }
        } else {
            audioManager?.abandonAudioFocus(this)
        }
    }

    fun requestAudioFocus(): Boolean {
        val listener = this
        val result: Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .run {
                            setAudioAttributes(AudioAttributes.Builder().run {
                                setUsage(AudioAttributes.USAGE_MEDIA)
                                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                build()
                            })
                            setOnAudioFocusChangeListener(listener)
                            build()
                        }
                audioManager?.requestAudioFocus(focusRequest!!)
            } else {
                audioManager?.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            } ?: -1234
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // mediaSession?.controller?.transportControls?.play()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                mediaSession?.controller?.transportControls?.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaSession?.controller?.transportControls?.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mMediaPlayer?.let {
                    if (it.isPlaying) {
                        it.setVolume(0.2f, 0.2f)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "ServicePlayNotificationChannel$notificationId"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun getNotification(state: Int): Notification {

        val builder: NotificationCompat.Builder = fromHelper(this, mediaSession!!)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_skip_previous_36,
                    "previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_pause_36,
                    "pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                )
            )
        } else {
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_play_arrow_36,
                    "play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY
                    )
                )
            )
        }
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_skip_next_36,
                    "next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_clear_36,
                    "stop",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            this,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )

            )
            .setSmallIcon(R.drawable.ic_baseline_music_note_24)
            .setColor(
                ContextCompat.getColor(
                    this,
                    R.color.colorPrimaryLight
                )
            )
            .setOnlyAlertOnce(true)
            .setChannelId(channelId)

        return builder.build()
    }

    private fun updateData(action: Int) {

        when (action) {

            PlaybackStateCompat.STATE_PLAYING -> {

                val newJob = Job()
                job.add(newJob)
                val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                mCoroutineScope.launch {

                    if (databaseIsBlock) {
                        delay(100)
                    }
                    databaseIsBlock = true

                    if (databaseMusic.getFirstActive() == null) {
                        currentTrack = databaseMusic.getItemASC()
                        currentTrack?.active = 1
                        currentTrack?.let { databaseMusic.update(it) }
                        localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                        databaseIsBlock = false
                        return@launch
                    } else {
                        if (databaseMusic.getPlaylistByActive(1)?.size == 1
                        ) {
                            currentTrack = databaseMusic.getFirstActive()
                            localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                            databaseIsBlock = false
                            return@launch
                        }
                    }

                    currentTrack = databaseMusic.getFirstActive()
                    if (currentTrack == null) {
                        databaseIsBlock = false
                        return@launch
                    }
                    val sortList = mutableListOf<MusicTrack>()
                    databaseMusic.sortByActiveAndWithoutItem(currentTrack!!.musicTrackId)
                        ?.let { sortList.addAll(it) }
                    sortList.forEach {
                        it.active = 0
                    }

                    sortList.add(0, currentTrack!!)
                    val insertList = mutableListOf<MusicTrack>()
                    sortList.forEach {
                        val item = MusicTrack()
                        item.myCopy(it)
                        item.active = it.active
                        item.playListName = StringKey.currentList
                        insertList.add(item)
                    }

                    insertList.forEach {
                        Log.e("sdsfsfsfsf", " service ${it.musicTrackId}")
                    }

                    databaseMusic.listDelByNameInsByList(StringKey.currentList, insertList)
                    databaseIsBlock = false
                    localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                }
            }

            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {

                val newJob = Job()
                job.add(newJob)
                val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                mCoroutineScope.launch {

                    if (databaseIsBlock) {
                        delay(100)
                    }
                    databaseIsBlock = true
                    databaseMusic.getPlaylistByActive(1)?.forEach {
                        it.active = 0
                        databaseMusic.update(it)
                    }

                    val curTrack =
                        currentTrack?.musicId?.let { databaseMusic.getTrackByMusicID(it) }
                    val nextTrack = curTrack?.musicTrackId?.let {
                        databaseMusic.getNextTrackByKey(
                            it
                        )
                    }
                    if (nextTrack == null) {
                        if (isRepeat) {
                            currentTrack = databaseMusic.getItemASC()
                            currentTrack?.let {
                                it.active = 1
                                databaseMusic.update(it)
                                databaseIsBlock = false
                                localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                            }
                            return@launch
                        } else {
                            pausePosition = 0
                            mediaSession?.controller?.transportControls?.pause()
                            return@launch
                        }
                    } else {
                        currentTrack = nextTrack
                        currentTrack?.let {
                            it.active = 1
                            databaseMusic.update(it)
                            localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                        }
                    }
                    databaseIsBlock = false
                }
            }
            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> {

                val newJob = Job()
                job.add(newJob)
                val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                mCoroutineScope.launch {

                    if (databaseIsBlock) {
                        delay(100)
                    }
                    databaseIsBlock = true
                    if (currentTrack == null) {
                        databaseIsBlock = false
                        return@launch
                    }

                    databaseMusic.getPlaylistByActive(1)?.forEach {
                        it.active = 0
                        databaseMusic.update(it)
                    }

                    val curTrack =
                        currentTrack?.musicId?.let { databaseMusic.getTrackByMusicID(it) }
                    val previousTrack = curTrack?.musicTrackId?.let {
                        databaseMusic.getPreviousTrackByKey(
                            it
                        )
                    }

                    if (previousTrack == null) {
                        if (isRepeat) {
                            currentTrack = databaseMusic.getItemDESC()
                            currentTrack?.let {
                                it.active = 1
                                databaseMusic.update(it)
                                databaseIsBlock = false
                                localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                            }
                            return@launch
                        } else {
                            pausePosition = 0
                            mediaSession?.controller?.transportControls?.pause()
                            return@launch

                        }
                    } else {
                        currentTrack = previousTrack
                        currentTrack?.let {
                            it.active = 1
                            databaseMusic.update(it)
                            localBroadcastManager?.sendBroadcast(Intent("UpdateCurrentTrack"))
                        }
                    }
                    databaseIsBlock = false
                }
            }
        }
    }

    private fun disableActiveItems() {

        val newJob = Job()
        job.add(newJob)
        val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
        mCoroutineScope.launch {

            if (databaseIsBlock) {
                delay(100)
            }
            databaseIsBlock = true
            val disableList = databaseMusic.getPlaylistByActive(1)
            disableList?.forEach {
                it.active = 0
                databaseMusic.update(it)
            }
            databaseIsBlock = false
        }
    }

    private fun parseDuration(s: String): Long {

        val list = s.split(":")
        return if (list.size == 1) {
            list[0].toLong() * 60 * 1000
        } else {
            list[0].toLong() * 60 * 1000 + list[1].toLong() * 1000
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_AUDIO_BECOMING_NOISY) {
                mediaCallback.onPause()
            }
        }
    }

    private val updateDataReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "UpdateCurrentTrack") {
                startPlay()
            }
            if (intent.action == "StopIfCurrentTrack") {
                val id = intent.getStringExtra("id")
                if (!id.isNullOrEmpty() && id == currentTrack?.musicId) {
                    mediaSession?.controller?.transportControls?.stop()
                }
            }
            if (intent.action == "deleteTrack") {
                val deleteArray = intent.getStringArrayExtra("deleteArray")
                deleteArray?.forEach {
                    if (it == currentTrack?.musicId) {
                        mediaSession?.controller?.transportControls?.stop()
                        return@forEach
                    }
                }
            }
            if (intent.action == "isRepeat") {
                isRepeat = intent.getBooleanExtra("isRepeat", false)
            }
            if (intent.action == "startPlay") {

                val state = mediaSession?.controller?.playbackState?.state

                when (intent.getStringExtra("startPlay")) {

                    "start" -> {
                        if (state != PlaybackStateCompat.STATE_PAUSED
                            && state != PlaybackStateCompat.STATE_PLAYING
                        ) {
                            mediaSession?.controller?.transportControls?.play()
                        }
                    }
                    "stopStart" -> {
                        mediaSession?.controller?.transportControls?.stop()
                        mediaSession?.controller?.transportControls?.play()
                    }
                }

            }
        }
    }

    inner class ServicePlayerBinder : Binder() {
        fun getMediaSessionToken() = mediaSession?.sessionToken
    }
}

// from https://habr.com/ru/company/oleg-bunin/blog/429908/
class CompositeJob {

    private val map = hashMapOf<String, Job>()

    fun add(job: Job, key: String = job.hashCode().toString()) = map.put(key, job)?.cancel()

    fun cancel(key: String) = map[key]?.cancel()

    fun cancel() = map.forEach { (_, u) -> u.cancel() }
}