package comanch.simpleplayer.detailFragment

import androidx.lifecycle.*
import comanch.simpleplayer.LiveDataEvent
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.dataBase.MusicTrackDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class DetailViewModel @Inject constructor() : ViewModel() {

    private var _toast = MutableLiveData<LiveDataEvent<String?>>()
    val toast: LiveData<LiveDataEvent<String?>>
        get() = _toast

    private var _click = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val click: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _click

    private val _setMusicActive = MutableLiveData<LiveDataEvent<Int?>>()
    val setMusicActive: LiveData<LiveDataEvent<Int?>>
        get() = _setMusicActive


    fun onItemClicked(item: MusicTrack) {
        _click.value = LiveDataEvent(item)
    }

    fun setMusicActive(){
        _setMusicActive.value = LiveDataEvent(1)
    }
}


