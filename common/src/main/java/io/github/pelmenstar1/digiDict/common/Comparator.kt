package io.github.pelmenstar1.digiDict.common

inline fun <T, R : Comparable<R>> createComparatorFromField(crossinline field: (T) -> R): Comparator<T> {
    return Comparator { a, b -> field(a).compareTo(field(b)) }
}

inline fun <T, R : Comparable<R>> createComparatorFromFieldInverted(crossinline field: (T) -> R): Comparator<T> {
    return Comparator { a, b -> field(b).compareTo(field(a)) }
}