package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationCompatInfo
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

@Serializable
class BackupCompatInfo(val newMeaningFormat: Boolean = false) {
    fun toBinarySerializationCompatInfo(): BinarySerializationCompatInfo {
        var bits = 0L

        if (newMeaningFormat) {
            bits = bits or (1L shl NEW_MEANING_FORMAT_INDEX)
        }

        return BinarySerializationCompatInfo(bits)
    }

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        newMeaningFormat == o.newMeaningFormat
    }

    override fun hashCode(): Int {
        return if (newMeaningFormat) 1 else 0
    }

    override fun toString(): String {
        return "BackupCompatInfo(newMeaningFormat=$newMeaningFormat)"
    }

    companion object {
        private val EMPTY = BackupCompatInfo()

        const val NEW_MEANING_FORMAT_INDEX = 0

        fun empty() = EMPTY
    }
}