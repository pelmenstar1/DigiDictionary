package io.github.pelmenstar1.digiDict.time

import java.util.*

internal fun PackedDate(year: Int, month: Int, dayOfMonth: Int): PackedDate {
    require(year in 0..PackedDate.MAX_YEAR) { "year=$year" }
    require(month in 1..12) { "month=$month" }

    val daysInMonth = getDaysInMonth(year, month)

    require(dayOfMonth in 1..daysInMonth) { "dayOfMonth=$dayOfMonth (daysInMonth=$daysInMonth)" }

    return PackedDate(year shl 16 or (month shl 8) or dayOfMonth)
}
// Some code was taken from OpenJDK
// https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/time/LocalDate.java
@JvmInline
internal value class PackedDate(val bits: Int) {
    /* Packed values:
       - 32-16 bits - year
       - 16-8 bits - month
       - 8-0 bits - day
     */

    val year: Int
        get() = (bits shr 16) and 0xffff

    val month: Int
        get() = (bits shr 8) and 0xff

    val dayOfMonth: Int
        get() = bits and 0xff

    companion object {
        const val MAX_YEAR = 65535

        private const val MIN_DATE_EPOCH = -719528L
        private const val MAX_DATE_EPOCH = 23217002L

        private const val DAYS_PER_CYCLE = 146097L
        private const val DAYS_0000_TO_1970 = DAYS_PER_CYCLE * 5 - (30 * 365 + 7)

        fun today(timeZone: TimeZone = TimeZone.getDefault()): PackedDate {
            return fromEpochDay(todayEpochDay(timeZone))
        }

        private fun todayEpochDay(timeZone: TimeZone = TimeZone.getDefault()): Long {
            return zonedCurrentEpochMillis(timeZone) / MILLIS_IN_DAY
        }

        fun fromEpochDay(epochDay: Long): PackedDate {
            require(isValidEpochDay(epochDay)) { "epochDay is out of range" }

            val zeroDay = epochDay + DAYS_0000_TO_1970 - 60L

            var yearEst = (400L * zeroDay + 591L) / DAYS_PER_CYCLE
            var doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L)

            if (doyEst < 0) {
                yearEst--
                doyEst = zeroDay - (365L * yearEst + yearEst / 4L - yearEst / 100L + yearEst / 400L)
            }

            val marchDoy0 = doyEst.toInt()
            val marchMonth0 = (marchDoy0 * 5 + 2) / 153
            val month = (marchMonth0 + 2) % 12 + 1
            val dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1
            yearEst += (marchMonth0 / 10).toLong()

            return PackedDate(yearEst.toInt(), month, dom)
        }

        private fun isValidEpochDay(epochDay: Long): Boolean {
            return epochDay in MIN_DATE_EPOCH..MAX_DATE_EPOCH
        }
    }
}
