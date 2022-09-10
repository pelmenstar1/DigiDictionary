package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationSectionKeys
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationStaticInfo
import io.github.pelmenstar1.digiDict.common.binarySerialization.key
import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo

object BinarySerializing {
    object SectionKeys : BinarySerializationSectionKeys {
        override val size: Int
            get() = 3

        val records = key<_, Record>(ordinal = 0)
        val badges = key<_, RecordBadgeInfo>(ordinal = 1)
        val badgeToMultipleRecordEntries = key<_, BackupBadgeToMultipleRecordEntry>(ordinal = 2)
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