package io.github.pelmenstar1.digiDict.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteDictionaryProviderDao {
    @Insert
    suspend fun insert(value: RemoteDictionaryProviderInfo)

    @Delete
    suspend fun delete(value: RemoteDictionaryProviderInfo)

    @Query("SELECT * FROM remote_dict_providers")
    fun getAllFlow(): Flow<Array<RemoteDictionaryProviderInfo>>

    @Query("SELECT * FROM remote_dict_providers")
    suspend fun getAll(): Array<RemoteDictionaryProviderInfo>

    @Query(
        """
        SELECT rdp.id AS id, rdp.name AS name, rdp.schema AS schema, rdp.urlEncodingRules AS urlEncodingRules 
        FROM remote_dict_providers AS rdp 
        LEFT JOIN remote_dict_provider_stats AS rdp_stats ON rdp.id=rdp_stats.id
        ORDER BY rdp_stats.visitCount DESC
        """
    )
    suspend fun getMostUsedProviders(): Array<RemoteDictionaryProviderInfo>

    @Query("SELECT * FROM remote_dict_providers WHERE name=:name")
    suspend fun getByName(name: String): RemoteDictionaryProviderInfo?
}