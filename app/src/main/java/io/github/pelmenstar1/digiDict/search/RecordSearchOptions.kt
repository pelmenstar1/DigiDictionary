package io.github.pelmenstar1.digiDict.search

data class RecordSearchOptions(val flags: Int) {
    companion object {
        const val FLAG_SEARCH_FOR_EXPRESSION = 1
        const val FLAG_SEARCH_FOR_MEANING = 1 shl 1
    }
}