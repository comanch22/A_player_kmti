package comanch.simpleplayer.playListFragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import comanch.simpleplayer.helpers.LiveDataEvent
import comanch.simpleplayer.dataBase.PlayList
import comanch.simpleplayer.dataBase.PlayListDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayListViewModel @Inject constructor(databasePlayList: PlayListDAO
) : ViewModel() {

    val items = databasePlayList.getAllItems()
    private val playListActiveList = mutableListOf<String>()

    private val _playList = MutableLiveData<LiveDataEvent<MutableList<String>?>>()
    val playList: LiveData<LiveDataEvent<MutableList<String>?>>
        get() = _playList

    private val _playListDel = MutableLiveData<LiveDataEvent<MutableList<String>?>>()
    val playListDel: LiveData<LiveDataEvent<MutableList<String>?>>
        get() = _playListDel

    private val _click = MutableLiveData<LiveDataEvent<PlayList?>>()
    val click: LiveData<LiveDataEvent<PlayList?>>
        get() = _click

    fun onItemClicked(item: PlayList) {
        _click.value = LiveDataEvent(item)
    }

    fun addPlayList(playList: PlayList) {
        playListActiveList.add(playList.name)
    }

    fun removePlayListFromList(playList: PlayList) {
        playListActiveList.remove(playList.name)
    }

    fun delete() {
        _playListDel.value = LiveDataEvent(playListActiveList)
    }

    fun getPlaylists() {
        _playList.value = LiveDataEvent(playListActiveList)
    }
}


