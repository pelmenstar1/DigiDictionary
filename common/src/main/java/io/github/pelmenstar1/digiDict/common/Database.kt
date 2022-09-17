package io.github.pelmenstar1.digiDict.common

import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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
    tables: Array<String>,
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
    try {
        val cursor = query(sql, bindArgs)

        return cursor.use {
            val count = it.count
            val result = unsafeNewArray<T>(count)

            trackLoopProgressWith(progressReporter, count) { i ->
                cursor.moveToPosition(i)
                result[i] = convertCursor(cursor)
            }

            result
        }
    } catch (th: Throwable) {
        progressReporter?.reportError()
        throw th
    }
}