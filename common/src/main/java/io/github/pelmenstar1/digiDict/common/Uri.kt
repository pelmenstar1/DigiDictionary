package io.github.pelmenstar1.digiDict.common

import android.net.Uri

fun Uri.fileExtensionOrNull(): String? {
    val path = path ?: return null
    val lastDotIndex = path.lastIndexOf('.')
    if (lastDotIndex < 0 || lastDotIndex == path.length - 1) {
        return null
    }

    // There can be files like 'file.ext (2)' or 'file.ext (3)'
    val bracketIndex = path.indexOf('(', lastDotIndex)

    return if (bracketIndex >= 0) {
        path.substring(lastDotIndex + 1, bracketIndex).trim()
    } else {
        path.substring(lastDotIndex + 1)
    }
}