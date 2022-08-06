package io.github.pelmenstar1.digiDict.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.pelmenstar1.digiDict.utils.NULL_CHAR
import io.github.pelmenstar1.digiDict.utils.appendReducedNonLettersOrDigitsReplacedToSpace
import io.github.pelmenstar1.digiDict.utils.reduceNonLettersOrDigitsReplacedToSpace
import io.github.pelmenstar1.digiDict.utils.splitToLazyRanges
import java.util.*

/**
 * Entity with id, which corresponds to actual record id in 'records' table, and keywords string prepared for record search.
 * It allows search to be faster.
 *
 * 'keywords' is special-formatted string, it contains both expression and meaning from the actual record.
 * There are also some more rules for 'keywords':
 * - It should contain only letters or digits and spaces.
 * - There cannot be more than 1 space in row.
 * - All letters should be in lowercase.
 * - End of expression and start of meaning is joined by null character, not space. It's done to disallow
 *   cross-word search between expression and meaning. Without it, there can be false positives. Say we have a record with keywords 'expr1 expr2 meaning1 meaning2'
 *   Then user searches for 'expr2 m' and then it gives the user this record (startsWith is used in the search).
 *   It's wrong behaviour.
 *   But if there's some rare (or even illegal) character like null character, it won't happen.
 *   'expr2\0meaning1' does not starts with 'expr2 m'
 */
@Entity(tableName = "search_prepared_records")
data class SearchPreparedRecord(
    @PrimaryKey
    val id: Int,
    val keywords: String
) : EntityWithPrimaryKeyId<SearchPreparedRecord> {
    override fun equalsNoId(other: SearchPreparedRecord): Boolean {
        return keywords == other.keywords
    }

    companion object {
        fun prepare(id: Int, expression: CharSequence, rawMeaning: CharSequence, locale: Locale): SearchPreparedRecord {
            val keywords = prepareToKeywords(expression, rawMeaning, needToLower = true, locale = locale)

            return SearchPreparedRecord(id, keywords)
        }

        fun prepareToKeywords(
            expression: CharSequence,
            rawMeaning: CharSequence,
            needToLower: Boolean,
            locale: Locale? = null
        ): String {
            var expr = expression
            var meaning = rawMeaning

            if (needToLower) {
                val l = locale ?: Locale.ROOT

                expr = expression.toString().lowercase(l)
                meaning = rawMeaning.toString().lowercase(l)
            }

            val reducedExpr = expr.reduceNonLettersOrDigitsReplacedToSpace()
            val reducedMeaning = meaning.reduceRawMeaning()

            val result = buildString(reducedExpr.length + reducedMeaning.length + 1) {
                reducedExpr.splitBySpaceAndAppend(this)
                append(NULL_CHAR)
                reducedMeaning.splitBySpaceAndAppend(this)
            }

            return result
        }

        private fun String.splitBySpaceAndAppend(sb: StringBuilder) {
            val length = length

            splitToLazyRanges(delimiter = ' ') { start, end ->
                sb.append(this, start, end)

                if (end != length) {
                    sb.append(' ')
                }
            }
        }

        private fun CharSequence.reduceRawMeaning(): String {
            val value = this
            val valueLength = length

            return buildString {
                ComplexMeaning.typeSwitch(
                    value,
                    start = 0,
                    end = valueLength,
                    onCommon = {
                        appendReducedNonLettersOrDigitsReplacedToSpace(value, 1, valueLength)
                    },
                    onList = {
                        var isFirst = true

                        ComplexMeaning.iterateListElementRanges(value, 0, valueLength) { rangeStart, rangeEnd ->
                            if (isFirst) {
                                isFirst = false
                            } else {
                                append(' ')
                            }

                            appendReducedNonLettersOrDigitsReplacedToSpace(value, rangeStart, rangeEnd)
                        }
                    }
                )
            }
        }
    }
}