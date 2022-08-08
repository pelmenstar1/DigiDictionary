package io.github.pelmenstar1.digiDict.ui.home.search

import io.github.pelmenstar1.digiDict.data.Record
import io.github.pelmenstar1.digiDict.data.RecordWithSearchInfo
import io.github.pelmenstar1.digiDict.data.SearchPreparedRecord
import io.github.pelmenstar1.digiDict.utils.*
import org.jetbrains.annotations.TestOnly
import java.util.*

object RecordSearchUtil {
    /**
     * Filters given [recordsWithSearchInfo] array using given [query].
     * [locale] is used to convert [query] to lowercase.
     */
    fun filter(
        recordsWithSearchInfo: Array<out RecordWithSearchInfo>,
        query: String,
        locale: Locale
    ): FilteredArray<Record> {
        val preparedQuery = query.lowercase(locale).reduceNonLettersOrDigitsReplacedToSpace()

        return recordsWithSearchInfo.filterFast {
            var keywords = it.keywords
            if (keywords == null) {
                keywords = SearchPreparedRecord.prepareToKeywords(
                    it.expression,
                    it.rawMeaning,
                    needToLower = true,
                    locale = locale
                )
            }

            filterPredicate(keywords, preparedQuery)
        }
    }

    @TestOnly
    fun filterPredicate(keywords: String, query: String): Boolean {
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

            // Try to find the index of the next space.
            // If it's not found ( == -1 ), it's the last keyword we are checking and to handle everything in correct way,
            // nextIndex should be equal to the length of 'keywords'
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