package io.github.pelmenstar1.digiDict.common

object EmptyArray {
    @JvmField
    val BYTE = ByteArray(0)

    @JvmField
    val LONG = LongArray(0)

    @JvmField
    val INT = IntArray(0)

    @JvmField
    val STRING = emptyArray<String>()
}