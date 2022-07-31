package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.utils.withAddedElement
import io.github.pelmenstar1.digiDict.utils.withRemovedElementAt
import org.junit.Test
import kotlin.test.assertContentEquals

class CollectionsTests {
    @Test
    fun withAddedElementTest() {
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
    fun withRemovedElementAtTest() {
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
}