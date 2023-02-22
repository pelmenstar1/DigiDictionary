package io.github.pelmenstar1.digiDict.backup

import io.github.pelmenstar1.digiDict.common.unsafeNewArray

class BadgeNameToIdMap(capacity: Int) {
    private val names = unsafeNewArray<String>(capacity)
    private val ids = IntArray(capacity)
    private var size = 0

    fun getIdByName(value: String): Int {
        val index = names.indexOf(value)

        return if (index >= 0) ids[index] else -1
    }

    fun add(name: String, id: Int) {
        val index = size

        names[index] = name
        ids[index] = id

        size = index + 1
    }
}