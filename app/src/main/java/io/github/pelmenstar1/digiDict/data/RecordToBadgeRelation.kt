package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "record_to_badge_relations")
class RecordToBadgeRelation(
    @PrimaryKey(autoGenerate = true)
    val relationId: Int = 0,
    val recordId: Int,
    val badgeId: Int
)