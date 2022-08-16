package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.RecordBadgeNameUtil
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecordBadgeNameUtilTests {
    @Test
    fun encodeThrowsWhenTextIsEmpty() {
        assertFailsWith(IllegalStateException::class) {
            RecordBadgeNameUtil.encode("")
        }
    }

    @Test
    fun encodeTest() {
        fun testCase(input: String, expected: String) {
            val actual = RecordBadgeNameUtil.encode(input)

            assertEquals(expected, actual)
        }

        testCase(input = "123", expected = "123")
        testCase(input = ",", expected = "\\,")
        testCase(input = "12,12", expected = "12\\,12")
        testCase(input = "12,", expected = "12\\,")
        testCase(input = "1,", expected = "1\\,")
        testCase(input = "1,1", expected = "1\\,1")
        testCase(input = "1,,1", expected = "1\\,\\,1")
        testCase(input = ",,11,,1", expected = "\\,\\,11\\,\\,1")
        testCase(input = "1,,", expected = "1\\,\\,")
    }

    @Test
    fun encodeArrayTest() {
        fun testCase(input: Array<out String>, expected: String) {
            val actual = RecordBadgeNameUtil.encodeArray(input)

            assertEquals(expected, actual)
        }

        testCase(input = emptyArray(), expected = "")
        testCase(input = arrayOf("1", "2"), expected = "1,2")
        testCase(input = arrayOf("1"), expected = "1")
        testCase(input = arrayOf("123", "321", "111"), expected = "123,321,111")
        testCase(input = arrayOf("1,", ",,1", "3"), expected = "1\\,,\\,\\,1,3")
        testCase(input = arrayOf(",", "1", "2"), expected = "\\,,1,2")
    }

    @Test
    fun decodeThrowsWhenTextIsEmpty() {
        assertFailsWith(IllegalStateException::class) {
            RecordBadgeNameUtil.decode("")
        }
    }

    @Test
    fun decodeThrowsWhenCommaIsUnescaped() {
        assertFailsWith(IllegalStateException::class) {
            RecordBadgeNameUtil.decode(",")
        }

        assertFailsWith(IllegalStateException::class) {
            RecordBadgeNameUtil.decode("11,")
        }
    }

    @Test
    fun decodeTest() {
        fun testCase(input: String, expected: String) {
            val actual = RecordBadgeNameUtil.decode(input)

            assertEquals(expected, actual)
        }

        testCase(input = "\\,", expected = ",")
        testCase(input = "1\\,", expected = "1,")
        testCase(input = "123", expected = "123")
        testCase(input = "\\,1", expected = ",1")
        testCase(input = "1\\,11", expected = "1,11")
        testCase(input = "1\\,\\,11", expected = "1,,11")
        testCase(input = "\\,\\,1", expected = ",,1")
        testCase(input = "abc\\, bbb", expected = "abc, bbb")
        testCase(input = "\\,1\\,", expected = ",1,")
    }

    @Test
    fun decodeArrayTest() {
        fun testCase(input: String, expected: Array<String>) {
            val actual = RecordBadgeNameUtil.decodeArray(input)

            assertContentEquals(expected, actual)
        }

        testCase(input = "123", expected = arrayOf("123"))
        testCase(input = "123,12", expected = arrayOf("123", "12"))
        testCase(input = "1,2,3", expected = arrayOf("1", "2", "3"))
        testCase(input = "1\\,,2", expected = arrayOf("1,", "2"))
        testCase(input = "1\\,2", expected = arrayOf("1,2"))
        testCase(input = "\\,1,2", expected = arrayOf(",1", "2"))
        testCase(input = "\\,1\\,2,3", expected = arrayOf(",1,2", "3"))
        testCase(input = "aaa\\,bbb,ccc,ddd\\,b", expected = arrayOf("aaa,bbb", "ccc", "ddd,b"))
        testCase(input = "11,12\\,\\,13\\,", expected = arrayOf("11", "12,,13,"))
    }

    @Test
    fun decodeArrayThrowsWhenNameIsEmpty() {
        fun testCase(input: String) {
            assertFailsWith(IllegalStateException::class) {
                RecordBadgeNameUtil.decodeArray(input)
            }
        }

        testCase("1,")
        testCase("1,,1")
        testCase(",,11")
    }

    @Test
    fun decodeArrayThrowsWhenDuplicate() {
        fun testCase(input: String) {
            assertFailsWith(IllegalStateException::class) {
                RecordBadgeNameUtil.decodeArray(input)
            }
        }

        testCase("1,1,1")
        testCase("1,1")
        testCase("1\\,,1\\,")
    }
}