package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "record_badges")
data class RecordBadgeInfo(
    @PrimaryKey
    val name: String
)