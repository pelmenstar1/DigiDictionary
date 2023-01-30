package io.github.pelmenstar1.digiDict.search

class RecordSearchOptions(val flags: Int) {
    constructor(properties: Array<out RecordSearchProperty>) : this(createFlags(properties))

    companion object {
        const val FLAG_SEARCH_FOR_EXPRESSION = 1
        const val FLAG_SEARCH_FOR_MEANING = 1 shl 1

        internal fun createFlags(properties: Array<out RecordSearchProperty>): Int {
            var flags = 0
            for (property in properties) {
                flags = flags or (1 shl property.ordinal)
            }

            return flags
        }
    }
}