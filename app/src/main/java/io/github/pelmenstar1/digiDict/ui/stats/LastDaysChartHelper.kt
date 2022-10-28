package io.github.pelmenstar1.digiDict.ui.stats

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.time.MILLIS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.SECONDS_IN_DAY
import io.github.pelmenstar1.digiDict.common.time.TimeUtils
import io.github.pelmenstar1.digiDict.common.writePaddedFourDigit
import io.github.pelmenstar1.digiDict.common.writePaddedTwoDigit
import java.util.*

object LastDaysChartHelper {
    class ChartOptions(@ColorInt val textColor: Int, @ColorInt val dataColor: Int, val dataSetLabel: String)

    private class LastDaysValueFormatter(todayEpochDay: Long, lastDays: Int) : ValueFormatter() {
        private val labels: Array<String>

        init {
            val startEpochSeconds = (todayEpochDay - lastDays + 1) * SECONDS_IN_DAY
            val startCalendar = Calendar.getInstance().apply { timeInMillis = startEpochSeconds * 1000 }
            val endCalendar = Calendar.getInstance().apply { timeInMillis = todayEpochDay * MILLIS_IN_DAY }

            val startYear = startCalendar[Calendar.YEAR]
            val startMonth = startCalendar[Calendar.MONTH] + 1
            val startDay = startCalendar[Calendar.DAY_OF_MONTH]

            val type = if (startYear == endCalendar[Calendar.YEAR]) {
                if (startMonth == endCalendar[Calendar.MONTH] + 1) {
                    TYPE_DAY
                } else {
                    TYPE_DAY_MONTH
                }
            } else {
                TYPE_DAY_MONTH_YEAR
            }

            val bufferLength = when (type) {
                TYPE_DAY -> 2
                TYPE_DAY_MONTH -> 5
                TYPE_DAY_MONTH_YEAR -> 10
                else -> throw RuntimeException("Invalid type value")
            }
            val buffer = CharArray(bufferLength)

            var currentYear = startYear
            var currentMonth = startMonth
            var currentDay = startDay

            var currentDaysInMonth = TimeUtils.getDaysInMonth(currentYear, currentMonth)

            labels = Array(lastDays) {
                val label = buffer.let {
                    it.writePaddedTwoDigit(currentDay, 0)

                    if (type and TYPE_MONTH_BIT != 0) {
                        it[2] = '.'
                        it.writePaddedTwoDigit(currentMonth, 3)
                    }

                    if (type and TYPE_YEAR_BIT != 0) {
                        it[5] = '.'
                        it.writePaddedFourDigit(currentYear, 6)
                    }

                    String(it)
                }

                var updateDaysInMonth = false

                currentDay++

                if (currentDay > currentDaysInMonth) {
                    currentDay = 1
                    currentMonth++
                    updateDaysInMonth = true
                }

                if (currentMonth > 12) {
                    currentMonth = 1
                    currentYear++
                }

                if (updateDaysInMonth) {
                    currentDaysInMonth = TimeUtils.getDaysInMonth(currentYear, currentMonth)
                }

                label
            }
        }

        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()

            return labels[index]
        }

        companion object {
            private const val TYPE_MONTH_BIT = 1 shl 1
            private const val TYPE_YEAR_BIT = 1 shl 2

            private const val TYPE_DAY = 0
            private const val TYPE_DAY_MONTH = TYPE_MONTH_BIT
            private const val TYPE_DAY_MONTH_YEAR = TYPE_MONTH_BIT or TYPE_YEAR_BIT
        }
    }

    private fun createLastDaysChartData(raw: IntArray, start: Int, length: Int, options: ChartOptions): BarData {
        val entries = ArrayList<BarEntry>(length)

        for (i in start until (start + length)) {
            val entry = BarEntry((i - start).toFloat(), raw[i].toFloat())

            entries.add(entry)
        }

        val dataSet = BarDataSet(entries, options.dataSetLabel).apply {
            valueFormatter = IntegerWithoutZeroValueFormatter
            valueTextColor = options.textColor
            color = options.dataColor
        }

        return BarData(dataSet)
    }

    private fun setupAxis(axis: AxisBase, options: ChartOptions) {
        axis.granularity = 1f
        axis.textColor = options.textColor
    }

    fun createChartOptions(context: Context): ChartOptions {
        val res = context.resources
        val theme = context.theme

        val textColor = ResourcesCompat.getColor(res, R.color.stats_chart_text_color, theme)
        val dataColor = ResourcesCompat.getColor(res, R.color.stats_last_days_chart_color, theme)
        val dataSetLabel = res.getString(R.string.stats_chart_lastDaysDataSetLabel)

        return ChartOptions(textColor, dataColor, dataSetLabel)
    }

    fun setupChart(
        chart: BarChart,
        raw: IntArray,
        start: Int,
        lastDays: Int,
        todayEpochDay: Long,
        options: ChartOptions
    ) {
        chart.run {
            xAxis.apply {
                setupAxis(this, options)

                valueFormatter = LastDaysValueFormatter(todayEpochDay, lastDays)
            }

            legend.textColor = options.textColor
            description.isEnabled = false

            axisLeft.apply {
                setupAxis(this, options)

                axisMinimum = 0f
            }

            axisRight.isEnabled = false

            isScaleYEnabled = false

            data = createLastDaysChartData(raw, start, lastDays, options)
        }
    }
}