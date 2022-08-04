package io.github.pelmenstar1.digiDict

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.utils.getEnumFieldCount
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFails

// ReflectionTests is executed as Instrumentation test on the Android device
// because Desktop JVM and Android JVM can have differences in Reflection API.
@Suppress("unused")
@RunWith(AndroidJUnit4::class)
class ReflectionTests {
    private enum class EmptyEnum
    private class NotEnum
    private enum class EnumWith1Field { A }
    private enum class EnumWith5Fields { F1, F2, F3, F4, F5 }

    @Test
    fun getEnumFieldCountTest() {
        fun <T : Enum<T>> testCase(c: Class<T>, expectedCount: Int) {
            val actualCount = getEnumFieldCount(c)

            assertEquals(expectedCount, actualCount)
        }

        testCase(EmptyEnum::class.java, 0)
        testCase(EnumWith1Field::class.java, 1)
        testCase(EnumWith5Fields::class.java, 5)
    }

    @Suppress("UNCHECKED_CAST", "TYPE_MISMATCH_WARNING")
    @Test
    fun getEnumFieldCountThrowsWhenClassInNotEnum() {
        assertFails {
            getEnumFieldCount(NotEnum::class.java as Class<out Enum<*>>)
        }
    }

    @Test
    fun getEnumFieldCacheTest() {
        assertEquals(0, getEnumFieldCount<EmptyEnum>())
        assertEquals(0, getEnumFieldCount<EmptyEnum>())
        assertEquals(1, getEnumFieldCount<EnumWith1Field>())
        assertEquals(0, getEnumFieldCount<EmptyEnum>())
        assertEquals(1, getEnumFieldCount<EnumWith1Field>())
        assertEquals(5, getEnumFieldCount<EnumWith5Fields>())
        assertEquals(1, getEnumFieldCount<EnumWith1Field>())
        assertEquals(5, getEnumFieldCount<EnumWith5Fields>())
    }
}