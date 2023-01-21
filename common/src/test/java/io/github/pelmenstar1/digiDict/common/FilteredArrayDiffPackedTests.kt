package io.github.pelmenstar1.digiDict.common

import org.junit.Test
import kotlin.test.assertEquals

class FilteredArrayDiffPackedTests {
    @Test
    fun packedDiffRange_roundtrip() {
        fun testCase(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int) {
            val range = PackedDiffRange(oldStart, oldEnd, newStart, newEnd)

            assertEquals(oldStart, range.oldStart)
            assertEquals(oldEnd, range.oldEnd)
            assertEquals(newStart, range.newStart)
            assertEquals(newEnd, range.newEnd)
        }

        testCase(oldStart = 0, oldEnd = 0, newStart = 0, newEnd = 0)
        testCase(oldStart = 1, oldEnd = 1, newStart = 1, newEnd = 1)
        testCase(oldStart = PACKED_MAX_VALUE, oldEnd = 1, newStart = 1, newEnd = 1)
        testCase(oldStart = 1, oldEnd = 1, newStart = PACKED_MAX_VALUE, newEnd = 1)
        testCase(oldStart = 1, oldEnd = 1, newStart = PACKED_MAX_VALUE, newEnd = 1)
        testCase(oldStart = 1, oldEnd = 1, newStart = 1, newEnd = PACKED_MAX_VALUE)
        testCase(PACKED_MAX_VALUE, PACKED_MAX_VALUE, PACKED_MAX_VALUE, PACKED_MAX_VALUE)
        testCase(1, PACKED_MAX_VALUE, PACKED_MAX_VALUE, PACKED_MAX_VALUE)
    }

    @Test
    fun packedDiffDiagonal_roundtrip() {
        fun testCase(x: Int, y: Int, size: Int) {
            val diagonal = PackedDiffDiagonal(x, y, size)

            assertEquals(x, diagonal.x)
            assertEquals(y, diagonal.y)
            assertEquals(size, diagonal.size)
        }

        testCase(0, 0, 0)
        testCase(1, 1, 1)
        testCase(100, 1, 5)
        testCase(PACKED_MAX_VALUE, 1, 2)
        testCase(5, PACKED_MAX_VALUE, 2)
        testCase(0, 1, PACKED_MAX_VALUE)
        testCase(PACKED_MAX_VALUE, PACKED_MAX_VALUE, PACKED_MAX_VALUE)
    }

    @Test
    fun packedDiffDiagonal_none() {
        val none = PackedDiffDiagonal.NONE
        assertEquals(0xFFFF, none.x)
        assertEquals(0xFFFF, none.y)
        assertEquals(0xFFFF, none.size)
    }

    companion object {
        private const val PACKED_MAX_VALUE = 0xFFFF - 1
    }
}