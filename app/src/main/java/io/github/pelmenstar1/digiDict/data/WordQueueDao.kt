package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WordQueueDao {
    @Query("SELECT * FROM word_queue")
    suspend fun getAll(): Array<WordQueueDao>

    @Insert
    suspend fun insert(entry: WordQueueEntry)

    @Query("DELETE FROM word_queue WHERE id=:id")
    suspend fun deleteById(id: Int)
}