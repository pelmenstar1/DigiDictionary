package io.github.pelmenstar1.digiDict.utils

import io.github.pelmenstar1.digiDict.common.mapToArray
import io.github.pelmenstar1.digiDict.data.*
import kotlin.test.fail

fun <T : EntityWithPrimaryKeyId> assertContentEqualsNoId(expected: Array<out T>, actual: Array<out T>) {
    assertContentEqualsNoId(expected, actual) { other -> equalsNoId(other) }
}

fun <T> assertContentEqualsNoId(expected: Array<out T>, actual: Array<out T>, equalsNoId: T.(other: T) -> Boolean) {
    val expectedSize = expected.size
    val actualSize = actual.size

    if (expectedSize != actualSize) {
        fail("Size of expected array differs from size of actual array. Expected size: $expectedSize, actual size: $actualSize")
    }

    for (i in 0 until expectedSize) {
        val expectedElement = expected[i]
        val actualElement = actual[i]

        if (!expectedElement.equalsNoId(actualElement)) {
            fail("Elements at index $i differ: Expected element: '$expectedElement' \n Actual element: '$actualElement'")
        }
    }
}

fun AppDatabase.reset() {
    clearAllTables()

    query("DELETE FROM sqlite_sequence", null)
}

suspend fun AppDatabase.addRecordWithBadges(value: RecordWithBadges) {
    val record =
        Record(value.id, value.expression, value.meaning, value.additionalNotes, value.score, value.epochSeconds)

    addRecordAndBadges(record, value.badges)
}

suspend fun AppDatabase.addRecordAndBadges(
    record: Record,
    badges: Array<RecordBadgeInfo>
): RecordWithBadges {
    val recordDao = recordDao()
    val recordBadgeDao = recordBadgeDao()
    val recordToBadgeRelationDao = recordToBadgeRelationDao()

    val recordWithBadges = RecordWithBadges.create(record, badges)

    recordDao.insert(record)
    recordBadgeDao.insertAll(badges)
    recordToBadgeRelationDao.insertAll(
        badges.mapToArray { RecordToBadgeRelation(0, record.id, it.id) }
    )

    return recordWithBadges
}

fun Array<out RecordToBadgeRelation>.toPackedArray(): PackedRecordToBadgeRelationArray {
    val result = PackedRecordToBadgeRelationArray(size)

    forEachIndexed { index, value ->
        result[index] = PackedRecordToBadgeRelation(value.recordId, value.badgeId)
    }

    return result
}