package io.github.pelmenstar1.digiDict.common.time

import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.DisplayContext
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.ULocale
import android.text.format.DateFormat
import io.github.pelmenstar1.digiDict.common.android.getLocaleCompat
import java.text.FieldPosition

@SuppressLint("SimpleDateFormat")
class CompatDateTimeFormatter(context: Context, format: String) {
    private val dateFormatter: SimpleDateFormat
    private val calendar: Calendar

    private val buffer = StringBuffer(64)

    init {
        val locale = context.getLocaleCompat()

        // Use best localized format.
        val bestFormat = DateFormat.getBestDateTimePattern(locale, format)

        // SimpleDateFormat and Calendar will look up the cache on passing simple Locale.
        // To make it happen only once, cache it.
        val uLocale = ULocale.forLocale(locale)

        dateFormatter = SimpleDateFormat(bestFormat, uLocale).apply {
            setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE)
        }

        calendar = Calendar.getInstance(uLocale)
    }

    fun format(epochSeconds: Long): String {
        val buf = buffer

        // buffer is always re-used. Make it append from the start.
        buf.setLength(0)

        calendar.timeInMillis = epochSeconds * 1000L
        dateFormatter.format(calendar, buf, FIELD_POSITION)

        return buf.toString()
    }

    companion object {
        private val FIELD_POSITION = FieldPosition(0)
    }
}
