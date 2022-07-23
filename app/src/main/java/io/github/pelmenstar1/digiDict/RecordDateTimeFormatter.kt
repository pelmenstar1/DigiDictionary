package io.github.pelmenstar1.digiDict

import android.content.Context
import android.icu.text.DisplayContext
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.text.format.DateFormat
import io.github.pelmenstar1.digiDict.utils.getLocaleCompat
import java.util.*

class RecordDateTimeFormatter(context: Context) {
    private val dateFormatter: java.text.SimpleDateFormat?
    private val dateFormatter24: SimpleDateFormat?

    private val date = Date()

    init {
        val locale = context.getLocaleCompat()

        val format = DateFormat.getBestDateTimePattern(locale, DATE_TIME_FORMAT)

        if (Build.VERSION.SDK_INT >= 24) {
            dateFormatter24 = SimpleDateFormat(format, locale).apply {
                setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
            }

            dateFormatter = null
        } else {
            dateFormatter = java.text.SimpleDateFormat(format, locale)
            dateFormatter24 = null
        }
    }

    fun format(epochSeconds: Long): String {
        date.time = epochSeconds * 1000

        return if (Build.VERSION.SDK_INT >= 24) {
            dateFormatter24!!.format(date)
        } else {
            dateFormatter!!.format(date)
        }
    }

    companion object {
        private const val DATE_TIME_FORMAT = "dd MMMM yyyy HH:mm"
    }

}