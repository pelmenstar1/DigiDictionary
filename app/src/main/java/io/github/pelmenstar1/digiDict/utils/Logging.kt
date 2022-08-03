package io.github.pelmenstar1.digiDict.utils

import android.annotation.SuppressLint
import android.util.Log
import io.github.pelmenstar1.digiDict.BuildConfig

// It's actually the purpose of the class whose methods will only be called in debug builds.
@SuppressLint("LogConditional")
class DebugLog(private val tag: String) {
    object Action

    private inline fun action(block: () -> Unit) = Action.also { block() }

    fun info(text: String) = action { Log.i(tag, text) }

    fun infoIf(condition: Boolean, text: String) = action {
        if (condition) {
            Log.i(tag, text)
        }
    }
}

inline fun debugLog(tag: String, block: DebugLog.() -> DebugLog.Action) {
    if (BuildConfig.DEBUG) {
        DebugLog(tag).block()
    }
}