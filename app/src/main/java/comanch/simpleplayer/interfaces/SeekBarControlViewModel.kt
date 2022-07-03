package comanch.simpleplayer.interfaces

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import comanch.simpleplayer.helpers.CompositeJob
import comanch.simpleplayer.helpers.LiveDataEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface SeekBarControlViewModel {

    var isCheckPos: Boolean
    val scope: CoroutineScope

    val checkPosOpen: MutableLiveData<LiveDataEvent<Int?>>
    val checkPos: LiveData<LiveDataEvent<Int?>>
        get() = checkPosOpen

    val checkPosOnceOpen: MutableLiveData<LiveDataEvent<Int?>>
    val checkPosOnce: LiveData<LiveDataEvent<Int?>>
        get() = checkPosOnceOpen

    val compositeJob: CompositeJob

    var checkCurrentPosJob: Job
    var checkCurrentPosJobName: String

    fun setIsCheckPos(b: Boolean) {
        isCheckPos = b
    }

    fun addCurrentPosJob(){
        checkCurrentPosJob = Job()
        checkCurrentPosJobName = compositeJob.add(checkCurrentPosJob)
    }

    fun cancelJobCycleCoroutines(){
        compositeJob.cancel()
    }

    fun cancelCheckCurrentPosCoroutine(){
        compositeJob.cancel(checkCurrentPosJobName)
    }

    fun startCheckCurrentPos(){
        if (isCheckPos) {
            scope.launch(checkCurrentPosJob) {
                delay(500)
                checkPosOpen.value = LiveDataEvent(1)
            }
        }
    }

    fun startCheckCurrentPosOnce() {
        scope.launch {
            checkPosOnceOpen.value = LiveDataEvent(1)
        }
    }
}