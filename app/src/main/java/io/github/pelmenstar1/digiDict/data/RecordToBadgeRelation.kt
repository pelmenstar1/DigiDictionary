package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializerResolver
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "record_to_badge_relations")
class RecordToBadgeRelation(
    @PrimaryKey(autoGenerate = true)
    val relationId: Int = 0,
    val recordId: Int,
    val badgeId: Int
) {
    companion object {
        val SERIALIZER_RESOLVER = BinarySerializerResolver<RecordToBadgeRelation> {
            register<RecordToBadgeRelation>(
                version = 1,
                write = { value ->
                    emit(value.recordId)
                    emit(value.badgeId)
                },
                read = {
                    val recordId = consumeInt()
                    val badgeId = consumeInt()

                    RecordToBadgeRelation(0, recordId, badgeId)
                }
            )
        }
    }
}