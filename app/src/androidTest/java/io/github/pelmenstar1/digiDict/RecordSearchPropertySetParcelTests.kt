package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.commonTestUtils.ParcelTestHelper
import io.github.pelmenstar1.digiDict.search.RecordSearchProperty
import io.github.pelmenstar1.digiDict.search.RecordSearchPropertySet
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordSearchPropertySetParcelTests {
    @Test
    fun readWriteTest() {
        fun testCase(elements: Array<RecordSearchProperty>) {
            val set = RecordSearchPropertySet(elements)

            ParcelTestHelper.readWriteTest(set, RecordSearchPropertySet.CREATOR)
        }

        testCase(emptyArray())
        testCase(arrayOf(RecordSearchProperty.EXPRESSION))
        testCase(arrayOf(RecordSearchProperty.MEANING))
        testCase(arrayOf(RecordSearchProperty.EXPRESSION, RecordSearchProperty.MEANING))
    }
}