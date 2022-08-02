package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.utils.mapOffset
import io.github.pelmenstar1.digiDict.utils.withAddedElement
import io.github.pelmenstar1.digiDict.utils.withRemovedElementAt
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class CollectionsTests {
    @Test
    fun `withAddedElement test`() {
        kotlin.run {
            val actual = emptyArray<String>().withAddedElement("123")

            assertContentEquals(arrayOf("123"), actual)
        }

        kotlin.run {
            val actual = arrayOf("1").withAddedElement("2")

            assertContentEquals(arrayOf("1", "2"), actual)
        }
    }

    @Test
    fun `withRemovedElementAt test`() {
        kotlin.run {
            val actual = arrayOf("1").withRemovedElementAt(0)

            assertContentEquals(emptyArray(), actual)
        }

        kotlin.run {
            val actual = arrayOf("1", "2").withRemovedElementAt(0)

            assertContentEquals(arrayOf("2"), actual)
        }

        kotlin.run {
            val actual = arrayOf("1", "2", "3").withRemovedElementAt(2)

            assertContentEquals(arrayOf("1", "2"), actual)
        }

        kotlin.run {
            val actual = arrayOf("1", "2", "3", "4").withRemovedElementAt(1)

            assertContentEquals(arrayOf("1", "3", "4"), actual)
        }
    }

    @Test
    fun `mapOffset should throw exception when offset is negative`() {
        assertFailsWith(IllegalArgumentException::class) {
            emptyArray<String>().mapOffset(-1) { it }
        }
    }

    @Test
    fun `mapOffset should throw exception when offset is greater than size`() {
        fun <T> testCase(initial: Array<T>, offset: Int) {
            assertFailsWith(IllegalArgumentException::class) {
                initial.mapOffset(offset) { it }
            }
        }

        testCase(emptyArray<String>(), 1)
        testCase(arrayOf("1", "2", "3"), 5)
    }

    @Test
    fun `mapOffset test`() {
        fun testCasePlus0(initial: Array<String>, offset: Int, expected: List<String>) {
            val actual = initial.mapOffset(offset) { it + '0' }

            assertContentEquals(expected, actual)
        }

        testCasePlus0(initial = arrayOf("u", "a", "b", "c"), offset = 1, expected = listOf("a0", "b0", "c0"))
        testCasePlus0(initial = arrayOf("a", "b"), offset = 0, expected = listOf("a0", "b0"))
        testCasePlus0(initial = arrayOf("a"), offset = 0, expected = listOf("a0"))
        testCasePlus0(initial = arrayOf("a", "b", "c"), offset = 2, listOf("c0"))
        testCasePlus0(initial = arrayOf("a", "b", "c"), offset = 3, emptyList())
    }
}