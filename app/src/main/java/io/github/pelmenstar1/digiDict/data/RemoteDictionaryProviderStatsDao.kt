package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class RemoteDictionaryProviderStatsDao {
    @Query("SELECT * FROM remote_dict_provider_stats WHERE id=:id")
    abstract suspend fun getById(id: Int): RemoteDictionaryProviderStats?

    @Insert
    abstract suspend fun insert(stats: RemoteDictionaryProviderStats)

    @Query("UPDATE remote_dict_provider_stats SET visitCount = visitCount + 1 WHERE id=:id")
    abstract suspend fun updateIncrementVisitCount(id: Int): Int

    @Query("SELECT id, MAX(visitCount) AS visitCount FROM remote_dict_provider_stats")
    abstract suspend fun getMostUsedProviderStats(): RemoteDictionaryProviderStats?

    @Transaction
    open suspend fun incrementVisitCount(id: Int) {
        val updatedCount = updateIncrementVisitCount(id)

        // Then there's no entry with given id, need to insert new one.
        if (updatedCount == 0) {
            // If there's no stats entry, then the visit count is supposed to be 0 and after increment it becomes 1.
            insert(RemoteDictionaryProviderStats(id, visitCount = 1))
        }
    }
}