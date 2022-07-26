package io.github.pelmenstar1.digiDict.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Event {
    @Volatile
    private var _handler: (() -> Unit)? = null

    var handler: (() -> Unit)?
        get() = _handler
        set(value) {
            synchronized(this) {
                _handler = value
            }
        }

    fun raise() {
        synchronized(this) {
            _handler.let { it?.invoke() }
        }
    }

    suspend fun raiseOnMainThread() {
        withContext(Dispatchers.Main) {
            raise()
        }
    }
}