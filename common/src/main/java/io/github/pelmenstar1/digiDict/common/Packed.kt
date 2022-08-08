package io.github.pelmenstar1.digiDict.common

fun PackedIntRange(start: Int, end: Int): PackedIntRange {
    return PackedIntRange((start.toLong() shl 32) or (end.toLong() and 0xFFFFFFFFL))
}

@Suppress("NOTHING_TO_INLINE")
@JvmInline
value class PackedIntRange(@JvmField val bits: Long) {
    val start: Int
        get() = (bits shr 32).toInt()

    val end: Int
        get() = (bits and 0xFFFFFFFFL).toInt()

    inline operator fun component1() = start
    inline operator fun component2() = end
}