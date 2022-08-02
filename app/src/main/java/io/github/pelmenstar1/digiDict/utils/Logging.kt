package io.github.pelmenstar1.digiDict.utils

import android.util.Log
import io.github.pelmenstar1.digiDict.BuildConfig

inline fun <reified T> T.logInfo(text: () -> String) {
    logInfo(T::class.java.simpleName, text)
}

inline fun <reified T> T.logInfo(text: String) {
    logInfo { text }
}

inline fun logInfo(tag: String, text: () -> String) {
    if (BuildConfig.DEBUG) {
        Log.i(tag, text())
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun logInfo(tag: String, text: String) {
    if (BuildConfig.DEBUG) {
        Log.i(tag, text)
    }
}