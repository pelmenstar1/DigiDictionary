package io.github.pelmenstar1.digiDict.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordBadgeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(value: RecordBadgeInfo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(values: Array<out RecordBadgeInfo>)

    @Update
    suspend fun update(value: RecordBadgeInfo)

    @Delete
    suspend fun delete(value: RecordBadgeInfo)

    @Query("SELECT * FROM record_badges")
    fun getAllFlow(): Flow<Array<RecordBadgeInfo>>

    @Query("SELECT * FROM record_badges WHERE id=:id")
    fun getByIdFlow(id: Int): Flow<RecordBadgeInfo>

    @Query("SELECT * FROM record_badges WHERE id IN (:ids)")
    suspend fun getByIds(ids: IntArray): Array<RecordBadgeInfo>
}