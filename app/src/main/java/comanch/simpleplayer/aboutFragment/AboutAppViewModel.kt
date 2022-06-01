package comanch.simpleplayer.aboutFragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AboutAppViewModel @Inject constructor() : ViewModel() {

    private val _ossLicense = MutableLiveData<Int?>()
    val ossLicense: LiveData<Int?>
        get() = _ossLicense

    fun ossLicense() {
        _ossLicense.value = 1
    }
}