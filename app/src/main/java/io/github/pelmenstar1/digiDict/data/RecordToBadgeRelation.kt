package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "record_to_badge_relations")
data class RecordToBadgeRelation(
    @PrimaryKey(autoGenerate = true)
    val relationId: Int = 0,
    val recordId: Int,
    val badgeId: Int
) {
    fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        recordId == o.recordId && badgeId == o.badgeId
    }
}