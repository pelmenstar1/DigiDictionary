package io.github.pelmenstar1.digiDict.utils

import android.os.Looper
import androidx.lifecycle.ViewModel
import io.github.pelmenstar1.digiDict.data.AppDatabase
import io.github.pelmenstar1.digiDict.data.EntityWithPrimaryKeyId
import io.github.pelmenstar1.digiDict.widgets.AppWidgetUpdater
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.assertEquals
import kotlin.test.fail

fun <T : EntityWithPrimaryKeyId<T>> assertContentEqualsNoId(expected: Array<T>, actual: Array<T>) {
    val expectedSize = expected.size
    val actualSize = actual.size

    if (expectedSize != actualSize) {
        fail("Size of expected array differs from size of actual array. Expected size: $expectedSize, actual size: $actualSize")
    }

    for (i in 0 until expectedSize) {
        val expectedElement = expected[i]
        val actualElement = actual[i]

        if (!expectedElement.equalsNoId(actualElement)) {
            fail("Elements at index $i differ: Expected element: '$expectedElement' \n Actual element: '$actualElement'")
        }
    }
}

fun IntRange.toIntArray() = toList().toIntArray()

fun assertOnMainThread() {
    assertEquals(Looper.getMainLooper().thread, Thread.currentThread())
}

fun ViewModel.clearThroughReflection() {
    val method = ViewModel::class.java.getDeclaredMethod("clear")
    method.isAccessible = true

    method.invoke(this)
}

inline fun <T : ViewModel> T.use(block: (vm: T) -> Unit) {
    try {
        block(this)
    } finally {
        clearThroughReflection()
    }
}

fun assertOnMainThreadAndClear(vm: ViewModel) {
    try {
        assertOnMainThread()
    } finally {
        vm.clearThroughReflection()
    }
}

suspend fun Event.setHandlerAndWait(block: () -> Unit) {
    suspendCoroutine<Unit> {
        handler = {
            block()
            it.resume(Unit)
        }
    }
}

suspend fun <T : ViewModel> assertEventHandlerOnMainThread(
    vm: T,
    event: Event,
    triggerAction: T.() -> Unit
) {
    suspendCoroutine<Unit> {
        event.handler = {
            assertOnMainThreadAndClear(vm)

            it.resume(Unit)
        }

        vm.triggerAction()
    }
}

suspend fun <T : ViewModel> assertAppWidgetUpdateCalledOnMainThread(
    createVm: (AppWidgetUpdater) -> T,
    triggerAction: T.() -> Unit
) {
    suspendCoroutine<Unit> {
        var vm: T? = null
        val updater = object : AppWidgetUpdater {
            override fun updateAllWidgets() {
                assertOnMainThreadAndClear(vm!!)
                it.resume(Unit)
            }
        }

        vm = createVm(updater)
        vm.triggerAction()
    }
}

fun AppDatabase.reset() {
    clearAllTables()

    query("DELETE FROM sqlite_sequence", null)
}