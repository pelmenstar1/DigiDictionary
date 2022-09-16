package io.github.pelmenstar1.digiDict.common.binarySerialization

class BinarySerializationException(
    val reason: Int,
    msg: String = "",
    cause: Exception? = null
) : Exception(msg, cause) {
    companion object {
        const val REASON_DATA_VALIDATION = 0
        const val REASON_UNKNOWN_VERSION = 1
        const val REASON_INTERNAL = 2
    }
}

inline fun checkDataValidity(msg: String, condition: () -> Boolean) {
    if (!condition()) {
        throw BinarySerializationException(BinarySerializationException.REASON_DATA_VALIDATION, msg)
    }
}