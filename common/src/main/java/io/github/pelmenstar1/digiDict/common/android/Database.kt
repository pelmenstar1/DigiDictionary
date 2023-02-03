package io.github.pelmenstar1.digiDict.common.android

import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.pelmenstar1.digiDict.common.ProgressReporter
import io.github.pelmenstar1.digiDict.common.trackLoopProgressWith
import io.github.pelmenstar1.digiDict.common.trackProgressWith
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

// TODO: The method emits three try-catch blocks but apparently there can be only one.
inline fun <reified T : Any> RoomDatabase.queryArrayWithProgressReporter(
    sql: String,
    bindArgs: Array<Any>?,
    progressReporter: ProgressReporter?,
    convertCursor: (Cursor) -> T
): Array<T> {
    return trackProgressWith(progressReporter) {
        val cursor = query(sql, bindArgs)

        cursor.use {
            val count = it.count
            val result = unsafeNewArray<T>(count)

            trackLoopProgressWith(progressReporter, count) { i ->
                cursor.moveToPosition(i)
                result[i] = convertCursor(cursor)
            }

            result
        }
    }
}