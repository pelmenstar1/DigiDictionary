package io.github.pelmenstar1.digiDict

import io.github.pelmenstar1.digiDict.data.Record

data class RecordNoId(
    val expression: String,
    val rawMeaning: String,
    val additionalNotes: String,
    val score: Int,
    val epochSeconds: Long
) {
    companion object {
        fun fromRecord(value: Record) = RecordNoId(
            value.expression,
            value.rawMeaning,
            value.additionalNotes,
            value.score,
            value.epochSeconds
        )
    }
}
