package io.github.pelmenstar1.digiDict.common.time

import android.content.Context
import androidx.annotation.PluralsRes
import io.github.pelmenstar1.digiDict.common.R
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import io.github.pelmenstar1.digiDict.common.getLazyValue
import java.text.NumberFormat

class TimeDifferenceFormatter(context: Context) {
    private val resources = context.resources
    private val numberFormat = NumberFormat.getIntegerInstance(context.getLocaleCompat())

    private var lessThanMinuteHolder: String? = null

    fun formatDifference(diffSeconds: Long): String {
        if (diffSeconds < 60) {
            return getLessThanMinuteString()
        }

        var remDiff = diffSeconds

        val weeks = remDiff / SECONDS_IN_WEEK
        remDiff -= weeks * SECONDS_IN_WEEK

        val days = remDiff / SECONDS_IN_DAY
        remDiff -= days * SECONDS_IN_DAY

        val hours = remDiff / SECONDS_IN_HOUR
        remDiff -= hours * SECONDS_IN_HOUR

        val minutes = remDiff / 60

        return buildString(32) {
            if (weeks > 0) {
                append(getNumericalPluralString(R.plurals.week, weeks))
            }

            if (days > 0) {
                appendSpaceIfNotEmpty()
                append(getNumericalPluralString(R.plurals.day, days))
            }

            if (hours > 0) {
                appendSpaceIfNotEmpty()
                append(getNumericalPluralString(R.plurals.hour, hours))
            }

            if (minutes > 0) {
                appendSpaceIfNotEmpty()
                append(getNumericalPluralString(R.plurals.minute, minutes))
            }
        }
    }

    private fun StringBuilder.appendSpaceIfNotEmpty() {
        if (isNotEmpty()) {
            append(' ')
        }
    }

    private fun getLessThanMinuteString(): String {
        return getLazyValue(
            lessThanMinuteHolder,
            { resources.getString(R.string.less_than_minute) },
            { lessThanMinuteHolder = it }
        )
    }

    private fun getNumericalPluralString(@PluralsRes resId: Int, quantity: Long): String {
        val numStr = numberFormat.format(quantity)

        return resources.getQuantityString(resId, quantity.toInt(), numStr)
    }
}