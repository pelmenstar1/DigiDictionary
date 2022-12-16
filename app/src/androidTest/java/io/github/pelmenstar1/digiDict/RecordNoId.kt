package io.github.pelmenstar1.digiDict

data class RecordNoId(
    val expression: String,
    val rawMeaning: String,
    val additionalNotes: String,
    val score: Int,
    val epochSeconds: Long
)
