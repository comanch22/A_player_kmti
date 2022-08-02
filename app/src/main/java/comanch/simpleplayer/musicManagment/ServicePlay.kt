package comanch.simpleplayer.musicManagment

import android.app.Service
import android.app.PendingIntent
import android.app.Notification
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Intent
import android.content.Context
import android.content.IntentFilter
import android.content.ContentUris
import android.content.BroadcastReceiver
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.*
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer.*
import android.net.Uri
import android.os.Binder
import android.os.PowerManager
import android.os.IBinder
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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import comanch.simpleplayer.helpers.CompositeJob
import comanch.simpleplayer.MainActivity
import comanch.simpleplayer.R
import comanch.simpleplayer.helpers.StringKey
import comanch.simpleplayer.musicManagment.MediaStyleHelper.fromHelper
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.dataBase.MusicTrackDAO
import comanch.simpleplayer.preferences.DefaultPreference
import comanch.simpleplayer.preferences.PreferenceKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ServicePlay : Service(),
    OnPreparedListener,
    OnErrorListener,
    OnCompletionListener,
    OnAudioFocusChangeListener {

    @Inject
    lateinit var databaseMusic: MusicTrackDAO

    @Inject
    lateinit var preferences: DefaultPreference

    private val job: CompositeJob = CompositeJob()
    private var previewUri: Uri? = null
    private var largeIcon: Bitmap? = null
    private var keyArt: Bitmap? = null
    private var keyArtUri: Uri? = null

    private var mMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var pausePosition: Int = 0

    private val channelId = "playerKTMi service channel id"
    private val channelName = "service channel playerKTMi"
    private val notificationId = 17131415
    private val intentFilterNOISY = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
    private val intentFilterUpdateData = IntentFilter(StringKey.UpdateCurrentTrack)
    private var localBroadcastManager: LocalBroadcastManager? = null
    private var context: Context? = null
    private var currentTrack: MusicTrack? = null
    private var isRepeat: Int = 0

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

    companion object {

        var mediaSession: MediaSessionCompat? = null
        val servicePlayerBinder = Binder()
        fun getMediaSessionToken(): MediaSessionCompat.Token? {
            return mediaSession?.sessionToken
        }
    }

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

        try {
            previewUri = Uri.parse(preferences.getString(PreferenceKeys.previewUri))
            setLargeIcon()
        } catch (e: Exception) {
            //
        }

        keyArtUri = Uri.parse(preferences.getString(PreferenceKeys.playScreenUri))
        setKeyArt()

        val activityIntent = Intent(context, MainActivity::class.java)
        activityIntent.action = StringKey.openPlayList
        activityIntent.putExtra(StringKey.openPlayList, true)
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
        intentFilterUpdateData.addAction(StringKey.StopIfCurrentTrack)
        intentFilterUpdateData.addAction(StringKey.deleteTrack)
        intentFilterUpdateData.addAction(StringKey.isRepeat)
        intentFilterUpdateData.addAction(StringKey.startPlay)
        intentFilterUpdateData.addAction(StringKey.previewUriChange)
        intentFilterUpdateData.addAction(StringKey.screenUriChange)
        intentFilterUpdateData.addAction(StringKey.startPlayByItemButtonPlay)
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

            if (mediaSession?.controller?.playbackState?.state != PlaybackStateCompat.STATE_PLAYING) {
                return
            }

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
                //
            }
            this@ServicePlay.stopSelf()

            mediaSession?.isActive = false
            mediaSession?.setPlaybackState(
                stateBuilder.setState(
                    PlaybackStateCompat.STATE_STOPPED,
                    0,
                    1F
                )
                    .build()
            )

            pausePosition = 0

            disableCurrentTrack()
            releasePlayer()
            this@ServicePlay.stopForeground(true)
        }

        override fun onSeekTo(pos: Long) {

            mMediaPlayer?.seekTo(pos.toInt())

            when (mediaSession?.controller?.playbackState?.state) {
                PlaybackStateCompat.STATE_PAUSED -> {
                    mediaSession?.setPlaybackState(
                        stateBuilder.setState(
                            PlaybackStateCompat.STATE_PAUSED,
                            pos,
                            1F
                        )
                            .build()
                    )
                    pausePosition = pos.toInt()
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    mediaSession?.setPlaybackState(
                        stateBuilder.setState(
                            PlaybackStateCompat.STATE_PLAYING,
                            pos,
                            1F
                        )
                            .build()
                    )
                }
                else -> {
                    //
                }
            }
        }
    }

    private fun updateMetadata(duration: Long = 0L) {

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack?.title)
        metadataBuilder.putString(
            MediaMetadataCompat.METADATA_KEY_ARTIST,
            currentTrack?.artist
        )
        metadataBuilder.putLong(
            MediaMetadataCompat.METADATA_KEY_DURATION,
            if (duration == 0L) parseDuration(currentTrack?.duration ?: "0") else duration
        )

        if (keyArt != null) {
            try {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, keyArt)
            } catch (e: Exception) {
                //
            }
        }
        mediaSession?.setMetadata(metadataBuilder.build())
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
        mMediaPlayer?.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

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
                //
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
        mediaSession = null
        job.cancel()
        try {
            localBroadcastManager?.unregisterReceiver(updateDataReceiver)
        } catch (e: Exception) {
            //
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder {
        return servicePlayerBinder
    }


    private fun abandonAudioFocus() {

        focusRequest?.let {
            audioManager?.abandonAudioFocusRequest(focusRequest!!)
        }
    }

    fun requestAudioFocus(): Boolean {

        val listener = this
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
        val result: Int? = audioManager?.requestAudioFocus(focusRequest!!)

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (mediaSession?.controller?.playbackState?.state ==
                    PlaybackStateCompat.STATE_PAUSED
                ) {
                    mediaSession?.controller?.transportControls?.play()
                }
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
                    R.color.controlMain
                )
            )
            .setOnlyAlertOnce(true)
            .setChannelId(channelId)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }
        return builder.build()
    }

    private fun setLargeIcon(fromReceiver: Boolean = false) {

        if (previewUri != null) {
            try {
                Glide.with(this)
                    .asBitmap()
                    .load(previewUri)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            largeIcon = resource
                            if (fromReceiver) {
                                refreshNotification()
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            //
                        }

                    })
            } catch (e: Exception) {
                largeIcon = null
            }
        } else {
            largeIcon = null
        }
    }

    private fun refreshNotification() {

        try {
            mediaSession?.controller?.playbackState?.state?.let {
                NotificationManagerCompat.from(this@ServicePlay)
                    .notify(notificationId, getNotification(it))
            }
        } catch (e: Exception) {

        }
    }

    private fun updateData(action: Int) {

        when (action) {

            PlaybackStateCompat.STATE_PLAYING -> {

                val newJob = Job()
                job.add(newJob)
                val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                mCoroutineScope.launch {

                    if (databaseMusic.getCount() == 0) {
                        setNoneState()
                        return@launch
                    }

                    if (databaseMusic.getFirstActive() == null) {
                        playFirstItem()
                        return@launch
                    } else {
                        if (databaseMusic.getPlaylistByActive(1)?.size == 1) {
                            playOnlyOneActiveItem()
                            return@launch
                        }
                    }

                    currentTrack = databaseMusic.getFirstActive()

                    if (currentTrack == null) {
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
                        item.isPlaying = false
                        item.playListName = StringKey.currentList
                        insertList.add(item)
                    }

                    databaseMusic.listDelByNameInsNewList(StringKey.currentList, insertList)
                    currentTrack?.isPlaying = true
                    databaseMusic.update(currentTrack!!)
                    localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
                }
            }

            PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {

                val newJob = Job()
                job.add(newJob)
                val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                mCoroutineScope.launch {

                    val mediaSessionState = mediaSession?.controller?.playbackState?.state

                    if (databaseMusic.getCount() == 0) {
                        setNoneState()
                        return@launch
                    }

                    if (isRepeat == 2 && mediaSessionState != PlaybackStateCompat.STATE_STOPPED) {
                        currentTrack?.let {
                            it.active = 1
                            databaseMusic.update(it)
                            localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
                            return@launch
                        }
                    }

                    var currentTrackByMusId = databaseMusic.getIsPlaying()

                    if (currentTrackByMusId == null &&
                        (mediaSessionState != PlaybackStateCompat.STATE_PLAYING ||
                                mediaSessionState != PlaybackStateCompat.STATE_PAUSED)
                    ) {
                        currentTrackByMusId = databaseMusic.getFirstActive()
                        setInactiveForListItems()

                        currentTrackByMusId?.musicTrackId?.let {
                            val nextActiveTrack = databaseMusic.getNextTrackByKey(it)
                            nextActiveTrack?.let { track ->
                                track.active = 1
                                databaseMusic.update(track)
                                mediaSessionSetStopped()
                                return@launch
                            }
                        }
                    } else {
                        currentTrack = currentTrackByMusId
                    }

                    val nextTrack = currentTrackByMusId?.musicTrackId?.let {
                        databaseMusic.getNextTrackByKey(it)
                    }

                    setInactiveForListItems()

                    if (nextTrack == null && mediaSessionState != PlaybackStateCompat.STATE_STOPPED) {

                        when (isRepeat) {
                            0 -> {
                                pausePosition = 0
                                mediaSession?.controller?.transportControls?.stop()
                            }
                            1 -> {
                                currentTrack?.isPlaying = false
                                currentTrack?.active = 0
                                databaseMusic.update(currentTrack!!)
                                repeatList()
                            }
                            2 -> {
                                repeatTrack()
                            }
                        }
                    } else {
                        currentTrack?.let {
                            it.isPlaying = false
                            it.active = 0
                            if (databaseMusic.update(it) > 0) {
                                playNextTrack(nextTrack)
                            }
                        }
                    }
                }
            }

            PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS -> {

                val newJob = Job()
                job.add(newJob)
                val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                mCoroutineScope.launch {

                    val mediaSessionState = mediaSession?.controller?.playbackState?.state

                    if (databaseMusic.getCount() == 0) {
                        setNoneState()
                        return@launch
                    }

                    var currentTrackByMusId = databaseMusic.getIsPlaying()

                    if (currentTrackByMusId == null &&
                        (mediaSessionState != PlaybackStateCompat.STATE_PLAYING ||
                                mediaSessionState != PlaybackStateCompat.STATE_PAUSED)
                    ) {
                        currentTrackByMusId = databaseMusic.getFirstActive()
                        setInactiveForListItems()

                        currentTrackByMusId?.musicTrackId?.let {
                            val previousActiveTrack = databaseMusic.getPreviousTrackByKey(it)
                            previousActiveTrack?.let { track ->
                                track.active = 1
                                databaseMusic.update(track)
                                mediaSessionSetStopped()
                                return@launch
                            }
                        }
                    } else {
                        currentTrack = currentTrackByMusId
                    }

                    val previousTrack = currentTrackByMusId?.musicTrackId?.let {
                        databaseMusic.getPreviousTrackByKey(
                            it
                        )
                    }

                    setInactiveForListItems()

                    if (previousTrack == null) {
                        currentTrack?.let {
                            it.active = 1
                            databaseMusic.update(it)
                            localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
                        }
                    } else {
                        currentTrack?.isPlaying = false
                        currentTrack?.active = 0
                        if (databaseMusic.update(currentTrack!!) > 0) {
                            currentTrack = previousTrack
                            currentTrack?.let {
                                it.active = 1
                                it.isPlaying = true
                                databaseMusic.update(it)
                                localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun setInactiveForListItems() {
        
        databaseMusic.getPlaylistByActive(1)?.let {
            it.forEach { track ->
                track.active = 0
                track.isButtonPlayVisible = 0
            }
            databaseMusic.updatePlayList(it)
        }
    }

    private suspend fun playNextTrack(nextTrack: MusicTrack?) {

        currentTrack = nextTrack
        currentTrack?.let {
            it.active = 1
            it.isPlaying = true
            databaseMusic.update(it)
            localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
        }
    }

    private suspend fun repeatList() {

        currentTrack = databaseMusic.getItemASC()
        currentTrack?.let {
            it.active = 1
            it.isPlaying = true
            databaseMusic.update(it)
            localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
        }
    }

    private suspend fun repeatTrack() {

        currentTrack?.let {
            it.active = 1
            databaseMusic.update(it)
            localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
        }
    }

    private fun mediaSessionSetStopped() {

        mediaSession?.setPlaybackState(
            stateBuilder.setState(
                PlaybackStateCompat.STATE_STOPPED,
                0,
                1F
            )
                .build()
        )
    }

    private suspend fun playOnlyOneActiveItem() {

        currentTrack = databaseMusic.getFirstActive()
        currentTrack?.isButtonPlayVisible = 0
        currentTrack?.isPlaying = true
        currentTrack?.let { databaseMusic.update(it) }
        localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
    }

    private suspend fun playFirstItem() {

        currentTrack = databaseMusic.getItemASC()
        currentTrack?.active = 1
        currentTrack?.isPlaying = true
        currentTrack?.let { databaseMusic.update(it) }
        localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
    }

    private fun setNoneState() {

        currentTrack = null
        mediaSession?.isActive = false
        mediaSession?.setPlaybackState(
            stateBuilder.setState(
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                1F
            )
                .build()
        )
        try {
            unregisterReceiver(becomingNoisyReceiver)
        } catch (e: Exception) {
            //
        }
    }

    private fun disableCurrentTrack() {

        val newJob = Job()
        job.add(newJob)
        val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
        mCoroutineScope.launch {

            currentTrack?.isPlaying = false
            currentTrack?.active = 0
            currentTrack?.isButtonPlayVisible = 0
            currentTrack?.let { databaseMusic.update(it) }
            currentTrack = null
        }
    }

    private fun disableActiveItemsSetCurrentActive(id: Long) {

        val newJob = Job()
        job.add(newJob)
        val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
        mCoroutineScope.launch {

            databaseMusic.getPlaylistByActiveWithOutItem(1, id)?.let {
                it.forEach { track ->
                    track.active = 0
                    track.isButtonPlayVisible = 0
                }
                databaseMusic.updatePlayList(it)
            }
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
            when (intent.action) {
                StringKey.UpdateCurrentTrack -> {
                    if (mMediaPlayer == null) {
                        initMediaPlayer()
                    }
                    startPlay()
                }
                StringKey.StopIfCurrentTrack -> {
                    val id = intent.getStringExtra("id")
                    if (!id.isNullOrEmpty() && id == currentTrack?.musicId) {
                        val newJob = Job()
                        job.add(newJob)
                        val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                        mCoroutineScope.launch {

                            mediaSession?.controller?.transportControls?.stop()

                        }
                    }

                }
                StringKey.startPlayByItemButtonPlay -> {

                    val newJob = Job()
                    job.add(newJob)
                    val mCoroutineScope = CoroutineScope(newJob + Dispatchers.IO)
                    mCoroutineScope.launch {

                        currentTrack?.isPlaying = false
                        currentTrack?.active = 0
                        currentTrack?.let { databaseMusic.update(it) }
                        val id = intent.getLongExtra("id", -1)
                        mMediaPlayer?.stop()
                        if (id > -1) {
                            disableActiveItemsSetCurrentActive(id)
                            currentTrack = databaseMusic.getItemById(id)
                        }
                        currentTrack?.active = 1
                        currentTrack?.isPlaying = true
                        if (currentTrack?.let { databaseMusic.update(it) } != 0) {
                            currentTrack?.let {
                                pausePosition = 0
                                mediaSession?.setPlaybackState(
                                    stateBuilder.setState(
                                        PlaybackStateCompat.STATE_PLAYING,
                                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                                        1F
                                    )
                                        .build()
                                )
                                localBroadcastManager?.sendBroadcast(Intent(StringKey.UpdateCurrentTrack))
                            }
                        }
                    }
                }
                StringKey.deleteTrack -> {
                    val deleteArray = intent.getStringArrayExtra(StringKey.deleteArray)
                    deleteArray?.forEach {
                        if (it == currentTrack?.musicId) {
                            mediaSession?.controller?.transportControls?.stop()
                            return@forEach
                        }
                    }
                }
                StringKey.isRepeat -> {
                    isRepeat = intent.getIntExtra(StringKey.isRepeat, 0)
                }
                StringKey.startPlay -> {
                    val state = mediaSession?.controller?.playbackState?.state
                    when (intent.getStringExtra(StringKey.startPlay)) {
                        StringKey.start -> {
                            if (state != PlaybackStateCompat.STATE_PAUSED
                                && state != PlaybackStateCompat.STATE_PLAYING
                            ) {
                                mediaSession?.controller?.transportControls?.play()
                            }
                        }
                        StringKey.stopStart -> {
                            mediaSession?.controller?.transportControls?.stop()
                            mediaSession?.controller?.transportControls?.play()
                        }
                        StringKey.nothing -> {
                            if (state != PlaybackStateCompat.STATE_PLAYING) {
                                mediaSession?.controller?.transportControls?.play()
                            }
                        }
                    }
                }
                StringKey.previewUriChange -> {

                    intent.getStringExtra(StringKey.uri)?.let {
                        if (it.isNotEmpty()) {
                            previewUri = Uri.parse(it)
                            setLargeIcon(true)
                        }
                    }
                }
                StringKey.screenUriChange -> {

                    intent.getStringExtra(StringKey.uri)?.let {
                        if (it.isNotEmpty()) {
                            keyArtUri = Uri.parse(it)
                            setKeyArt()
                        }
                    }
                }
            }
        }
    }

    private fun setKeyArt() {

        try {
            Glide.with(this)
                .asBitmap()
                .load(keyArtUri)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        val dimension = (resource.height).coerceAtMost(resource.width)
                        keyArt = ThumbnailUtils.extractThumbnail(resource, dimension, dimension)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        //
                    }

                })
        } catch (e: Exception) {
            //
        }
    }
}