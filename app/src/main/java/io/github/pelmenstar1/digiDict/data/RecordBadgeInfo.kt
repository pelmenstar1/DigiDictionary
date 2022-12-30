package io.github.pelmenstar1.digiDict.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.common.android.readStringOrThrow
import io.github.pelmenstar1.digiDict.common.binarySerialization.BinarySerializerResolver
import io.github.pelmenstar1.digiDict.common.equalsPattern
import kotlinx.serialization.Serializable

@Entity(tableName = "record_badges", indices = [Index("name", unique = true)])
@Serializable
class RecordBadgeInfo : Parcelable, EntityWithPrimaryKeyId {
    @PrimaryKey(autoGenerate = true)
    override val id: Int
    val name: String
    val outlineColor: Int

    constructor(id: Int = 0, name: String, outlineColor: Int) {
        this.id = id
        this.name = name
        this.outlineColor = outlineColor
    }

    constructor(source: Parcel) {
        id = source.readInt()
        name = source.readStringOrThrow()
        outlineColor = source.readInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.run {
            writeInt(id)
            writeString(name)
            writeInt(outlineColor)
        }
    }

    override fun describeContents() = 0

    override fun equals(other: Any?) = equalsPattern(other) { o ->
        return o.id == id && o.name == name && o.outlineColor == outlineColor
    }

    override fun equalsNoId(other: Any?) = equalsPattern(other) { o ->
        o.name == name && o.outlineColor == outlineColor
    }

    override fun hashCode(): Int {
        var result = id
        result = result * 31 + name.hashCode()
        result = result * 31 + outlineColor

        return result
    }

    override fun toString(): String {
        return "RecordBadgeInfo(id=$id, name='$name', outlineColor=$outlineColor)"
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<RecordBadgeInfo> {
            override fun createFromParcel(parcel: Parcel) = RecordBadgeInfo(parcel)
            override fun newArray(size: Int) = arrayOfNulls<RecordBadgeInfo>(size)
        }

        val SERIALIZER_RESOLVER = BinarySerializerResolver<RecordBadgeInfo> {
            register<RecordBadgeInfo>(
                version = 1,
                write = { value: RecordBadgeInfo ->
                    emit(value.name)
                    emit(value.outlineColor)
                },
                read = {
                    val name = consumeStringUtf16()
                    val color = consumeInt()

                    RecordBadgeInfo(0, name, color)
                }
            )
        }
    }
}