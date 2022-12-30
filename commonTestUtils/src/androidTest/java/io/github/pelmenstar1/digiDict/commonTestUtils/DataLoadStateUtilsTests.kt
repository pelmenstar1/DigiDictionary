package io.github.pelmenstar1.digiDict.commonTestUtils

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFails

@RunWith(AndroidJUnit4::class)
class DataLoadStateUtilsTests {
    @Test
    fun waitUntilSuccessOrThrowOnError_success() = runTest {
        val expectedValue = Any()

        val manager = DataLoadStateManager<Any>("TAG")
        val flow = manager.buildFlow(CoroutineScope(Dispatchers.Main)) {
            fromAction { expectedValue }
        }

        val actualValue = flow.waitUntilSuccessOrThrowOnError()

        assertEquals(expectedValue, actualValue)
    }

    @Test
    fun waitUntilSuccessOrThrowOnError_error() = runTest {
        val manager = DataLoadStateManager<Any>("TAG")
        val flow = manager.buildFlow(CoroutineScope(Dispatchers.Main)) {
            fromAction {
                throw Exception()
            }
        }

        assertFails {
            flow.waitUntilSuccessOrThrowOnError()
        }
    }
}