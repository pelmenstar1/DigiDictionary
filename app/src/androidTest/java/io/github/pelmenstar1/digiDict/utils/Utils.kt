package io.github.pelmenstar1.digiDict.utils

import androidx.lifecycle.ViewModel
import io.github.pelmenstar1.digiDict.common.*
import io.github.pelmenstar1.digiDict.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlin.test.fail

fun <T : EntityWithPrimaryKeyId> assertContentEqualsNoId(expected: Array<T>, actual: Array<T>) {
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

fun AppDatabase.reset() {
    clearAllTables()

    query("DELETE FROM sqlite_sequence", null)
}

suspend fun AppDatabase.addRecordAndBadges(
    record: Record,
    badges: Array<RecordBadgeInfo>
): RecordWithBadges {
    val recordDao = recordDao()
    val recordBadgeDao = recordBadgeDao()
    val recordToBadgeRelationDao = recordToBadgeRelationDao()

    val recordWithBadges = RecordWithBadges.create(record, badges)

    recordDao.insert(record)
    recordBadgeDao.insertAll(badges)
    recordToBadgeRelationDao.insertAll(
        badges.mapToArray { RecordToBadgeRelation(0, record.id, it.id) }
    )

    return recordWithBadges
}

suspend fun ViewModelAction.waitForResult() {
    try {
        coroutineScope {
            launchFlowCollector(errorFlow) { throw it }
            launchFlowCollector(successFlow) {
                cancel()
            }
        }
    } catch (e: Throwable) {
        if (e !is CancellationException) {
            throw e
        }
    }
}

suspend fun NoArgumentViewModelAction.runAndWaitForResult() {
    run()
    waitForResult()
}

suspend fun <T> SingleArgumentViewModelAction<T>.runAndWaitForResult(arg: T) {
    run(arg)
    waitForResult()
}