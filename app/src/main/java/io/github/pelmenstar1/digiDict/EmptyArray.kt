package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.Record

object EmptyArray {
    @JvmField
    val LONG = LongArray(0)

    @JvmField
    val INT = IntArray(0)

    @JvmField
    val STRING = emptyArray<String>()

    @JvmField
    val RECORD = emptyArray<Record>()
}