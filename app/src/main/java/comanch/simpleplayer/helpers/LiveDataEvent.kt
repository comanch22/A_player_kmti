// the example was taken from here
// https://gist.github.com/JoseAlcerreca/5b661f1800e1e654f07cc54fe87441af

package comanch.simpleplayer.helpers

open class LiveDataEvent<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    fun getContentIfNotHandled(): T? {

        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun getContent(): T = content
}