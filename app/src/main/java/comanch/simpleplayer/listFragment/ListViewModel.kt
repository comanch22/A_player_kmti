package comanch.simpleplayer.listFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import comanch.simpleplayer.helpers.LiveDataEvent
import comanch.simpleplayer.interfaces.WorkMusicListViewModel
import comanch.simpleplayer.dataBase.MusicTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ListViewModel @Inject constructor() : ViewModel(), WorkMusicListViewModel {

    override val clickOpen = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    override val setMusicActiveOpen = MutableLiveData<LiveDataEvent<Int?>>()

    private val _navigateToDetailFragment = MutableLiveData<LiveDataEvent<MusicTrack?>>()
    val navigateToDetailFragment: LiveData<LiveDataEvent<MusicTrack?>>
        get() = _navigateToDetailFragment

    private val _loadFolders = MutableLiveData<LiveDataEvent<Int?>>()
    val loadFolders: LiveData<LiveDataEvent<Int?>>
        get() = _loadFolders

    fun openFolder(item: MusicTrack) {
        _navigateToDetailFragment.value = LiveDataEvent(item)
    }

    fun startLoadFolders() {
        _loadFolders.value = LiveDataEvent(1)
    }
}
