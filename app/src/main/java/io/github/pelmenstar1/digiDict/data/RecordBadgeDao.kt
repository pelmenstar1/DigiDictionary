package io.github.pelmenstar1.digiDict.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordBadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: RecordBadgeInfo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(values: Array<out RecordBadgeInfo>)

    @Delete
    suspend fun delete(value: RecordBadgeInfo)

    @Query("SELECT * FROM record_badges")
    fun getAllFlow(): Flow<Array<String>>
}