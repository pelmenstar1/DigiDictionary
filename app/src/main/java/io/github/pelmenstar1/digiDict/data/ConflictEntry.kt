package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class ConflictEntry(
    val id: Int,
    val expression: String,
    val oldRawMeaning: String,
    val oldAdditionalNotes: String,
    val oldScore: Int,
    val oldEpochSeconds: Long,
    val newRawMeaning: String,
    val newAdditionalNotes: String,
    val newScore: Int,
    val newEpochSeconds: Long,
) {
    companion object {
        fun fromRecordPair(id: Int, old: Record, new: Record): ConflictEntry {
            return ConflictEntry(
                id,
                old.expression,
                old.rawMeaning, old.additionalNotes, old.score, old.epochSeconds,
                new.rawMeaning, new.additionalNotes, new.score, new.epochSeconds
            )
        }
    }
}