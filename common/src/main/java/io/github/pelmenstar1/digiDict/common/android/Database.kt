package io.github.pelmenstar1.digiDict.common.android

import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.closeInFinally
import io.github.pelmenstar1.digiDict.common.unsafeNewArray

inline fun <T : SupportSQLiteDatabase> T.runInTransactionBlocking(block: T.() -> Unit) {
    beginTransaction()

    try {
        block()

        setTransactionSuccessful()
    } finally {
        endTransaction()
    }
}

inline fun ViewModel.onDatabaseTablesUpdated(
    db: RoomDatabase,
    tables: Array<out String>,
    crossinline callback: () -> Unit
) {
    val observer = object : InvalidationTracker.Observer(tables) {
        override fun onInvalidated(tables: MutableSet<String>) {
            callback()
        }
    }

    val invalidationTracker = db.invalidationTracker
    invalidationTracker.addObserver(observer)

    addCloseable {
        invalidationTracker.removeObserver(observer)
    }
}

inline fun <reified T : Any> RoomDatabase.queryArrayWithProgressReporter(
    sql: String,
    bindArgs: Array<Any>?,
    progressReporter: ProgressReporter?,
    convertCursor: (Cursor) -> T
): Array<T> {
    var cursor: Cursor? = null
    var cause: Exception? = null

    try {
        progressReporter?.start()

        cursor = query(sql, bindArgs)
        val count = cursor.count
        val result = unsafeNewArray<T>(count)

        for (i in 0 until count) {
            cursor.moveToPosition(i)
            result[i] = convertCursor(cursor)

            progressReporter?.onProgress(i + 1, count)
        }

        return result
    } catch (e: Exception) {
        cause = e
        progressReporter?.reportError()

        throw e
    } finally {
        cursor.closeInFinally(cause)
    }
}