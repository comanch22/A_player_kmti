package comanch.simpleplayer.helpers

import kotlinx.coroutines.Job

// from https://habr.com/ru/company/oleg-bunin/blog/429908/
class CompositeJob {

    val map = hashMapOf<String, Job>()

    fun add(job: Job): String{

        val key = job.hashCode().toString()
        map.put(key, job)?.cancel()
        return key
    }

    fun cancel(key: String){
        map[key]?.cancel()
        map.remove(key)
    }

    fun cancel() {
        map.forEach { (_, job) -> job.cancel() }
        map.clear()
    }
}