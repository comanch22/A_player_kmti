package comanch.simpleplayer.interfaces

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import comanch.simpleplayer.helpers.CompositeJob
import comanch.simpleplayer.helpers.LiveDataEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface InactiveScreenControlViewModel {

    val scope: CoroutineScope

    val compositeJobInactiveScreen: CompositeJob
    var checkScreenInactiveJob: Job
    var checkScreenInactiveJobName: String

    val checkScreenInactiveOpen: MutableLiveData<LiveDataEvent<Int?>>
    val checkScreenInactive: LiveData<LiveDataEvent<Int?>>
        get() = checkScreenInactiveOpen

    val navigateToImageScreenOpen: MutableLiveData<LiveDataEvent<Int?>>
    val navigateToImageScreen: LiveData<LiveDataEvent<Int?>>
        get() = navigateToImageScreenOpen

    fun addScreenInactiveJob() {

        checkScreenInactiveJob = Job()
        checkScreenInactiveJobName = compositeJobInactiveScreen.add(checkScreenInactiveJob)
    }

    fun startCheckScreenInactive(isScreensaver: Boolean) {

        scope.launch(checkScreenInactiveJob) {
            delay(2000)
            if (isScreensaver) {
                navigateToImageScreenOpen.value = LiveDataEvent(1)
            } else {
                checkScreenInactiveOpen.value = LiveDataEvent(1)
            }
        }
    }

    fun cancelCheckScreenInactiveCoroutine() {
        compositeJobInactiveScreen.cancel(checkScreenInactiveJobName)
    }
}