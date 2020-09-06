package util


inline fun <T>List<T>.forEachConcurrentSafe(func: (T) -> Unit) {
    for (i in indices) if (i < size) func(this[i])
}

inline fun <T>List<T>.forEachIndexedConcurrentSafe(func: (Int, T) -> Unit) {
    for (i in indices) if (i < size) func(i, this[i])
}

inline fun <T>List<T>.forEachBreakableConcurrentSafe(func: (T) -> Boolean) {
    for (i in indices) if (i < size) if (func(this[i])) break
}

inline fun <T>List<T>.forEachBreakableIndexedConcurrentSafe(func: (Int, T) -> Boolean) {
    for (i in indices) if (i < size) if (func(i, this[i])) break
}
