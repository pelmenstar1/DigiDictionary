package io.github.pelmenstar1.digiDict.data

interface EntityWithPrimaryKeyId {
    val id: Int

    override fun equals(other: Any?): Boolean
    fun equalsNoId(other: Any?): Boolean

    override fun hashCode(): Int
}