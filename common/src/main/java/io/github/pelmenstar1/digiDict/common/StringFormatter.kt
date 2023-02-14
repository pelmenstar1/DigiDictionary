package io.github.pelmenstar1.digiDict.common

/**
 * Provides a single method [format] that returns string representation of any object of type [T].
 */
fun interface StringFormatter<in T> {
    /**
     * Returns a string representation of specified [value].
     */
    fun format(value: T): String
}