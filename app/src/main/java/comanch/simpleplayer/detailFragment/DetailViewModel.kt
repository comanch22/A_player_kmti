package comanch.simpleplayer.detailFragment

import androidx.lifecycle.*
import comanch.simpleplayer.helpers.LiveDataEvent
import comanch.simpleplayer.interfaces.WorkMusicListViewModel
import comanch.simpleplayer.dataBase.MusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor() : ViewModel(), WorkMusicListViewModel {

    override val clickOpen = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    override val setMusicActiveOpen = MutableLiveData<LiveDataEvent<Int?>>()

    private val _toast = MutableLiveData<LiveDataEvent<String?>>()
    val toast: LiveData<LiveDataEvent<String?>>
        get() = _toast
}


