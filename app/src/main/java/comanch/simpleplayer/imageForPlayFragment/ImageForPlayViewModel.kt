package comanch.simpleplayer.imageForPlayFragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import comanch.simpleplayer.helpers.CompositeJob
import comanch.simpleplayer.helpers.LiveDataEvent
import comanch.simpleplayer.interfaces.SeekBarControlViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ImageForPlayViewModel @Inject constructor() : ViewModel(), SeekBarControlViewModel {

    override val scope: CoroutineScope = viewModelScope
    override val compositeJob: CompositeJob = CompositeJob()
    override var isCheckPos = false
    override var checkPosOpen = MutableLiveData<LiveDataEvent<Int?>>()
    override var checkPosOnceOpen = MutableLiveData<LiveDataEvent<Int?>>()
    override var checkCurrentPosJob: Job = Job()
    override var checkCurrentPosJobName = ""

    private var isControlPanelVisible: Boolean? = null
    private var playPause: Boolean? = null

    private val _setPlayScreenImage = MutableLiveData<LiveDataEvent<Int?>>()
    val setPlayScreenImage: LiveData<LiveDataEvent<Int?>>
        get() = _setPlayScreenImage

    private val _setPlayPauseVisible = MutableLiveData<LiveDataEvent<Boolean?>>()
    val setPlayPauseVisible: LiveData<LiveDataEvent<Boolean?>>
        get() = _setPlayPauseVisible

    private val _setControlPanelAnimation = MutableLiveData<LiveDataEvent<Boolean?>>()
    val setControlPanelAnimation: LiveData<LiveDataEvent<Boolean?>>
        get() = _setControlPanelAnimation

    private val _setControlPanelVisible = MutableLiveData<LiveDataEvent<Boolean?>>()
    val setControlPanelVisible: LiveData<LiveDataEvent<Boolean?>>
        get() = _setControlPanelVisible

    private val _start = MutableLiveData<LiveDataEvent<Int?>>()
    val start: LiveData<LiveDataEvent<Int?>>
        get() = _start

    fun setControlPanelVisible(b: Boolean){
        isControlPanelVisible = b
        _setControlPanelAnimation.value = LiveDataEvent(b)
    }

    fun setPlayPauseVisible(b: Boolean){
        playPause = b
        _setPlayPauseVisible.value = LiveDataEvent(b)
    }

    fun setPlayPauseVisible(){
        _setPlayPauseVisible.value = LiveDataEvent(playPause)
    }

    fun setControlPanelVisible(){
        if (isControlPanelVisible == false) {
            _setControlPanelAnimation.value = LiveDataEvent(isControlPanelVisible)
        }
    }

    fun setPlayScreenImage(){
        viewModelScope.launch {
            _setPlayScreenImage.value = LiveDataEvent(1)
        }
    }
}


