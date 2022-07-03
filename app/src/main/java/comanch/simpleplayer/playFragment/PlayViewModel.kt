package comanch.simpleplayer.playFragment

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import comanch.simpleplayer.helpers.CompositeJob
import comanch.simpleplayer.interfaces.InactiveScreenControlViewModel
import comanch.simpleplayer.helpers.LiveDataEvent
import comanch.simpleplayer.interfaces.SeekBarControlViewModel
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.dataBase.MusicTrackDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayViewModel @Inject constructor(private val databaseMusic: MusicTrackDAO) : ViewModel(),
    SeekBarControlViewModel, InactiveScreenControlViewModel {

    val items = databaseMusic.getCurrentItems()

    override val scope: CoroutineScope = viewModelScope
    override val compositeJob: CompositeJob = CompositeJob()
    override var isCheckPos = false
    override var checkPosOpen = MutableLiveData<LiveDataEvent<Int?>>()
    override var checkPosOnceOpen = MutableLiveData<LiveDataEvent<Int?>>()
    override var checkCurrentPosJob: Job = Job()
    override var checkCurrentPosJobName = ""

    override val compositeJobInactiveScreen: CompositeJob = CompositeJob()
    override var checkScreenInactiveJob: Job = Job()
    override var checkScreenInactiveOpen = MutableLiveData<LiveDataEvent<Int?>>()
    override var checkScreenInactiveJobName = ""
    override var navigateToImageScreenOpen = MutableLiveData<LiveDataEvent<Int?>>()

    private val _scrollList = MutableLiveData<LiveDataEvent<Int?>>()
    val scrollList: LiveData<LiveDataEvent<Int?>>
        get() = _scrollList

    private val _setPlayPauseVisible = MutableLiveData<LiveDataEvent<Boolean?>>()
    val setPlayPauseVisible: LiveData<LiveDataEvent<Boolean?>>
        get() = _setPlayPauseVisible

    private val _trackActiveUpdate = MutableLiveData<LiveDataEvent<Int?>>()
    val trackActiveUpdate: LiveData<LiveDataEvent<Int?>>
        get() = _trackActiveUpdate

    private val _stopStartPlayNewTrack = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val stopStartPlayNewTrack: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _stopStartPlayNewTrack

    private val _sendStop = MutableLiveData<LiveDataEvent<String?>>()
    val sendStop: LiveData<LiveDataEvent<String?>>
        get() = _sendStop

    private val _click = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val click: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _click

    private val _playClick = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val playClick: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _playClick

    fun setPlayPauseVisible(b: Boolean) {
        _setPlayPauseVisible.value = LiveDataEvent(b)
    }

    fun onItemClicked(item: MusicTrack) {
        _click.value = LiveDataEvent(item)
    }

    fun onPlayClicked(item: MusicTrack) {
        _playClick.value = LiveDataEvent(item)
    }

    fun trackActiveEnable(musicTrack: MusicTrack) {
        viewModelScope.launch {
            musicTrack.active = 1
            musicTrack.isButtonPlayVisible = 1
            if (databaseMusic.update(musicTrack) > 0) {
                _trackActiveUpdate.value = LiveDataEvent(musicTrack.position)
            }
        }
    }

    fun trackActiveDisable(musicTrack: MusicTrack) {
        viewModelScope.launch {
            musicTrack.active = 0
            musicTrack.isButtonPlayVisible = 0
            databaseMusic.update(musicTrack)
            if (databaseMusic.update(musicTrack) > 0) {
                _trackActiveUpdate.value = LiveDataEvent(musicTrack.position)
                _sendStop.value = LiveDataEvent(musicTrack.musicId)
            }
        }
    }

    fun buttonPlayDisableAndStart(musicTrack: MusicTrack) {
        viewModelScope.launch {
            musicTrack.isButtonPlayVisible = 0
            if (databaseMusic.update(musicTrack) > 0) {
                _trackActiveUpdate.value = LiveDataEvent(musicTrack.position)
                _stopStartPlayNewTrack.value = LiveDataEvent(musicTrack)
            }
        }
    }

    fun scrollList() {
        viewModelScope.launch {
            val pos = databaseMusic.getPosition()?.toInt() ?: return@launch
            delay(200)
            _scrollList.value = LiveDataEvent(pos)
        }
    }
}



