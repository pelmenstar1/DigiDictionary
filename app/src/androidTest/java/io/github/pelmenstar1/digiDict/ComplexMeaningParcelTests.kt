package io.github.pelmenstar1.digiDict

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.data.ComplexMeaning
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ComplexMeaningParcelTests {
    private fun readWriteTest(value: ComplexMeaning) {
        val parcel = Parcel.obtain()
        value.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdFromParcel = ComplexMeaning.CREATOR.createFromParcel(parcel)

        assertEquals(value, createdFromParcel)
    }

    @Test
    fun commonReadWrite() {
        fun testCase(value: ComplexMeaning) {
            readWriteTest(value)
        }

        testCase(ComplexMeaning.common(""))
        testCase(ComplexMeaning.common("123"))
    }

    @Test
    fun listReadWrite() {
        fun testCase(vararg elements: String) {
            readWriteTest(ComplexMeaning.list(elements))
        }

        testCase()
        testCase("1")
        testCase("1", "2", "3")
    }
}