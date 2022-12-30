package io.github.pelmenstar1.digiDict.common

/**
 * Responsible for mapping value of enum [T] to string
 */
interface MessageMapper<T : Enum<T>> {
    fun map(type: T): String
}