package io.github.pelmenstar1.digiDict.common

import androidx.lifecycle.ViewModel
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

fun ViewModel.databaseObserverFlow(db: RoomDatabase, tables: Array<String>): Flow<Unit> {
    val channel = Channel<Unit>(capacity = Channel.CONFLATED)
    val observer = object : InvalidationTracker.Observer(tables) {
        override fun onInvalidated(tables: MutableSet<String>) {
            channel.trySendBlocking(Unit)
        }
    }

    val invalidationTracker = db.invalidationTracker
    invalidationTracker.addObserver(observer)

    addCloseable {
        invalidationTracker.removeObserver(observer)
    }

    return channel.receiveAsFlow()
}