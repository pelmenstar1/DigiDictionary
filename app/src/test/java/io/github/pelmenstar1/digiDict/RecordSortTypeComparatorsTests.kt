package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.ConciseRecordWithBadges
import io.github.pelmenstar1.digiDict.data.RecordSortType
import io.github.pelmenstar1.digiDict.data.getComparatorForConciseRecordWithBadges
import org.junit.Test
import kotlin.test.assertEquals

class RecordSortTypeComparatorsTests {
    private enum class Sign {
        NEGATIVE,
        POSITIVE,
        ZERO
    }

    private fun Int.getSign(): Sign = when {
        this < 0 -> Sign.NEGATIVE
        this > 0 -> Sign.POSITIVE
        else -> Sign.ZERO
    }

    private fun createRecord(
        expression: String = "ABC",
        score: Int = 0,
        epochSeconds: Long = 0L
    ): ConciseRecordWithBadges {
        return ConciseRecordWithBadges(id = 0, expression, "CMeaning", score, epochSeconds, emptyArray())
    }

    private fun comparatorTestHelper(
        sortType: RecordSortType,
        a: ConciseRecordWithBadges,
        b: ConciseRecordWithBadges,
        expectedSign: Sign
    ) {
        val comparator = sortType.getComparatorForConciseRecordWithBadges()

        val actualResult = comparator.compare(a, b)
        val actualSign = actualResult.getSign()

        assertEquals(expectedSign, actualSign)
    }

    @Test
    fun newestTest() {
        fun testCase(a: ConciseRecordWithBadges, b: ConciseRecordWithBadges, expectedSign: Sign) {
            comparatorTestHelper(RecordSortType.NEWEST, a, b, expectedSign)
        }

        testCase(createRecord(epochSeconds = 100L), createRecord(epochSeconds = 50L), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(epochSeconds = 1000L), createRecord(epochSeconds = 999L), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(epochSeconds = 123L), createRecord(epochSeconds = 555L), expectedSign = Sign.POSITIVE)
        testCase(createRecord(epochSeconds = 321L), createRecord(epochSeconds = 321L), expectedSign = Sign.ZERO)
    }

    @Test
    fun oldestTest() {
        fun testCase(a: ConciseRecordWithBadges, b: ConciseRecordWithBadges, expectedSign: Sign) {
            comparatorTestHelper(RecordSortType.OLDEST, a, b, expectedSign)
        }

        testCase(createRecord(epochSeconds = 100L), createRecord(epochSeconds = 50L), expectedSign = Sign.POSITIVE)
        testCase(createRecord(epochSeconds = 1000L), createRecord(epochSeconds = 999L), expectedSign = Sign.POSITIVE)
        testCase(createRecord(epochSeconds = 123L), createRecord(epochSeconds = 555L), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(epochSeconds = 321L), createRecord(epochSeconds = 321L), expectedSign = Sign.ZERO)
    }

    @Test
    fun greatestScoreTest() {
        fun testCase(a: ConciseRecordWithBadges, b: ConciseRecordWithBadges, expectedSign: Sign) {
            comparatorTestHelper(RecordSortType.GREATEST_SCORE, a, b, expectedSign)
        }

        testCase(createRecord(score = 10), createRecord(score = 5), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(score = 12), createRecord(score = 11), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(score = 5), createRecord(score = 10), expectedSign = Sign.POSITIVE)
        testCase(createRecord(score = 123), createRecord(score = 123), expectedSign = Sign.ZERO)
    }

    @Test
    fun leastScoreTest() {
        fun testCase(a: ConciseRecordWithBadges, b: ConciseRecordWithBadges, expectedSign: Sign) {
            comparatorTestHelper(RecordSortType.LEAST_SCORE, a, b, expectedSign)
        }

        testCase(createRecord(score = 10), createRecord(score = 5), expectedSign = Sign.POSITIVE)
        testCase(createRecord(score = 12), createRecord(score = 11), expectedSign = Sign.POSITIVE)
        testCase(createRecord(score = 5), createRecord(score = 10), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(score = 123), createRecord(score = 123), expectedSign = Sign.ZERO)
    }

    @Test
    fun alphabeticByExpressionTest() {
        fun testCase(a: ConciseRecordWithBadges, b: ConciseRecordWithBadges, expectedSign: Sign) {
            comparatorTestHelper(RecordSortType.ALPHABETIC_BY_EXPRESSION, a, b, expectedSign)
        }

        testCase(createRecord(expression = "A"), createRecord(expression = "B"), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(expression = "B"), createRecord(expression = "A"), expectedSign = Sign.POSITIVE)
        testCase(createRecord(expression = "C"), createRecord(expression = "C"), expectedSign = Sign.ZERO)
    }

    @Test
    fun alphabeticByExpressionInverseTest() {
        fun testCase(a: ConciseRecordWithBadges, b: ConciseRecordWithBadges, expectedSign: Sign) {
            comparatorTestHelper(RecordSortType.ALPHABETIC_BY_EXPRESSION_INVERSE, a, b, expectedSign)
        }

        testCase(createRecord(expression = "A"), createRecord(expression = "B"), expectedSign = Sign.POSITIVE)
        testCase(createRecord(expression = "B"), createRecord(expression = "A"), expectedSign = Sign.NEGATIVE)
        testCase(createRecord(expression = "C"), createRecord(expression = "C"), expectedSign = Sign.ZERO)
    }
}