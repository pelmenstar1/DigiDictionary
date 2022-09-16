package io.github.pelmenstar1.digiDict.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordBadgeDao {
    @Insert
    suspend fun insert(value: RecordBadgeInfo): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(value: RecordBadgeInfo): Long

    @Insert
    suspend fun insertAll(values: Array<out RecordBadgeInfo>)

    @Update
    suspend fun update(value: RecordBadgeInfo)

    @Delete
    suspend fun delete(value: RecordBadgeInfo)

    @Query("SELECT * FROM record_badges ORDER BY id ASC")
    suspend fun getAllOrderByIdAsc(): Array<RecordBadgeInfo>

    @Query("SELECT * FROM record_badges")
    fun getAllFlow(): Flow<Array<RecordBadgeInfo>>

    @Query(GET_BY_ID_QUERY)
    suspend fun getById(id: Int): RecordBadgeInfo?

    @Query(GET_BY_ID_QUERY)
    fun getByIdFlow(id: Int): Flow<RecordBadgeInfo>

    @Query("SELECT * FROM record_badges WHERE id IN (:ids)")
    suspend fun getByIds(ids: IntArray): Array<RecordBadgeInfo>

    @Query("SELECT id FROM record_badges WHERE name=:name")
    suspend fun getIdByName(name: String): Int?

    companion object {
        const val GET_BY_ID_QUERY = "SELECT * FROM record_badges WHERE id=:id"
    }
}