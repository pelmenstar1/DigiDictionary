package io.github.pelmenstar1.digiDict.ui.home

import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.SearchPreparedRecord
import io.github.pelmenstar1.digiDict.utils.*
import org.jetbrains.annotations.TestOnly
import java.util.*

object RecordSearchUtil {
    /**
     * Filters given [records] array with corresponding [searchPreparedRecords] helping array using given [query].
     * [locale] is used to convert [query] to lowercase.
     *
     * It's very important for [records] and [searchPreparedRecords] to have exact order
     * (```records[i].id == searchPreparedRecords[i]``` should hold)
     */
    fun filter(
        records: Array<out Record>,
        searchPreparedRecords: Array<out SearchPreparedRecord>,
        query: String,
        locale: Locale
    ): FilteredArray<Record> {
        val preparedQuery = query.lowercase(locale).reduceNonLettersOrDigitsReplacedToSpace()

        val bitSet = searchPreparedRecords.filterToBitSet {
            filterPredicate(it, preparedQuery)
        }

        // That's why records and searchPreparedRecords are expected to have exact order.
        // 'searchPreparedRecords' has more useful information for search but id doesn't have
        // another important information that 'records' has.
        //
        // Firstly, we filter searchPreparedRecords to get the bitset. And then we pass the bitset
        // FilteredArray but for records to be filtered. Again, for this to work records and searchPreparedRecords
        // should have exact order.
        return FilteredArray(records, bitSet)
    }

    @TestOnly
    fun filterPredicate(preparedRecord: SearchPreparedRecord, query: String): Boolean {
        val keywords = preparedRecord.keywords
        val nullCharIndex = keywords.indexOf(NULL_CHAR)
        if (nullCharIndex < 0) {
            throw IllegalStateException("Invalid format")
        }

        val length = keywords.length
        var prevIndex = 0

        while (true) {
            if (keywords.startsWith(query, prevIndex)) {
                return true
            }

            var nextIndex = keywords.indexOf(' ', prevIndex, length)
            if (nextIndex == -1) {
                nextIndex = length
            }

            if (nullCharIndex in prevIndex..nextIndex) {
                // See SearchPreparedRecord to understand why null character is needed in 'keywords'.
                //
                // If null character is in range [prevIndex; nextIndex], then the range represents sequence is like that:
                // 'exprN\0meaning1' (\0 is null character)
                // If we are still here, then exprN is already checked if it starts with 'query'.
                // It means that we only need to check the first keyword in meaning segment
                // starts with query.
                //
                // If it's not done, the first keywords in meaning segment won't be checked and it can lead to wrong results.
                if (keywords.startsWith(query, nullCharIndex + 1)) {
                    return true
                }
            }

            prevIndex = nextIndex + 1
            if (prevIndex > length) {
                break
            }
        }

        return false
    }
}