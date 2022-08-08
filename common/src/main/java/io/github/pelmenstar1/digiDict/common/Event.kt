package io.github.pelmenstar1.digiDict.common

import android.os.Handler
import android.os.Looper
import android.os.Message
import kotlinx.coroutines.CancellationException

class Event {
    @Volatile
    private var _handler: (() -> Unit)? = null

    // It's true in situation when the event is raised but handler is null.
    @Volatile
    private var isCalled = false

    var handler: (() -> Unit)?
        get() = synchronized(this) { _handler }
        set(value) {
            synchronized(this) {
                _handler = value

                if (isCalled) {
                    isCalled = false

                    raiseOnMainThread()
                }
            }
        }

    fun raise() {
        synchronized(this) {
            val h = _handler

            if (h != null) {
                h.invoke()
            } else {
                isCalled = true
            }
        }
    }

    fun raiseOnMainThread() {
        synchronized(this) {
            val mainThread = Looper.getMainLooper().thread
            val h = _handler

            if (mainThread == Thread.currentThread()) {
                if (h != null) {
                    h.invoke()
                } else {
                    isCalled = true
                }
            } else {
                if (h != null) {
                    val msg = Message.obtain().also {
                        it.obj = h
                    }

                    mainThreadHandler.sendMessage(msg)
                } else {
                    isCalled = true
                }
            }
        }
    }

    fun raiseOnMainThreadIfNotCancellation(e: Throwable) {
        if (e !is CancellationException) {
            raiseOnMainThread()
        }
    }

    companion object {
        private val mainThreadHandler = object : Handler(Looper.getMainLooper()) {
            @Suppress("UNCHECKED_CAST")
            override fun handleMessage(msg: Message) {
                val eventHandler = msg.obj as (() -> Unit)

                eventHandler()
            }
        }
    }
}