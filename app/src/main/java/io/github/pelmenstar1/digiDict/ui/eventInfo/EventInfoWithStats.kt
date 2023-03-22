package io.github.pelmenstar1.digiDict.ui.eventInfo

data class EventInfoWithStats(
    val id: Int,
    val name: String,
    val startEpochSeconds: Long,
    val endEpochSeconds: Long,
    val totalRecordsAdded: Int
)