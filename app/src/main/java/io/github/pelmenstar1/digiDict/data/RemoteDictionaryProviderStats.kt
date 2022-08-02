package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_dict_provider_stats")
class RemoteDictionaryProviderStats(
    @PrimaryKey val id: Int,
    val visitCount: Int
)