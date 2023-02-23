package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.indexOf
import io.github.pelmenstar1.digiDict.data.EntityWithPrimaryKeyId

/**
 * Represents a simple map which is able to transform entity id to its ordinal number and vise versa.
 *
 * The map is more efficient when ids are consecutive and are in ascending order.
 * In that case, it doesn't create the supplementary array and doesn't perform either linear or binary search.
 */
class IdToOrdinalMap {
    // If null, it means that map's ids are consecutive. If not null, it holds the array of ids
    // that allows to find ordinal (position) of specific id
    internal var idArray: IntArray? = null
    private val capacity: Int

    // Amount of ids added
    private var index = 0

    // The first id
    private var start = 0

    // Marks whether idArray, if not null, is sorted in ascending order.
    internal var isSorted = true

    /**
     * Constructs [IdToOrdinalMap] instance with specified [capacity] of the map.
     *
     * @param capacity maximum amount of elements that can be added to the map, shouldn't be negative.
     */
    constructor(capacity: Int) {
        if (capacity < 0) {
            throw IllegalArgumentException("capacity < 0")
        }

        this.capacity = capacity
    }

    /**
     * Constructs the [IdToOrdinalMap] instance with specified [values] array from which ids are extracted.
     *
     * Note that it will be the final state of the map and it can't be no longer mutated.
     */
    constructor(values: Array<out EntityWithPrimaryKeyId>) {
        capacity = values.size

        values.forEach { add(it.id) }
    }

    /**
     * Constructs the [IdToOrdinalMap] instance with specified array of ids.
     *
     * Note that it will be the final state of the map and it can't be no longer mutated.
     */
    constructor(ids: IntArray) {
        capacity = ids.size

        ids.forEach(::add)
    }

    /**
     * Adds given [id] to the map. Note that if the capacity is reached, it throws [IllegalStateException].
     */
    fun add(id: Int) {
        val idx = index
        val cap = capacity

        if (idx == cap) {
            throw IllegalStateException("Cannot add item because the capacity has been reached")
        }

        if (idx == 0) {
            start = id
        } else {
            val st = start
            var ids = idArray

            // Check if by adding this id, all ids remain consecutive. If not, fallback to using array.
            if (ids == null && idx + st != id) {
                ids = IntArray(cap)

                // Fill the array with ids that have been added so far.
                for (i in 0 until idx) {
                    ids[i] = st + i
                }

                idArray = ids
            }

            if (ids != null) {
                // idx > 0, so no underflow.
                val prevId = ids[idx - 1]
                ids[idx] = id

                if (prevId > id) {
                    isSorted = false
                }
            }
        }

        index = idx + 1
    }

    /**
     * Gets specified id's ordinal position in the map.
     * In other words, if [add] operation is the Nth operation applied to the map, it returns N.
     *
     * If given [id] is not in the map, the method returns negative value (not necessarily `-1`)
     */
    fun getOrdinalById(id: Int): Int {
        val ids = idArray
        val idx = index

        return if (ids != null) {
            if (isSorted) {
                ids.binarySearch(id, 0, idx)
            } else {
                ids.indexOf(id, 0, idx)
            }
        } else {
            val st = start
            val ordinal = id - st

            if (ordinal !in 0 until idx) {
                return -1
            }

            ordinal
        }
    }

    /**
     * Gets id that is located at specified [ordinal] (position) in the map.
     */
    fun getIdByOrdinal(ordinal: Int): Int {
        val ids = idArray

        if (ordinal !in 0 until index) {
            throw IndexOutOfBoundsException("ordinal")
        }

        return if (ids != null) {
            ids[ordinal]
        } else {
            start + ordinal
        }
    }
}