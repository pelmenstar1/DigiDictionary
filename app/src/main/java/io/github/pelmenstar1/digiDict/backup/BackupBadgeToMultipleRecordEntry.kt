package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializerResolver
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

/**
 * A class which is used store a badge ordinal and multiple record ordinals to which the badge is related.
 *
 * It's used to optimize a common case when there's small amount of badge and much greater amount of records to which these badges are related.
 */
@Serializable
data class BackupBadgeToMultipleRecordEntry(val badgeOrdinal: Int, val recordOrdinals: IntArray) {
    override fun equals(other: Any?) = equalsPattern(other) { o ->
        badgeOrdinal == o.badgeOrdinal && recordOrdinals.contentEquals(o.recordOrdinals)
    }

    override fun hashCode(): Int {
        var result = badgeOrdinal
        result = result * 31 + recordOrdinals.contentHashCode()

        return result
    }

    override fun toString(): String {
        return "BackupBadgeToMultipleRecordEntry(badgeId=$badgeOrdinal, recordIds=${recordOrdinals.contentToString()})"
    }

    companion object {
        val SERIALIZER_RESOLVER = BinarySerializerResolver<BackupBadgeToMultipleRecordEntry> {
            register<BackupBadgeToMultipleRecordEntry>(
                version = 1,
                write = { (badgeId, recordIds) ->
                    emit(badgeId)
                    emitArrayAndLength(recordIds)
                },
                read = {
                    val badgeId = consumeInt()
                    val recordIds = consumeIntArrayAndLength()

                    BackupBadgeToMultipleRecordEntry(badgeId, recordIds)
                }
            )
        }
    }
}