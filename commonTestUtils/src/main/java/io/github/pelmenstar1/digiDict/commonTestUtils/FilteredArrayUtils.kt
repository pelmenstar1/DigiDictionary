package io.github.pelmenstar1.digiDict.commonTestUtils

import io.github.pelmenstar1.digiDict.common.FilteredArray
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

inline fun <reified T> FilteredArray<out T>.toArray(): Array<T> {
    val array = unsafeNewArray<T>(size)
    System.arraycopy(origin, 0, array, 0, size)

    return array
}