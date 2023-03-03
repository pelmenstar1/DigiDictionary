package io.github.pelmenstar1.digiDict.common.time

import android.content.Context
import android.icu.text.DisplayContext
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.text.format.DateFormat
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import java.util.*

class CompatDateTimeFormatter(context: Context, format: String) {
    private val dateFormatter: java.text.SimpleDateFormat?
    private val dateFormatter24: SimpleDateFormat?

    private val date = Date()

    init {
        val locale = context.getLocaleCompat()

        val bestFormat = DateFormat.getBestDateTimePattern(locale, format)

        if (Build.VERSION.SDK_INT >= 24) {
            dateFormatter24 = SimpleDateFormat(bestFormat, locale).apply {
                setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
            }

            dateFormatter = null
        } else {
            dateFormatter = java.text.SimpleDateFormat(bestFormat, locale)
            dateFormatter24 = null
        }
    }

    fun format(epochSeconds: Long): String {
        date.time = epochSeconds * 1000

        // TODO: Investigate using Calendar in dateFormatter24
        return if (Build.VERSION.SDK_INT >= 24) {
            dateFormatter24!!.format(date)
        } else {
            dateFormatter!!.format(date)
        }
    }
}