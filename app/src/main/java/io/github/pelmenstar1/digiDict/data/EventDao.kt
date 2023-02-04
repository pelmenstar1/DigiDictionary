package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface EventDao {
    @Insert
    suspend fun insert(value: EventInfo)
}