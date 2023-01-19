package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertEquals

class PackedIntRangeTests {
    @Test
    fun createRoundtrip() {
        fun testCase(start: Int, end: Int) {
            val range = PackedIntRange(start, end)

            assertEquals(start, range.start, "start")
            assertEquals(end, range.end, "end")
        }

        testCase(0, 0)
        testCase(0, 1)
        testCase(-100, -1)
        testCase(-1, 100)
        testCase(100, 100)
        testCase(Int.MAX_VALUE, Int.MAX_VALUE)
        testCase(Int.MIN_VALUE, Int.MAX_VALUE)
        testCase(Int.MIN_VALUE, Int.MIN_VALUE)
        testCase(Int.MIN_VALUE, 1)
        testCase(Int.MIN_VALUE, -1)
        testCase(Int.MIN_VALUE, 0)
        testCase(0, Int.MAX_VALUE)
    }
}