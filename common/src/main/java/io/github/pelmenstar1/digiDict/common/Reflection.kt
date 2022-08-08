package io.github.pelmenstar1.digiDict.common

import androidx.collection.SimpleArrayMap

inline fun <reified T : Enum<T>> getEnumFieldCount() = getEnumFieldCount(T::class.java)

// The default constructor with no parameters initializes the map with 0 capacity.
// getEnumFieldCount() will be used frequently in the app, so 8 is the best value for initial capacity.
//
// Side note: java.lang.Integer's instances are cached for ints -128..128
// I don't think there is an enum with more than 128 values, which means
// wrapping Int won't allocate at all in this case.
private val enumFieldCountCache = SimpleArrayMap<Class<out Enum<*>>, Int>(8)

fun <T : Enum<T>> getEnumFieldCount(c: Class<out T>): Int {
    val cache = enumFieldCountCache

    var count = cache[c]

    if (count == null) {
        // Although T is constrained to be enum, specified class still can be not enum
        // (example: getEnumFieldCount(NotEnum::class.java as Class<out Enum<*>>))
        val enumConstants = c.enumConstants ?: throw IllegalArgumentException("Specified class is not enum")

        count = enumConstants.size
        cache.put(c, count)
    }

    return count
}