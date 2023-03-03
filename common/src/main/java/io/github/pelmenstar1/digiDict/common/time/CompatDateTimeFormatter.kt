package io.github.pelmenstar1.digiDict.common.time

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.DisplayContext
import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import android.os.Build
import android.text.format.DateFormat
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import java.text.FieldPosition
import java.util.*

@SuppressLint("SimpleDateFormat")
class CompatDateTimeFormatter(context: Context, format: String) {
    // If API level >= 24, the type is android.icu.text.SimpleDateFormat, otherwise java.text.SimpleDateFormat
    private val dateFormatter: Any

    // If API level >= 24, type is android.icu.util.Calendar, otherwise Date
    private val dateOrCalendar: Any

    private val buffer = StringBuffer(64)

    init {
        val locale = context.getLocaleCompat()

        // Use best localized format.
        val bestFormat = DateFormat.getBestDateTimePattern(locale, format)

        if (Build.VERSION.SDK_INT >= 24) {
            // SimpleDateFormat and Calendar will look up the cache on passing simple Locale.
            // To make it happen only once, cache it.
            val uLocale = ULocale.forLocale(locale)

            dateFormatter = SimpleDateFormat(bestFormat, uLocale).apply {
                setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
            }

            dateOrCalendar = android.icu.util.Calendar.getInstance(uLocale)
        } else {
            dateFormatter = java.text.SimpleDateFormat(bestFormat, locale)
            dateOrCalendar = Date()
        }
    }

    fun format(epochSeconds: Long): String {
        val millis = epochSeconds * 1000L

        val buf = buffer

        // buffer is always re-used. Make it append from the start.
        buf.setLength(0)

        if (Build.VERSION.SDK_INT >= 24) {
            val calendar = dateOrCalendar as android.icu.util.Calendar
            calendar.timeInMillis = millis

            (dateFormatter as SimpleDateFormat).format(calendar, buf, FIELD_POSITION)
        } else {
            val date = dateOrCalendar as Date
            date.time = millis

            (dateFormatter as java.text.SimpleDateFormat).format(date, buf, FIELD_POSITION)
        }

        return buf.toString()
    }

    companion object {
        private val FIELD_POSITION = FieldPosition(0)
    }
}