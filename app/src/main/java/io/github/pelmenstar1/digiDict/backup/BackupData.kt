package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.data.Record
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(val records: Array<out Record>) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        records.contentEquals(o.records)
    }

    override fun hashCode(): Int {
        return records.contentHashCode()
    }

    override fun toString(): String {
        return "BackupData(records=${records.contentToString()})"
    }
}