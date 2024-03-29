package io.github.pelmenstar1.digiDict.commonTestUtils

import kotlinx.coroutines.*
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.fail

suspend fun assertTimeout(timeout: Int, block: suspend () -> Unit) = coroutineScope {
    launch(Dispatchers.Default) {
        try {
            withTimeout(timeout.toLong()) {
                block()
            }
            fail("The block should timeout")
        } catch (_: TimeoutCancellationException) {
        }
    }
}

fun <T> assertSameIf(condition: Boolean, expected: T, actual: T) {
    if (condition) assertSame(expected, actual) else assertNotSame(expected, actual)
}