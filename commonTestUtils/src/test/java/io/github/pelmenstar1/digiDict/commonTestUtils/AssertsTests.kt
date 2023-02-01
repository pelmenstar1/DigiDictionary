package io.github.pelmenstar1.digiDict.commonTestUtils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertFailsWith

class AssertsTests {
    @Test
    fun assertTimeoutShouldThrowTest(): Unit = runBlocking {
        assertFailsWith<AssertionError> {
            assertTimeout(timeout = 100) {}
        }
    }

    @Test
    fun assertTimeoutShouldNotThrowTest(): Unit = runBlocking {
        assertTimeout(timeout = 100) {
            delay(200)
        }
    }
}