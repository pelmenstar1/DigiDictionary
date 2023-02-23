package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.backup.IdToOrdinalMap
import org.junit.Test
import kotlin.test.*

class IdToOrdinalMapTests {
    private fun createConsecutiveMap(startId: Int, endId: Int): IdToOrdinalMap {
        return IdToOrdinalMap(endId - startId).also { map ->
            for (id in startId until endId) {
                map.add(id)
            }
        }
    }

    @Test
    fun capacityConstructorThrowsWhenCapacityNegativeTest() {
        assertFailsWith<IllegalArgumentException> {
            IdToOrdinalMap(capacity = -1)
        }
    }

    @Test
    fun addThrowsWhenCapacityReachedTest() {
        fun testCase(capacity: Int) {
            val map = IdToOrdinalMap(capacity)

            for (i in 0 until capacity) {
                map.add(i)
            }

            assertFailsWith<IllegalStateException> {
                map.add(capacity)
            }
        }

        testCase(1)
        testCase(16)
    }

    @Test
    fun getOrdinalByIdTest_consecutive() {
        fun testCase(startId: Int, endId: Int) {
            val map = createConsecutiveMap(startId, endId)

            // Validate that array isn't created. If it is, the logic of not creating the array doesn't work.
            assertNull(map.idArray)

            for (id in startId until endId) {
                val expectedOrdinal = id - startId
                val actualOrdinal = map.getOrdinalById(id)

                assertEquals(expectedOrdinal, actualOrdinal, "id: $id")
            }
        }

        testCase(startId = 1, endId = 5)
        testCase(startId = 1, endId = 100)
        testCase(startId = 1, endId = 2)
        testCase(startId = 3, endId = 10)
        testCase(startId = 0, endId = 2)

        // In practice, there's no negative ids but it should work.
        testCase(startId = -100, endId = -50)
    }

    @Test
    fun getOrdinalByIdTest_nonConsecutive() {
        fun testCase(ids: IntArray, expectedIsSorted: Boolean) {
            val map = IdToOrdinalMap(ids)

            // Pre-validates the map state because if this array is null, the resulting ordinals can't be correct.
            assertNotNull(map.idArray)

            assertEquals(expectedIsSorted, map.isSorted)

            ids.forEachIndexed { expectedOrdinal, id ->
                val actualOrdinal = map.getOrdinalById(id)

                assertEquals(expectedOrdinal, actualOrdinal, "id: $id")
            }
        }

        testCase(ids = intArrayOf(1, 5, 10, 4, 17, 20), expectedIsSorted = false)
        testCase(ids = intArrayOf(10, 5, 3, 6, 100), expectedIsSorted = false)

        // getOrdinalById has special case if ids are sorted. It needs to be checked.
        testCase(ids = intArrayOf(1, 10), expectedIsSorted = true)
        testCase(ids = intArrayOf(1, 3, 5, 8, 10), expectedIsSorted = true)
    }

    @Test
    fun getOrdinalByIdTest_fromConsecutiveToNonConsecutive() {
        fun testCase(initialStart: Int, initialEnd: Int, nextIds: IntArray, expectedIsSorted: Boolean) {
            val consecutiveLength = initialEnd - initialStart
            val map = IdToOrdinalMap(consecutiveLength + nextIds.size)

            for (id in initialStart until initialEnd) {
                map.add(id)
            }

            for (id in nextIds) {
                map.add(id)
            }

            assertNotNull(map.idArray)
            assertEquals(expectedIsSorted, map.isSorted)

            for (id in initialStart until initialEnd) {
                val expectedOrdinal = id - initialStart
                val actualOrdinal = map.getOrdinalById(id)

                assertEquals(expectedOrdinal, actualOrdinal, "id: $id")
            }

            for ((index, id) in nextIds.withIndex()) {
                val expectedOrdinal = consecutiveLength + index
                val actualOrdinal = map.getOrdinalById(id)

                assertEquals(expectedOrdinal, actualOrdinal, "id: $id")
            }
        }

        testCase(initialStart = 1, initialEnd = 5, nextIds = intArrayOf(7, 9, 10, 12), expectedIsSorted = true)
        testCase(initialStart = 1, initialEnd = 3, nextIds = intArrayOf(10, 7, 5, 12), expectedIsSorted = false)
    }

    @Test
    fun getIdByOrdinalTest_consecutive() {
        fun testCase(startId: Int, endId: Int) {
            val map = createConsecutiveMap(startId, endId)

            assertNull(map.idArray)

            for ((index, id) in (startId until endId).withIndex()) {
                val actualId = map.getIdByOrdinal(index)

                assertEquals(id, actualId, "ordinal: $index")
            }
        }

        testCase(startId = 0, endId = 5)
        testCase(startId = 5, endId = 10)
        testCase(startId = 3, endId = 5)
        testCase(startId = -10, endId = -5)
    }

    @Test
    fun getIdByOrdinalTest_nonConsecutive() {
        fun testCase(ids: IntArray) {
            val map = IdToOrdinalMap(ids)

            assertNotNull(map.idArray)

            ids.forEachIndexed { index, id ->
                val actualId = map.getIdByOrdinal(index)

                assertEquals(id, actualId, "ordinal: $index")
            }
        }

        testCase(ids = intArrayOf(5, 6, 7, 12, 13, 14))
        testCase(ids = intArrayOf(1, 10, 5, 6, 2, 3))
        testCase(ids = intArrayOf(1, 3, 4, 5, 6, 100))
    }

    @Test
    fun getIdByOrdinalThrowsWhenOutOfBounds() {
        fun testCase(capacity: Int, size: Int, ordinal: Int) {
            val map = IdToOrdinalMap(capacity)

            for (i in 0 until size) {
                map.add(i)
            }

            assertFails {
                map.getIdByOrdinal(ordinal)
            }
        }

        testCase(capacity = 5, size = 3, ordinal = 3)
        testCase(capacity = 5, size = 3, ordinal = 4)
        testCase(capacity = 5, size = 3, ordinal = -1)
    }
}