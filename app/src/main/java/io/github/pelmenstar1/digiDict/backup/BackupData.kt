package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.equalsPattern
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import kotlinx.serialization.Serializable

/**
 * Stores data required for both importing and exporting logic.
 *
 * Id of elements of [records], [badges] are not stored and are actually 0.
 * To reference an element, ordinal number (position in the array) can be used.
 */
@Serializable
data class BackupData(
    val records: Array<out Record>,
    val badges: Array<out RecordBadgeInfo>,
    val badgeToMultipleRecordEntries: Array<out BackupBadgeToMultipleRecordEntry>
) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        records.contentEquals(o.records) &&
                badges.contentEquals(o.badges) &&
                badgeToMultipleRecordEntries.contentEquals(o.badgeToMultipleRecordEntries)
    }

    override fun hashCode(): Int {
        var result = records.contentHashCode()
        result = result * 31 + badges.contentHashCode()
        result = result * 31 + badgeToMultipleRecordEntries.contentHashCode()

        return result
    }

    override fun toString(): String {
        return "BackupData(records=${records.contentToString()}, badges=${badges.contentToString()}, recordToBadgeRelations=${badgeToMultipleRecordEntries.contentToString()})"
    }
}