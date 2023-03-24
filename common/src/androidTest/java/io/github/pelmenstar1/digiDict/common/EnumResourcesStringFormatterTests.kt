package io.github.pelmenstar1.digiDict.common

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.pelmenstar1.digiDict.common.android.EnumResourcesStringFormatter
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals


@RunWith(AndroidJUnit4::class)
class EnumResourcesStringFormatterTests {
    private enum class TestEnum {
        RES_1,
        RES_2,
        RES_3
    }

    private class TestEnumStringFormatter(context: Context) :
        EnumResourcesStringFormatter<TestEnum>(context, TestEnum::class.java) {
        override fun getResourceId(value: TestEnum): Int = when (value) {
            TestEnum.RES_1 -> R.string.test_res_1
            TestEnum.RES_2 -> R.string.test_res_2
            TestEnum.RES_3 -> R.string.test_res_3
        }
    }

    @Test
    fun formatTest() {
        val formatter = TestEnumStringFormatter(InstrumentationRegistry.getInstrumentation().context)

        fun testCase(value: TestEnum, expectedStr: String) {
            val actualStr = formatter.format(value)

            assertEquals(expectedStr, actualStr)
        }

        testCase(TestEnum.RES_1, expectedStr = "Test res 1")
        testCase(TestEnum.RES_2, expectedStr = "Test res 2")
        testCase(TestEnum.RES_3, expectedStr = "Test res 3")

        // To test the cache.
        testCase(TestEnum.RES_2, expectedStr = "Test res 2")
        testCase(TestEnum.RES_3, expectedStr = "Test res 3")
        testCase(TestEnum.RES_1, expectedStr = "Test res 1")
    }
}