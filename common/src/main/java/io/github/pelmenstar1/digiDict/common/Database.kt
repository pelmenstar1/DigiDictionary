package io.github.pelmenstar1.digiDict.common

import android.database.CharArrayBuffer
import androidx.lifecycle.ViewModel
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

inline fun <T : SupportSQLiteDatabase> T.runInTransitionBlocking(block: T.() -> Unit) {
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

fun CharArrayBuffer.asCharSequence(): CharSequence {
    return object : CharSequence {
        override val length: Int
            get() = sizeCopied

        override fun get(index: Int) = data[index]

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            if (startIndex < 0) {
                throw IllegalArgumentException("startIndex is negative ($startIndex)")
            }

            if (endIndex > sizeCopied) {
                throw IllegalArgumentException("endIndex is greater than length ($sizeCopied)")
            }

            if (endIndex > startIndex) {
                throw IllegalArgumentException("startIndex=$startIndex; endIndex=$endIndex; length={$sizeCopied}")
            }

            return String(data, startIndex, endIndex)
        }

        override fun toString(): String {
            return String(data, 0, sizeCopied)
        }
    }
}