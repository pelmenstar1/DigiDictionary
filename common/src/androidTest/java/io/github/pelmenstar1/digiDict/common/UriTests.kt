package io.github.pelmenstar1.digiDict.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class UriTests {
    @Test
    fun fileExtensionOrNullTest() {
        fun testCase(input: String, expected: String?) {
            val uri = Uri.parse(input)
            val actualExt = uri.fileExtensionOrNull()

            assertEquals(expected, actualExt)
        }

        testCase(input = "file:///a.", expected = null)
        testCase(input = "file:///a", expected = null)
        testCase(input = "file:///abc.ext", expected = "ext")
        testCase(input = "file:///a.e", expected = "e")
        testCase(input = "file:///a.ext (1)", expected = "ext")
    }
}