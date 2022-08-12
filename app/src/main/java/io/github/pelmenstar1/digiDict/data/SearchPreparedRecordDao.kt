package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SearchPreparedRecordDao {
    @Query("SELECT keywords FROM search_prepared_records")
    suspend fun getAllKeywords(): Array<String>

    @Insert
    suspend fun insert(value: SearchPreparedRecord)

    @Update
    suspend fun update(value: SearchPreparedRecord)

    @Query("DELETE FROM search_prepared_records WHERE id=:id")
    suspend fun deleteById(id: Int)
}