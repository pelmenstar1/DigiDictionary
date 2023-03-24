package io.github.pelmenstar1.digiDict.common

import android.text.GetChars
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class TextInstrumentedTests {
    private class GetCharsImpl(private val data: String) : GetChars {
        override val length: Int
            get() = data.length

        override fun get(index: Int) = data[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return GetCharsImpl(data.substring(startIndex, endIndex))
        }

        override fun getChars(start: Int, end: Int, dest: CharArray, destoff: Int) {
            data.toCharArray(dest, destoff, start, end)
        }

        override fun toString() = data
    }

    @Test
    fun subSequenceToStringOnGetCharsImplTest() {
        fun testCase(input: String, start: Int, end: Int, expected: String) {
            val actual = GetCharsImpl(input).subSequenceToString(start, end)

            assertEquals(expected, actual)
        }

        testCase(input = "A123B", start = 1, end = 4, expected = "123")
        testCase(input = "123", start = 0, end = 0, expected = "")
        testCase(input = "123", start = 0, end = 1, expected = "1")
    }
}