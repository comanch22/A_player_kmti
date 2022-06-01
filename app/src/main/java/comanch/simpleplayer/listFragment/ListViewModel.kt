package comanch.simpleplayer.listFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import comanch.simpleplayer.LiveDataEvent
import comanch.simpleplayer.dataBase.MusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor() : ViewModel() {

    private val _navigateToDetailFragment = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val navigateToDetailFragment: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _navigateToDetailFragment

    private val _click = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val click: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _click

    private val _loadFolders = MutableLiveData<LiveDataEvent<Int?>>()
    val loadFolders: LiveData<LiveDataEvent<Int?>>
        get() = _loadFolders

    private val _setFolderActive = MutableLiveData<LiveDataEvent<Int?>>()
    val setFolderActive: LiveData<LiveDataEvent<Int?>>
        get() = _setFolderActive

    fun openFolder(item: MusicTrack) {
        _navigateToDetailFragment.value = LiveDataEvent(item)
    }

    fun click(item: MusicTrack) {
        _click.value = LiveDataEvent(item)
    }

    fun setFolderActive() {
        _setFolderActive.value = LiveDataEvent(1)
    }

    fun startLoadFolders() {
        _loadFolders.value = LiveDataEvent(1)
    }
}
