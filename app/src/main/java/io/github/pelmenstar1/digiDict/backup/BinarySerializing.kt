package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationStaticInfo
import io.github.pelmenstar1.digiDict.common.binarySerialization.SimpleBinarySerializationSectionKeys
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

object BinarySerializing {
    object SectionKeys : SimpleBinarySerializationSectionKeys<SectionKeys>() {
        val records = key<Record>(ordinal = 0, name = "records")
        val badges = key<RecordBadgeInfo>(ordinal = 1, name = "badges")
        val badgeToMultipleRecordEntries =
            key<BackupBadgeToMultipleRecordEntry>(ordinal = 2, name = "badgeToMultipleRecordEntries")

        override fun getAll() = arrayOf(records, badges, badgeToMultipleRecordEntries)
    }

    val staticInfo = BinarySerializationStaticInfo(keys = SectionKeys) {
        section(
            key = SectionKeys.records,
            resolver = Record.SERIALIZER_RESOLVER
        )

        section(
            key = SectionKeys.badges,
            resolver = RecordBadgeInfo.SERIALIZER_RESOLVER
        )

        section(
            key = SectionKeys.badgeToMultipleRecordEntries,
            resolver = BackupBadgeToMultipleRecordEntry.SERIALIZER_RESOLVER
        )
    }
}