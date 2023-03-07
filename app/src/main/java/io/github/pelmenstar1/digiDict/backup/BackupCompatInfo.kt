package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationCompatInfo
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

// For now, it's empty. In future, some properties will be added.
@Serializable
class BackupCompatInfo {
    fun toBinarySerializationCompatInfo(): BinarySerializationCompatInfo {
        return BinarySerializationCompatInfo.empty()
    }

    override fun equals(other: Any?) = equalsPattern(other) { true }

    override fun hashCode(): Int {
        return 0
    }

    override fun toString(): String {
        return "BackupCompatInfo()"
    }

    companion object {
        private val EMPTY = BackupCompatInfo()

        fun empty() = EMPTY
    }
}