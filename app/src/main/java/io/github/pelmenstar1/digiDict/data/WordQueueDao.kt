package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WordQueueDao {
    @Query("SELECT * FROM word_queue")
    fun getAllFlow(): Flow<Array<WordQueueEntry>>

    @Query("SELECT word FROM word_queue")
    suspend fun getAllWords(): Array<String>

    @Insert
    suspend fun insert(entry: WordQueueEntry)

    @Query("DELETE FROM word_queue WHERE id=:id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM word_queue WHERE word=:word")
    suspend fun deleteByWord(word: String)
}