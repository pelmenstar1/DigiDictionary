package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializationStaticInfo
import io.github.pelmenstar1.digiDict.common.binarySerialization.connectedWith
import io.github.pelmenstar1.digiDict.common.binarySerialization.key
import io.github.pelmenstar1.digiDict.data.Record

object BinarySerializing {
    object Sections : BinarySerializationStaticInfo.SectionsInfo {
        override val count: Int
            get() = 1

        val records = key<Record>(ordinal = 0)
    }

    val staticInfo = BinarySerializationStaticInfo(
        Sections,
        Record.SERIALIZER_RESOLVER connectedWith Sections.records
    )
}