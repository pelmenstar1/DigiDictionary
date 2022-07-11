package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComplexMeaningParcelTests {
    @Test
    fun commonReadWrite() {
        fun testCase(value: ComplexMeaning.Common) {
            ParcelTestHelper.readWriteTest(value, ComplexMeaning.Common)
        }

        testCase(ComplexMeaning.Common(""))
        testCase(ComplexMeaning.Common("123"))
    }

    @Test
    fun listReadWrite() {
        fun testCase(vararg elements: String) {
            ParcelTestHelper.readWriteTest(
                ComplexMeaning.List(elements),
                ComplexMeaning.List
            )
        }

        testCase()
        testCase("1")
        testCase("1", "2", "3")
    }
}