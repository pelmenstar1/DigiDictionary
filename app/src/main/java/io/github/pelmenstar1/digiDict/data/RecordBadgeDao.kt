package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface RecordBadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: RecordBadgeInfo)

    @Delete
    suspend fun delete(value: RecordBadgeInfo)
}