package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordBadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: RecordBadgeInfo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(values: Array<out RecordBadgeInfo>)

    @Query("DELETE FROM record_badges WHERE name=:name")
    suspend fun delete(name: String)

    @Query("SELECT * FROM record_badges")
    fun getAllFlow(): Flow<Array<String>>
}