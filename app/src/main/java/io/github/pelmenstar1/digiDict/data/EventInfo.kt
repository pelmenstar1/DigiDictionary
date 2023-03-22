package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.equalsPattern

/**
 * Stores the information about an event.
 *
 * @param id id of the event
 * @param name name of the event
 * @param startEpochSeconds start time of the event measured in epoch seconds UTC
 * @param endEpochSeconds end time of the event measured in epoch seconds UTC.
 * The event always has a start time but it might not have an end time as it's simply ongoing.
 * In that case, [endEpochSeconds] is `-1`.
 */
@Entity(tableName = "events")
data class EventInfo(
    @PrimaryKey(autoGenerate = true) override val id: Int = 0,
    val name: String,
    val startEpochSeconds: Long,
    val endEpochSeconds: Long
) : EntityWithPrimaryKeyId {
    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        name == o.name && startEpochSeconds == o.startEpochSeconds && endEpochSeconds == o.endEpochSeconds
    }
}