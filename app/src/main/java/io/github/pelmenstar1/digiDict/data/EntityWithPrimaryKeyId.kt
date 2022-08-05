package io.github.pelmenstar1.digiDict.data

interface EntityWithPrimaryKeyId<TSelf : EntityWithPrimaryKeyId<TSelf>> {
    fun equalsNoId(other: TSelf): Boolean
}