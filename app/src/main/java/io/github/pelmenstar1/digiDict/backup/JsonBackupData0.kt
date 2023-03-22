package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import kotlinx.serialization.Serializable

/**
 * Copies all the fields of [BackupData] when version is 0.
 */
@Serializable
class JsonBackupData0(
    val records: Array<out Record>,
    val badges: Array<out RecordBadgeInfo>,
    val badgeToMultipleRecordEntries: Array<out BackupBadgeToMultipleRecordEntry>
)