package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchPreparedRecordDao {
    @Query("SELECT * FROM search_prepared_records ORDER BY id DESC")
    suspend fun getAllOrderById(): Array<SearchPreparedRecord>

    @Query("SELECT * FROM search_prepared_records ORDER BY id DESC")
    fun getAllOrderByIdFlow(): Flow<Array<SearchPreparedRecord>>

    @Insert
    suspend fun insert(value: SearchPreparedRecord)

    @Update
    suspend fun update(value: SearchPreparedRecord)

    @Query("DELETE FROM search_prepared_records WHERE id=:id")
    suspend fun deleteById(id: Int)
}