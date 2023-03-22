package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationCompatInfo
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

@Serializable
class BackupCompatInfo(
    val newMeaningFormat: Boolean = false,

    // Transient because only JSON format is used with Kotlin serialization and it's always UTF8.
    // This is property is only used in binary serialization.
    @kotlinx.serialization.Transient val isUtf8Strings: Boolean = false
) {
    private fun toBinarySerializationCompatInfoBits(): Long {
        var bits = 0L

        if (newMeaningFormat) {
            bits = bits or (1L shl NEW_MEANING_FORMAT_INDEX)
        }

        if (isUtf8Strings) {
            bits = bits or (1L shl IS_UTF8_STRINGS_INDEX)
        }

        return bits
    }

    fun toBinarySerializationCompatInfo(): BinarySerializationCompatInfo {
        return BinarySerializationCompatInfo(toBinarySerializationCompatInfoBits())
    }

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        newMeaningFormat == o.newMeaningFormat && isUtf8Strings == o.isUtf8Strings
    }

    override fun hashCode(): Int {
        // toInt() is lossless because currently only 2 bits are used.
        return toBinarySerializationCompatInfoBits().toInt()
    }

    override fun toString(): String {
        return "BackupCompatInfo(newMeaningFormat=$newMeaningFormat, isUtf8Used=$isUtf8Strings)"
    }

    companion object {
        private val EMPTY = BackupCompatInfo()

        const val NEW_MEANING_FORMAT_INDEX = 0
        const val IS_UTF8_STRINGS_INDEX = 1

        fun empty() = EMPTY
    }
}