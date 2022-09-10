package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.backup.BackupBadgeToMultipleRecordEntry
import io.github.pelmenstar1.digiDict.backup.BackupHelpers
import io.github.pelmenstar1.digiDict.backup.IdToOrdinalMap
import io.github.pelmenstar1.digiDict.common.mapToArray
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordToBadgeRelation
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BackupHelpersTests {
    @Test
    fun groupRecordToBadgeRelationsTest() {
        fun testCase(
            relations: Array<out Pair<Int, Int>>,
            expected: Array<BackupBadgeToMultipleRecordEntry>
        ) {
            val relationObjects = relations.mapToArray { (badgeId, recordId) ->
                RecordToBadgeRelation(0, recordId, badgeId)
            }

            relationObjects.sortBy { it.badgeId }

            val recordIds = relationObjects.map { it.recordId }.distinct().sorted().toIntArray()

            val recordIdToOrdinalMap = IdToOrdinalMap(recordIds)
            val actual = BackupHelpers.groupRecordToBadgeRelations(relationObjects, recordIdToOrdinalMap)

            assertContentEquals(expected, actual)
        }

        testCase(
            relations = arrayOf(
                3 to 3,
                3 to 4,

                5 to 7,
                5 to 8,
                5 to 9,

                6 to 5
            ),
            expected = arrayOf(
                BackupBadgeToMultipleRecordEntry(0, intArrayOf(0, 1)),
                BackupBadgeToMultipleRecordEntry(1, intArrayOf(3, 4, 5)),
                BackupBadgeToMultipleRecordEntry(2, intArrayOf(2))
            )
        )

        testCase(
            relations = arrayOf(
                3 to 3,
            ),
            expected = arrayOf(
                BackupBadgeToMultipleRecordEntry(0, intArrayOf(0))
            )
        )

        testCase(
            relations = arrayOf(
                3 to 3,
                4 to 4,
                5 to 5,
            ),
            expected = arrayOf(
                BackupBadgeToMultipleRecordEntry(0, intArrayOf(0)),
                BackupBadgeToMultipleRecordEntry(1, intArrayOf(1)),
                BackupBadgeToMultipleRecordEntry(2, intArrayOf(2))
            )
        )
    }

    @Test
    fun containsDuplicateExpressionsTest() {
        fun testCase(expressions: Array<String>, expected: Boolean) {
            val records = expressions.mapToArray { expr ->
                Record(
                    id = 0,
                    expression = expr,
                    meaning = "CM",
                    additionalNotes = "",
                    score = 0,
                    epochSeconds = 0L
                )
            }

            val actual = BackupHelpers.containsDuplicateExpressions(records)
            assertEquals(expected, actual)
        }

        testCase(
            expressions = arrayOf("A", "B", "A", "C"),
            expected = true
        )

        testCase(
            expressions = arrayOf("A", "B", "C"),
            expected = false
        )

        testCase(
            expressions = (0..1024).map { it.toString() }.toTypedArray(),
            expected = false
        )

        testCase(
            expressions = (0..1024).map { it.toString() }.toTypedArray(),
            expected = false
        )

        // Random with 0 seed to make a test case deterministic.
        testCase(
            expressions = (0..1024).map { it.toString() }.shuffled(Random(0)).toTypedArray(),
            expected = false
        )

        testCase(
            expressions = emptyArray(),
            expected = false
        )

        testCase(
            expressions = arrayOf("A"),
            expected = false
        )

        testCase(
            expressions = arrayOf("C", "A", "B", "D"),
            expected = false
        )
    }
}