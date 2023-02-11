package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
abstract class EventDao {
    @Insert
    abstract suspend fun insert(value: EventInfo)

    @Update
    abstract suspend fun update(value: EventInfo)

    @Query("UPDATE events SET endEpochSeconds=:endEpochSeconds WHERE id=:id")
    abstract suspend fun updateEndEpochSecondsById(id: Int, endEpochSeconds: Long)

    @Query("DELETE FROM events WHERE id=:id")
    abstract suspend fun delete(id: Int)

    @Query("SELECT * FROM events")
    abstract fun getAllFlow(): Flow<Array<EventInfo>>

    @Query("SELECT * FROM events WHERE id=:id")
    abstract suspend fun getById(id: Int): EventInfo?

    @Query("SELECT name FROM events")
    abstract suspend fun getAllNames(): Array<String>

    @Query("SELECT COUNT(*) FROM events WHERE endEpochSeconds=-1")
    abstract fun notEndedEventCountFlow(): Flow<Int>

    open fun isAllEventsEndedFlow(): Flow<Boolean> {
        return notEndedEventCountFlow().map { it == 0 }
    }


}