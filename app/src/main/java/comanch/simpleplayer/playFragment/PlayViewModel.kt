package comanch.simpleplayer.playFragment

import android.util.Log
import androidx.lifecycle.*
import comanch.simpleplayer.LiveDataEvent
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.dataBase.MusicTrackDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PlayViewModel @Inject constructor(private val databaseMusic: MusicTrackDAO) : ViewModel() {

    val items = databaseMusic.getCurrentItems()
    private var isCheckPos = false

    private var _start = MutableLiveData<LiveDataEvent<Int?>>()
    val start: LiveData<LiveDataEvent<Int?>>
        get() = _start

    private var _checkPos = MutableLiveData<LiveDataEvent<Int?>>()
    val checkPos: LiveData<LiveDataEvent<Int?>>
        get() = _checkPos

    private var _scrollList = MutableLiveData<LiveDataEvent<Int?>>()
    val scrollList: LiveData<LiveDataEvent<Int?>>
        get() = _scrollList

    private var _sendStop = MutableLiveData<LiveDataEvent<String?>>()
    val sendStop: LiveData<LiveDataEvent<String?>>
        get() = _sendStop

    private var _longStart = MutableLiveData<LiveDataEvent<Int?>>()
    val longStart: LiveData<LiveDataEvent<Int?>>
        get() = _longStart

    private var _longStop = MutableLiveData<LiveDataEvent<Int?>>()
    val longStop: LiveData<LiveDataEvent<Int?>>
        get() = _longStop

    private var _click = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val click: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _click

    private var _longClick = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val longClick: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _longClick

    private var _getActiveListForDelete = MutableLiveData<LiveDataEvent<MutableList<MusicTrack>?>>()
    val getActiveListForDelete: LiveData<LiveDataEvent<MutableList<MusicTrack>?>>
        get() = _getActiveListForDelete

    fun onItemClicked(item: MusicTrack) {
        _click.value = LiveDataEvent(item)
    }

    fun onItemLongClicked(item: MusicTrack): Boolean {
        _longClick.value = LiveDataEvent(item)
        return true
    }

    fun trackActiveEnable(musicTrack: MusicTrack) {
        viewModelScope.launch {
            musicTrack.active = 1
            databaseMusic.update(musicTrack)
        }
    }

    fun trackActiveLongEnable(musicTrack: MusicTrack) {
        viewModelScope.launch {
            _longStop.value = LiveDataEvent(1)
            delay(200)
            musicTrack.active = 1
            databaseMusic.update(musicTrack)
            _longStart.value = LiveDataEvent(1)
        }
    }

    fun trackActiveDisable(musicTrack: MusicTrack) {
        viewModelScope.launch {
            musicTrack.active = 0
            databaseMusic.update(musicTrack)
            _sendStop.value = LiveDataEvent(musicTrack.musicId)
        }
    }

    fun setIsCheckPos(b: Boolean) {
        isCheckPos = b
    }

    fun startCheckCurrentPos() {
        if (isCheckPos) {
            viewModelScope.launch {
                delay(500)
                _checkPos.value = LiveDataEvent(1)
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

    fun startCheckCurrentPosOnce() {
        viewModelScope.launch {
            _checkPos.value = LiveDataEvent(1)
        }
    }
}


