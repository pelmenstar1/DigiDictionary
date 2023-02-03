package io.github.pelmenstar1.digiDict.common

import java.io.Closeable

fun Closeable?.closeInFinally(cause: Exception?) = when {
    this == null -> {}
    cause == null -> close()
    else -> {
        try {
            close()
        } catch (closeEx: Exception) {
            cause.addSuppressed(closeEx)
        }
    }
}