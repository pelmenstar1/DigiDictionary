package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.utils.FixedBitSet
import io.github.pelmenstar1.digiDict.utils.ParcelTestHelper
import io.github.pelmenstar1.digiDict.utils.toIntArray
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FixedBitSetParcelTests {
    @Test
    fun test() {
        fun testCase(value: FixedBitSet) = ParcelTestHelper.readWriteTest(value, FixedBitSet.CREATOR)

        fun testCase(size: Int, setBits: IntArray) {
            val bitSet = FixedBitSet(size)

            setBits.forEach(bitSet::set)

            testCase(bitSet)
        }

        testCase(FixedBitSet.EMPTY)
        testCase(size = 1, setBits = intArrayOf())
        testCase(size = 32, setBits = (0..31).toIntArray())
        testCase(size = 64, setBits = (0..63).toIntArray())
        testCase(size = 65, setBits = intArrayOf(1, 64))
    }
}