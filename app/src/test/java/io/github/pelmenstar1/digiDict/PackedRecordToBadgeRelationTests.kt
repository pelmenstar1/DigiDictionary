package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.PackedRecordToBadgeRelation
import io.github.pelmenstar1.digiDict.data.PackedRecordToBadgeRelationArray
import org.junit.Test
import kotlin.test.assertEquals

class PackedRecordToBadgeRelationTests {
    @Test
    fun createTest() {
        fun testCase(recordId: Int, badgeId: Int) {
            val packed = PackedRecordToBadgeRelation(recordId, badgeId)

            val actualRecordId = packed.recordId
            val actualBadgeId = packed.badgeId

            assertEquals(recordId, actualRecordId)
            assertEquals(badgeId, actualBadgeId)
        }

        testCase(recordId = 0, badgeId = 0)
        testCase(recordId = 0, badgeId = 1)
        testCase(recordId = 1, badgeId = 0)
        testCase(recordId = 1, badgeId = 1)
        testCase(recordId = -1, badgeId = -1)
        testCase(recordId = Int.MAX_VALUE, badgeId = 0)
        testCase(recordId = Int.MAX_VALUE, badgeId = 123)
        testCase(recordId = Int.MAX_VALUE, badgeId = -123)
        testCase(recordId = Int.MAX_VALUE, badgeId = Int.MIN_VALUE)
        testCase(recordId = Int.MAX_VALUE, badgeId = Int.MAX_VALUE)
        testCase(recordId = Int.MIN_VALUE, badgeId = Int.MIN_VALUE)
        testCase(recordId = -1000, badgeId = -1000)
        testCase(recordId = 1000, badgeId = 1000)
    }

    @Test
    fun binarySearchRecordIdTest() {
        fun testCase(elements: Array<out PackedRecordToBadgeRelation>, recordId: Int, expectedIndex: Int) {
            val packedElements = PackedRecordToBadgeRelationArray(elements.size)

            elements.forEachIndexed { index, value ->
                packedElements[index] = value
            }

            val actualIndex = packedElements.binarySearchRecordId(recordId)
            assertEquals(expectedIndex, actualIndex)
        }

        testCase(
            elements = arrayOf(
                PackedRecordToBadgeRelation(recordId = 1, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 3, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 4, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 5, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 6, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 7, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 10, badgeId = 2),
            ),
            recordId = 6,
            expectedIndex = 4
        )

        testCase(
            elements = arrayOf(
                PackedRecordToBadgeRelation(recordId = 1, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 3, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 4, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 5, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 6, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 7, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 10, badgeId = 2),
            ),
            recordId = 1,
            expectedIndex = 0
        )

        testCase(
            elements = arrayOf(
                PackedRecordToBadgeRelation(recordId = 1, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 3, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 4, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 5, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 6, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 7, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 10, badgeId = 2),
            ),
            recordId = 10,
            expectedIndex = 6
        )

        testCase(
            elements = arrayOf(
                PackedRecordToBadgeRelation(recordId = 1, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 3, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 4, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 5, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 6, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 7, badgeId = 2),
                PackedRecordToBadgeRelation(recordId = 10, badgeId = 2),
            ),
            recordId = 100,
            expectedIndex = -1
        )

        testCase(
            elements = arrayOf(
                PackedRecordToBadgeRelation(recordId = 1, badgeId = 2),
            ),
            recordId = 1,
            expectedIndex = 0
        )

        testCase(
            elements = arrayOf(
                PackedRecordToBadgeRelation(recordId = 1, badgeId = 2),
            ),
            recordId = 5,
            expectedIndex = -1
        )

        testCase(
            elements = emptyArray(),
            recordId = 1,
            expectedIndex = -1
        )
    }
}