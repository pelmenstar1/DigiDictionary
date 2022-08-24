package io.github.pelmenstar1.digiDict.common

@Suppress("UNCHECKED_CAST")
inline fun <T : Any> T.equalsPattern(other: Any?, comparison: (other: T) -> Boolean): Boolean {
    if (other === this) return true
    if (other == null || javaClass != other.javaClass) return false

    return comparison(other as T)
}