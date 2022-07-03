package comanch.simpleplayer.imageFragment

import androidx.lifecycle.*
import comanch.simpleplayer.helpers.LiveDataEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageChoiceViewModel @Inject constructor() : ViewModel() {

    private val _setImagePreview = MutableLiveData<LiveDataEvent<Int?>>()
    val setImagePreview: LiveData<LiveDataEvent<Int?>>
        get() = _setImagePreview

    fun setImagePreview(){
        viewModelScope.launch {
            _setImagePreview.value = LiveDataEvent(1)
        }
    }
}


