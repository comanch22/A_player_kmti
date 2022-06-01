package comanch.simpleplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import comanch.simpleplayer.dataBase.MusicTrack
import comanch.simpleplayer.dataBase.MusicTrackDAO
import comanch.simpleplayer.dataBase.PlayList
import comanch.simpleplayer.dataBase.PlayListDAO
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StateViewModel @Inject constructor(
    private val databaseMusic: MusicTrackDAO,
    private val databasePlayList: PlayListDAO
) : ViewModel() {

    private val musicActiveList = mutableListOf<MusicTrack>()
    private var sortFlag = 0

    private val _deleteArray = MutableLiveData<LiveDataEvent<Array<String>?>>()
    val deleteArray: LiveData<LiveDataEvent<Array<String>?>>
        get() = _deleteArray

    private val _startPlay = MutableLiveData<LiveDataEvent<Int?>>()
    val startPlay: LiveData<LiveDataEvent<Int?>>
        get() = _startPlay

    private val _navigationToPlay = MutableLiveData<LiveDataEvent<Int?>>()
    val navigationToPlay: LiveData<LiveDataEvent<Int?>>
        get() = _navigationToPlay

    private val _toast = MutableLiveData<LiveDataEvent<String?>>()
    val toast: LiveData<LiveDataEvent<String?>>
        get() = _toast

    private val _musicListIsEmpty = MutableLiveData<LiveDataEvent<Boolean?>>()
    val musicListIsEmpty: LiveData<LiveDataEvent<Boolean?>>
        get() = _musicListIsEmpty

    private val _isPlaying = MutableLiveData<LiveDataEvent<Boolean?>>()
    val isPlaying: LiveData<LiveDataEvent<Boolean?>>
        get() = _isPlaying

    private val _containedInMusicList = MutableLiveData<LiveDataEvent<List<MusicTrack>?>>()
    val containedInMusicList: LiveData<LiveDataEvent<List<MusicTrack>?>>
        get() = _containedInMusicList

    private val _musicList = MutableLiveData<LiveDataEvent<MutableList<MusicTrack>?>>()
    val musicList: LiveData<LiveDataEvent<MutableList<MusicTrack>?>>
        get() = _musicList

    fun addMusicTrack(musicTrack: MusicTrack) {
        musicActiveList.add(musicTrack)
    }

    fun removeMusicTrackFromList(musicTrack: MusicTrack) {
        musicActiveList.remove(musicTrack)
    }

    fun musicListIsEmpty() {
        _musicListIsEmpty.value = LiveDataEvent(musicActiveList.isNullOrEmpty())
    }

    fun containedInMusicList(listTrack: List<MusicTrack>) {

        val activeList = mutableListOf<MusicTrack>()
        listTrack.forEach {
            if (musicActiveList.contains(it)) {
                activeList.add(it)
            }
        }
        if (activeList.isNotEmpty()) {
            _containedInMusicList.value = LiveDataEvent(activeList)
        }
    }

    fun getMusicList() {
        _musicList.value = LiveDataEvent(musicActiveList)
    }

    fun setIsPlaying(b: Boolean) {
        _isPlaying.value = LiveDataEvent(b)
    }

    fun sortCurrentList() {

        if (sortFlag == 4) {
            sortFlag = 1
        } else {
            sortFlag++
        }

        viewModelScope.launch {

            val currentList = databaseMusic.getPlaylistByName(StringKey.currentList)
            var sortList = listOf<MusicTrack>()
            currentList?.let { list ->

                when (sortFlag) {
                    1 -> sortList = list.sortedBy { it.title }
                    2 -> sortList = list.sortedBy { it.artist }
                    3 -> sortList = list.sortedBy { it.album }
                    4 -> sortList = list.sortedBy { it.musicTrackId }
                }

                val insertList = mutableListOf<MusicTrack>()
                sortList.forEach {
                    val item = MusicTrack()
                    item.myCopy(it)
                    item.active = it.active
                    item.playListName = StringKey.currentList
                    insertList.add(item)
                }
                databaseMusic.listDelByNameInsByList(StringKey.currentList, insertList.toList())
            }
        }
    }

    fun shuffleCurrentList() {
        viewModelScope.launch {

            val currentList = databaseMusic.getPlaylistByName(StringKey.currentList)
            currentList?.let {
                val shuffleList = currentList.shuffled()
                val insertList = mutableListOf<MusicTrack>()
                shuffleList.forEach {
                    val item = MusicTrack()
                    item.myCopy(it)
                    item.active = it.active
                    item.playListName = StringKey.currentList
                    insertList.add(item)
                }
                databaseMusic.listDelByNameInsByList(StringKey.currentList, insertList.toList())
            }
        }
    }

    fun setCurrentPlaylist(list: List<MusicTrack>) {

        clearMusicActiveList()

        viewModelScope.launch {

            checkCurrentPlaylist()
            val insertList = mutableListOf<MusicTrack>()
            list.forEach {
                val item = MusicTrack()
                item.myCopy(it)
                item.playListName = StringKey.currentList
                insertList.add(item)
            }
            if (insertList.isNotEmpty()) {
                databaseMusic.deletePlaylist(StringKey.currentList)
                _navigationToPlay.value = LiveDataEvent(1)
                databaseMusic.insertPlayList(insertList.toList())
            } else {
                _navigationToPlay.value = LiveDataEvent(1)
            }
            delay(200)
            if (insertList.isNotEmpty()) {
                _startPlay.value = LiveDataEvent(2)
            } else {
                _startPlay.value = LiveDataEvent(1)
            }
        }
    }

    fun savePlaylist(name: String) {

        viewModelScope.launch {

            databasePlayList.getCount()?.let {
                if (it >= 20) {
                    _toast.value = LiveDataEvent("max")
                    return@launch
                }
            }
            val newPlaylist = PlayList()
            newPlaylist.name = name
            databasePlayList.delByNameAdd(name, newPlaylist)

            val insertList = mutableListOf<MusicTrack>()
            databaseMusic.getPlaylistByName(StringKey.currentList)?.forEach {
                val item = MusicTrack()
                item.myCopy(it)
                item.playListName = name
                insertList.add(item)
            }
            databaseMusic.listDelByNameInsByList(name, insertList.toList())
        }
    }

    fun deleteItems() {

        viewModelScope.launch {
            val deleteList = mutableListOf<String>()
            databaseMusic.getPlaylistByName(StringKey.currentList)?.forEach {
                if (it.active == 1) {
                    deleteList.add(it.musicId)
                    databaseMusic.delete(it)
                }
            }
            _deleteArray.value = LiveDataEvent(deleteList.toTypedArray())
        }
    }

    fun setPlaylistAndPlay(playListActive: List<String>) {

        val newList = mutableListOf<MusicTrack>()

        viewModelScope.launch {
            playListActive.forEach {
                databaseMusic.getPlaylistByName(it)?.let { list ->
                    newList.addAll(
                        list
                    )
                }
            }
            setCurrentPlaylist(newList)
        }
    }

    fun deletePlayList(names: List<String>) {

        viewModelScope.launch {

            names.forEach { name ->

                databasePlayList.getByName(name)?.let {
                    databasePlayList.delete(it)
                }
                databaseMusic.deletePlaylist(name)
            }
        }
    }

    private fun checkCurrentPlaylist() {

        viewModelScope.launch {

            val playList = databasePlayList.getByName(StringKey.currentList)

            if (playList == null) {
                val newPlaylist = PlayList()
                newPlaylist.name = StringKey.currentList
                databasePlayList.insert(newPlaylist)
            }
        }
    }

    private fun clearMusicActiveList() {
        musicActiveList.clear()
    }
}

