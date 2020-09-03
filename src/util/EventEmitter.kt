package util

open class EventEmitter<T> {
    private val handlers: MutableMap<String, MutableList<(T) -> Unit>> = HashMap()

    protected fun emit(type: String, event: T) {
        handlers[type]?.forEach { it(event) }
    }

    fun on(type: String, handler: (T) -> Unit) {
        if (handlers[type] === null) {
            handlers[type] = ArrayList()
        }
        handlers[type]!!.add(handler)
    }
}
