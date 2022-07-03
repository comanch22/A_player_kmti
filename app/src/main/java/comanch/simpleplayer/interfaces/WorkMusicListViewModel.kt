package comanch.simpleplayer.interfaces

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import comanch.simpleplayer.helpers.LiveDataEvent
import comanch.simpleplayer.dataBase.MusicTrack

interface WorkMusicListViewModel {

    val clickOpen: MutableLiveData<LiveDataEvent<MusicTrack?>>
    val click: LiveData<LiveDataEvent<MusicTrack?>>
        get() = clickOpen

    val setMusicActiveOpen: MutableLiveData<LiveDataEvent<Int?>>
    val setMusicActive: LiveData<LiveDataEvent<Int?>>
        get() = setMusicActiveOpen


    fun onItemClicked(item: MusicTrack) {
        clickOpen.value = LiveDataEvent(item)
    }

    fun setMusicActive(){
        setMusicActiveOpen.value = LiveDataEvent(1)
    }
}