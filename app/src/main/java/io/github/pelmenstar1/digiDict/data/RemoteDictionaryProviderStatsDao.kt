package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class RemoteDictionaryProviderStatsDao {
    @Insert
    abstract suspend fun insert(stats: RemoteDictionaryProviderStats)

    @Query("UPDATE remote_dict_provider_stats SET visitCount=:visitCount WHERE id=:id")
    abstract suspend fun update(id: Int, visitCount: Int)

    @Query("SELECT * FROM remote_dict_provider_stats WHERE id=:id")
    abstract suspend fun getStats(id: Int): RemoteDictionaryProviderStats?

    @Query("SELECT id, MAX(visitCount) AS visitCount FROM remote_dict_provider_stats")
    abstract suspend fun getMostUsedProviderStats(): RemoteDictionaryProviderStats?

    @Transaction
    open suspend fun incrementVisitCount(id: Int) {
        val oldStats = getStats(id)

        if(oldStats != null) {
            update(id, oldStats.visitCount + 1)
        } else {
            // If there is no stats entry, then the visit count is supposed to be 0 and after increment it becomes 1.
            insert(RemoteDictionaryProviderStats(id, visitCount = 1))
        }
    }
}